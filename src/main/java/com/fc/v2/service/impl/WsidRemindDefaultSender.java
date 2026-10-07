package com.fc.v2.service.impl;

import com.fc.v2.model.auto.TWsidRemindItem;
import com.fc.v2.model.custom.wsid.WsidChanReceipt;
import com.fc.v2.model.custom.wsid.WsidRemindConsts;
import com.fc.v2.service.WsidRemindSender;
import com.fc.v2.util.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 三式通道的默认递送：按册上口径判定通与不通，回执留痕。
 *
 * <p>站内：册面条目搁着即递得到（系统内的信总在）。短信息：行上 {@code mobile_no}
 * 那一栏没有可用号子即不通。上门：默认递得到（上门是人去，总落得了地）；若连上门也
 * 落不了地（如主体已注销），在册面备注里写「上门不通」四字，本式才回不通。</p>
 *
 * @author fuce
 * @date 2026-10-07
 */
@Component
public class WsidRemindDefaultSender implements WsidRemindSender {

    private static final String VISIT_BLOCK = "上门不通";

    @Override
    public WsidChanReceipt viaStation(TWsidRemindItem item) {
        return WsidChanReceipt.done(chanName(WsidRemindConsts.CHAN_STATION) + "已递");
    }

    @Override
    public WsidChanReceipt viaSms(TWsidRemindItem item) {
        if (StringUtils.isEmpty(item.getMobileNo())) {
            return WsidChanReceipt.fail(chanName(WsidRemindConsts.CHAN_SMS) + "无可用号子");
        }
        return WsidChanReceipt.done(chanName(WsidRemindConsts.CHAN_SMS) + "已发至" + item.getMobileNo().trim());
    }

    @Override
    public WsidChanReceipt viaVisit(TWsidRemindItem item) {
        if (item.getRemark() != null && item.getRemark().contains(VISIT_BLOCK)) {
            return WsidChanReceipt.fail(chanName(WsidRemindConsts.CHAN_VISIT) + VISIT_BLOCK);
        }
        return WsidChanReceipt.done(chanName(WsidRemindConsts.CHAN_VISIT) + "已告知");
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
                return "第" + chan + "式";
        }
    }
}
