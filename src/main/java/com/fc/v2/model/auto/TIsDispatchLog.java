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

import java.io.Serializable;
import java.util.Date;

/**
 * 催办派发留痕 t_is_dispatch_log
 *
 * 生成/挂起/派出/催办/撤条/转办/调档全过痕。撤条时把最初整改日 origDueAt、
 * 最初真正派出时刻 firstDispatchedAt 原样抄存——它们从催办条出生起不许被后到动作覆盖。
 *
 * @author fuce
 * @date 2026-09-30
 */
@TableName("t_is_dispatch_log")
@ApiModel(value = "TIsDispatchLog", description = "催办派发留痕")
public class TIsDispatchLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @TableField("round_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long roundId;

    @TableField("bill_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long billId;

    @TableField("bill_no")
    private String billNo;

    /** CREATE生成 SUSPEND挂起 DISPATCH派出 URGE催办 REVOKE撤条 TRANSFER转办 TUNE调档 */
    @TableField("action")
    private String action;

    @TableField("from_status")
    private Integer fromStatus;

    @TableField("to_status")
    private Integer toStatus;

    @TableField("dept_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;

    @TableField("dept_name")
    private String deptName;

    /** 抄存：最初整改期限最后一刻 */
    @TableField("orig_due_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date origDueAt;

    /** 抄存：最初真正派出时刻 */
    @TableField("first_dispatched_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date firstDispatchedAt;

    @TableField("detail")
    private String detail;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRoundId() { return roundId; }
    public void setRoundId(Long roundId) { this.roundId = roundId; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public String getBillNo() { return billNo; }
    public void setBillNo(String billNo) { this.billNo = billNo; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Integer getFromStatus() { return fromStatus; }
    public void setFromStatus(Integer fromStatus) { this.fromStatus = fromStatus; }
    public Integer getToStatus() { return toStatus; }
    public void setToStatus(Integer toStatus) { this.toStatus = toStatus; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public Date getOrigDueAt() { return origDueAt; }
    public void setOrigDueAt(Date origDueAt) { this.origDueAt = origDueAt; }
    public Date getFirstDispatchedAt() { return firstDispatchedAt; }
    public void setFirstDispatchedAt(Date firstDispatchedAt) { this.firstDispatchedAt = firstDispatchedAt; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
