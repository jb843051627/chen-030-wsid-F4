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
 * 年度核查与换证提醒条目对象 t_wsid_remind_item
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_wsid_remind_item")
@ApiModel(value = "TWsidRemindItem", description = "年度核查与换证提醒条目")
public class TWsidRemindItem implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 年度核查与换证提醒条目代号 */
    @TableField("item_no")
    @ApiModelProperty(value = "年度核查与换证提醒条目代号")
    private String itemNo;

    /** 一处主体代号（繁育主体册代号） */
    @TableField("holder_no")
    @ApiModelProperty(value = "一处主体代号（繁育主体册代号）")
    private String holderNo;

    /** 层别名目（挂在哪一层／哪一名目下） */
    @TableField("level_name")
    @ApiModelProperty(value = "层别名目（挂在哪一层／哪一名目下）")
    private String levelName;

    /** 一桩事由名目（年度核查／换证） */
    @TableField("affair_kind")
    @ApiModelProperty(value = "一桩事由名目（年度核查／换证）")
    private String affairKind;

    /** 该发那一一日（应交之日／届满之日，落库为 datetime） */
    @TableField("due_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "该发那一一日（应交之日／届满之日，落库为 datetime）")
    private Date dueAt;

    /** 至多许提前几日（空着/取负/大过定数上限，本行本轮不动） */
    @TableField("amount")
    @ApiModelProperty(value = "至多许提前几日（空着/取负/大过定数上限，本行本轮不动）")
    private BigDecimal amount;

    /** 通道取用式 0只走站内 1先站内后短信息 2三式依次回落 */
    @TableField("channel_kind")
    @ApiModelProperty(value = "通道取用式 0只走站内 1先站内后短信息 2三式依次回落")
    private Integer channelKind;

    /** 短信息那一栏的可用号子（空着则短信息一式不通） */
    @TableField("mobile_no")
    @ApiModelProperty(value = "短信息那一栏的可用号子（空着则短信息一式不通）")
    private String mobileNo;

    /** 事由与所对主体的记要 */
    @TableField("content")
    @ApiModelProperty(value = "事由与所对主体的记要")
    private String content;

    /** 条目情形 0搁着 1已发出 2发不成 3文面缺栏 */
    @TableField("status")
    @ApiModelProperty(value = "条目情形 0搁着 1已发出 2发不成 3文面缺栏")
    private Integer status;

    /** 够上条件的那一瞬（静默后起话仍记此瞬，供年底对流水） */
    @TableField("eligible_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "够上条件的那一瞬（静默后起话仍记此瞬）")
    private Date eligibleAt;

    /** 实际起话那一刻 */
    @TableField("fired_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "实际起话那一刻")
    private Date firedAt;

    /** 走通的是第几式 0站内 1短信息 2上门（发不成时空着） */
    @TableField("chan_used")
    @ApiModelProperty(value = "走通的是第几式 0站内 1短信息 2上门（发不成时空着）")
    private Integer chanUsed;

    /** 各式回执连缀（只追加，前一式的回执不擦） */
    @TableField("receipts")
    @ApiModelProperty(value = "各式回执连缀（只追加，前一式的回执不擦）")
    private String receipts;

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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getChannelKind() {
        return channelKind;
    }

    public void setChannelKind(Integer channelKind) {
        this.channelKind = channelKind;
    }

    public String getMobileNo() {
        return mobileNo;
    }

    public void setMobileNo(String mobileNo) {
        this.mobileNo = mobileNo;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
