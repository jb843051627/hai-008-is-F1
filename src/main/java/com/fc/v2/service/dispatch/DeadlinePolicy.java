package com.fc.v2.service.dispatch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * 催办条期限口径——全系统唯一一把尺子。
 *
 * <p>一张条子的期限是「整改期限那一日的最后一刻」（due_at，23:59:59）。任何地方要算
 * 「到没到期」「还剩几日」，都只能走这里的方法，翻条（扫描）和回写（派发/详情）不许各算各的。
 *
 * <ul>
 *   <li>{@link #isDue}：墙钟走到 due_at 那一刻即到期（at &ge; due_at）；到期日当天白天
 *       还没到最后一刻，不算到期。</li>
 *   <li>{@link #daysLeft}：以 GMT+8 自然日计，due 当日为 0，之后为负（负几即逾期几日）。
 *       与 {@link #isDue} 同源，不允许另一处再用小时差另算。</li>
 *   <li>{@link #shouldSurface}：还剩 amount 个自然日（含当日 0）就该露面进待派；已逾期恒露面。</li>
 * </ul>
 *
 * 口径只允许调「还剩几日」这一档（amount），期限那一刻（due_at）落定后任何入口都不得改。
 *
 * @author fuce
 * @date 2026-09-30
 */
public final class DeadlinePolicy {

    /** 业务库与 Jackson 统一东八区 */
    public static final ZoneId ZONE = ZoneId.of("GMT+8");

    private DeadlinePolicy() {
    }

    /** 到没到期：墙钟走到/越过期限最后一刻。 */
    public static boolean isDue(Date dueAt, Date at) {
        if (dueAt == null || at == null) {
            return false;
        }
        return !at.before(dueAt);
    }

    /** 还剩几日（自然日，GMT+8）：到期日为 0，到期前为正，逾期后为负（-n 即逾期 n 天）。 */
    public static int daysLeft(Date dueAt, Date at) {
        if (dueAt == null || at == null) {
            return Integer.MAX_VALUE;
        }
        LocalDate dueDay = toLocalDate(dueAt);
        LocalDate today = toLocalDate(at);
        return (int) ChronoUnit.DAYS.between(today, dueDay);
    }

    /** 逾期几日（未逾期/到期当日返回 0）。 */
    public static int overdueDays(Date dueAt, Date at) {
        if (!isDue(dueAt, at)) {
            return 0;
        }
        return Math.max(0, -daysLeft(dueAt, at));
    }

    /**
     * 到期前还剩几日该露面：还剩 leadDays 个自然日（含更少/到期/逾期）即为应露面。
     * leadDays 为空或负视为该条不设提前露面（只有真正到期才露面）。
     */
    public static boolean shouldSurface(Date dueAt, BigDecimal leadDays, Date at) {
        if (dueAt == null || at == null) {
            return false;
        }
        if (isDue(dueAt, at)) {
            return true;
        }
        if (leadDays == null || leadDays.signum() < 0) {
            return false;
        }
        return daysLeft(dueAt, at) <= leadDays.intValue();
    }

    private static LocalDate toLocalDate(Date d) {
        return d.toInstant().atZone(ZONE).toLocalDate();
    }
}
