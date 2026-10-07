package com.fc.v2.model.custom.wsid;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 一行提醒在本轮扫描后的去向（沉进库底的同一笔，回面也取自这一趟）。
 *
 * @author fuce
 * @date 2026-10-07
 */
@ApiModel(value = "WsidRemindLineOutcome", description = "一行提醒本轮去向")
public class WsidRemindLineOutcome {

    @ApiModelProperty(value = "条目代号")
    private String itemNo;

    @ApiModelProperty(value = "一处主体代号")
    private String holderNo;

    @ApiModelProperty(value = "层别名目")
    private String levelName;

    @ApiModelProperty(value = "一桩事由名目（年度核查／换证）")
    private String affairKind;

    @ApiModelProperty(value = "该发那一一日")
    private Date dueAt;

    /** 去向 wait/sent/fail/lack/dup/deferred，取值含义见 WsidScanResult 上的说明 */
    @ApiModelProperty(value = "本轮去向 wait搁着 sent发出 fail发不成 lack缺栏 dup同日已发过 deferred静默搁着")
    private String outcome;

    @ApiModelProperty(value = "走通／退回的第几式 0站内 1短信息 2上门（没走通道时空着）")
    private Integer chanUsed;

    @ApiModelProperty(value = "各式回执连缀（只追加，前一式回执不擦）")
    private String receipts;

    @ApiModelProperty(value = "够上条件的那一瞬（静默后起话仍记此瞬）")
    private Date eligibleAt;

    @ApiModelProperty(value = "实际起话那一刻")
    private Date firedAt;

    @ApiModelProperty(value = "缺着的栏（原样报：主体代号／层别名目／该发的那一天），其余情形空")
    private List<String> missingFields = new ArrayList<String>();

    public String getItemNo() {
        return itemNo;
    }

    public void setItemNo(String itemNo) {
        this.itemNo = itemNo;
    }

    public String getHolderNo() {
        return holderNo;
    }

    public void setHolderNo(String holderNo) {
        this.holderNo = holderNo;
    }

    public String getLevelName() {
        return levelName;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

    public String getAffairKind() {
        return affairKind;
    }

    public void setAffairKind(String affairKind) {
        this.affairKind = affairKind;
    }

    public Date getDueAt() {
        return dueAt;
    }

    public void setDueAt(Date dueAt) {
        this.dueAt = dueAt;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public Integer getChanUsed() {
        return chanUsed;
    }

    public void setChanUsed(Integer chanUsed) {
        this.chanUsed = chanUsed;
    }

    public String getReceipts() {
        return receipts;
    }

    public void setReceipts(String receipts) {
        this.receipts = receipts;
    }

    public Date getEligibleAt() {
        return eligibleAt;
    }

    public void setEligibleAt(Date eligibleAt) {
        this.eligibleAt = eligibleAt;
    }

    public Date getFiredAt() {
        return firedAt;
    }

    public void setFiredAt(Date firedAt) {
        this.firedAt = firedAt;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields;
    }
}
