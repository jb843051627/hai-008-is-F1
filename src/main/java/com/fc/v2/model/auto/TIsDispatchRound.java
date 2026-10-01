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
 * 催办派发轮次点算 t_is_dispatch_round
 *
 * 同一事务里随派发一起落库：屏上待派/已派/挂起三个数与库底数出自同一次点算。
 * resultType=2 表示本轮无活——这是正常结果，不是系统出错；窗口关闭/出错不写轮次。
 *
 * @author fuce
 * @date 2026-09-30
 */
@TableName("t_is_dispatch_round")
@ApiModel(value = "TIsDispatchRound", description = "催办派发轮次点算")
public class TIsDispatchRound implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 本轮日（墙钟 GMT+8） */
    @TableField("round_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date roundDate;

    /** 本轮窗口 1日常 2专项 */
    @TableField("win_type")
    private Integer winType;

    @TableField("win_name")
    private String winName;

    @TableField("started_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startedAt;

    @TableField("finished_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date finishedAt;

    /** 本轮待派几条 */
    @TableField("wait_cnt")
    private Integer waitCnt;

    /** 本轮已派几条 */
    @TableField("dispatched_cnt")
    private Integer dispatchedCnt;

    /** 本轮挂起几条 */
    @TableField("suspended_cnt")
    private Integer suspendedCnt;

    /** 结果 1已扫 2本轮无活 */
    @TableField("result_type")
    private Integer resultType;

    @TableField("message")
    private String message;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private String updateBy;

    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Date getRoundDate() { return roundDate; }
    public void setRoundDate(Date roundDate) { this.roundDate = roundDate; }
    public Integer getWinType() { return winType; }
    public void setWinType(Integer winType) { this.winType = winType; }
    public String getWinName() { return winName; }
    public void setWinName(String winName) { this.winName = winName; }
    public Date getStartedAt() { return startedAt; }
    public void setStartedAt(Date startedAt) { this.startedAt = startedAt; }
    public Date getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Date finishedAt) { this.finishedAt = finishedAt; }
    public Integer getWaitCnt() { return waitCnt; }
    public void setWaitCnt(Integer waitCnt) { this.waitCnt = waitCnt; }
    public Integer getDispatchedCnt() { return dispatchedCnt; }
    public void setDispatchedCnt(Integer dispatchedCnt) { this.dispatchedCnt = dispatchedCnt; }
    public Integer getSuspendedCnt() { return suspendedCnt; }
    public void setSuspendedCnt(Integer suspendedCnt) { this.suspendedCnt = suspendedCnt; }
    public Integer getResultType() { return resultType; }
    public void setResultType(Integer resultType) { this.resultType = resultType; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
