package com.fc.v2.model.custom.wsid;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * 中途停过一回后续办的话头：只补没定下来的那几行，行次照旧。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "WsidResumeCmd", description = "一整串续办（只补未定行）")
public class WsidResumeCmd {

    @ApiModelProperty(value = "申领名册核收批次码", required = true)
    private String batchNo;

    @ApiModelProperty(value = "来路：站所自录／县局代收", required = true)
    private String putKind;

    @ApiModelProperty(value = "补报行（按原行次）")
    private List<WsidApplyLinePatch> rows;

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

    public List<WsidApplyLinePatch> getRows() {
        return rows;
    }

    public void setRows(List<WsidApplyLinePatch> rows) {
        this.rows = rows;
    }
}
