package com.fc.v2.service;

import com.fc.v2.model.auto.TWsidRemindItem;
import com.fc.v2.model.custom.wsid.WsidChanReceipt;

/**
 * 提醒三式通道的递送客。
 *
 * <p>站内、短信息、上门告知各走一法；每一式回一张 {@link WsidChanReceipt}，
 * 说不说得通由实现按册上口径定。扫描方法只认回执，不自己猜通道通不通。
 * 日后真接站内信／短信网关，另出一个实现替上，扫描本体不动。</p>
 *
 * @author fuce
 * @date 2026-10-07
 */
public interface WsidRemindSender {

    /** 头一式：站内 */
    WsidChanReceipt viaStation(TWsidRemindItem item);

    /** 第二式：短信息（行上无可用号子即不通） */
    WsidChanReceipt viaSms(TWsidRemindItem item);

    /** 第三式：上门告知 */
    WsidChanReceipt viaVisit(TWsidRemindItem item);
}
