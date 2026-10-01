package com.fc.v2.service.dispatch;

/**
 * 一轮派发跑完亮出的一屏。
 *
 * <p>待派/已派/挂起三个数与库里同一次点算（轮末同一事务内一次 GROUP BY 点出来），
 * 不是代码侧各自累加的；本轮无活（{@link #hasWork}=false 且非异常、非窗外）要与
 * 「系统出错」分清楚。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class RoundResult {

    /** 本轮窗口：DAILY / SPECIAL；窗外静默为 null */
    private String window;
    /** 轮次号（窗口+自然日），同窗口当日重复扫描拿到同一个 */
    private String roundNo;
    /** 窗外静默：有到期条也原地不动 */
    private boolean windowClosed;
    /** 本轮是否真有到期/需催办的活；false 即「本轮无活」，不是系统出错 */
    private boolean hasWork;

    /** 屏上三数：轮末同一次点算的库存口径（del_flag=0） */
    private int waitingCount;
    private int dispatchedCount;
    private int suspendedCount;

    /** 本轮增量（只数本轮） */
    private int roundDispatched;
    private int roundSuspended;
    private int roundUrged;
    /** 本轮派不出去（逾期落 STATUS_STUCK）的条数 */
    private int roundStuck;

    private String message;

    public static RoundResult closed(String message) {
        RoundResult r = new RoundResult();
        r.windowClosed = true;
        r.message = message;
        return r;
    }

    public String getWindow() {
        return window;
    }

    public void setWindow(String window) {
        this.window = window;
    }

    public String getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(String roundNo) {
        this.roundNo = roundNo;
    }

    public boolean isWindowClosed() {
        return windowClosed;
    }

    public void setWindowClosed(boolean windowClosed) {
        this.windowClosed = windowClosed;
    }

    public boolean isHasWork() {
        return hasWork;
    }

    public void setHasWork(boolean hasWork) {
        this.hasWork = hasWork;
    }

    public int getWaitingCount() {
        return waitingCount;
    }

    public void setWaitingCount(int waitingCount) {
        this.waitingCount = waitingCount;
    }

    public int getDispatchedCount() {
        return dispatchedCount;
    }

    public void setDispatchedCount(int dispatchedCount) {
        this.dispatchedCount = dispatchedCount;
    }

    public int getSuspendedCount() {
        return suspendedCount;
    }

    public void setSuspendedCount(int suspendedCount) {
        this.suspendedCount = suspendedCount;
    }

    public int getRoundDispatched() {
        return roundDispatched;
    }

    public void setRoundDispatched(int roundDispatched) {
        this.roundDispatched = roundDispatched;
    }

    public int getRoundSuspended() {
        return roundSuspended;
    }

    public void setRoundSuspended(int roundSuspended) {
        this.roundSuspended = roundSuspended;
    }

    public int getRoundUrged() {
        return roundUrged;
    }

    public void setRoundUrged(int roundUrged) {
        this.roundUrged = roundUrged;
    }

    public int getRoundStuck() {
        return roundStuck;
    }

    public void setRoundStuck(int roundStuck) {
        this.roundStuck = roundStuck;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
