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
 * 限期整改隐患源对象 t_is_hazard
 *
 * @author fuce
 * @date 2026-09-30
 */
@TableName("t_is_hazard")
@ApiModel(value = "TIsHazard", description = "限期整改隐患源")
public class TIsHazard implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 隐患号（检查记录内唯一） */
    @TableField("hazard_no")
    private String hazardNo;

    /** 隐患概况 */
    @TableField("summary")
    private String summary;

    /** 责任条线（候选部门分组代号） */
    @TableField("dept_group")
    private String deptGroup;

    /** 原定责任部门（可空；撤并判定看它） */
    @TableField("pin_dept_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long pinDeptId;

    /** 限改之日（当天最后一刻为条子期限） */
    @TableField("rectify_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date rectifyDate;

    /** 是否已销号结案 0否 1是 */
    @TableField("closed")
    private Integer closed;

    @TableField("del_flag")
    private Integer delFlag;

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

    @TableField("remark")
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getHazardNo() { return hazardNo; }
    public void setHazardNo(String hazardNo) { this.hazardNo = hazardNo; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getDeptGroup() { return deptGroup; }
    public void setDeptGroup(String deptGroup) { this.deptGroup = deptGroup; }
    public Long getPinDeptId() { return pinDeptId; }
    public void setPinDeptId(Long pinDeptId) { this.pinDeptId = pinDeptId; }
    public Date getRectifyDate() { return rectifyDate; }
    public void setRectifyDate(Date rectifyDate) { this.rectifyDate = rectifyDate; }
    public Integer getClosed() { return closed; }
    public void setClosed(Integer closed) { this.closed = closed; }
    public Integer getDelFlag() { return delFlag; }
    public void setDelFlag(Integer delFlag) { this.delFlag = delFlag; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
