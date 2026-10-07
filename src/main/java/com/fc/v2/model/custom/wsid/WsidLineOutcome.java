package com.fc.v2.model.custom.wsid;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

/**
 * 一行落定后的去处回面。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "WsidLineOutcome", description = "一行去处")
public class WsidLineOutcome {

    @ApiModelProperty(value = "系统在本串排定的行次")
    private Integer rowNo;

    @ApiModelProperty(value = "被对上的繁育主体册代号")
    private String itemCode;

    @ApiModelProperty(value = "本行申领张数")
    private BigDecimal qty;

    @ApiModelProperty(value = "与册面核对得上的张数")
    private BigDecimal chkNum;

    @ApiModelProperty(value = "去处 0未勾 1收下 2退回（只此三样）")
    private Integer status;

    @ApiModelProperty(value = "退回缘由（为哪一条退，逐行答得上）")
    private String reason;

    public Integer getRowNo() {
        return rowNo;
    }

    public void setRowNo(Integer rowNo) {
        this.rowNo = rowNo;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public BigDecimal getChkNum() {
        return chkNum;
    }

    public void setChkNum(BigDecimal chkNum) {
        this.chkNum = chkNum;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
