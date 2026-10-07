package com.fc.v2.model.custom.wsid;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

/**
 * 进门一行的话头：一行对一个繁育主体。
 *
 * <p>行次由系统在本串里排，这里没有敲行次的地方；两个数（申领几张、对上几张）
 * 少任何一个这一行不办，系统不代填、不按零认。</p>
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "WsidApplyLineCmd", description = "进门一行（行次系统排，不设手填格子）")
public class WsidApplyLineCmd {

    @ApiModelProperty(value = "被对上的繁育主体册代号")
    private String itemCode;

    @ApiModelProperty(value = "本行申领张数（缺着不办，不按零认）")
    private BigDecimal qty;

    @ApiModelProperty(value = "与册面核对得上的张数（缺着不办，不按零认）")
    private BigDecimal chkNum;

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
}
