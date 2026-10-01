package com.fc.v2.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TIsHazTaskLogMapper;
import com.fc.v2.mapper.auto.TIsHazTaskMapper;
import com.fc.v2.model.auto.TIsHazTask;
import com.fc.v2.model.auto.TIsHazTaskLog;
import com.fc.v2.service.dispatch.DeadlinePolicy;

/**
 * 催办条逐条派发处理器：一条一个事务（REQUIRES_NEW 由调用方非事务入口展开），
 * 单条失败回滚自身、不拖垮整轮。
 *
 * @author fuce
 * @date 2026-09-30
 */
@Component
public class HazTaskDispatchHandler {

    static final int STATUS_WAIT = 0;
    static final int STATUS_DISPATCHED = 1;
    static final int STATUS_STUCK = 2;
    static final int STATUS_SUSPENDED = 3;

    static final int ACT_SUSPEND = 1;
    static final int ACT_DISPATCH = 2;
    static final int ACT_CANCEL = 3;
    static final int ACT_TRANSFER = 4;
    static final int ACT_URGE = 5;

    /** 逾期整 3 天仍派不出去：顶到最前头并落「派不出去」 */
    static final int OVERDUE_PRIORITY_DAYS = 3;

    /** 本轮逐条处置结果 */
    enum Outcome {
        /** 跳过（非待办/本窗口已派过） */
        SKIP,
        /** 已派出（接单时刻 + 状态翻转同事务落定） */
        DISPATCHED,
        /** 无部门可接，催办一轮 */
        URGED,
        /** 无部门可接且逾期满 3 天，落「派不出去」并催办一轮 */
        STUCK
    }

    /** 一轮内共享的候选部门与在手负载（负载随派出即时累加） */
    static final class RoundContext {
        final Date at;
        final String roundNo;
        final List<Long> activeDeptIds;
        final Map<Long, Long> workload;

        RoundContext(Date at, String roundNo, List<Long> activeDeptIds, Map<Long, Long> workload) {
            this.at = at;
            this.roundNo = roundNo;
            this.activeDeptIds = activeDeptIds;
            this.workload = workload;
        }
    }

    @javax.annotation.Resource
    private TIsHazTaskMapper taskMapper;
    @javax.annotation.Resource
    private TIsHazTaskLogMapper logMapper;

    /**
     * 派发一条。独立事务：接单时刻写入与状态 待派→已派 必须同成同败，两样缺一一律回滚，
     * 该单不算派成。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Outcome dispatchOne(Long id, RoundContext ctx) {
        TIsHazTask r = taskMapper.selectById(id);
        if (r == null || r.getDelFlag() == null || r.getDelFlag() != 0
                || (r.getStatus() != STATUS_WAIT && r.getStatus() != STATUS_STUCK)) {
            // 已派/挂起/撤条不再当待办；「派不出去」的条一旦有部门空出，仍按置顶序再试
            return Outcome.SKIP;
        }

        // 幂等软闸：同一窗口同一条已派过，当日二次进来直接忽略，先前那次原样留着
        if (countLog(r.getId(), ctx.roundNo, ACT_DISPATCH) > 0) {
            return Outcome.SKIP;
        }

        if (ctx.activeDeptIds.isEmpty()) {
            return handleNoDept(r, ctx);
        }
        return assign(r, ctx);
    }

    private Outcome handleNoDept(TIsHazTask r, RoundContext ctx) {
        int overdue = DeadlinePolicy.overdueDays(r.getDueAt(), ctx.at);
        boolean urged = urgeOncePerRound(r, ctx,
                "无启用责任部门可接单（逾期 " + overdue + " 日）");
        boolean turnStuck = overdue >= OVERDUE_PRIORITY_DAYS && r.getStatus() != STATUS_STUCK;
        if (turnStuck) {
            r.setStatus(STATUS_STUCK);
        }
        if (urged || turnStuck) {
            taskMapper.updateById(r);
        }
        return turnStuck ? Outcome.STUCK : (urged ? Outcome.URGED : Outcome.SKIP);
    }

    private Outcome assign(TIsHazTask r, RoundContext ctx) {
        Long originDept = r.getDeptId();
        Long target = pickFreestDept(r, ctx);

        // 接单双写：接手时刻 + 状态翻转，缺一不可，同在本事务
        Date acceptedAt = r.getAcceptedAt() == null ? ctx.at : r.getAcceptedAt();
        r.setDeptId(target);
        r.setAcceptedAt(acceptedAt);
        r.setStatus(STATUS_DISPATCHED);
        if (taskMapper.updateById(r) <= 0) {
            throw new IllegalStateException("催办条接单落库失败 id=" + r.getId());
        }

        TIsHazTaskLog log = newLog(r, ACT_DISPATCH, ctx.roundNo);
        log.setFromDeptId(originDept);
        log.setToDeptId(target);
        log.setAcceptedAt(acceptedAt);
        log.setReason("派至责任部门（在手活最少者优先）");
        logMapper.insert(log); // uk(task,round,action) 硬幂等：重复派发在此炸出并整体回滚

        ctx.workload.merge(target, 1L, Long::sum);
        return Outcome.DISPATCHED;
    }

    /**
     * 负载择部门：手上活越少权重越高（先接）；平手时该条原定责任部门若启用则黏住，
     * 再平手取部门 id 小者，保证派发结果确定、可复算。
     */
    private Long pickFreestDept(TIsHazTask r, RoundContext ctx) {
        Long best = null;
        long bestLoad = Long.MAX_VALUE;
        for (Long deptId : ctx.activeDeptIds) {
            long load = ctx.workload.getOrDefault(deptId, 0L);
            if (load < bestLoad) {
                bestLoad = load;
                best = deptId;
            }
        }
        if (originActiveAndTied(r.getDeptId(), bestLoad, ctx)) {
            return r.getDeptId();
        }
        return best;
    }

    private boolean originActiveAndTied(Long origin, long bestLoad, RoundContext ctx) {
        return origin != null && ctx.activeDeptIds.contains(origin)
                && ctx.workload.getOrDefault(origin, 0L) == bestLoad;
    }

    /** 同一窗口同一催办动作只记一轮；返回本轮是否真的记了一次。 */
    private boolean urgeOncePerRound(TIsHazTask r, RoundContext ctx, String reason) {
        if (countLog(r.getId(), ctx.roundNo, ACT_URGE) > 0) {
            return false;
        }
        r.setUrgeCount((r.getUrgeCount() == null ? 0 : r.getUrgeCount()) + 1);
        TIsHazTaskLog log = newLog(r, ACT_URGE, ctx.roundNo);
        log.setReason(reason);
        logMapper.insert(log);
        return true;
    }

    private int countLog(Long taskId, String roundNo, int action) {
        Integer cnt = logMapper.selectCount(new QueryWrapper<TIsHazTaskLog>()
                .eq("task_id", taskId)
                .eq("round_no", roundNo)
                .eq("action", action));
        return cnt == null ? 0 : cnt;
    }

    static TIsHazTaskLog newLog(TIsHazTask r, int action, String roundNo) {
        TIsHazTaskLog log = new TIsHazTaskLog();
        log.setTaskId(r.getId());
        log.setItemNo(r.getItemNo());
        log.setAction(action);
        // 最初期限与首派时刻随动作快照，撤条/转办等后到动作永不覆盖条上原值
        log.setDueAt(r.getDueAt());
        log.setAcceptedAt(r.getAcceptedAt());
        log.setRoundNo(roundNo);
        log.setDelFlag(0);
        return log;
    }
}
