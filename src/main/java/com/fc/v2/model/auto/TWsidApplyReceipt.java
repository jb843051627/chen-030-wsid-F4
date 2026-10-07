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
 * 标识申领名册核收批次收讫单面对象 t_wsid_apply_receipt
 *
 * <p>一串一面：同一串站所自录、县局代收各走一遍，各留各的单面。
 * 点行数与册头上限进门头一步刻进单面；末尾记进门／收下／退回三笔，
 * 三笔取自「逐行定去处」那一趟，后两笔相加恒等于头一笔。</p>
 *
 * @author fuce
 * @date 2026-10-06
 */
@TableName("t_wsid_apply_receipt")
@ApiModel(value = "TWsidApplyReceipt", description = "标识申领名册核收批次收讫单面")
public class TWsidApplyReceipt implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 申领名册核收批次码（一串一个） */
    @TableField("batch_no")
    @ApiModelProperty(value = "申领名册核收批次码（一串一个）")
    private String batchNo;

    /** 本面来路（站所自录／县局代收，两条来路各留一面） */
    @TableField("put_kind")
    @ApiModelProperty(value = "本面来路（站所自录／县局代收，两条来路各留一面）")
    private String putKind;

    /** 进门点出的行数（空册为 0） */
    @TableField("file_num")
    @ApiModelProperty(value = "进门点出的行数（空册为 0）")
    private Integer fileNum;

    /** 册头那一处写定的本行数上限（空着/负数/没边的大数按 150 落） */
    @TableField("row_limit")
    @ApiModelProperty(value = "册头那一处写定的本行数上限")
    private Integer rowLimit;

    /** 收讫三笔·进门行（取自逐行定去处那一趟） */
    @TableField("in_num")
    @ApiModelProperty(value = "收讫三笔·进门行")
    private Integer inNum;

    /** 收讫三笔·收下行 */
    @TableField("ok_num")
    @ApiModelProperty(value = "收讫三笔·收下行")
    private Integer okNum;

    /** 收讫三笔·退回行 */
    @TableField("back_num")
    @ApiModelProperty(value = "收讫三笔·退回行")
    private Integer backNum;

    /** 空册记号 0正经串 1空册 */
    @TableField("empty_flag")
    @ApiModelProperty(value = "空册记号 0正经串 1空册")
    private Integer emptyFlag;

    /** 单面来头 0头一遍 1第二遍只对照不改账 */
    @TableField("face_kind")
    @ApiModelProperty(value = "单面来头 0头一遍 1第二遍只对照不改账")
    private Integer faceKind;

    /** 撞上限记号 0没撞 1点行数撞上上限已收手 */
    @TableField("hit_limit")
    @ApiModelProperty(value = "撞上限记号 0没撞 1点行数撞上上限已收手")
    private Integer hitLimit;

    /** 本面尚未定去处的行数（未勾行计数） */
    @TableField("pending_num")
    @ApiModelProperty(value = "本面尚未定去处的行数（未勾行计数）")
    private Integer pendingNum;

    /** 本面累计回撤摆回未勾的行数 */
    @TableField("rollback_num")
    @ApiModelProperty(value = "本面累计回撤摆回未勾的行数")
    private Integer rollbackNum;

    /** 单面文面（收讫那一句／空册字样／对照与回撤一记） */
    @TableField("content")
    @ApiModelProperty(value = "单面文面")
    private String content;

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

    public Integer getEmptyFlag() {
        return emptyFlag;
    }

    public void setEmptyFlag(Integer emptyFlag) {
        this.emptyFlag = emptyFlag;
    }

    public Integer getFaceKind() {
        return faceKind;
    }

    public void setFaceKind(Integer faceKind) {
        this.faceKind = faceKind;
    }

    public Integer getHitLimit() {
        return hitLimit;
    }

    public void setHitLimit(Integer hitLimit) {
        this.hitLimit = hitLimit;
    }

    public Integer getPendingNum() {
        return pendingNum;
    }

    public void setPendingNum(Integer pendingNum) {
        this.pendingNum = pendingNum;
    }

    public Integer getRollbackNum() {
        return rollbackNum;
    }

    public void setRollbackNum(Integer rollbackNum) {
        this.rollbackNum = rollbackNum;
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
