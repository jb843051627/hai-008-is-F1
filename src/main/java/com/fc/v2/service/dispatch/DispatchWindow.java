package com.fc.v2.service.dispatch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

/**
 * 派发窗口日历——几点派、派不派，窗口说了算。
 *
 * <ul>
 *   <li>白天一个<b>日常窗</b>（默认 08:00–18:00，天天开）；</li>
 *   <li>夜里与法定节假日整段<b>静默</b>：窗外即使有到期条子也原地不动，等下一轮窗口再派；</li>
 *   <li>专项检查月那半个月单独开一个<b>专项窗</b>，不走日常窗（默认 08:00–20:00，节假日仍静默）；</li>
 *   <li>窗口标识 + 自然日拼成轮次号 {@link #roundNo}，供「同一窗口同一条只派一次、
 *       当日二次进来直接忽略」做幂等键。</li>
 * </ul>
 *
 * 本类不持有线程状态，节假日集合与专项区间由构造方给定（生产走配置，测试直接 new）。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class DispatchWindow {

    public static final String DAILY = "DAILY";
    public static final String SPECIAL = "SPECIAL";
    /** 窗外：静默，不派 */
    public static final String CLOSED = null;

    private final LocalTime dailyFrom;
    private final LocalTime dailyTo;
    private final LocalTime specialFrom;
    private final LocalTime specialTo;
    private final Set<LocalDate> holidays;
    private final LocalDate specialStart;
    private final LocalDate specialEnd;

    public DispatchWindow(Set<LocalDate> holidays, LocalDate specialStart, LocalDate specialEnd) {
        this(LocalTime.of(8, 0), LocalTime.of(18, 0),
                LocalTime.of(8, 0), LocalTime.of(20, 0),
                holidays, specialStart, specialEnd);
    }

    public DispatchWindow(LocalTime dailyFrom, LocalTime dailyTo,
                          LocalTime specialFrom, LocalTime specialTo,
                          Set<LocalDate> holidays,
                          LocalDate specialStart, LocalDate specialEnd) {
        this.dailyFrom = dailyFrom;
        this.dailyTo = dailyTo;
        this.specialFrom = specialFrom;
        this.specialTo = specialTo;
        this.holidays = holidays == null ? Collections.<LocalDate>emptySet() : holidays;
        this.specialStart = specialStart;
        this.specialEnd = specialEnd;
    }

    /**
     * 当前时刻落在哪个窗口；返回 {@link #CLOSED}（null）表示窗外静默，本轮一律不派。
     * 静默口径只认两样：夜里（窗外时段）与法定节假日；叙事未规定周末整日静默，不自行加码。
     */
    public String activeWindow(Date at) {
        if (at == null) {
            return CLOSED;
        }
        LocalDateTime ldt = toLdt(at);
        LocalDate day = ldt.toLocalDate();
        // 法定节假日整段静默，任何窗口都不开
        if (holidays.contains(day)) {
            return CLOSED;
        }
        LocalTime time = ldt.toLocalTime();
        // 专项窗优先：专项期间不走日常窗
        if (inSpecial(day) && !time.isBefore(specialFrom) && time.isBefore(specialTo)) {
            return SPECIAL;
        }
        if (!inSpecial(day) && !time.isBefore(dailyFrom) && time.isBefore(dailyTo)) {
            return DAILY;
        }
        return CLOSED;
    }

    /** 窗口是否开着。 */
    public boolean isOpen(Date at) {
        return activeWindow(at) != CLOSED;
    }

    /**
     * 轮次号：窗口 + 东八区自然日。同一窗口当日多次扫描拿到同一轮次号，
     * 作为「同一条子在同一窗口只派一次」的幂等键口径。
     */
    public String roundNo(Date at) {
        String win = activeWindow(at);
        if (win == CLOSED) {
            return null;
        }
        return win + ":" + toLdt(at).toLocalDate();
    }

    private boolean inSpecial(LocalDate day) {
        if (specialStart == null || specialEnd == null) {
            return false;
        }
        return !day.isBefore(specialStart) && !day.isAfter(specialEnd);
    }

    private static LocalDateTime toLdt(Date d) {
        return d.toInstant().atZone(DeadlinePolicy.ZONE).toLocalDateTime();
    }
}
