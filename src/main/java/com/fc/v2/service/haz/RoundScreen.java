package com.fc.v2.service.haz;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * 一轮跑完亮的一屏：待派几条、已派几条、挂起几条（只数本轮）。
 *
 * 三个数在派发事务里同一次点算落库（t_is_dispatch_round），屏与库永远同源。
 * state 把三种语义彻底分开，避免再闹"本轮无活被当成系统出错"的误会：
 * SCANNED 已扫、NO_WORK 本轮无活、CLOSED 窗口关闭（没派，不是出错）、ERROR 系统出错。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class RoundScreen {

    public enum State {
        /** 正常扫过一轮（可能一条没派出，但有待办在池子里） */
        SCANNED,
        /** 本轮确实没有到期露面的活——正常结果，不是错误 */
        NO_WORK,
        /** 窗口关闭/节假日静默：本轮未执行，条子原地不动 */
        CLOSED,
        /** 当日该窗口已扫过，二次进来直接忽略，先前那次原样留着 */
        SKIPPED,
        /** 系统出错 */
        ERROR
    }

    private State state;
    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date finishedAt;

    private Integer winType;
    private String winName;

    /** 本轮待派（扫完仍没派出去的） */
    private int waitCnt;
    /** 本轮已派 */
    private int dispatchedCnt;
    /** 本轮挂起 */
    private int suspendedCnt;

    /** 轮次点算主键（已落库时带回） */
    private Long roundId;

    public static RoundScreen closed(String message) {
        RoundScreen s = new RoundScreen();
        s.state = State.CLOSED;
        s.message = message;
        s.waitCnt = 0;
        s.dispatchedCnt = 0;
        s.suspendedCnt = 0;
        return s;
    }

    public static RoundScreen skipped(String message) {
        RoundScreen s = new RoundScreen();
        s.state = State.SKIPPED;
        s.message = message;
        return s;
    }

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Date getStartedAt() { return startedAt; }
    public void setStartedAt(Date startedAt) { this.startedAt = startedAt; }
    public Date getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Date finishedAt) { this.finishedAt = finishedAt; }
    public Integer getWinType() { return winType; }
    public void setWinType(Integer winType) { this.winType = winType; }
    public String getWinName() { return winName; }
    public void setWinName(String winName) { this.winName = winName; }
    public int getWaitCnt() { return waitCnt; }
    public void setWaitCnt(int waitCnt) { this.waitCnt = waitCnt; }
    public int getDispatchedCnt() { return dispatchedCnt; }
    public void setDispatchedCnt(int dispatchedCnt) { this.dispatchedCnt = dispatchedCnt; }
    public int getSuspendedCnt() { return suspendedCnt; }
    public void setSuspendedCnt(int suspendedCnt) { this.suspendedCnt = suspendedCnt; }
    public Long getRoundId() { return roundId; }
    public void setRoundId(Long roundId) { this.roundId = roundId; }
}
