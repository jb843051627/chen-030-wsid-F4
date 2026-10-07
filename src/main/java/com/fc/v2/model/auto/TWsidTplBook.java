package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 专用标识证面版式模板册对象 t_wsid_tpl_book
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_wsid_tpl_book")
@ApiModel(value = "TWsidTplBook", description = "专用标识证面版式模板册")
public class TWsidTplBook implements Serializable {
    private static final long serialVersionUID = 1L;
    /** 核验串 */
    @TableField("content")
    @ApiModelProperty(value = "核验串")
    private String content;


    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 证面版式代号（由系统排上，不留手填的格子） */
    @TableField("tpl_no")
    @ApiModelProperty(value = "证面版式代号（由系统排上，不留手填的格子）")
    private String tplNo;

    /** 版式名目（正联名／副本联名／槽位段名） */
    @TableField("tpl_name")
    @ApiModelProperty(value = "版式名目（正联名／副本联名／槽位段名）")
    private String tplName;

    /** 版式取用三式 0只出正联 1只出副本联 2按层回落 */
    @TableField("tpl_style")
    @ApiModelProperty(value = "版式取用三式 0只出正联 1只出副本联 2按层回落")
    private Integer tplStyle;

    /** 一屏并列排出的联数 1..3（量程外的数视同没填） */
    @TableField("style_num")
    @ApiModelProperty(value = "一屏并列排出的联数 1..3（量程外的数视同没填）")
    private Integer styleNum;

    /** 槽位文面 */
    @TableField("tpl_text")
    @ApiModelProperty(value = "槽位文面")
    private String tplText;

    /** 定档层级（县定／市定／省定／系统兜底） */
    @TableField("level_kind")
    @ApiModelProperty(value = "定档层级（县定／市定／省定／系统兜底）")
    private String levelKind;

    /** 继承自哪一版式 */
    @TableField("parent_no")
    @ApiModelProperty(value = "继承自哪一版式")
    private String parentNo;

    /** 挂在哪一个名录物种名下 */
    @TableField("spec_id")
    @ApiModelProperty(value = "挂在哪一个名录物种名下")
    private Integer specId;

    /** 适用名录物种代号（冗余自物种底册） */
    @TableField("spec_no")
    @ApiModelProperty(value = "适用名录物种代号（冗余自物种底册）")
    private String specNo;

    /** 名录物种称谓（冗余一份，以物种底册为准回写） */
    @TableField("spec_name")
    @ApiModelProperty(value = "名录物种称谓（冗余一份，以物种底册为准回写）")
    private String specName;

    /** 改动次序（数值大的先说话） */
    @TableField("ver_no")
    @ApiModelProperty(value = "改动次序（数值大的先说话）")
    private Integer verNo;

    /** 本套应发张数 */
    @TableField("plan_num")
    @ApiModelProperty(value = "本套应发张数")
    private Integer planNum;

    /** 已渲张数 */
    @TableField("done_num")
    @ApiModelProperty(value = "已渲张数")
    private Integer doneNum;

    /** 完成比（轧出来，不留填数的空位） */
    @TableField("cover_num")
    @ApiModelProperty(value = "完成比（轧出来，不留填数的空位）")
    private BigDecimal coverNum;

    /** 启用那一一日 */
    @TableField("eff_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "启用那一一日")
    private Date effStart;

    /** 交班那一一日(不含) */
    @TableField("eff_end")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "交班那一一日(不含)")
    private Date effEnd;

    /** 行的情形 0在位 1已撤下 */
    @TableField("status")
    @ApiModelProperty(value = "行的情形 0在位 1已撤下")
    private Integer status;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTplNo() {
        return tplNo;
    }

    public void setTplNo(String tplNo) {
        this.tplNo = tplNo;
    }

    public String getTplName() {
        return tplName;
    }

    public void setTplName(String tplName) {
        this.tplName = tplName;
    }

    public Integer getTplStyle() {
        return tplStyle;
    }

    public void setTplStyle(Integer tplStyle) {
        this.tplStyle = tplStyle;
    }

    public Integer getStyleNum() {
        return styleNum;
    }

    public void setStyleNum(Integer styleNum) {
        this.styleNum = styleNum;
    }

    public String getTplText() {
        return tplText;
    }

    public void setTplText(String tplText) {
        this.tplText = tplText;
    }

    public String getLevelKind() {
        return levelKind;
    }

    public void setLevelKind(String levelKind) {
        this.levelKind = levelKind;
    }

    public String getParentNo() {
        return parentNo;
    }

    public void setParentNo(String parentNo) {
        this.parentNo = parentNo;
    }

    public Integer getSpecId() {
        return specId;
    }

    public void setSpecId(Integer specId) {
        this.specId = specId;
    }

    public String getSpecNo() {
        return specNo;
    }

    public void setSpecNo(String specNo) {
        this.specNo = specNo;
    }

    public String getSpecName() {
        return specName;
    }

    public void setSpecName(String specName) {
        this.specName = specName;
    }

    public Integer getVerNo() {
        return verNo;
    }

    public void setVerNo(Integer verNo) {
        this.verNo = verNo;
    }

    public Integer getPlanNum() {
        return planNum;
    }

    public void setPlanNum(Integer planNum) {
        this.planNum = planNum;
    }

    public Integer getDoneNum() {
        return doneNum;
    }

    public void setDoneNum(Integer doneNum) {
        this.doneNum = doneNum;
    }

    public BigDecimal getCoverNum() {
        return coverNum;
    }

    public void setCoverNum(BigDecimal coverNum) {
        this.coverNum = coverNum;
    }

    public Date getEffStart() {
        return effStart;
    }

    public void setEffStart(Date effStart) {
        this.effStart = effStart;
    }

    public Date getEffEnd() {
        return effEnd;
    }

    public void setEffEnd(Date effEnd) {
        this.effEnd = effEnd;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }


    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
