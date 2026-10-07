package com.fc.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.mapper.auto.TWsidApplyReceiptMapper;
import com.fc.v2.mapper.auto.TWsidApplyRowMapper;
import com.fc.v2.model.auto.TWsidApplyReceipt;
import com.fc.v2.model.auto.TWsidApplyRow;
import com.fc.v2.model.custom.wsid.WsidApplyConsts;
import com.fc.v2.model.custom.wsid.WsidApplyLineCmd;
import com.fc.v2.model.custom.wsid.WsidApplyLinePatch;
import com.fc.v2.model.custom.wsid.WsidAuditResult;
import com.fc.v2.model.custom.wsid.WsidIntakeCmd;
import com.fc.v2.model.custom.wsid.WsidIntakeResult;
import com.fc.v2.model.custom.wsid.WsidLineOutcome;
import com.fc.v2.model.custom.wsid.WsidResumeCmd;
import com.fc.v2.model.custom.wsid.WsidRollbackResult;
import com.fc.v2.service.IWsidApplyIntakeService;
import com.fc.v2.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 标识申领名册核收 Service 实现。
 *
 * <p>六段规矩的落点：</p>
 * <ol>
 *   <li>进门只认整串：{@link #intake} 一个事务里连着办四步——点行数、瞅两个数、
 *       按科里校验规则逐栏解析、逐行落去处并写出本串收讫单面；中途不等人手。</li>
 *   <li>上限只在册头一处：{@link #normalizeRowLimit}，空着/负数/没边的大数统一按 150；
 *       点行数与上限头一步刻进单面，撞上限收手，余下另起一串，空串只落「空册」。</li>
 *   <li>两个数同时落到行：缺一不办（未勾，不代填不按零）；两数摆不平退回，
 *       退回缘由（差 N 张）写明在行上。去处只 0/1/2 三样。</li>
 *   <li>行次系统在本串排定（row_no 唯一键）；一家主体两行各自落处；退回再报认原行次；
 *       续办只补未定行。</li>
 *   <li>同一串走两回：两条来路各留一面；第二遍只对照不改账，不回未勾、不另立第二处行账；
 *       回撤只摆未勾行，行面单面同一事务一起动。</li>
 *   <li>单面三笔取自逐行定去处那一趟（ok+back 恒等于 in）；{@link #audit} 重扫行表
 *       倒着核，对不上按行点名。存量行一条不减。</li>
 * </ol>
 *
 * @author fuce
 * @date 2026-10-06
 */
@Service
public class WsidApplyIntakeServiceImpl implements IWsidApplyIntakeService {

    @Resource
    private TWsidApplyReceiptMapper receiptMapper;

    @Resource
    private TWsidApplyRowMapper rowMapper;

    // ---------------------------------------------------------------- 进门

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WsidIntakeResult intake(WsidIntakeCmd cmd) {
        if (cmd == null) {
            throw new IllegalArgumentException("没接着整串话头");
        }
        String batchNo = requireBatchNo(cmd.getBatchNo());
        String putKind = normalizePutKind(cmd.getPutKind());
        List<WsidApplyLineCmd> in = cmd.getRows() == null
                ? new ArrayList<WsidApplyLineCmd>() : cmd.getRows();
        int fileNum = in.size();
        int rowLimit = normalizeRowLimit(cmd.getRowLimit());

        TWsidApplyReceipt existing = selectReceipt(batchNo, putKind);
        if (existing != null) {
            // 同一面再到（同一份册子补报）或另一来路第二遍到：只对照不改账，
            // 头一遍定下的行不回未勾，也不许再摆一遍另立一处。
            return compareOnly(existing, in);
        }

        TWsidApplyReceipt otherFace = selectAnyReceipt(batchNo);
        if (otherFace != null) {
            // 行账只立一处，归头一遍那一面；本面只落对照单面。
            return secondFace(batchNo, putKind, fileNum, rowLimit, in, otherFace);
        }

        // 头一遍：连着办四步
        TWsidApplyReceipt receipt = newReceipt(batchNo, putKind, fileNum, rowLimit);

        if (fileNum == 0) {
            // 连一行都没够着的空册是正经输入：只落「空册」，收讫那一句不出，也不拉别串行填数
            receipt.setEmptyFlag(1);
            receipt.setInNum(0);
            receipt.setOkNum(0);
            receipt.setBackNum(0);
            receipt.setPendingNum(0);
            receipt.setContent("空册");
            receiptMapper.insert(receipt);
            return toResult(receipt, new ArrayList<TWsidApplyRow>(), 0, false, new ArrayList<String>());
        }

        boolean hitLimit = fileNum > rowLimit;
        int take = Math.min(fileNum, rowLimit);
        int overflow = fileNum - take;

        List<TWsidApplyRow> stored = new ArrayList<TWsidApplyRow>();
        int ok = 0;
        int back = 0;
        int pending = 0;
        for (int i = 0; i < take; i++) {
            WsidApplyLineCmd line = in.get(i);
            int rowNo = i + 1; // 行次系统在本串里排，没有敲键的地方
            TWsidApplyRow row = new TWsidApplyRow();
            row.setBatchNo(batchNo);
            row.setPutKind(putKind);
            row.setRowNo(rowNo);
            row.setItemCode(line == null ? null : trimToNull(line.getItemCode()));
            row.setQty(line == null ? null : line.getQty());
            row.setChkNum(line == null ? null : line.getChkNum());
            row.setDelFlag(0);
            settle(row);
            rowMapper.insert(row);
            stored.add(row);
            if (row.getStatus() == WsidApplyConsts.STATUS_OK) {
                ok++;
            } else if (row.getStatus() == WsidApplyConsts.STATUS_BACK) {
                back++;
            } else {
                pending++;
            }
        }

        receipt.setEmptyFlag(0);
        receipt.setHitLimit(hitLimit ? 1 : 0);
        receipt.setInNum(ok + back);
        receipt.setOkNum(ok);
        receipt.setBackNum(back);
        receipt.setPendingNum(pending);
        receipt.setContent(buildFaceText(fileNum, rowLimit, ok, back, pending, overflow));
        receiptMapper.insert(receipt);

        return toResult(receipt, stored, overflow, false, new ArrayList<String>());
    }

    // ---------------------------------------------------------------- 续办

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WsidIntakeResult resume(WsidResumeCmd cmd) {
        if (cmd == null) {
            throw new IllegalArgumentException("没接着续办话头");
        }
        String batchNo = requireBatchNo(cmd.getBatchNo());
        String putKind = normalizePutKind(cmd.getPutKind());
        TWsidApplyReceipt face = firstFace(batchNo);
        List<TWsidApplyRow> rows = listAccountRows(batchNo);
        Map<Integer, TWsidApplyRow> byNo = indexByRowNo(rows);

        List<WsidApplyLinePatch> patches = cmd.getRows() == null
                ? new ArrayList<WsidApplyLinePatch>() : cmd.getRows();

        // 中途停过一回：续办只补没定下来的那几行（未勾、退回再报）；已收下的不受惊动
        for (WsidApplyLinePatch patch : patches) {
            if (patch == null || patch.getRowNo() == null) {
                throw new IllegalArgumentException("补报行没带当初排定的行次");
            }
            int rowNo = patch.getRowNo();
            TWsidApplyRow row = byNo.get(Integer.valueOf(rowNo));
            if (row == null) {
                // 不属本串的行不许塞进来；新行另起一串去接
                throw new IllegalArgumentException("第 " + rowNo + " 行不属本串，续办不许往里添行（余下的另起一串）");
            }
            int st = row.getStatus() == null ? WsidApplyConsts.STATUS_PENDING : row.getStatus();
            if (st == WsidApplyConsts.STATUS_OK) {
                continue; // 先前收下的不回未勾、不受惊动
            }
            // 吃了退回的那一行过几天再报，认的还是当初排定的那一个行次，不重新起步
            row.setQty(patch.getQty());
            row.setChkNum(patch.getChkNum());
            settle(row);
            UpdateWrapper<TWsidApplyRow> uw = new UpdateWrapper<TWsidApplyRow>()
                    .eq("id", row.getId())
                    .eq("del_flag", 0);
            uw.set("qty", row.getQty());
            uw.set("chk_num", row.getChkNum());
            uw.set("status", row.getStatus());
            uw.set("remark", row.getRemark());
            uw.set("update_time", new Date());
            rowMapper.update(null, uw);
        }

        int[] cnt = countByStatus(rows);
        int ok = cnt[WsidApplyConsts.STATUS_OK];
        int back = cnt[WsidApplyConsts.STATUS_BACK];
        int pending = cnt[WsidApplyConsts.STATUS_PENDING];
        face.setInNum(ok + back);
        face.setOkNum(ok);
        face.setBackNum(back);
        face.setPendingNum(pending);
        face.setContent(buildFaceText(face.getFileNum(), face.getRowLimit(), ok, back, pending,
                face.getHitLimit() != null && face.getHitLimit() == 1
                        ? Math.max(0, face.getFileNum() - rows.size()) : 0)
                + "（续办补报 " + patches.size() + " 行，已收下的一段未惊动）");
        UpdateWrapper<TWsidApplyReceipt> fw = new UpdateWrapper<TWsidApplyReceipt>()
                .eq("id", face.getId()).eq("del_flag", 0);
        fw.set("in_num", face.getInNum());
        fw.set("ok_num", face.getOkNum());
        fw.set("back_num", face.getBackNum());
        fw.set("pending_num", face.getPendingNum());
        fw.set("content", clip(face.getContent(), 1000));
        fw.set("update_time", new Date());
        receiptMapper.update(null, fw);

        return toResult(face, rows, 0, false, new ArrayList<String>());
    }

    // ---------------------------------------------------------------- 回撤

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WsidRollbackResult rollback(String batchNo0, String putKind0, List<Integer> rowNos) {
        String batchNo = requireBatchNo(batchNo0);
        String putKind = normalizePutKind(putKind0);
        TWsidApplyReceipt face = firstFace(batchNo);
        if (selectReceipt(batchNo, putKind) == null) {
            throw new IllegalArgumentException("本面（" + putKind + "）还没进门，谈不上回撤");
        }
        if (rowNos == null || rowNos.isEmpty()) {
            throw new IllegalArgumentException("没点要回撤哪几行");
        }

        List<TWsidApplyRow> rows = listAccountRows(batchNo);
        Map<Integer, TWsidApplyRow> byNo = indexByRowNo(rows);

        WsidRollbackResult r = new WsidRollbackResult();
        r.setBatchNo(batchNo);
        r.setPutKind(putKind);
        List<Integer> reset = new ArrayList<Integer>();
        List<Integer> untouched = new ArrayList<Integer>();
        List<Integer> unknown = new ArrayList<Integer>();

        // 只有还没定下来的那几行摆得回未勾；已经落定的那一截一行也不跟着回来
        for (Integer rowNoObj : rowNos) {
            if (rowNoObj == null) {
                continue;
            }
            int rowNo = rowNoObj.intValue();
            TWsidApplyRow row = byNo.get(Integer.valueOf(rowNo));
            if (row == null) {
                unknown.add(Integer.valueOf(rowNo)); // 行不会飘到别串去
                continue;
            }
            int st = row.getStatus() == null ? WsidApplyConsts.STATUS_PENDING : row.getStatus();
            if (st != WsidApplyConsts.STATUS_PENDING) {
                untouched.add(Integer.valueOf(rowNo)); // 收下/退回已落定，不跟回
                continue;
            }
            reset.add(Integer.valueOf(rowNo));
        }

        // 行面与单面两处一起动，只动一处就算残留——同一条 SQL 只认未勾行，与单面同一事务
        if (!reset.isEmpty()) {
            UpdateWrapper<TWsidApplyRow> uw = new UpdateWrapper<TWsidApplyRow>()
                    .eq("batch_no", batchNo)
                    .eq("put_kind", face.getPutKind())
                    .eq("del_flag", 0)
                    .in("row_no", reset)
                    .eq("status", WsidApplyConsts.STATUS_PENDING);
            uw.set("qty", null);
            uw.set("chk_num", null);
            uw.set("remark", null);
            uw.set("status", WsidApplyConsts.STATUS_PENDING);
            uw.set("update_time", new Date());
            int affected = rowMapper.update(null, uw);
            if (affected != reset.size()) {
                // 行面与单面没能一起动，整趟回滚，不许留残
                throw new IllegalStateException("回撤行面只动了 " + affected + "/" + reset.size()
                        + " 行，本趟作废，不许残留");
            }
        }

        // 重扫行表，以这一趟为准回写单面三笔
        List<TWsidApplyRow> fresh = listAccountRows(batchNo);
        int[] cnt = countByStatus(fresh);
        int ok = cnt[WsidApplyConsts.STATUS_OK];
        int back = cnt[WsidApplyConsts.STATUS_BACK];
        int pending = cnt[WsidApplyConsts.STATUS_PENDING];

        int rolled = (face.getRollbackNum() == null ? 0 : face.getRollbackNum().intValue()) + reset.size();
        StringBuilder note = new StringBuilder();
        note.append(buildFaceText(face.getFileNum(), face.getRowLimit(), ok, back, pending,
                face.getHitLimit() != null && face.getHitLimit() == 1
                        ? Math.max(0, face.getFileNum() - fresh.size()) : 0));
        note.append(" 回撤：");
        note.append(reset.isEmpty() ? "无行摆得回" : "第 " + joinRowNos(reset) + " 行摆回未勾");
        if (!untouched.isEmpty()) {
            note.append("；第 ").append(joinRowNos(untouched)).append(" 行已落定不跟回");
        }
        if (!unknown.isEmpty()) {
            note.append("；第 ").append(joinRowNos(unknown)).append(" 行不属本串点不动");
        }
        note.append("。");

        UpdateWrapper<TWsidApplyReceipt> fw = new UpdateWrapper<TWsidApplyReceipt>()
                .eq("id", face.getId()).eq("del_flag", 0);
        fw.set("in_num", ok + back);
        fw.set("ok_num", ok);
        fw.set("back_num", back);
        fw.set("pending_num", pending);
        fw.set("rollback_num", rolled);
        fw.set("content", clip(note.toString(), 1000));
        fw.set("update_time", new Date());
        receiptMapper.update(null, fw);

        r.setResetRowNos(reset);
        r.setUntouchedRowNos(untouched);
        r.setUnknownRowNos(unknown);
        r.setPendingNum(pending);
        r.setInNum(ok + back);
        r.setOkNum(ok);
        r.setBackNum(back);
        r.setContent(note.toString());
        return r;
    }

    // ---------------------------------------------------------------- 倒查

    @Override
    public WsidAuditResult audit(String batchNo0) {
        String batchNo = requireBatchNo(batchNo0);
        TWsidApplyReceipt face = firstFace(batchNo);
        List<TWsidApplyRow> rows = listAccountRows(batchNo);
        int[] cnt = countByStatus(rows);
        int scanOk = cnt[WsidApplyConsts.STATUS_OK];
        int scanBack = cnt[WsidApplyConsts.STATUS_BACK];
        int scanPending = cnt[WsidApplyConsts.STATUS_PENDING];
        int scanIn = scanOk + scanBack;

        WsidAuditResult a = new WsidAuditResult();
        a.setBatchNo(batchNo);
        a.setFirstPutKind(face.getPutKind());
        a.setFileNum(face.getFileNum());
        a.setRowLimit(face.getRowLimit());
        a.setFaceInNum(nz(face.getInNum()));
        a.setFaceOkNum(nz(face.getOkNum()));
        a.setFaceBackNum(nz(face.getBackNum()));
        a.setFacePendingNum(nz(face.getPendingNum()));
        a.setScanInNum(scanIn);
        a.setScanOkNum(scanOk);
        a.setScanBackNum(scanBack);
        a.setScanPendingNum(scanPending);

        List<String> problems = new ArrayList<String>();

        // 点行数：没撞上限时行面行数须等于点行数；撞了上限须恰好落了上限行、且点行数更多
        boolean hit = face.getHitLimit() != null && face.getHitLimit() == 1;
        int fileNum = nz(face.getFileNum());
        if (!hit) {
            if (rows.size() != fileNum) {
                problems.add("行面现存 " + rows.size() + " 行，与当初点行 " + fileNum + " 不一致（存量行一条不减）");
            }
        } else {
            if (rows.size() != face.getRowLimit() || fileNum <= face.getRowLimit()) {
                problems.add("刻了撞上限，但行面 " + rows.size() + " 行／点行 " + fileNum
                        + "／上限 " + face.getRowLimit() + " 圆不上");
            }
        }

        // 单面三笔 vs 重扫行表那一趟
        if (nz(face.getOkNum()) != scanOk || nz(face.getBackNum()) != scanBack
                || nz(face.getInNum()) != scanIn || nz(face.getPendingNum()) != scanPending) {
            problems.add("单面三笔（进门 " + nz(face.getInNum()) + "／收下 " + nz(face.getOkNum())
                    + "／退回 " + nz(face.getBackNum()) + "，未勾 " + nz(face.getPendingNum())
                    + "）与重扫行表（进门 " + scanIn + "／收下 " + scanOk + "／退回 " + scanBack
                    + "，未勾 " + scanPending + "）对不上；以重扫那一趟为准");
        }
        // 后两笔相加须等于头一笔
        if (scanOk + scanBack != scanIn) {
            problems.add("行表收下+退回=" + (scanOk + scanBack) + "，不等于进门 " + scanIn);
        }

        // 逐行点名：第四样去处存不住、退回没写缘由、收下行两数摆不平，都按行点出来
        List<Integer> mismatch = new ArrayList<Integer>();
        for (TWsidApplyRow row : rows) {
            boolean bad = false;
            int st = row.getStatus() == null ? -1 : row.getStatus();
            if (st != WsidApplyConsts.STATUS_PENDING
                    && st != WsidApplyConsts.STATUS_OK
                    && st != WsidApplyConsts.STATUS_BACK) {
                problems.add("第 " + row.getRowNo() + " 行去处是第四样（" + row.getStatus() + "），存不住");
                bad = true;
            } else if (st == WsidApplyConsts.STATUS_BACK && StringUtils.isEmpty(row.getRemark())) {
                problems.add("第 " + row.getRowNo() + " 行判了退回，却没写明为哪一条退");
                bad = true;
            } else if (st == WsidApplyConsts.STATUS_OK) {
                if (row.getQty() == null || row.getChkNum() == null
                        || row.getQty().compareTo(row.getChkNum()) != 0) {
                    problems.add("第 " + row.getRowNo() + " 行落了收下，但两个数缺着或摆不平");
                    bad = true;
                }
            }
            // 未勾行允许留着来件时报上的半拉数（原样记账，系统不代填），不算对不齐；
            // 只有经回撤摆回的未勾才该两数清干净，那一笔由回撤 SQL 的影响行数兜底。
            if (bad) {
                mismatch.add(row.getRowNo());
            }
        }
        a.setMismatchRowNos(mismatch);

        // 另一来路的第二面：两份单面都要留，点行数须对得上（只对照不改账的凭据）
        String otherPut = WsidApplyConsts.PUT_STATION.equals(face.getPutKind())
                ? WsidApplyConsts.PUT_COUNTY : WsidApplyConsts.PUT_STATION;
        TWsidApplyReceipt second = selectReceipt(batchNo, otherPut);
        if (second != null) {
            a.setSecondFacePresent(true);
            boolean consistent = nz(second.getFileNum()) == fileNum
                    && nz(second.getRowLimit()) == nz(face.getRowLimit());
            a.setSecondFaceConsistent(consistent);
            if (!consistent) {
                problems.add("第二面（" + otherPut + "）点行 " + nz(second.getFileNum())
                        + "／上限 " + nz(second.getRowLimit()) + " 与头一面对不上");
            }
        }

        a.setBalanced(problems.isEmpty());
        a.setProblems(problems);
        return a;
    }

    // ---------------------------------------------------------------- 查询

    @Override
    public TWsidApplyReceipt getReceipt(String batchNo, String putKind) {
        return selectReceipt(requireBatchNo(batchNo), normalizePutKind(putKind));
    }

    @Override
    public List<TWsidApplyRow> listRows(String batchNo, String putKind) {
        // 行账只立一处、归本串，不按面分；putKind 给着就核一下来路写法，不给也查得到
        requireBatchNo(batchNo);
        if (StringUtils.isNotEmpty(putKind)) {
            normalizePutKind(putKind);
        }
        return listAccountRows(batchNo);
    }

    // ------------------------------------------------------------ 第二遍到

    /**
     * 同一面重复到、或头一面已在时的另一来路到件：只对照不改账。
     * 行账不另立第二处，回面以头一遍账面为准，不符逐行写进 compareNotes。
     */
    private WsidIntakeResult compareOnly(TWsidApplyReceipt thisFace, List<WsidApplyLineCmd> in) {
        TWsidApplyReceipt first = firstFace(thisFace.getBatchNo());
        List<TWsidApplyRow> rows = listAccountRows(thisFace.getBatchNo());
        List<String> notes = new ArrayList<String>();
        // compareOnly 只会在「同一串同一来路又到一遍」时进来（另一来路走 secondFace）
        notes.add("本面已收讫，同一份册子再到只对照，头一遍勾好的记号不翻回去重走");
        compareRows(first, rows, in, notes);
        // 回面三笔、去处一律以头一遍账面为准，本面不另立一处
        return toResult(first, rows, 0, true, notes);
    }

    /**
     * 另一来路的第二遍到（本面还没留过）：落本面收讫单面存档，行账不另立。
     */
    private WsidIntakeResult secondFace(String batchNo, String putKind, int fileNum, int rowLimit,
                                        List<WsidApplyLineCmd> in, TWsidApplyReceipt first) {
        List<TWsidApplyRow> rows = listAccountRows(batchNo);
        List<String> notes = new ArrayList<String>();
        notes.add("第二遍到（" + putKind + "）：只对照不改账，头一遍定下的行不回未勾、不另立一处");
        compareRows(first, rows, in, notes);

        TWsidApplyReceipt face = newReceipt(batchNo, putKind, fileNum, rowLimit);
        face.setEmptyFlag(first.getEmptyFlag());
        face.setFaceKind(1);
        face.setHitLimit(0);
        face.setInNum(first.getInNum());
        face.setOkNum(first.getOkNum());
        face.setBackNum(first.getBackNum());
        face.setPendingNum(first.getPendingNum());
        face.setContent(clip("第二遍到（" + putKind + "），只对照不改账。"
                + (notes.size() <= 1 ? "行行相符。" : joinNotes(notes)), 1000));
        receiptMapper.insert(face);

        return toResult(first, rows, 0, true, notes);
    }

    /**
     * 把这一遍递来的行，按行次与头一遍账面逐行对：点行数、主体代号、两个数。
     */
    private void compareRows(TWsidApplyReceipt first, List<TWsidApplyRow> rows,
                             List<WsidApplyLineCmd> in, List<String> notes) {
        int fileNum = nz(first.getFileNum());
        if (in.size() != fileNum) {
            notes.add("这一遍点行 " + in.size() + "，头一遍点行 " + fileNum + "，不是同一整串的行数");
        }
        int n = Math.min(in.size(), rows.size());
        for (int i = 0; i < n; i++) {
            TWsidApplyRow row = rows.get(i);
            WsidApplyLineCmd line = in.get(i);
            int rowNo = i + 1;
            String code = line == null ? null : trimToNull(line.getItemCode());
            BigDecimal qty = line == null ? null : line.getQty();
            BigDecimal chk = line == null ? null : line.getChkNum();
            if (!eqStr(code, row.getItemCode())
                    || cmpNum(qty, row.getQty()) != 0 || cmpNum(chk, row.getChkNum()) != 0) {
                notes.add("第 " + rowNo + " 行对不上：头一遍「" + row.getItemCode() + "／"
                        + num(row.getQty()) + "／" + num(row.getChkNum()) + "」，这一遍「"
                        + code + "／" + num(qty) + "／" + num(chk) + "」");
            }
        }
        for (int i = n; i < in.size(); i++) {
            notes.add("第 " + (i + 1) + " 行头一遍账面没有，不许在本串另立一处");
        }
        if (notes.size() == 1) {
            notes.add("行行相符");
        }
    }

    // ------------------------------------------------------------ 逐行落去处

    /**
     * 四步里的后两步落一行：逐栏解析 → 落去处。
     * 去处只三样：两个数缺（或不成正数）落未勾；齐而不平落退回并写明差几张；齐而平落收下。
     */
    private void settle(TWsidApplyRow row) {
        String itemCode = trimToNull(row.getItemCode());
        row.setItemCode(itemCode);
        BigDecimal qty = row.getQty();
        BigDecimal chk = row.getChkNum();

        List<String> missing = new ArrayList<String>();
        if (itemCode == null) {
            missing.add("繁育主体册代号");
        }
        if (qty == null) {
            missing.add("申领张数");
        } else if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            missing.add("申领张数不成正数（零与负数不认）");
        }
        if (chk == null) {
            missing.add("对上张数");
        } else if (chk.compareTo(BigDecimal.ZERO) <= 0) {
            missing.add("对上张数不成正数（零与负数不认）");
        }
        if (!missing.isEmpty()) {
            // 少任何一个这一行不办；系统不代填，也不按零认
            row.setStatus(WsidApplyConsts.STATUS_PENDING);
            row.setRemark(joinMissing(missing) + "缺着，本行不办");
            return;
        }

        int c = qty.compareTo(chk);
        if (c == 0) {
            row.setStatus(WsidApplyConsts.STATUS_OK);
            row.setRemark(null);
        } else if (c > 0) {
            // 例：第 7 行报 20 张、只对上 18 张，退就退在「差 2 张」那一句上
            row.setStatus(WsidApplyConsts.STATUS_BACK);
            row.setRemark("差 " + num(qty.subtract(chk)) + " 张");
        } else {
            row.setStatus(WsidApplyConsts.STATUS_BACK);
            row.setRemark("多 " + num(chk.subtract(qty)) + " 张（报得少、对得上的多）");
        }
    }

    // ------------------------------------------------------------ 单面文面/装配

    private String buildFaceText(int fileNum, int rowLimit, int ok, int back, int pending, int overflow) {
        StringBuilder sb = new StringBuilder();
        sb.append("点行 ").append(fileNum).append("、册头上限 ").append(rowLimit).append("。");
        sb.append("收讫：进门 ").append(ok + back)
                .append("、收下 ").append(ok)
                .append("、退回 ").append(back)
                .append("（未勾 ").append(pending).append("）。");
        if (overflow > 0) {
            sb.append("点行数撞上限已收手，余 ").append(overflow).append(" 行另起一串去接，本串不自行添。");
        }
        return sb.toString();
    }

    private WsidIntakeResult toResult(TWsidApplyReceipt face, List<TWsidApplyRow> rows,
                                      int overflow, boolean secondPass, List<String> notes) {
        WsidIntakeResult r = new WsidIntakeResult();
        r.setBatchNo(face.getBatchNo());
        r.setPutKind(face.getPutKind());
        r.setFaceKind(face.getFaceKind() == null ? 0 : face.getFaceKind());
        r.setFileNum(face.getFileNum());
        r.setRowLimit(face.getRowLimit());
        r.setOverflowNum(overflow);
        r.setInNum(face.getInNum());
        r.setOkNum(face.getOkNum());
        r.setBackNum(face.getBackNum());
        r.setPendingNum(face.getPendingNum());
        r.setEmptyFlag(face.getEmptyFlag());
        r.setHitLimit(face.getHitLimit());
        r.setContent(face.getContent());
        r.setSecondPass(secondPass);
        r.setCompareNotes(notes);
        List<WsidLineOutcome> outcomes = new ArrayList<WsidLineOutcome>();
        for (TWsidApplyRow row : rows) {
            WsidLineOutcome o = new WsidLineOutcome();
            o.setRowNo(row.getRowNo());
            o.setItemCode(row.getItemCode());
            o.setQty(row.getQty());
            o.setChkNum(row.getChkNum());
            o.setStatus(row.getStatus());
            o.setReason(row.getRemark());
            outcomes.add(o);
        }
        r.setRows(outcomes);
        return r;
    }

    // ------------------------------------------------------------ 小工具

    private TWsidApplyReceipt newReceipt(String batchNo, String putKind, int fileNum, int rowLimit) {
        TWsidApplyReceipt r = new TWsidApplyReceipt();
        r.setBatchNo(batchNo);
        r.setPutKind(putKind);
        r.setFileNum(fileNum);
        r.setRowLimit(rowLimit);
        r.setEmptyFlag(0);
        r.setFaceKind(0);
        r.setHitLimit(0);
        r.setRollbackNum(0);
        r.setDelFlag(0);
        return r;
    }

    private TWsidApplyReceipt selectReceipt(String batchNo, String putKind) {
        return receiptMapper.selectOne(new QueryWrapper<TWsidApplyReceipt>()
                .eq("batch_no", batchNo)
                .eq("put_kind", putKind)
                .eq("del_flag", 0));
    }

    private TWsidApplyReceipt selectAnyReceipt(String batchNo) {
        List<TWsidApplyReceipt> all = receiptMapper.selectList(new QueryWrapper<TWsidApplyReceipt>()
                .eq("batch_no", batchNo)
                .eq("del_flag", 0)
                .orderByAsc("face_kind")
                .orderByAsc("id")
                .last("limit 1"));
        return all.isEmpty() ? null : all.get(0);
    }

    /** 头一遍那一面（face_kind=0 为先；同面重报时它也就是本面），行账归它。 */
    private TWsidApplyReceipt firstFace(String batchNo) {
        TWsidApplyReceipt face = selectAnyReceipt(batchNo);
        if (face == null) {
            throw new IllegalArgumentException("批次 " + batchNo + " 还没进门，谈不上这桩事");
        }
        return face;
    }

    private List<TWsidApplyRow> listAccountRows(String batchNo) {
        List<TWsidApplyRow> rows = rowMapper.selectList(new QueryWrapper<TWsidApplyRow>()
                .eq("batch_no", batchNo)
                .eq("del_flag", 0)
                .orderByAsc("row_no"));
        return rows == null ? new ArrayList<TWsidApplyRow>() : rows;
    }

    private Map<Integer, TWsidApplyRow> indexByRowNo(List<TWsidApplyRow> rows) {
        Map<Integer, TWsidApplyRow> map = new LinkedHashMap<Integer, TWsidApplyRow>();
        for (TWsidApplyRow row : rows) {
            map.put(row.getRowNo(), row);
        }
        return map;
    }

    /** 下标 0/1/2 分别为未勾/收下/退回的计数；第四样不计入三笔。 */
    private int[] countByStatus(List<TWsidApplyRow> rows) {
        int[] cnt = new int[3];
        for (TWsidApplyRow row : rows) {
            int st = row.getStatus() == null ? WsidApplyConsts.STATUS_PENDING : row.getStatus();
            if (st == WsidApplyConsts.STATUS_OK) {
                cnt[WsidApplyConsts.STATUS_OK]++;
            } else if (st == WsidApplyConsts.STATUS_BACK) {
                cnt[WsidApplyConsts.STATUS_BACK]++;
            } else if (st == WsidApplyConsts.STATUS_PENDING) {
                cnt[WsidApplyConsts.STATUS_PENDING]++;
            }
        }
        return cnt;
    }

    /**
     * 册头那一处写定的上限：空着按 150；写成负数、写成没边的大数，与空着等价。
     * 上限只此一处，别处不设第二道。
     */
    private int normalizeRowLimit(Integer raw) {
        if (raw == null || raw.intValue() <= 0 || raw.intValue() > WsidApplyConsts.ROW_LIMIT_SANITY) {
            return WsidApplyConsts.DEFAULT_ROW_LIMIT;
        }
        return raw.intValue();
    }

    private String normalizePutKind(String raw) {
        String put = trimToNull(raw);
        if (!WsidApplyConsts.PUT_STATION.equals(put) && !WsidApplyConsts.PUT_COUNTY.equals(put)) {
            throw new IllegalArgumentException("来路只认「" + WsidApplyConsts.PUT_STATION
                    + "」「" + WsidApplyConsts.PUT_COUNTY + "」两条，接到的是：" + raw);
        }
        return put;
    }

    private String requireBatchNo(String raw) {
        String b = trimToNull(raw);
        if (b == null) {
            throw new IllegalArgumentException("批次码空着，没法认这一整串");
        }
        return b;
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private int nz(Integer v) {
        return v == null ? 0 : v.intValue();
    }

    private int cmpNum(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null || b == null) {
            return -1;
        }
        return a.compareTo(b);
    }

    private boolean eqStr(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private String num(BigDecimal v) {
        return v == null ? "缺" : v.stripTrailingZeros().toPlainString();
    }

    private String joinMissing(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append("、");
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    private String joinRowNos(List<Integer> nos) {
        List<Integer> sorted = new ArrayList<Integer>(nos);
        Collections.sort(sorted, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return a.intValue() - b.intValue();
            }
        });
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) {
                sb.append("、");
            }
            sb.append(sorted.get(i));
        }
        return sb.toString();
    }

    private String joinNotes(List<String> notes) {
        StringBuilder sb = new StringBuilder();
        for (String n : notes) {
            sb.append(n).append('；');
        }
        return sb.toString();
    }

    private String clip(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
