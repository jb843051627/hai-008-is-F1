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
 * 催办派发窗口 t_is_dispatch_window
 *
 * winType=1 日常：起止日为空，逢非节假日按点开闭；winType=2 专项检查月：起止日内单开，不走日常。
 *
 * @author fuce
 * @date 2026-09-30
 */
@TableName("t_is_dispatch_window")
@ApiModel(value = "TIsDispatchWindow", description = "催办派发窗口")
public class TIsDispatchWindow implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 窗口种类 1日常 2专项检查月 */
    @TableField("win_type")
    private Integer winType;

    @TableField("win_name")
    private String winName;

    /** 专项起始日（含，日常为空） */
    @TableField("start_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    /** 专项截止日（含，日常为空） */
    @TableField("end_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    /** 开窗时点（整点，含） */
    @TableField("open_hh")
    private Integer openHh;

    /** 关窗时点（整点，不含） */
    @TableField("close_hh")
    private Integer closeHh;

    /** 情形 0启用 1停用 */
    @TableField("status")
    private Integer status;

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
    public Integer getWinType() { return winType; }
    public void setWinType(Integer winType) { this.winType = winType; }
    public String getWinName() { return winName; }
    public void setWinName(String winName) { this.winName = winName; }
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }
    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }
    public Integer getOpenHh() { return openHh; }
    public void setOpenHh(Integer openHh) { this.openHh = openHh; }
    public Integer getCloseHh() { return closeHh; }
    public void setCloseHh(Integer closeHh) { this.closeHh = closeHh; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
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
