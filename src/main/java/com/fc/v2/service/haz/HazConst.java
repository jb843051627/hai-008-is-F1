package com.fc.v2.service.haz;

/**
 * 催办派发领域常量。
 *
 * @author fuce
 * @date 2026-09-30
 */
public final class HazConst {

    private HazConst() {
    }

    /** 催办条情形 */
    public static final int BILL_WAIT = 0;
    public static final int BILL_DISPATCHED = 1;
    public static final int BILL_STUCK = 2;
    public static final int BILL_SUSPENDED = 3;
    public static final int BILL_REVOKED = 4;
    public static final int BILL_TRANSFERRED = 5;

    /** 窗口种类 */
    public static final int WIN_DAILY = 1;
    public static final int WIN_SPECIAL = 2;

    /** 轮次结果 */
    public static final int ROUND_SCANNED = 1;
    public static final int ROUND_NO_WORK = 2;

    /** 顶头档：过了期限整三日还没派出去 */
    public static final int OVERDUE_TOP_DAYS = 3;

    /** 条子默认提前露面日数（只落进 amount 这一档，期限一刻不动） */
    public static final int DEFAULT_LEAD_DAYS = 3;

    /** 留痕动作 */
    public static final String ACT_CREATE = "CREATE";
    public static final String ACT_SUSPEND = "SUSPEND";
    public static final String ACT_DISPATCH = "DISPATCH";
    public static final String ACT_URGE = "URGE";
    public static final String ACT_REVOKE = "REVOKE";
    public static final String ACT_TRANSFER = "TRANSFER";
    public static final String ACT_TUNE = "TUNE";

    public static String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case BILL_WAIT: return "待派";
            case BILL_DISPATCHED: return "已派";
            case BILL_STUCK: return "派不出去";
            case BILL_SUSPENDED: return "挂起";
            case BILL_REVOKED: return "已撤";
            case BILL_TRANSFERRED: return "转办";
            default: return "未知";
        }
    }
}
