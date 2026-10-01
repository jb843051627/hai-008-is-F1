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
 * 隐患整改催办条对象 t_is_haz_task
 *
 * 一张催办条五样写死：条号 billNo、整改期限最后一刻 dueAt、到期前还剩几日露面 amount、
 * 当前情形 status、隐患概况 hazardSummary 与负责部门（pin 原定 / accept 接手）。
 * 期限口径全系统一把尺（见 DeadlineRuler）：dueAt 生成即定死，任何回写不得改它；要调只许动 amount。
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_is_haz_task")
@ApiModel(value = "TIsHazTask", description = "隐患整改催办条")
public class TIsHazTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 催办条号 */
    @TableField("bill_no")
    @ApiModelProperty(value = "催办条号")
    private String billNo;

    /** 整改事项号（溯源隐患号） */
    @TableField("item_no")
    @ApiModelProperty(value = "整改事项号")
    private String itemNo;

    /** 整改期限那一天最后一刻（23:59:59，定了不许动） */
    @TableField("due_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "整改期限那一天最后一刻")
    private Date dueAt;

    /** 到期前还剩几日就该露面（唯一允许调度的一档） */
    @TableField("amount")
    @ApiModelProperty(value = "到期前还剩几日就该露面")
    private BigDecimal amount;

    /** 隐患概况 */
    @TableField("hazard_summary")
    @ApiModelProperty(value = "隐患概况")
    private String hazardSummary;

    /** 责任条线（候选部门分组） */
    @TableField("dept_group")
    @ApiModelProperty(value = "责任条线")
    private String deptGroup;

    /** 原定负责部门（条子生成时落定） */
    @TableField("pin_dept_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "原定负责部门")
    private Long pinDeptId;

    /** 原定负责部门名 */
    @TableField("pin_dept_name")
    @ApiModelProperty(value = "原定负责部门名")
    private String pinDeptName;

    /** 实际接手部门 */
    @TableField("accept_dept_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "实际接手部门")
    private Long acceptDeptId;

    /** 实际接手部门名 */
    @TableField("accept_dept_name")
    @ApiModelProperty(value = "实际接手部门名")
    private String acceptDeptName;

    /** 接手时刻（与翻已派同写，缺一不算派成） */
    @TableField("accepted_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "接手时刻")
    private Date acceptedAt;

    /** 最初真正派出时刻（撤条留痕，永不覆盖） */
    @TableField("first_dispatched_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "最初真正派出时刻")
    private Date firstDispatchedAt;

    /** 催过几轮 */
    @TableField("urge_rounds")
    @ApiModelProperty(value = "催过几轮")
    private Integer urgeRounds;

    /** 挂起原由 */
    @TableField("suspend_reason")
    @ApiModelProperty(value = "挂起原由")
    private String suspendReason;

    /** 隐患事由与受派部门记要 */
    @TableField("content")
    @ApiModelProperty(value = "隐患事由与受派部门记要")
    private String content;

    /** 情形 0待派 1已派 2派不出去 3挂起 4已撤 5转办 */
    @TableField("status")
    @ApiModelProperty(value = "情形 0待派 1已派 2派不出去 3挂起 4已撤 5转办")
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

    /** 翻条展示：还剩几日（不入库，由 DeadlineRuler 统一算，回写/翻条同一把尺） */
    @TableField(exist = false)
    @ApiModelProperty(value = "还剩几日（负为已过期限）")
    private Long daysLeft;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getItemNo() {
        return itemNo;
    }

    public void setItemNo(String itemNo) {
        this.itemNo = itemNo;
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

    public String getHazardSummary() {
        return hazardSummary;
    }

    public void setHazardSummary(String hazardSummary) {
        this.hazardSummary = hazardSummary;
    }

    public String getDeptGroup() {
        return deptGroup;
    }

    public void setDeptGroup(String deptGroup) {
        this.deptGroup = deptGroup;
    }

    public Long getPinDeptId() {
        return pinDeptId;
    }

    public void setPinDeptId(Long pinDeptId) {
        this.pinDeptId = pinDeptId;
    }

    public String getPinDeptName() {
        return pinDeptName;
    }

    public void setPinDeptName(String pinDeptName) {
        this.pinDeptName = pinDeptName;
    }

    public Long getAcceptDeptId() {
        return acceptDeptId;
    }

    public void setAcceptDeptId(Long acceptDeptId) {
        this.acceptDeptId = acceptDeptId;
    }

    public String getAcceptDeptName() {
        return acceptDeptName;
    }

    public void setAcceptDeptName(String acceptDeptName) {
        this.acceptDeptName = acceptDeptName;
    }

    public Date getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Date acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public Date getFirstDispatchedAt() {
        return firstDispatchedAt;
    }

    public void setFirstDispatchedAt(Date firstDispatchedAt) {
        this.firstDispatchedAt = firstDispatchedAt;
    }

    public Integer getUrgeRounds() {
        return urgeRounds;
    }

    public void setUrgeRounds(Integer urgeRounds) {
        this.urgeRounds = urgeRounds;
    }

    public String getSuspendReason() {
        return suspendReason;
    }

    public void setSuspendReason(String suspendReason) {
        this.suspendReason = suspendReason;
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

    public Long getDaysLeft() {
        return daysLeft;
    }

    public void setDaysLeft(Long daysLeft) {
        this.daysLeft = daysLeft;
    }
}
