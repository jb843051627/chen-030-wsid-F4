package com.fc.v2.model.custom.wsid;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * 一串进门（或第二遍对照、续办）之后的回面：本串收讫单面的要紧处 + 逐行去处。
 *
 * <p>三笔（进门／收下／退回）取自「逐行定去处」那一趟，后两笔相加恒等于头一笔；
 * 页面抢先亮出的数字不算依据，一切以这里为准。</p>
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "WsidIntakeResult", description = "一整串核收回面")
public class WsidIntakeResult {

    @ApiModelProperty(value = "批次码")
    private String batchNo;

    @ApiModelProperty(value = "本面来路")
    private String putKind;

    @ApiModelProperty(value = "单面来头 0头一遍 1第二遍只对照不改账")
    private Integer faceKind;

    @ApiModelProperty(value = "进门点出的行数")
    private Integer fileNum;

    @ApiModelProperty(value = "册头写定的上限")
    private Integer rowLimit;

    @ApiModelProperty(value = "撞上限被挡在本串之外、须另起一串的行数")
    private Integer overflowNum;

    @ApiModelProperty(value = "收讫三笔·进门（已逐行定下的行；收下+退回恒等于此数）")
    private Integer inNum;

    @ApiModelProperty(value = "收讫三笔·收下")
    private Integer okNum;

    @ApiModelProperty(value = "收讫三笔·退回")
    private Integer backNum;

    @ApiModelProperty(value = "还没定下来（未勾）的行数")
    private Integer pendingNum;

    @ApiModelProperty(value = "空册记号")
    private Integer emptyFlag;

    @ApiModelProperty(value = "撞上限记号")
    private Integer hitLimit;

    @ApiModelProperty(value = "单面文面")
    private String content;

    @ApiModelProperty(value = "是否第二遍到（只对照不改账）")
    private boolean secondPass;

    @ApiModelProperty(value = "第二遍对照时，与头一遍账面逐行对不上的说法（空着即行行相符）")
    private List<String> compareNotes = new ArrayList<String>();

    @ApiModelProperty(value = "逐行去处")
    private List<WsidLineOutcome> rows = new ArrayList<WsidLineOutcome>();

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

    public Integer getFaceKind() {
        return faceKind;
    }

    public void setFaceKind(Integer faceKind) {
        this.faceKind = faceKind;
    }

    public Integer getFileNum() {
        return fileNum;
    }

    public void setFileNum(Integer fileNum) {
        this.fileNum = fileNum;
    }

    public Integer getRowLimit() {
        return rowLimit;
    }

    public void setRowLimit(Integer rowLimit) {
        this.rowLimit = rowLimit;
    }

    public Integer getOverflowNum() {
        return overflowNum;
    }

    public void setOverflowNum(Integer overflowNum) {
        this.overflowNum = overflowNum;
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

    public Integer getPendingNum() {
        return pendingNum;
    }

    public void setPendingNum(Integer pendingNum) {
        this.pendingNum = pendingNum;
    }

    public Integer getEmptyFlag() {
        return emptyFlag;
    }

    public void setEmptyFlag(Integer emptyFlag) {
        this.emptyFlag = emptyFlag;
    }

    public Integer getHitLimit() {
        return hitLimit;
    }

    public void setHitLimit(Integer hitLimit) {
        this.hitLimit = hitLimit;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isSecondPass() {
        return secondPass;
    }

    public void setSecondPass(boolean secondPass) {
        this.secondPass = secondPass;
    }

    public List<String> getCompareNotes() {
        return compareNotes;
    }

    public void setCompareNotes(List<String> compareNotes) {
        this.compareNotes = compareNotes;
    }

    public List<WsidLineOutcome> getRows() {
        return rows;
    }

    public void setRows(List<WsidLineOutcome> rows) {
        this.rows = rows;
    }
}
