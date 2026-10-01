package com.fc.v2.service.haz;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * 期限口径——全系统唯一一把尺。
 *
 * 算"到没到期"（{@link #isDue}）和算"还剩几日"（{@link #daysLeft}）都只许用本类，
 * 翻条（列表/明细）与回写（派发轮次）不得各算各的，否则半年下来账对不上。
 *
 * 统一走 GMT+8 墙钟：JDBC 的 serverTimezone 与本机时区不对称时，直接拿 Date 串库会整体偏移，
 * 恰好到期这类边界就会错判（与本包既有实现的教训一致）。
 *
 * 期限那一刻（{@link #deadlineOf}）在条子生成时定死，后续任何回写都不许动；
 * 真要调，只许调"还剩几日露面"这一档（amount）。
 *
 * @author fuce
 * @date 2026-09-30
 */
public final class DeadlineRuler {

    /** 全区统一业务时区 */
    public static final TimeZone ZONE = TimeZone.getTimeZone("GMT+8");

    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    private DeadlineRuler() {
    }

    private static Calendar cal(Date d) {
        Calendar c = Calendar.getInstance(ZONE);
        c.setTime(d);
        return c;
    }

    /** 限改之日的最后一刻（当天 23:59:59，墙钟 GMT+8）——催办条期限只此一个出处。 */
    public static Date deadlineOf(Date rectifyDate) {
        Calendar c = cal(rectifyDate);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /** 某日墙钟零点（GMT+8）。 */
    public static Date startOfDay(Date d) {
        Calendar c = cal(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /**
     * 到没到期：at 已到期限最后一刻（含）即算到期。
     * 与 {@link #daysLeft} 同一只尺子（GMT+8 瞬间比较）。
     */
    public static boolean isDue(Date dueAt, Date at) {
        if (dueAt == null || at == null) {
            return false;
        }
        return !at.before(dueAt);
    }

    /**
     * 还剩几日：期限日墙钟零点 − at 墙钟零点，按自然日取整。
     * 期限当日 = 0；已过期为负。翻条、回写都从这里取数。
     */
    public static long daysLeft(Date dueAt, Date at) {
        if (dueAt == null || at == null) {
            throw new IllegalArgumentException("期限与比对时刻都不能为空");
        }
        return (startOfDay(dueAt).getTime() - startOfDay(at).getTime()) / DAY_MS;
    }

    /**
     * 是否已过期限整 n 日（at − 期限一刻 ≥ n 个自然日）。
     * 顶头档（过期限三日）用它，与到期判定同一把尺。
     */
    public static boolean overdueAtLeast(Date dueAt, Date at, int days) {
        if (dueAt == null || at == null) {
            return false;
        }
        return at.getTime() - dueAt.getTime() >= (long) days * DAY_MS;
    }

    /**
     * 该时刻条子该不该露面：已到期，或还剩日数已不多于提前档 leadDays。
     * leadDays 为空按 0（期限当天才露面）。
     */
    public static boolean visible(BigDecimal leadDays, Date dueAt, Date at) {
        if (isDue(dueAt, at)) {
            return true;
        }
        long left = daysLeft(dueAt, at);
        BigDecimal lead = leadDays == null ? BigDecimal.ZERO : leadDays;
        return BigDecimal.valueOf(left).compareTo(lead) <= 0;
    }
}
