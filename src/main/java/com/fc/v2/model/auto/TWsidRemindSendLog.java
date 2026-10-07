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
import java.util.Date;

/**
 * 提醒起话沉底（去重键：一处主体＋一桩事由＋一日）对象 t_wsid_remind_send_log
 *
 * <p>每起成一桩话沉一笔；同日第二趟扫到只对照不重发，隔日重新累计。
 * 同刻两趟并发各排各的队，唯一键兜底，后到那一趟只对照，不被挤掉。</p>
 *
 * @author fuce
 * @date 2026-10-07
 */
@TableName("t_wsid_remind_send_log")
@ApiModel(value = "TWsidRemindSendLog", description = "提醒起话沉底")
public class TWsidRemindSendLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所对条目 */
    @TableField("item_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所对条目")
    private Long itemId;

    /** 一处主体代号 */
    @TableField("holder_no")
    @ApiModelProperty(value = "一处主体代号")
    private String holderNo;

    /** 一桩事由名目（年度核查／换证） */
    @TableField("affair_kind")
    @ApiModelProperty(value = "一桩事由名目（年度核查／换证）")
    private String affairKind;

    /** 起话那一日（yyyy-MM-dd，去重键里的「一日」） */
    @TableField("send_day")
    @ApiModelProperty(value = "起话那一日（去重键里的一日）")
    private String sendDay;

    /** 走通的是第几式 0站内 1短信息 2上门 */
    @TableField("chan_used")
    @ApiModelProperty(value = "走通的是第几式 0站内 1短信息 2上门")
    private Integer chanUsed;

    /** 够上条件的那一瞬（年底拿这一瞬去对流水） */
    @TableField("eligible_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "够上条件的那一瞬")
    private Date eligibleAt;

    /** 实际起话那一刻 */
    @TableField("fired_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "实际起话那一刻")
    private Date firedAt;

    /** 本趟排队先后（同刻两趟分开排队，各留各的先后） */
    @TableField("run_seq")
    @ApiModelProperty(value = "本趟排队先后")
    private String runSeq;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getHolderNo() {
        return holderNo;
    }

    public void setHolderNo(String holderNo) {
        this.holderNo = holderNo;
    }

    public String getAffairKind() {
        return affairKind;
    }

    public void setAffairKind(String affairKind) {
        this.affairKind = affairKind;
    }

    public String getSendDay() {
        return sendDay;
    }

    public void setSendDay(String sendDay) {
        this.sendDay = sendDay;
    }

    public Integer getChanUsed() {
        return chanUsed;
    }

    public void setChanUsed(Integer chanUsed) {
        this.chanUsed = chanUsed;
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

    public String getRunSeq() {
        return runSeq;
    }

    public void setRunSeq(String runSeq) {
        this.runSeq = runSeq;
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
}
