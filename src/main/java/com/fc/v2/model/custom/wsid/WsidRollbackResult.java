package com.fc.v2.model.custom.wsid;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * 回撤回面：只有还没定下来的那几行摆得回未勾，已经落定的那一截一行也不跟着回来。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "WsidRollbackResult", description = "一串回撤回面")
public class WsidRollbackResult {

    @ApiModelProperty(value = "批次码")
    private String batchNo;

    @ApiModelProperty(value = "来路")
    private String putKind;

    @ApiModelProperty(value = "本次摆回未勾的行次")
    private List<Integer> resetRowNos = new ArrayList<Integer>();

    @ApiModelProperty(value = "已落定（收下/退回）不跟着回来的行次")
    private List<Integer> untouchedRowNos = new ArrayList<Integer>();

    @ApiModelProperty(value = "不属本串、点不动的行次（行不许飘到别串去）")
    private List<Integer> unknownRowNos = new ArrayList<Integer>();

    @ApiModelProperty(value = "摆回后的未勾行数")
    private Integer pendingNum;

    @ApiModelProperty(value = "收讫三笔·进门")
    private Integer inNum;

    @ApiModelProperty(value = "收讫三笔·收下")
    private Integer okNum;

    @ApiModelProperty(value = "收讫三笔·退回")
    private Integer backNum;

    @ApiModelProperty(value = "单面文面（行面与单面同一趟一起动）")
    private String content;

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getPutKind() {
        return putKind;
    }

    public void setPutKind(String putKind) {
        this.putKind = putKind;
    }

    public List<Integer> getResetRowNos() {
        return resetRowNos;
    }

    public void setResetRowNos(List<Integer> resetRowNos) {
        this.resetRowNos = resetRowNos;
    }

    public List<Integer> getUntouchedRowNos() {
        return untouchedRowNos;
    }

    public void setUntouchedRowNos(List<Integer> untouchedRowNos) {
        this.untouchedRowNos = untouchedRowNos;
    }

    public List<Integer> getUnknownRowNos() {
        return unknownRowNos;
    }

    public void setUnknownRowNos(List<Integer> unknownRowNos) {
        this.unknownRowNos = unknownRowNos;
    }

    public Integer getPendingNum() {
        return pendingNum;
    }

    public void setPendingNum(Integer pendingNum) {
        this.pendingNum = pendingNum;
    }

    public Integer getInNum() {
        return inNum;
    }

    public void setInNum(Integer inNum) {
        this.inNum = inNum;
    }

    public Integer getOkNum() {
        return okNum;
    }

    public void setOkNum(Integer okNum) {
        this.okNum = okNum;
    }

    public Integer getBackNum() {
        return backNum;
    }

    public void setBackNum(Integer backNum) {
        this.backNum = backNum;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
