package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.common.conf.WsidRemindProperties;
import com.fc.v2.mapper.auto.TWsidRemindItemMapper;
import com.fc.v2.mapper.auto.TWsidRemindSendLogMapper;
import com.fc.v2.model.auto.TWsidRemindItem;
import com.fc.v2.model.auto.TWsidRemindSendLog;
import com.fc.v2.model.custom.wsid.WsidChanReceipt;
import com.fc.v2.model.custom.wsid.WsidRemindConsts;
import com.fc.v2.model.custom.wsid.WsidRemindLineOutcome;
import com.fc.v2.model.custom.wsid.WsidScanResult;
import com.fc.v2.service.ITWsidRemindItemService;
import com.fc.v2.service.WsidRemindSender;

/**
 * 年度核查与换证提醒条目 Service 实现。
 *
 * <p>六条按次序落，一条不并进另一条，六问只许 {@link #scan(Date)} 开口：</p>
 * <ol>
 *   <li><b>挑行</b>：一轮只认开跑那一刻。至多许提前几日（amount）空着、取负、或大过定数
 *       上限 {@link WsidRemindConsts#MAX_LEAD_DAYS}，本行本轮不动，也不借别行的数；
 *       存量条目与新立条目同走这一个挑法。</li>
 *   <li><b>回落</b>：按行上通道取用式 0/1/2 排定式次，站内说不通换短信息、无可用号子再换
 *       上门；一趟一行只准走通一式；退到第几式记第几式；每式回执只追加不擦；三式尽落不了地
 *       记「发不成」。</li>
 *   <li><b>静默</b>：开窗只认配置两栏（silence-from/silence-to，两栏齐才算开窗，程序不自己
 *       掐时辰）；窗内够条件的行不当日起话，等窗走完再起；eligible_at 记当初够上条件那一瞬，
 *       不记窗后那一刻。</li>
 *   <li><b>缺栏</b>：少主体代号／层别名目／该发的那一天（及事由名目），只停本行：不发、不
 *       代填，缺哪几栏原样进答复、沉进行面；其余各行照发，整轮不被一行卡住。</li>
 *   <li><b>同日只一回</b>：去重键钉在「一处主体＋一桩事由＋一日」，沉底表唯一键兜底；
 *       同日第二趟扫到只对照不重发，隔日重新累计。同刻两趟公平锁分开排队、各留各的先后，
 *       后到一趟对照出同一回面，不并趟、不挤掉。</li>
 *   <li><b>交数</b>：捞着／走通／回落三笔与逐行记号同一次算定，只信本轮返回，不另拉一轮
 *       逐行清点对冲；屏上不许先摆数后补依据。一行没捞着只落「本轮空跑」，不牵首行填、
 *       不抄上一轮。</li>
 * </ol>
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TWsidRemindItemServiceImpl implements ITWsidRemindItemService {

    /** 册面落库与「按日照跑」的时辰口径一律东八区，不跟 JDBC serverTimezone 走 */
    private static final ZoneId ZONE = ZoneId.of("GMT+8");

    private static final DateTimeFormatter DAY_F = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter STAMP_F = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter RUNSEQ_F = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @javax.annotation.Resource
    private TWsidRemindItemMapper wsidRemindItemMapper;

    @javax.annotation.Resource
    private TWsidRemindSendLogMapper wsidRemindSendLogMapper;

    @javax.annotation.Resource
    private WsidRemindProperties properties;

    @javax.annotation.Resource
    private WsidRemindSender sender;

    /** 同刻两趟分开排队：公平锁先到先排，后到一趟只对照，谁也不挤谁 */
    private final ReentrantLock runLock = new ReentrantLock(true);
    private final AtomicLong runCounter = new AtomicLong(0);

    // ---------------------------------------------------------------- 老路子（签名不动）

    @Override
    public TWsidRemindItem selectTWsidRemindItemById(Long id) {
        return this.wsidRemindItemMapper.selectById(id);
    }

    @Override
    public List<TWsidRemindItem> listDue(Date at) {
        Date moment = at == null ? new Date() : at;
        List<TWsidRemindItem> due = new ArrayList<TWsidRemindItem>();
        for (TWsidRemindItem r : listActive()) {
            if (isWithinWindow(r, moment)) {
                due.add(r);
            }
        }
        return due;
    }

    @Override
    public int runOnce(Date at) {
        // 老路子签名不动：交数仍以这一趟 scan 返回的为准，不另点一遍
        WsidScanResult r = scan(at);
        return r.isEmpty() ? 0 : r.getSentNum().intValue();
    }

    // ---------------------------------------------------------------- 唯一一道门

    @Override
    public WsidScanResult scan(Date at0) {
        Date at = at0 == null ? new Date() : at0;
        String runSeq = RUNSEQ_F.format(toInstant(at).atZone(ZONE).toLocalDateTime())
                + "-" + runCounter.incrementAndGet();
        runLock.lock();
        try {
            return doScan(at, runSeq);
        } finally {
            runLock.unlock();
        }
    }

    private WsidScanResult doScan(Date at, String runSeq) {
        LocalDate today = toInstant(at).atZone(ZONE).toLocalDate();
        boolean silenceNow = isWithinSilence(at);

        List<TWsidRemindItem> active = listActive();

        // 第一笔「捞着」与逐行记号在这同一趟里算定，别处不许另点一遍
        List<PickedRow> picked = new ArrayList<PickedRow>();
        for (TWsidRemindItem item : active) {
            // 一、挑行：amount 先过门——空着/取负/大过定数上限，本行本轮不动，
            // 也不许借别行的数来顶；这一读法定死，缺栏也压在它之后报。
            Integer leadDays = leadDaysOrNull(item);
            if (leadDays == null) {
                continue;
            }
            boolean window;
            if (item.getDueAt() == null) {
                window = false; // 该发的那一天缺着，日子无从数起，归缺栏那条道
            } else {
                long d = ChronoUnit.DAYS.between(today,
                        toInstant(item.getDueAt()).atZone(ZONE).toLocalDate());
                window = d >= 0 && d <= leadDays.intValue();
            }
            // 静默那一截若跨过届期日：先前已够上条件、却还没发成的行，窗走完后照样捞得起来
            boolean eligibleBefore = item.getEligibleAt() != null
                    && !Integer.valueOf(WsidRemindConsts.STATUS_SENT).equals(item.getStatus())
                    && !Integer.valueOf(WsidRemindConsts.STATUS_LACK).equals(item.getStatus());
            // 四、缺栏：due 缺着的行永远数不出日子，每轮照原样报；holder/level 缺着的行
            // 只在窗子罩得住（或先前已够上条件）时才是本轮的事，窗外不报。
            List<String> missing = missingFields(item);
            boolean lackDue = missing.contains("该发的那一天");
            if (!missing.isEmpty() && (window || eligibleBefore || lackDue)) {
                picked.add(new PickedRow(item, "lack", missing));
                continue;
            }
            if (window || eligibleBefore) {
                picked.add(new PickedRow(item, "due", missing));
            }
            // 窗外、栏齐：本轮不动（status 搁着/已发出/发不成都不影响挑行——挑行只认日子）
        }

        WsidScanResult result = new WsidScanResult();
        result.setRunSeq(runSeq);
        result.setScanAt(at);
        result.setPickedNum(Integer.valueOf(picked.size()));

        // 六、一行没捞着：只落这一句，不牵首行填，也不抄上一轮那三个数
        if (picked.isEmpty()) {
            result.setSentNum(0);
            result.setFallbackNum(0);
            result.setEmpty(true);
            result.setContent(WsidRemindConsts.EMPTY_TEXT);
            return result;
        }

        int sent = 0;
        int fallback = 0;
        List<WsidRemindLineOutcome> lines = new ArrayList<WsidRemindLineOutcome>();
        // 同一趟里前头已发成的去重键：后扫到同键的另一行只对照，不重发
        java.util.Map<String, Integer> usedChanByKey = new java.util.HashMap<String, Integer>();
        for (PickedRow p : picked) {
            TWsidRemindItem item = p.row;
            WsidRemindLineOutcome line = baseLine(item);

            if ("lack".equals(p.why)) {
                // 四、单行缺字：只停本行，不发不代填；缺哪几栏原样报、沉进行面
                line.setOutcome("lack");
                line.setMissingFields(p.missing);
                persistLack(item, p.missing, at);
                lines.add(line);
                continue;
            }

            // 够上条件的那一瞬先记下（窗内窗后都不再改它）
            Date eligibleAt = item.getEligibleAt() == null ? at : item.getEligibleAt();

            // 五、同日只一回（同趟先到那行刚发成）：同键的另一行只对照，不再递话
            String dedupKey = dedupKey(item);
            Integer usedThisRun = usedChanByKey.get(dedupKey);
            if (usedThisRun != null) {
                line.setOutcome("dup");
                line.setChanUsed(usedThisRun);
                line.setEligibleAt(eligibleAt);
                line.setReceipts(item.getReceipts());
                lines.add(line);
                continue;
            }

            // 五、同日只一回：先看沉底，再看本日上一波的留痕——只对照，不重发
            TWsidRemindSendLog logToday = findSendLog(item, today.format(DAY_F));
            List<Attempt> todayAttempts = attemptsOfDay(item.getReceipts(), today);
            if (logToday != null || (!todayAttempts.isEmpty()
                    && Integer.valueOf(WsidRemindConsts.STATUS_SENT).equals(item.getStatus()))) {
                int used = logToday != null && logToday.getChanUsed() != null
                        ? logToday.getChanUsed().intValue()
                        : item.getChanUsed() == null ? 0 : item.getChanUsed().intValue();
                mirrorSent(line, item, used, eligibleAt,
                        logToday != null ? logToday.getFiredAt() : item.getFiredAt());
                if (used >= WsidRemindConsts.CHAN_SMS) {
                    fallback++;
                }
                sent++;
                lines.add(line);
                ensureEligibleAt(item, eligibleAt, at);
                continue;
            }
            if (!todayAttempts.isEmpty()
                    && Integer.valueOf(WsidRemindConsts.STATUS_FAIL).equals(item.getStatus())) {
                // 同日早一趟已判发不成：只对照旧痕，不把三式再跑一遍（免得凑巧重发）
                mirrorFail(line, item, todayAttempts, eligibleAt);
                if (todayAttempts.size() > 1) {
                    fallback++;
                }
                lines.add(line);
                ensureEligibleAt(item, eligibleAt, at);
                continue;
            }

            // 三、静默：这一截里够上条件的行不当日起话，等这一截走完下一趟再起
            if (silenceNow) {
                line.setOutcome("deferred");
                line.setEligibleAt(eligibleAt);
                line.setReceipts(item.getReceipts());
                persistDeferred(item, eligibleAt, at);
                lines.add(line);
                continue;
            }

            // 二、回落：三式按行上取用式排定，挨个退，一趟只准走通一式
            int[] kindChans = channelsOfKind(item.getChannelKind());
            List<Attempt> attempts = new ArrayList<Attempt>();
            if (kindChans.length == 0) {
                attempts.add(new Attempt(-1, false,
                        "通道取用式「" + item.getChannelKind() + "」不在三式之内，一式也排不出"));
            }
            Integer usedChan = null;
            for (int chan : kindChans) {
                WsidChanReceipt rc = invokeChannel(chan, item);
                attempts.add(new Attempt(chan, rc.isOk(), rc.getNote()));
                if (rc.isOk()) {
                    usedChan = Integer.valueOf(chan);
                    break; // 一趟只准走通一式，后头的不再走
                }
            }
            String mergedReceipts = appendAttempts(item.getReceipts(), at, attempts);
            line.setReceipts(mergedReceipts);
            line.setEligibleAt(eligibleAt);

            if (usedChan != null) {
                line.setOutcome("sent");
                line.setChanUsed(usedChan);
                line.setFiredAt(at);
                persistSent(item, eligibleAt, at, usedChan.intValue(), mergedReceipts, runSeq);
                usedChanByKey.put(dedupKey, usedChan);
                sent++;
                if (attempts.size() > 1) {
                    fallback++; // 退到第二式及以后才算回落过
                }
            } else {
                // 连最后一式也落不了地：记「发不成」，各式旧回执一张不擦
                line.setOutcome("fail");
                persistFail(item, eligibleAt, at, mergedReceipts);
                if (attempts.size() > 1) {
                    fallback++;
                }
            }
            lines.add(line);
        }

        result.setEmpty(false);
        result.setSentNum(sent);
        result.setFallbackNum(fallback);
        result.setLines(lines);
        result.setContent(buildContent(at, picked.size(), sent, fallback, silenceNow));
        return result;
    }

    // ---------------------------------------------------------------- 挑行口径

    /**
     * 行上「至多许提前几日」过门：空着、取负、大过定数上限，一律不认（返回 null，
     * 本行本轮不动）。零认（只许届满当日）。不借别行的数。
     */
    private Integer leadDaysOrNull(TWsidRemindItem item) {
        BigDecimal amount = item.getAmount();
        if (amount == null || amount.signum() < 0
                || amount.compareTo(BigDecimal.valueOf(WsidRemindConsts.MAX_LEAD_DAYS)) > 0) {
            return null;
        }
        return Integer.valueOf(amount.intValue());
    }

    private boolean isWithinWindow(TWsidRemindItem item, Date at) {
        Integer lead = leadDaysOrNull(item);
        if (lead == null || item.getDueAt() == null) {
            return false;
        }
        LocalDate today = toInstant(at).atZone(ZONE).toLocalDate();
        long d = ChronoUnit.DAYS.between(today,
                toInstant(item.getDueAt()).atZone(ZONE).toLocalDate());
        return d >= 0 && d <= lead.intValue();
    }

    /** 四、缺哪几栏照原样点：事由名目、主体代号、层别名目、该发的那一天。 */
    private List<String> missingFields(TWsidRemindItem item) {
        List<String> missing = new ArrayList<String>();
        String affair = trimToNull(item.getAffairKind());
        if (affair == null
                || (!WsidRemindConsts.AFFAIR_CHECK.equals(affair)
                && !WsidRemindConsts.AFFAIR_RENEW.equals(affair))) {
            missing.add("事由名目");
        }
        if (trimToNull(item.getHolderNo()) == null) {
            missing.add("主体代号");
        }
        if (trimToNull(item.getLevelName()) == null) {
            missing.add("层别名目");
        }
        if (item.getDueAt() == null) {
            missing.add("该发的那一天");
        }
        return missing;
    }

    // ---------------------------------------------------------------- 通道

    private int[] channelsOfKind(Integer kind) {
        if (kind == null) {
            return new int[0];
        }
        switch (kind.intValue()) {
            case WsidRemindConsts.KIND_STATION_ONLY:
                return new int[] {WsidRemindConsts.CHAN_STATION};
            case WsidRemindConsts.KIND_STATION_THEN_SMS:
                return new int[] {WsidRemindConsts.CHAN_STATION, WsidRemindConsts.CHAN_SMS};
            case WsidRemindConsts.KIND_ALL_FALLBACK:
                return new int[] {WsidRemindConsts.CHAN_STATION,
                        WsidRemindConsts.CHAN_SMS, WsidRemindConsts.CHAN_VISIT};
            default:
                return new int[0];
        }
    }

    private WsidChanReceipt invokeChannel(int chan, TWsidRemindItem item) {
        switch (chan) {
            case WsidRemindConsts.CHAN_STATION:
                return sender.viaStation(item);
            case WsidRemindConsts.CHAN_SMS:
                return sender.viaSms(item);
            case WsidRemindConsts.CHAN_VISIT:
                return sender.viaVisit(item);
            default:
                return WsidChanReceipt.fail("排不出这一式");
        }
    }

    // ---------------------------------------------------------------- 静默

    /**
     * 开不开窗只由配置两栏说了算：silence-from、silence-to 两栏齐且解得析才算开窗；
     * 任一栏空着或写得不成时辰，就是不开窗。程序不自己掐时辰。
     */
    boolean isWithinSilence(Date at) {
        String from = trimToNull(properties.getSilenceFrom());
        String to = trimToNull(properties.getSilenceTo());
        if (from == null || to == null) {
            return false;
        }
        LocalTime t = toInstant(at).atZone(ZONE).toLocalTime();
        LocalTime f;
        LocalTime e;
        try {
            f = parseTime(from);
            e = parseTime(to);
        } catch (RuntimeException bad) {
            return false; // 两栏解得析不了，不当开窗，不替配置拍时辰
        }
        if (f.equals(e)) {
            return false;
        }
        if (f.isBefore(e)) {
            return !t.isBefore(f) && t.isBefore(e);
        }
        // 讫比起點小：跨午夜那一截
        return !t.isBefore(f) || t.isBefore(e);
    }

    private LocalTime parseTime(String s) {
        return s.length() <= 5 ? LocalTime.parse(s, DateTimeFormatter.ofPattern("HH:mm"))
                : LocalTime.parse(s, DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    // ---------------------------------------------------------------- 沉库

    private void persistLack(TWsidRemindItem item, List<String> missing, Date at) {
        String note = "【停发】文面缺栏（" + join(missing, "、") + "），本行按下不发，也不代填";
        String merged = appendNote(item.getReceipts(), at, note);
        UpdateWrapper<TWsidRemindItem> uw = new UpdateWrapper<TWsidRemindItem>()
                .eq("id", item.getId());
        uw.set("status", WsidRemindConsts.STATUS_LACK);
        uw.set("receipts", merged);
        uw.set("update_time", new Date());
        wsidRemindItemMapper.update(null, uw);
        item.setStatus(WsidRemindConsts.STATUS_LACK);
        item.setReceipts(merged);
    }

    private void persistDeferred(TWsidRemindItem item, Date eligibleAt, Date at) {
        UpdateWrapper<TWsidRemindItem> uw = new UpdateWrapper<TWsidRemindItem>()
                .eq("id", item.getId());
        uw.set("eligible_at", eligibleAt);
        uw.set("update_time", new Date());
        wsidRemindItemMapper.update(null, uw);
        item.setEligibleAt(eligibleAt);
    }

    private void ensureEligibleAt(TWsidRemindItem item, Date eligibleAt, Date at) {
        if (item.getEligibleAt() == null || !item.getEligibleAt().equals(eligibleAt)) {
            UpdateWrapper<TWsidRemindItem> uw = new UpdateWrapper<TWsidRemindItem>()
                    .eq("id", item.getId());
            uw.set("eligible_at", eligibleAt);
            uw.set("update_time", new Date());
            wsidRemindItemMapper.update(null, uw);
            item.setEligibleAt(eligibleAt);
        }
    }

    private void persistSent(TWsidRemindItem item, Date eligibleAt, Date firedAt,
                             int chanUsed, String receipts, String runSeq) {
        UpdateWrapper<TWsidRemindItem> uw = new UpdateWrapper<TWsidRemindItem>()
                .eq("id", item.getId());
        uw.set("status", WsidRemindConsts.STATUS_SENT);
        uw.set("eligible_at", eligibleAt);
        uw.set("fired_at", firedAt);
        uw.set("chan_used", Integer.valueOf(chanUsed));
        uw.set("receipts", receipts);
        uw.set("update_time", new Date());
        wsidRemindItemMapper.update(null, uw);

        // 五、去重键「一处主体＋一桩事由＋一日」沉一笔；唯一键兜底并发的后到一趟
        TWsidRemindSendLog log = new TWsidRemindSendLog();
        log.setItemId(item.getId());
        log.setHolderNo(item.getHolderNo().trim());
        log.setAffairKind(item.getAffairKind().trim());
        log.setSendDay(toInstant(firedAt).atZone(ZONE).toLocalDate().format(DAY_F));
        log.setChanUsed(Integer.valueOf(chanUsed));
        log.setEligibleAt(eligibleAt);
        log.setFiredAt(firedAt);
        log.setRunSeq(runSeq);
        log.setDelFlag(0);
        try {
            wsidRemindSendLogMapper.insert(log);
        } catch (DuplicateKeyException dup) {
            // 同刻两趟：后到一趟的插入被唯一键挡下，转对照，不并趟也不抛出去
        }
        item.setStatus(WsidRemindConsts.STATUS_SENT);
        item.setEligibleAt(eligibleAt);
        item.setFiredAt(firedAt);
        item.setChanUsed(Integer.valueOf(chanUsed));
        item.setReceipts(receipts);
    }

    private void persistFail(TWsidRemindItem item, Date eligibleAt, Date at, String receipts) {
        UpdateWrapper<TWsidRemindItem> uw = new UpdateWrapper<TWsidRemindItem>()
                .eq("id", item.getId());
        uw.set("status", WsidRemindConsts.STATUS_FAIL);
        uw.set("eligible_at", eligibleAt);
        uw.set("receipts", receipts);
        uw.set("update_time", new Date());
        wsidRemindItemMapper.update(null, uw);
        item.setStatus(WsidRemindConsts.STATUS_FAIL);
        item.setEligibleAt(eligibleAt);
        item.setReceipts(receipts);
    }

    // ---------------------------------------------------------------- 去重沉底

    private TWsidRemindSendLog findSendLog(TWsidRemindItem item, String day) {
        String holder = trimToNull(item.getHolderNo());
        String affair = trimToNull(item.getAffairKind());
        if (holder == null || affair == null) {
            return null;
        }
        List<TWsidRemindSendLog> list = wsidRemindSendLogMapper.selectList(
                new QueryWrapper<TWsidRemindSendLog>()
                        .eq("holder_no", holder)
                        .eq("affair_kind", affair)
                        .eq("send_day", day)
                        .eq("del_flag", 0)
                        .orderByAsc("fired_at")
                        .last("limit 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    /** 去重键：一处主体＋一桩事由（「一日」取开跑当日，在查沉底时另拼）。 */
    private String dedupKey(TWsidRemindItem item) {
        String holder = trimToNull(item.getHolderNo());
        String affair = trimToNull(item.getAffairKind());
        return (holder == null ? "?" : holder) + "|" + (affair == null ? "?" : affair);
    }

    // ---------------------------------------------------------------- 回执留痕

    /**
     * 回执只追加：前一式的旧记号不擦。每式一段「[时刻] 第几式·通道：说法」，
     * 同日对照靠段首日期认。
     */
    private String appendAttempts(String old, Date at, List<Attempt> attempts) {
        StringBuilder sb = new StringBuilder(old == null ? "" : old);
        String dayPrefix = STAMP_F.format(toInstant(at).atZone(ZONE).toLocalDateTime());
        for (Attempt a : attempts) {
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
                sb.append('\n');
            }
            sb.append('[').append(dayPrefix).append("] 第")
                    .append(a.chan < 0 ? '？' : String.valueOf(a.chan + 1))
                    .append("式·").append(chanName(a.chan)).append("：")
                    .append(a.ok ? "走通／" : "说不通／").append(a.note);
        }
        return clip(sb.toString(), 2000);
    }

    private String appendNote(String old, Date at, String note) {
        StringBuilder sb = new StringBuilder(old == null ? "" : old);
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
            sb.append('\n');
        }
        sb.append('[').append(STAMP_F.format(toInstant(at).atZone(ZONE).toLocalDateTime()))
                .append("] ").append(note);
        return clip(sb.toString(), 2000);
    }

    /** 挑出某一日的出式段（段首 [yyyy-MM-dd ...]），供同日第二趟只对照用。 */
    private List<Attempt> attemptsOfDay(String receipts, LocalDate day) {
        List<Attempt> out = new ArrayList<Attempt>();
        if (receipts == null) {
            return out;
        }
        String prefix = "[" + day.format(DAY_F);
        for (String line : receipts.split("\n")) {
            if (!line.startsWith(prefix) || !line.contains("式·")) {
                continue;
            }
            if (line.contains("【停发】")) {
                continue;
            }
            int dot = line.indexOf("式·");
            String ordinal = line.substring(line.indexOf(']') + 2, dot);
            String tail = line.substring(dot + 2);
            String cname = tail.substring(0, tail.indexOf('：'));
            boolean ok = tail.contains("走通／");
            String body = tail.substring(tail.indexOf('：') + 1);
            int chan = chanByName(cname);
            if (ordinal.startsWith("第？")) {
                chan = -1;
            }
            out.add(new Attempt(chan, ok, body));
        }
        return out;
    }

    // ---------------------------------------------------------------- 回面装配

    private WsidRemindLineOutcome baseLine(TWsidRemindItem item) {
        WsidRemindLineOutcome line = new WsidRemindLineOutcome();
        line.setItemNo(item.getItemNo());
        line.setHolderNo(item.getHolderNo());
        line.setLevelName(item.getLevelName());
        line.setAffairKind(item.getAffairKind());
        line.setDueAt(item.getDueAt());
        return line;
    }

    /** 五、对照回面与头一趟同一张脸：走通第几式、两瞬时刻都照沉底的抄。 */
    private void mirrorSent(WsidRemindLineOutcome line, TWsidRemindItem item,
                            int chanUsed, Date eligibleAt, Date firedAt) {
        line.setOutcome("sent");
        line.setChanUsed(Integer.valueOf(chanUsed));
        line.setEligibleAt(eligibleAt);
        line.setFiredAt(firedAt);
        line.setReceipts(item.getReceipts());
    }

    private void mirrorFail(WsidRemindLineOutcome line, TWsidRemindItem item,
                            List<Attempt> todayAttempts, Date eligibleAt) {
        line.setOutcome("fail");
        line.setEligibleAt(eligibleAt);
        line.setReceipts(item.getReceipts());
    }

    private String buildContent(Date at, int picked, int sent, int fallback, boolean silence) {
        StringBuilder sb = new StringBuilder();
        sb.append("本轮扫描（开跑 ")
                .append(STAMP_F.format(toInstant(at).atZone(ZONE).toLocalDateTime())).append("）：")
                .append("捞着 ").append(picked).append(" 行，走通 ").append(sent)
                .append(" 行，回落 ").append(fallback).append(" 行。");
        if (silence) {
            sb.append("此刻在静默时段内，够条件的行搁至窗走完再起，够上条件那一瞬已照常记下。");
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- 小工具

    private List<TWsidRemindItem> listActive() {
        List<TWsidRemindItem> list = wsidRemindItemMapper.selectList(
                new QueryWrapper<TWsidRemindItem>()
                        .eq("del_flag", 0)
                        .orderByAsc("due_at")
                        .orderByAsc("id"));
        return list == null ? new ArrayList<TWsidRemindItem>() : list;
    }

    private String chanName(int chan) {
        switch (chan) {
            case WsidRemindConsts.CHAN_STATION:
                return "站内";
            case WsidRemindConsts.CHAN_SMS:
                return "短信息";
            case WsidRemindConsts.CHAN_VISIT:
                return "上门告知";
            default:
                return "未知式";
        }
    }

    private int chanByName(String name) {
        if ("站内".equals(name)) {
            return WsidRemindConsts.CHAN_STATION;
        }
        if ("短信息".equals(name)) {
            return WsidRemindConsts.CHAN_SMS;
        }
        if ("上门告知".equals(name)) {
            return WsidRemindConsts.CHAN_VISIT;
        }
        return -1;
    }

    private Instant toInstant(Date d) {
        return d.toInstant();
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String join(List<String> parts, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append(sep);
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    private String clip(String s, int max) {
        return s == null ? null : (s.length() <= max ? s : s.substring(0, max));
    }

    /** 本轮捞进来的一行：due 日子罩得住，lack 文面缺栏。 */
    private static final class PickedRow {
        private final TWsidRemindItem row;
        private final String why;
        private final List<String> missing;

        PickedRow(TWsidRemindItem row, String why, List<String> missing) {
            this.row = row;
            this.why = why;
            this.missing = missing;
        }
    }

    /** 一式的尝试结果（行内暂记，随后连成回执留痕）。 */
    private static final class Attempt {
        private final int chan;
        private final boolean ok;
        private final String note;

        Attempt(int chan, boolean ok, String note) {
            this.chan = chan;
            this.ok = ok;
            this.note = note;
        }
    }
}
