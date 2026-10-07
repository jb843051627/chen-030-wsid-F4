package com.fc.v2.model.custom.wsid;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * 随机抽一行倒着核的回面：行上去处跟单面三笔要互相圆得上。
 *
 * <p>三笔以重扫行表那一趟为准，不另信页面先亮出的数；对不齐的行按行点名，
 * 不许用「大致相符」一类说法。</p>
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "WsidAuditResult", description = "一串账面对照倒查回面")
public class WsidAuditResult {

    @ApiModelProperty(value = "批次码")
    private String batchNo;

    @ApiModelProperty(value = "单面与行面是否处处圆得上")
    private boolean balanced;

    @ApiModelProperty(value = "头一遍来路")
    private String firstPutKind;

    @ApiModelProperty(value = "当初点行数")
    private Integer fileNum;

    @ApiModelProperty(value = "册头上限")
    private Integer rowLimit;

    @ApiModelProperty(value = "单面三笔：进门/收下/退回/未勾")
    private Integer faceInNum;
    private Integer faceOkNum;
    private Integer faceBackNum;
    private Integer facePendingNum;

    @ApiModelProperty(value = "重扫行表那一趟：进门/收下/退回/未勾")
    private Integer scanInNum;
    private Integer scanOkNum;
    private Integer scanBackNum;
    private Integer scanPendingNum;

    @ApiModelProperty(value = "第二面（另一条来路）是否也留着")
    private boolean secondFacePresent;

    @ApiModelProperty(value = "第二面三笔与头一遍是否相符")
    private boolean secondFaceConsistent;

    @ApiModelProperty(value = "对不齐的行次（按行点名）")
    private List<Integer> mismatchRowNos = new ArrayList<Integer>();

    @ApiModelProperty(value = "逐条不圆得上的说法")
    private List<String> problems = new ArrayList<String>();

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public boolean isBalanced() {
        return balanced;
    }

    public void setBalanced(boolean balanced) {
        this.balanced = balanced;
    }

    public String getFirstPutKind() {
        return firstPutKind;
    }

    public void setFirstPutKind(String firstPutKind) {
        this.firstPutKind = firstPutKind;
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

    public Integer getFaceInNum() {
        return faceInNum;
    }

    public void setFaceInNum(Integer faceInNum) {
        this.faceInNum = faceInNum;
    }

    public Integer getFaceOkNum() {
        return faceOkNum;
    }

    public void setFaceOkNum(Integer faceOkNum) {
        this.faceOkNum = faceOkNum;
    }

    public Integer getFaceBackNum() {
        return faceBackNum;
    }

    public void setFaceBackNum(Integer faceBackNum) {
        this.faceBackNum = faceBackNum;
    }

    public Integer getFacePendingNum() {
        return facePendingNum;
    }

    public void setFacePendingNum(Integer facePendingNum) {
        this.facePendingNum = facePendingNum;
    }

    public Integer getScanInNum() {
        return scanInNum;
    }

    public void setScanInNum(Integer scanInNum) {
        this.scanInNum = scanInNum;
    }

    public Integer getScanOkNum() {
        return scanOkNum;
    }

    public void setScanOkNum(Integer scanOkNum) {
        this.scanOkNum = scanOkNum;
    }

    public Integer getScanBackNum() {
        return scanBackNum;
    }

    public void setScanBackNum(Integer scanBackNum) {
        this.scanBackNum = scanBackNum;
    }

    public Integer getScanPendingNum() {
        return scanPendingNum;
    }

    public void setScanPendingNum(Integer scanPendingNum) {
        this.scanPendingNum = scanPendingNum;
    }

    public boolean isSecondFacePresent() {
        return secondFacePresent;
    }

    public void setSecondFacePresent(boolean secondFacePresent) {
        this.secondFacePresent = secondFacePresent;
    }

    public boolean isSecondFaceConsistent() {
        return secondFaceConsistent;
    }

    public void setSecondFaceConsistent(boolean secondFaceConsistent) {
        this.secondFaceConsistent = secondFaceConsistent;
    }

    public List<Integer> getMismatchRowNos() {
        return mismatchRowNos;
    }

    public void setMismatchRowNos(List<Integer> mismatchRowNos) {
        this.mismatchRowNos = mismatchRowNos;
    }

    public List<String> getProblems() {
        return problems;
    }

    public void setProblems(List<String> problems) {
        this.problems = problems;
    }
}
