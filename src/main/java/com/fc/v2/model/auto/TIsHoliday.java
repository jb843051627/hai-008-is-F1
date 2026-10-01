package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;

import java.io.Serializable;
import java.util.Date;

/**
 * 法定节假日历 t_is_holiday：落在表内的日子整段静默；调休上班也维护在此，全系统只认这一处。
 *
 * @author fuce
 * @date 2026-09-30
 */
@TableName("t_is_holiday")
@ApiModel(value = "TIsHoliday", description = "法定节假日历")
public class TIsHoliday implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("holiday_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date holidayDate;

    @TableField("holiday_name")
    private String holidayName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Date getHolidayDate() { return holidayDate; }
    public void setHolidayDate(Date holidayDate) { this.holidayDate = holidayDate; }
    public String getHolidayName() { return holidayName; }
    public void setHolidayName(String holidayName) { this.holidayName = holidayName; }
}
