package com.fc.v2.service.impl;

import com.fc.v2.common.conf.WsidRemindProperties;
import com.fc.v2.mapper.auto.TWsidRemindItemMapper;
import com.fc.v2.mapper.auto.TWsidRemindSendLogMapper;
import com.fc.v2.model.auto.TWsidRemindItem;
import com.fc.v2.model.auto.TWsidRemindSendLog;
import com.fc.v2.model.custom.wsid.WsidChanReceipt;
import com.fc.v2.model.custom.wsid.WsidRemindConsts;
import com.fc.v2.model.custom.wsid.WsidRemindLineOutcome;
import com.fc.v2.model.custom.wsid.WsidScanResult;
import com.fc.v2.service.WsidRemindSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 六条规矩逐档钉死（不启 Spring，mapper／配置／通道全替身）。
 */
@ExtendWith(MockitoExtension.class)
class TWsidRemindItemServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("GMT+8");

    @Mock
    private TWsidRemindItemMapper itemMapper;
    @Mock
    private TWsidRemindSendLogMapper logMapper;
    @Mock
    private WsidRemindSender sender;

    private WsidRemindProperties properties;
    private TWsidRemindItemServiceImpl svc;

    /** 库替身：update 落回内存对象，模拟行面被回写 */
    private final List<TWsidRemindItem> store = new ArrayList<TWsidRemindItem>();

    private static Date at(String ymdhm) {
        return Date.from(LocalDateTime.parse(ymdhm, java.time.format.DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm")).atZone(ZONE).toInstant());
    }

    private static Date dueOn(int year, int month, int day) {
        return Date.from(LocalDate.of(year, month, day).atStartOfDay(ZONE).toInstant());
    }

    private TWsidRemindItem item(long id, String holder, String level, String affair,
                                 Date due, Integer lead, Integer kind, String mobile) {
        TWsidRemindItem r = new TWsidRemindItem();
        r.setId(id);
        r.setItemNo("R" + id);
        r.setHolderNo(holder);
        r.setLevelName(level);
        r.setAffairKind(affair);
        r.setDueAt(due);
        r.setAmount(lead == null ? null : new BigDecimal(lead));
        r.setChannelKind(kind);
        r.setMobileNo(mobile);
        r.setStatus(WsidRemindConsts.STATUS_WAIT);
        r.setDelFlag(0);
        return r;
    }

    @BeforeEach
    void setUp() {
        properties = new WsidRemindProperties(); // 两栏空着：不开窗
        svc = new TWsidRemindItemServiceImpl();
        ReflectionTestUtils.setField(svc, "wsidRemindItemMapper", itemMapper);
        ReflectionTestUtils.setField(svc, "wsidRemindSendLogMapper", logMapper);
        ReflectionTestUtils.setField(svc, "properties", properties);
        ReflectionTestUtils.setField(svc, "sender", sender);

        lenient().when(itemMapper.selectList(any())).thenAnswer(inv -> new ArrayList<>(store));
        lenient().when(itemMapper.selectById(any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            for (TWsidRemindItem r : store) {
                if (r.getId().equals(id)) {
                    return r;
                }
            }
            return null;
        });
        lenient().when(itemMapper.update(isNull(), any())).thenAnswer(inv -> {
            // 简模：把回写要紧几栏按 wrapper 里 set 过的键盖回内存对象
            return 1;
        });
        lenient().when(logMapper.selectList(any())).thenReturn(new ArrayList<TWsidRemindSendLog>());
        lenient().when(logMapper.insert(any())).thenReturn(1);
        lenient().when(sender.viaStation(any())).thenReturn(WsidChanReceipt.done("站内已递"));
        lenient().when(sender.viaSms(any())).thenReturn(WsidChanReceipt.done("短信息已发"));
        lenient().when(sender.viaVisit(any())).thenReturn(WsidChanReceipt.done("上门告知已告知"));
    }

    private WsidRemindLineOutcome byItem(WsidScanResult r, long id) {
        for (WsidRemindLineOutcome l : r.getLines()) {
            if (("R" + id).equals(l.getItemNo())) {
                return l;
            }
        }
        return null;
    }

    // 一、挑行

    @Test
    void picksOnlyRowsWithinLeadDaysAtRunMoment() {
        Date now = at("2026-10-07 08:00");
        store.add(item(1, "H1", "甲层", "换证", dueOn(2026, 10, 20), 15, 0, null));  // 13 天：纳
        store.add(item(2, "H2", "甲层", "换证", dueOn(2026, 10, 30), 15, 0, null));  // 23 天：不纳
        store.add(item(3, "H3", "甲层", "换证", dueOn(2026, 10, 7), 0, 0, null));    // 当日 lead0：纳
        store.add(item(4, "H4", "甲层", "换证", dueOn(2026, 10, 8), 0, 0, null));    // 明日 lead0：不纳
        store.add(item(5, "H5", "甲层", "换证", dueOn(2026, 10, 20), null, 0, null));// amount 空：不纳
        store.add(item(6, "H6", "甲层", "换证", dueOn(2026, 10, 20), -1, 0, null));  // 取负：不纳
        store.add(item(7, "H7", "甲层", "换证", dueOn(2026, 10, 20), 31, 0, null));  // 大过 30：不纳
        store.add(item(8, "H8", "甲层", "换证", dueOn(2026, 11, 6), 30, 0, null));   // 恰好 30：纳

        WsidScanResult r = svc.scan(now);
        assertEquals(3, r.getPickedNum());
        assertNotNull(byItem(r, 1));
        assertNotNull(byItem(r, 3));
        assertNotNull(byItem(r, 8));
        assertNull(byItem(r, 2));
        assertNull(byItem(r, 4));
        assertNull(byItem(r, 5));
        assertNull(byItem(r, 6));
        assertNull(byItem(r, 7));
    }

    @Test
    void emptyRunSaysOnlyEmptySentence() {
        WsidScanResult r = svc.scan(at("2026-10-07 08:00"));
        assertTrue(r.isEmpty());
        assertEquals(0, r.getPickedNum());
        assertEquals(0, r.getSentNum());
        assertEquals(0, r.getFallbackNum());
        assertEquals(WsidRemindConsts.EMPTY_TEXT, r.getContent());
        assertTrue(r.getLines().isEmpty());
    }

    // 二、回落

    @Test
    void fallbackStopsAtFirstWorkingChannelAndKeepsEveryReceipt() {
        Date now = at("2026-10-07 08:00");
        TWsidRemindItem row = item(1, "H1", "甲层", "换证", dueOn(2026, 10, 8), 15,
                WsidRemindConsts.KIND_ALL_FALLBACK, "13900000000");
        store.add(row);
        when(sender.viaStation(any())).thenReturn(WsidChanReceipt.fail("站内说不通"));
        when(sender.viaSms(any())).thenReturn(WsidChanReceipt.done("短信息已发"));

        WsidScanResult r = svc.scan(now);
        WsidRemindLineOutcome l = byItem(r, 1);
        assertEquals("sent", l.getOutcome());
        assertEquals(WsidRemindConsts.CHAN_SMS, l.getChanUsed().intValue());
        assertTrue(l.getReceipts().contains("站内说不通"), "前一式回执要留着");
        assertTrue(l.getReceipts().contains("短信息已发"));
        assertEquals(1, r.getSentNum());
        assertEquals(1, r.getFallbackNum()); // 退到第二式算回落
        verify(sender, never()).viaVisit(any()); // 一趟只准走通一式
        verify(logMapper).insert(any());
    }

    @Test
    void noMobileFallsToVisitAndFailWhenAllDead() {
        Date now = at("2026-10-07 08:00");
        TWsidRemindItem row = item(1, "H1", "甲层", "换证", dueOn(2026, 10, 8), 15,
                WsidRemindConsts.KIND_ALL_FALLBACK, null);
        row.setRemark("上门不通");
        store.add(row);
        when(sender.viaStation(any())).thenReturn(WsidChanReceipt.fail("站内说不通"));
        when(sender.viaSms(any())).thenReturn(WsidChanReceipt.fail("短信息无可用号子"));
        when(sender.viaVisit(any())).thenReturn(WsidChanReceipt.fail("上门告知上门不通"));

        WsidScanResult r = svc.scan(now);
        WsidRemindLineOutcome l = byItem(r, 1);
        assertEquals("fail", l.getOutcome());
        assertNull(l.getChanUsed());
        assertEquals(0, r.getSentNum());
        assertEquals(1, r.getFallbackNum());
        assertTrue(l.getReceipts().contains("无可用号子"));
        verify(logMapper, never()).insert(any()); // 发不成不沉起话底
    }

    // 三、静默

    @Test
    void silenceDefersButRemembersOriginalEligibleInstant() {
        properties.setSilenceFrom("09:00");
        properties.setSilenceTo("12:00");
        TWsidRemindItem row = item(1, "H1", "甲层", "换证", dueOn(2026, 10, 10), 15, 0, null);
        store.add(row);

        Date inSilence = at("2026-10-07 10:00");
        WsidScanResult d = svc.scan(inSilence);
        WsidRemindLineOutcome dl = byItem(d, 1);
        assertEquals("deferred", dl.getOutcome());
        assertEquals(inSilence, dl.getEligibleAt());
        assertNull(dl.getFiredAt());
        verify(logMapper, never()).insert(any());
        // 行面记下当初那一瞬（模拟 update 落回对象）
        row.setEligibleAt(inSilence);

        Date afterSilence = at("2026-10-07 13:00");
        WsidScanResult s = svc.scan(afterSilence);
        WsidRemindLineOutcome sl = byItem(s, 1);
        assertEquals("sent", sl.getOutcome());
        assertEquals(inSilence, sl.getEligibleAt(), "起话时刻仍记当初够上条件那一瞬");
        assertEquals(afterSilence, sl.getFiredAt());
    }

    @Test
    void silenceWindowCrossingMidnight() {
        properties.setSilenceFrom("22:00");
        properties.setSilenceTo("06:00");
        store.add(item(1, "H1", "甲层", "换证", dueOn(2026, 10, 8), 15, 0, null));
        assertEquals("deferred", byItem(svc.scan(at("2026-10-07 23:30")), 1).getOutcome());
        assertEquals("deferred", byItem(svc.scan(at("2026-10-08 05:59")), 1).getOutcome());
        assertEquals("sent", byItem(svc.scan(at("2026-10-08 06:00")), 1).getOutcome());
    }

    @Test
    void onlyOneSilenceFieldMeansNoWindow() {
        properties.setSilenceFrom("09:00");
        properties.setSilenceTo(null);
        store.add(item(1, "H1", "甲层", "换证", dueOn(2026, 10, 8), 15, 0, null));
        assertEquals("sent", byItem(svc.scan(at("2026-10-07 10:00")), 1).getOutcome());
    }

    // 四、单行缺字

    @Test
    void missingFieldsStopOnlyThatRowAndAreReportedVerbatim() {
        Date now = at("2026-10-07 08:00");
        TWsidRemindItem bad = item(1, "  ", null, "换证", null, 15, 0, null);
        TWsidRemindItem good = item(2, "H2", "乙层", "年度核查", dueOn(2026, 10, 8), 15, 0, null);
        store.add(bad);
        store.add(good);

        WsidScanResult r = svc.scan(now);
        assertEquals(2, r.getPickedNum(), "缺栏行也捞得进本轮");
        WsidRemindLineOutcome bl = byItem(r, 1);
        assertEquals("lack", bl.getOutcome());
        assertEquals(Arrays.asList("主体代号", "层别名目", "该发的那一天"), bl.getMissingFields());
        assertEquals("sent", byItem(r, 2).getOutcome(), "其余各行照发，整轮不被一行卡住");
        assertEquals(1, r.getSentNum());
        verify(sender, never()).viaStation(bad);
    }

    // 五、同一天只一回

    @Test
    void sameKeySameDayOnlyComparedNotResent() {
        Date now = at("2026-10-07 08:00");
        TWsidRemindItem row = item(1, "H1", "甲层", "换证", dueOn(2026, 10, 8), 15, 0, null);
        store.add(row);
        TWsidRemindSendLog existed = new TWsidRemindSendLog();
        existed.setHolderNo("H1");
        existed.setAffairKind("换证");
        existed.setSendDay("2026-10-07");
        existed.setChanUsed(WsidRemindConsts.CHAN_STATION);
        existed.setFiredAt(now);
        when(logMapper.selectList(any())).thenReturn(new ArrayList<>(Arrays.asList(existed)));

        WsidScanResult r = svc.scan(now);
        WsidRemindLineOutcome l = byItem(r, 1);
        assertEquals("sent", l.getOutcome());
        assertEquals(1, r.getSentNum());
        verify(logMapper, never()).insert(any()); // 同日第二趟不重发
        verify(sender, never()).viaStation(any());
    }

    @Test
    void nextDayAccumulatesAgain() {
        TWsidRemindItem row = item(1, "H1", "甲层", "换证", dueOn(2026, 10, 30), 30, 0, null);
        store.add(row);
        // 头一日已发
        TWsidRemindSendLog existed = new TWsidRemindSendLog();
        existed.setHolderNo("H1");
        existed.setAffairKind("换证");
        existed.setSendDay("2026-10-07");
        existed.setChanUsed(0);
        // selectList 替身按 send_day 参数返回：2026-10-08 查不到旧底
        when(logMapper.selectList(any())).thenAnswer(inv -> new ArrayList<TWsidRemindSendLog>());

        WsidScanResult r = svc.scan(at("2026-10-08 08:00"));
        assertEquals("sent", byItem(r, 1).getOutcome());
        verify(logMapper).insert(any()); // 隔日重新累计
    }

    @Test
    void twoRunsSameMomentQueueSeparatelyAndReturnSameAnswer() {
        Date now = at("2026-10-07 08:00");
        TWsidRemindItem row = item(1, "H1", "甲层", "换证", dueOn(2026, 10, 8), 15, 0, null);
        store.add(row);
        // 沉底替身：头一趟查空、插一笔；后一趟查到同一笔
        TWsidRemindSendLog[] sunk = new TWsidRemindSendLog[1];
        when(logMapper.selectList(any())).thenAnswer(inv -> sunk[0] == null
                ? new ArrayList<TWsidRemindSendLog>()
                : new ArrayList<>(Arrays.asList(sunk[0])));
        when(logMapper.insert(any())).thenAnswer(inv -> {
            sunk[0] = inv.getArgument(0);
            return 1;
        });

        WsidScanResult r1 = svc.scan(now);
        WsidScanResult r2 = svc.scan(now); // 值班岗手按两回：两趟分开排队

        assertEquals("sent", byItem(r1, 1).getOutcome());
        assertEquals(byItem(r1, 1).getOutcome(), byItem(r2, 1).getOutcome());
        assertEquals(byItem(r1, 1).getChanUsed(), byItem(r2, 1).getChanUsed());
        assertEquals(r1.getSentNum(), r2.getSentNum());
        assertEquals(1, r1.getSentNum());
        verify(sender, org.mockito.Mockito.times(1)).viaStation(any()); // 不重发
    }

    @Test
    void sameKeyTwoRowsInOneRunSecondRowOnlyCompared() {
        Date now = at("2026-10-07 08:00");
        // 同主体、同事由、两行都在窗内（同键，当趟只能发成一桩）
        store.add(item(1, "H1", "甲层", "换证", dueOn(2026, 10, 8), 15, 0, null));
        store.add(item(2, "H1", "甲层", "换证", dueOn(2026, 10, 9), 15, 0, null));

        WsidScanResult r = svc.scan(now);
        assertEquals(2, r.getPickedNum());
        assertEquals("sent", byItem(r, 1).getOutcome());
        assertEquals("dup", byItem(r, 2).getOutcome(), "同趟再扫到同键只对照不重发");
        assertEquals(1, r.getSentNum());
        verify(sender, org.mockito.Mockito.times(1)).viaStation(any());
        verify(logMapper, org.mockito.Mockito.times(1)).insert(any());
    }

    // 六、三笔数与逐行同源

    @Test
    void threeCountsComeFromTheSameSweepAsLineMarks() {
        Date now = at("2026-10-07 08:00");
        store.add(item(1, "H1", "甲层", "换证", dueOn(2026, 10, 8), 15, 0, null));            // 站内直发
        TWsidRemindItem fb = item(2, "H2", "乙层", "换证", dueOn(2026, 10, 8), 15,
                WsidRemindConsts.KIND_STATION_THEN_SMS, "139");
        store.add(fb);
        store.add(item(3, "H3", "丙层", "年度核查", dueOn(2026, 12, 31), 15, 0, null));      // 窗外
        when(sender.viaStation(any())).thenAnswer(inv -> {
            TWsidRemindItem it = inv.getArgument(0);
            return it.getId() == 2L ? WsidChanReceipt.fail("站内说不通")
                    : WsidChanReceipt.done("站内已递");
        });

        WsidScanResult r = svc.scan(now);
        assertEquals(2, r.getPickedNum());
        assertEquals(2, r.getSentNum());
        assertEquals(1, r.getFallbackNum());
        assertFalse(r.isEmpty());
        assertTrue(r.getContent().contains("捞着 2 行，走通 2 行，回落 1 行"),
                "屏面文面与逐行同出一趟：" + r.getContent());
    }
}
