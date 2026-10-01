package com.fc.v2.service.haz;

import com.fc.v2.model.auto.TIsDispatchWindow;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * 派发窗口判定——几点派、派不派，窗口说了算。
 *
 * 口径（全区只此一处）：
 * 1. 法定节假日整段静默（以 t_is_holiday 为准；调休上班的周末不在表内，照常开窗）。
 * 2. 当日落在启用的专项检查月起止日内：只走专项窗口，绝不回退日常窗口。
 * 3. 其余日子走日常窗口；开窗时点（含）到关窗时点（不含）之间才派。
 * 4. 夜间 / 节假日 / 窗口外：哪怕有条子到期也原地不动，等下一轮窗口。
 *
 * 全部走 GMT+8 墙钟，与 {@link DeadlineRuler} 同一时区基准。
 *
 * @author fuce
 * @date 2026-09-30
 */
public final class DispatchWindows {

    private static final TimeZone ZONE = DeadlineRuler.ZONE;

    private DispatchWindows() {
    }

    /**
     * 解析 at 时刻是否在窗口内。
     *
     * @param at       比对时刻
     * @param holidays 法定节假日日期集合（已按墙钟截到日）
     * @param windows  启用窗口（日常一条 + 若干专项）
     * @return 命中的窗口；窗口外（夜间/节假日/无窗口）返回 null
     */
    public static TIsDispatchWindow resolve(Date at, List<Date> holidays, List<TIsDispatchWindow> windows) {
        if (at == null || windows == null || windows.isEmpty()) {
            return null;
        }
        // 法定节假日整段静默，专项也不例外。
        if (isHoliday(at, holidays)) {
            return null;
        }
        TIsDispatchWindow daily = null;
        boolean inSpecialDate = false;
        for (TIsDispatchWindow w : windows) {
            if (w.getStatus() != null && w.getStatus() != 0) {
                continue;
            }
            if (w.getWinType() != null && w.getWinType() == HazConst.WIN_SPECIAL) {
                if (inDateRange(at, w)) {
                    // 当日落在专项期：整段日子只认专项窗口，绝不回退日常。
                    inSpecialDate = true;
                    if (inHours(at, w)) {
                        return w;
                    }
                }
            } else if (w.getWinType() != null && w.getWinType() == HazConst.WIN_DAILY) {
                daily = w;
            }
        }
        // 专项日不在专项钟点内：静默，等下一轮专项窗口（哪怕日常此刻开着也不派）。
        if (inSpecialDate) {
            return null;
        }
        if (daily != null && inHours(at, daily)) {
            return daily;
        }
        return null;
    }

    /** 仅判断当日是否法定节假日（静默日）。 */
    public static boolean isHoliday(Date at, List<Date> holidays) {
        if (holidays == null) {
            return false;
        }
        long day = DeadlineRuler.startOfDay(at).getTime();
        for (Date h : holidays) {
            if (DeadlineRuler.startOfDay(h).getTime() == day) {
                return true;
            }
        }
        return false;
    }

    /** at 是否落在专项窗口起止日内（两端均含）。 */
    public static boolean inDateRange(Date at, TIsDispatchWindow w) {
        if (w.getStartDate() == null || w.getEndDate() == null) {
            return false;
        }
        long day = DeadlineRuler.startOfDay(at).getTime();
        return day >= DeadlineRuler.startOfDay(w.getStartDate()).getTime()
                && day <= DeadlineRuler.startOfDay(w.getEndDate()).getTime();
    }

    /** at 的墙钟整点是否在 [openHh, closeHh) 内（open 含、close 不含）。 */
    public static boolean inHours(Date at, TIsDispatchWindow w) {
        if (w.getOpenHh() == null || w.getCloseHh() == null) {
            return false;
        }
        Calendar c = Calendar.getInstance(ZONE);
        c.setTime(at);
        int hh = c.get(Calendar.HOUR_OF_DAY);
        return hh >= w.getOpenHh() && hh < w.getCloseHh();
    }
}
