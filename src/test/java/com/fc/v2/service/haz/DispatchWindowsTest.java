package com.fc.v2.service.haz;

import com.fc.v2.model.auto.TIsDispatchWindow;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 窗口判定测试：白天开窗、夜间/节假日静默、专项半月覆盖日常、关窗点不含。
 *
 * @author fuce
 * @date 2026-09-30
 */
class DispatchWindowsTest {

    private static final SimpleDateFormat F = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static Date t(String s) {
        try { return F.parse(s); } catch (Exception e) { throw new RuntimeException(e); }
    }
    static Date d(String s) {
        try { return new SimpleDateFormat("yyyy-MM-dd").parse(s); } catch (Exception e) { throw new RuntimeException(e); }
    }

    private TIsDispatchWindow daily(int open, int close) {
        TIsDispatchWindow w = new TIsDispatchWindow();
        w.setId(1L);
        w.setWinType(HazConst.WIN_DAILY);
        w.setWinName("日常");
        w.setOpenHh(open);
        w.setCloseHh(close);
        w.setStatus(0);
        w.setDelFlag(0);
        return w;
    }

    private TIsDispatchWindow special(String s, String e, int open, int close) {
        TIsDispatchWindow w = new TIsDispatchWindow();
        w.setId(2L);
        w.setWinType(HazConst.WIN_SPECIAL);
        w.setWinName("专项");
        w.setStartDate(d(s));
        w.setEndDate(d(e));
        w.setOpenHh(open);
        w.setCloseHh(close);
        w.setStatus(0);
        w.setDelFlag(0);
        return w;
    }

    @Test
    void dailyWindowOpenAndCloseBoundaries() {
        List<TIsDispatchWindow> wins = Collections.singletonList(daily(8, 20));
        assertSame(wins.get(0), DispatchWindows.resolve(t("2026-09-30 08:00:00"), null, wins), "开窗点含");
        assertSame(wins.get(0), DispatchWindows.resolve(t("2026-09-30 19:59:59"), null, wins), "关窗前一秒仍开");
        assertNull(DispatchWindows.resolve(t("2026-09-30 20:00:00"), null, wins), "关窗点不含");
        assertNull(DispatchWindows.resolve(t("2026-09-30 07:59:59"), null, wins), "清晨未开窗");
        assertNull(DispatchWindows.resolve(t("2026-09-30 23:30:00"), null, wins), "夜间静默");
    }

    @Test
    void holidaySilencesAllWindows() {
        List<TIsDispatchWindow> wins = Arrays.asList(daily(8, 20), special("2026-06-01", "2026-06-15", 8, 20));
        List<Date> hol = new ArrayList<>(Collections.singletonList(d("2026-06-02")));
        // 专项期内但逢法定节假日：整段静默，专项也不派
        assertNull(DispatchWindows.resolve(t("2026-06-02 10:00:00"), hol, wins));
        // 非节假日的专项日：走专项，不回退日常
        assertSame(wins.get(1), DispatchWindows.resolve(t("2026-06-03 10:00:00"), hol, wins));
    }

    @Test
    void specialWindowDoesNotFallBackToDaily() {
        TIsDispatchWindow da = daily(8, 20);
        TIsDispatchWindow sp = special("2026-06-01", "2026-06-15", 9, 11);
        List<TIsDispatchWindow> wins = Arrays.asList(da, sp);
        // 专项日但落在专项钟点外（日常此刻本开着）：也不派
        assertNull(DispatchWindows.resolve(t("2026-06-02 15:00:00"), null, wins));
        // 专项钟点内：走专项
        assertEquals(HazConst.WIN_SPECIAL, DispatchWindows.resolve(t("2026-06-02 10:00:00"), null, wins).getWinType());
        // 专项结束次日：回日常
        assertEquals(HazConst.WIN_DAILY, DispatchWindows.resolve(t("2026-06-16 10:00:00"), null, wins).getWinType());
    }
}
