package com.fc.v2.model.custom.wsid;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

/**
 * 续办补报一行：只补还没定下来的那几行（未勾／退回再报）。
 *
 * <p>行次认的是当初进门时系统排定的那一个，不重新起步；已经定下的那一段
 * 不受惊动，也不许拿补报把行飘到别串去。</p>
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "WsidApplyLinePatch", description = "续办补报一行（按原行次）")
public class WsidApplyLinePatch {

    @ApiModelProperty(value = "当初排定的原册行次", required = true)
    private Integer rowNo;

    @ApiModelProperty(value = "本行申领张数")
    private BigDecimal qty;

    @ApiModelProperty(value = "与册面核对得上的张数")
    private BigDecimal chkNum;

    public Integer getRowNo() {
        return rowNo;
    }

    public void setRowNo(Integer rowNo) {
        this.rowNo = rowNo;
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
