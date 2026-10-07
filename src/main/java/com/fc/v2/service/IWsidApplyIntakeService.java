package com.fc.v2.service;

import com.fc.v2.model.auto.TWsidApplyReceipt;
import com.fc.v2.model.auto.TWsidApplyRow;
import com.fc.v2.model.custom.wsid.WsidAuditResult;
import com.fc.v2.model.custom.wsid.WsidIntakeCmd;
import com.fc.v2.model.custom.wsid.WsidIntakeResult;
import com.fc.v2.model.custom.wsid.WsidResumeCmd;
import com.fc.v2.model.custom.wsid.WsidRollbackResult;

import java.util.List;

/**
 * 标识申领名册核收 Service：一整串册进门的去处、第二遍到的对付、回撤谁跟着撤。
 *
 * <p>本接口是「按一串册子核收」这一条路子的定死出口，入参与返回不再改；
 * 往后若按物种、按年度分串，另立一条并列的路子去担，不动这里。</p>
 *
 * <ul>
 *   <li>{@link #intake}：一整串一次进门，连着办四步（点行数／瞅两个数／逐栏解析／逐行落去处+写本串收讫单面）。</li>
 *   <li>{@link #resume}：中途停过后续办，只补没定下来的那几行（未勾、退回再报），行次认当初排定的。</li>
 *   <li>{@link #rollback}：回撤，只有还没定下来的行摆得回未勾，已落定的一行不跟回；行面单面一起动。</li>
 *   <li>{@link #audit}：随机抽行倒着核，行上去处跟单面三笔要圆得上，对不齐按行点名。</li>
 * </ul>
 *
 * @author fuce
 * @date 2026-10-06
 */
public interface IWsidApplyIntakeService {

    /**
     * 一整串进门。同一串同一来路再到（或另一来路的第二遍到）只对照不改账，
     * 不回未勾、不另立第二处行账；两条来路各留各的收讫单面。
     */
    WsidIntakeResult intake(WsidIntakeCmd cmd);

    /**
     * 续办：只补未勾／退回再报的行，按原行次；已收下的行不受惊动，不收新行（新行另起一串）。
     */
    WsidIntakeResult resume(WsidResumeCmd cmd);

    /**
     * 回撤指定行次：未勾行摆回未勾（清掉半落的两个数），收下/退回行不跟着回来，
     * 不属本串的行点不动。行面与单面同一事务一起动。
     */
    WsidRollbackResult rollback(String batchNo, String putKind, List<Integer> rowNos);

    /** 取本串本面收讫单面 */
    TWsidApplyReceipt getReceipt(String batchNo, String putKind);

    /** 取本串本面逐行（按行次升序，存量行一条不减） */
    List<TWsidApplyRow> listRows(String batchNo, String putKind);

    /** 倒着核：单面三笔与重扫行表那一趟逐项对账，不符按行点名 */
    WsidAuditResult audit(String batchNo);
}
