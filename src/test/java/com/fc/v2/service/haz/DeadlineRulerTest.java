package com.fc.v2.service.haz;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 统一期限口径测试：到期判定与剩余几日同一把尺（GMT+8）。
 *
 * @author fuce
 * @date 2026-09-30
 */
class DeadlineRulerTest {

    static {
        TimeZone.setDefault(DeadlineRuler.ZONE);
    }

    private static Date at(int y, int m, int d, int hh, int mi, int ss) {
        Calendar c = Calendar.getInstance(DeadlineRuler.ZONE);
        c.set(y, m - 1, d, hh, mi, ss);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    @Test
    void deadlineIsLastInstantOfRectifyDay() {
        Date due = DeadlineRuler.deadlineOf(at(2026, 9, 30, 8, 0, 0));
        // 期限一刻 = 当天 23:59:59（GMT+8）
        assertEquals(at(2026, 9, 30, 23, 59, 59), due);
    }

    @Test
    void isDueBoundaryUsesOneRuler() {
        Date due = DeadlineRuler.deadlineOf(at(2026, 9, 30, 8, 0, 0));
        // 恰好期限最后一刻：到期（含）
        assertTrue(DeadlineRuler.isDue(due, at(2026, 9, 30, 23, 59, 59)));
        // 早一秒：未到期
        assertFalse(DeadlineRuler.isDue(due, at(2026, 9, 30, 23, 59, 58)));
        // 次日：到期
        assertTrue(DeadlineRuler.isDue(due, at(2026, 10, 1, 0, 0, 0)));
    }

    @Test
    void daysLeftCountsNaturalDays() {
        Date due = DeadlineRuler.deadlineOf(at(2026, 9, 30, 8, 0, 0));
        assertEquals(2L, DeadlineRuler.daysLeft(due, at(2026, 9, 28, 9, 0, 0)));
        assertEquals(0L, DeadlineRuler.daysLeft(due, at(2026, 9, 30, 1, 0, 0)));
        assertEquals(-1L, DeadlineRuler.daysLeft(due, at(2026, 10, 1, 23, 0, 0)));
    }

    @Test
    void overdueAtLeastThreeDaysExact() {
        Date due = DeadlineRuler.deadlineOf(at(2026, 9, 10, 8, 0, 0));
        // 过了整三日：9/13 23:59:59 起顶头
        assertFalse(DeadlineRuler.overdueAtLeast(due, at(2026, 9, 13, 23, 59, 58), 3));
        assertTrue(DeadlineRuler.overdueAtLeast(due, at(2026, 9, 13, 23, 59, 59), 3));
    }

    @Test
    void visibleLeadDaysIsTheOnlyAdjustableSlot() {
        Date due = DeadlineRuler.deadlineOf(at(2026, 9, 30, 8, 0, 0));
        // 还剩 3 日露面：9/27（剩3）露面，9/26（剩4）不露面
        assertTrue(DeadlineRuler.visible(BigDecimal.valueOf(3), due, at(2026, 9, 27, 8, 0, 0)));
        assertFalse(DeadlineRuler.visible(BigDecimal.valueOf(3), due, at(2026, 9, 26, 8, 0, 0)));
        // 到期后必露面
        assertTrue(DeadlineRuler.visible(BigDecimal.valueOf(3), due, at(2026, 10, 2, 8, 0, 0)));
    }
}
