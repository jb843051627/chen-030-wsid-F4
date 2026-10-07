package com.fc.v2.model.custom.wsid;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * 一整串进门的话头。一次进门就是一整串，串里若干行，一行对一个主体。
 *
 * <p>把一串拆成几段分头递、把某一行塞进别串，这两种接法都不认：
 * 批次码 + 来路定下本串本面，行只收在这一串里。</p>
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "WsidIntakeCmd", description = "一整串名册进门")
public class WsidIntakeCmd {

    @ApiModelProperty(value = "申领名册核收批次码（一串一个）", required = true)
    private String batchNo;

    @ApiModelProperty(value = "来路：站所自录／县局代收", required = true)
    private String putKind;

    @ApiModelProperty(value = "册头那一处写定的行数上限；空着/负数/没边的大数按 150 认，别处不设第二道")
    private Integer rowLimit;

    @ApiModelProperty(value = "本串行（可空：连一行都没够着的空串是正经输入）")
    private List<WsidApplyLineCmd> rows;

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

    public Integer getRowLimit() {
        return rowLimit;
    }

    public void setRowLimit(Integer rowLimit) {
        this.rowLimit = rowLimit;
    }

    public List<WsidApplyLineCmd> getRows() {
        return rows;
    }

    public void setRows(List<WsidApplyLineCmd> rows) {
        this.rows = rows;
    }
}
