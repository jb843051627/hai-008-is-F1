package com.fc.v2.service.dispatch;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 生产用派发窗口日历：参数走 application 配置（is.dispatch.*）。
 *
 * <pre>
 * is.dispatch.daily-from: 08:00
 * is.dispatch.daily-to:   18:00
 * is.dispatch.special-from: 08:00
 * is.dispatch.special-to:   20:00
 * is.dispatch.special-start: 2026-06-16   # 专项检查月那半个月
 * is.dispatch.special-end:   2026-06-30
 * is.dispatch.holidays: 2026-10-01,2026-10-02   # 法定节假日，逗号分隔
 * </pre>
 *
 * @author fuce
 * @date 2026-09-30
 */
@Component
public class DispatchCalendar extends DispatchWindow {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public DispatchCalendar(
            @Value("${is.dispatch.daily-from:08:00}") String dailyFrom,
            @Value("${is.dispatch.daily-to:18:00}") String dailyTo,
            @Value("${is.dispatch.special-from:08:00}") String specialFrom,
            @Value("${is.dispatch.special-to:20:00}") String specialTo,
            @Value("${is.dispatch.holidays:}") String holidays,
            @Value("${is.dispatch.special-start:}") String specialStart,
            @Value("${is.dispatch.special-end:}") String specialEnd) {
        super(LocalTime.parse(dailyFrom, TIME_FMT), LocalTime.parse(dailyTo, TIME_FMT),
                LocalTime.parse(specialFrom, TIME_FMT), LocalTime.parse(specialTo, TIME_FMT),
                parseDays(holidays), parseDay(specialStart), parseDay(specialEnd));
    }

    private static Set<LocalDate> parseDays(String csv) {
        Set<LocalDate> days = new HashSet<>();
        if (csv == null) {
            return days;
        }
        for (String part : csv.split(",")) {
            String p = part.trim();
            if (!p.isEmpty()) {
                days.add(LocalDate.parse(p, DAY_FMT));
            }
        }
        return days;
    }

    private static LocalDate parseDay(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(s.trim(), DAY_FMT);
    }
}
