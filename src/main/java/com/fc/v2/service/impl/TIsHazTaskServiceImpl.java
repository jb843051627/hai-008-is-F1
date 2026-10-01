package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TIsHazTaskLogMapper;
import com.fc.v2.mapper.auto.TIsHazTaskMapper;
import com.fc.v2.mapper.auto.TSysDepartmentMapper;
import com.fc.v2.model.auto.TIsHazTask;
import com.fc.v2.model.auto.TIsHazTaskLog;
import com.fc.v2.model.auto.TSysDepartment;
import com.fc.v2.service.ITIsHazTaskService;
import com.fc.v2.service.dispatch.DeadlinePolicy;
import com.fc.v2.service.dispatch.DispatchWindow;
import com.fc.v2.service.dispatch.HazardClosurePort;
import com.fc.v2.service.dispatch.RoundResult;

/**
 * 隐患整改催办条 Service业务层处理（scheduling-job 形状：周期窗口扫描派发）。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TIsHazTaskServiceImpl implements ITIsHazTaskService {

    /** 本轮方法不开事务：逐条走 REQUIRES_NEW，单条失败只回滚自身，不拖垮整轮 */
    @javax.annotation.Resource
    private TIsHazTaskMapper isHazTaskMapper;
    @javax.annotation.Resource
    private TIsHazTaskLogMapper isHazTaskLogMapper;
    @javax.annotation.Resource
    private TSysDepartmentMapper departmentMapper;
    @javax.annotation.Resource
    private HazTaskDispatchHandler dispatchHandler;
    @javax.annotation.Resource
    private DispatchWindow dispatchWindow;
    /** 隐患销号台账未接入时，默认没有任何隐患销号 */
    private HazardClosurePort hazardClosurePort = HazardClosurePort.NONE;

    @Autowired(required = false)
    public void setHazardClosurePort(HazardClosurePort port) {
        if (port != null) {
            this.hazardClosurePort = port;
        }
    }

    @Override
    public TIsHazTask selectTIsHazTaskById(Long id) {
        return this.isHazTaskMapper.selectById(id);
    }

    @Override
    public List<TIsHazTask> selectList(com.baomidou.mybatisplus.core.conditions.Wrapper<TIsHazTask> wrapper) {
        return this.isHazTaskMapper.selectList(wrapper);
    }

    @Override
    public List<TIsHazTaskLog> listLogs(Long taskId) {
        return this.isHazTaskLogMapper.selectList(new QueryWrapper<TIsHazTaskLog>()
                .eq("task_id", taskId).eq("del_flag", 0).orderByAsc("id"));
    }

    @Override
    public List<TIsHazTask> listDue(Date at) {
        // 待派与「派不出去」都继续捞（后者一旦有部门空出按置顶序再试）；已派/挂起/撤条不捞
        List<TIsHazTask> all = this.isHazTaskMapper.selectList(new QueryWrapper<TIsHazTask>()
                .eq("del_flag", 0)
                .in("status", HazTaskDispatchHandler.STATUS_WAIT,
                        HazTaskDispatchHandler.STATUS_STUCK));
        List<TIsHazTask> due = new ArrayList<>();
        for (TIsHazTask r : all) {
            // 到期/露面统一走 DeadlinePolicy 这一把尺子，扫描处与回写处不许另算
            if (DeadlinePolicy.shouldSurface(r.getDueAt(), r.getAmount(), at)) {
                due.add(r);
            }
        }
        return due;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public TIsHazTask generate(TIsHazTask bill) {
        if (bill == null || bill.getDueAt() == null) {
            throw new IllegalArgumentException("催办条必须给出整改期限");
        }
        bill.setDueAt(endOfDueDay(bill.getDueAt()));
        if (bill.getAmount() == null) {
            bill.setAmount(BigDecimal.ZERO);
        }
        if (bill.getUrgeCount() == null) {
            bill.setUrgeCount(0);
        }
        bill.setDelFlag(0);
        bill.setAcceptedAt(null);

        // 条子生成前：隐患已销号结案 / 负责部门撤并没着落 → 就地挂起写明原由
        String suspendReason = checkSuspendBeforeGenerate(bill);
        if (suspendReason != null) {
            bill.setStatus(HazTaskDispatchHandler.STATUS_SUSPENDED);
            bill.setSuspendReason(suspendReason);
        } else {
            bill.setStatus(HazTaskDispatchHandler.STATUS_WAIT);
            bill.setSuspendReason(null);
        }
        this.isHazTaskMapper.insert(bill);
        if (suspendReason != null) {
            TIsHazTaskLog log = HazTaskDispatchHandler.newLog(bill,
                    HazTaskDispatchHandler.ACT_SUSPEND, "GENERATE");
            log.setReason(suspendReason);
            this.isHazTaskLogMapper.insert(log);
        }
        return bill;
    }

    private String checkSuspendBeforeGenerate(TIsHazTask bill) {
        if (bill.getItemNo() != null && hazardClosurePort.isClosed(bill.getItemNo())) {
            return "条子生成前隐患已销号结案";
        }
        if (bill.getDeptId() == null) {
            return "未定下整改责任部门";
        }
        TSysDepartment dept = this.departmentMapper.selectById(bill.getDeptId());
        if (dept == null) {
            return "责任部门已撤并，无着落";
        }
        if (dept.getStatus() == null || dept.getStatus() != 1) {
            return "责任部门已停用撤并，无着落";
        }
        return null;
    }

    @Override
    public RoundResult runOnce(Date at) {
        Date now = at == null ? new Date() : at;

        String window = dispatchWindow.activeWindow(now);
        if (window == DispatchWindow.CLOSED) {
            // 夜里/法定节假日窗外静默：哪怕有到期条也原地不动，等下一轮窗口
            return RoundResult.closed("当前不在派发窗口内，本轮静默不派");
        }
        String roundNo = dispatchWindow.roundNo(now);

        // 本轮候选：窗口内已到露面档、仍待派的条
        List<TIsHazTask> due = listDue(now);

        RoundResult result = new RoundResult();
        result.setWindow(window);
        result.setRoundNo(roundNo);
        result.setHasWork(!due.isEmpty());
        if (due.isEmpty()) {
            result.setMessage("本轮无活");
        }

        HazTaskDispatchHandler.RoundContext ctx = buildContext(now, roundNo);
        // 逾期满 3 天还派不出去的顶到最前头，普通条子不得插队；同档按录入序（id）派，结果可复算
        due.sort(new Comparator<TIsHazTask>() {
            @Override
            public int compare(TIsHazTask a, TIsHazTask b) {
                boolean oa = DeadlinePolicy.overdueDays(a.getDueAt(), now)
                        >= HazTaskDispatchHandler.OVERDUE_PRIORITY_DAYS;
                boolean ob = DeadlinePolicy.overdueDays(b.getDueAt(), now)
                        >= HazTaskDispatchHandler.OVERDUE_PRIORITY_DAYS;
                if (oa != ob) {
                    return oa ? -1 : 1;
                }
                return a.getId().compareTo(b.getId());
            }
        });

        for (TIsHazTask r : due) {
            try {
                HazTaskDispatchHandler.Outcome outcome = dispatchHandler.dispatchOne(r.getId(), ctx);
                switch (outcome) {
                    case DISPATCHED:
                        result.setRoundDispatched(result.getRoundDispatched() + 1);
                        break;
                    case URGED:
                        result.setRoundUrged(result.getRoundUrged() + 1);
                        break;
                    case STUCK:
                        result.setRoundUrged(result.getRoundUrged() + 1);
                        result.setRoundStuck(result.getRoundStuck() + 1);
                        break;
                    default:
                        break;
                }
            } catch (Exception ex) {
                // 卡一条不拖垮整轮：该条独立事务已回滚，下轮还能再捞
                result.setMessage((result.getMessage() == null ? "" : result.getMessage() + "；")
                        + "条[" + r.getItemNo() + "]派发失败已跳过:" + ex.getMessage());
            }
        }

        // 一屏三数：轮末对库存做同一次点算（一条 GROUP BY），屏上数与库里数同源
        tallyInventory(result);
        if (result.getMessage() == null) {
            result.setMessage("本轮派发完成");
        }
        return result;
    }

    private HazTaskDispatchHandler.RoundContext buildContext(Date at, String roundNo) {
        List<TSysDepartment> depts = this.departmentMapper.selectList(
                new QueryWrapper<TSysDepartment>().eq("status", 1));
        List<Long> activeIds = new ArrayList<>();
        for (TSysDepartment d : depts) {
            activeIds.add(d.getId());
        }
        // 派发先看各责任部门手上压着的活：已派出且未销项的条数即在手负载
        Map<Long, Long> workload = new HashMap<>();
        for (Long id : activeIds) {
            workload.put(id, 0L);
        }
        List<Map<String, Object>> rows = this.isHazTaskMapper.selectMaps(
                new QueryWrapper<TIsHazTask>()
                        .select("dept_id", "count(*) as cnt")
                        .eq("del_flag", 0)
                        .eq("status", HazTaskDispatchHandler.STATUS_DISPATCHED)
                        .groupBy("dept_id"));
        for (Map<String, Object> raw : rows) {
            Map<String, Object> row = lowerKeys(raw);
            Object deptVal = row.get("dept_id");
            if (deptVal == null) {
                continue;
            }
            Long deptId = ((Number) deptVal).longValue();
            if (workload.containsKey(deptId)) {
                workload.put(deptId, ((Number) row.get("cnt")).longValue());
            }
        }
        return new HazTaskDispatchHandler.RoundContext(at, roundNo, activeIds, workload);
    }

    /** selectMaps 列名大小写随驱动走（H2 大写、MySQL 原样），统一取小写键。 */
    private static Map<String, Object> lowerKeys(Map<String, Object> row) {
        Map<String, Object> normalized = new HashMap<>();
        for (Map.Entry<String, Object> e : row.entrySet()) {
            normalized.put(e.getKey() == null ? null : e.getKey().toLowerCase(), e.getValue());
        }
        return normalized;
    }

    /** 同一次点算库存：待派（0，含逾期落「派不出去」2 这一待派特例）/已派(1)/挂起(3)；撤条 del_flag=1 不计 */
    private void tallyInventory(RoundResult result) {
        List<Map<String, Object>> rows = this.isHazTaskMapper.selectMaps(
                new QueryWrapper<TIsHazTask>()
                        .select("status", "count(*) as cnt")
                        .eq("del_flag", 0)
                        .groupBy("status"));
        int waiting = 0;
        int dispatched = 0;
        int suspended = 0;
        for (Map<String, Object> raw : rows) {
            Map<String, Object> row = lowerKeys(raw);
            int status = ((Number) row.get("status")).intValue();
            int cnt = ((Number) row.get("cnt")).intValue();
            if (status == HazTaskDispatchHandler.STATUS_WAIT
                    || status == HazTaskDispatchHandler.STATUS_STUCK) {
                waiting += cnt;
            } else if (status == HazTaskDispatchHandler.STATUS_DISPATCHED) {
                dispatched = cnt;
            } else if (status == HazTaskDispatchHandler.STATUS_SUSPENDED) {
                suspended = cnt;
            }
        }
        result.setWaitingCount(waiting);
        result.setDispatchedCount(dispatched);
        result.setSuspendedCount(suspended);
    }

    @Override
    public boolean adjustLeadDays(Long id, BigDecimal leadDays) {
        if (id == null || leadDays == null || leadDays.signum() < 0) {
            return false;
        }
        TIsHazTask r = this.isHazTaskMapper.selectById(id);
        if (r == null || r.getDelFlag() != 0
                || r.getStatus() == HazTaskDispatchHandler.STATUS_SUSPENDED) {
            return false;
        }
        // 只改「还剩几日」这一档；期限那一刻（due_at）绝不动
        TIsHazTask patch = new TIsHazTask();
        patch.setId(id);
        patch.setAmount(leadDays);
        return this.isHazTaskMapper.updateById(patch) > 0;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public boolean cancel(Long id, String reason) {
        TIsHazTask r = this.isHazTaskMapper.selectById(id);
        if (r == null || r.getDelFlag() != 0
                || r.getStatus() == HazTaskDispatchHandler.STATUS_SUSPENDED) {
            return false;
        }
        // 撤条先留痕：最初整改期限与真正派出时刻快照进留痕
        TIsHazTaskLog log = HazTaskDispatchHandler.newLog(r, HazTaskDispatchHandler.ACT_CANCEL, null);
        log.setFromDeptId(r.getDeptId());
        log.setReason(reason);
        this.isHazTaskLogMapper.insert(log);

        // 软删撤条，条与留痕都在；不动 due_at / accepted_at
        TIsHazTask patch = new TIsHazTask();
        patch.setId(id);
        patch.setDelFlag(1);
        return this.isHazTaskMapper.updateById(patch) > 0;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public TIsHazTask transfer(Long id, Long toDeptId, String reason) {
        if (toDeptId == null) {
            return null;
        }
        TIsHazTask r = this.isHazTaskMapper.selectById(id);
        if (r == null || r.getDelFlag() != 0
                || r.getStatus() == HazTaskDispatchHandler.STATUS_SUSPENDED
                || r.getStatus() == HazTaskDispatchHandler.STATUS_DISPATCHED) {
            // 派不出去/待派可转办；已派条改责任部门走线下退单，不在此口
            return null;
        }
        TSysDepartment target = this.departmentMapper.selectById(toDeptId);
        if (target == null || target.getStatus() == null || target.getStatus() != 1) {
            return null;
        }

        Long fromDept = r.getDeptId();
        // 首派时刻原样保留；若此前从未派出，此刻才真正派出
        Date acceptedAt = r.getAcceptedAt() == null ? new Date() : r.getAcceptedAt();
        TIsHazTaskLog log = HazTaskDispatchHandler.newLog(r, HazTaskDispatchHandler.ACT_TRANSFER, null);
        log.setFromDeptId(fromDept);
        log.setToDeptId(toDeptId);
        log.setAcceptedAt(acceptedAt);
        log.setReason(reason);
        this.isHazTaskLogMapper.insert(log);

        r.setDeptId(toDeptId);
        r.setAcceptedAt(acceptedAt);
        r.setStatus(HazTaskDispatchHandler.STATUS_DISPATCHED);
        this.isHazTaskMapper.updateById(r);
        return r;
    }

    /** 期限统一口径：无论传入几点，一律规整成「整改期限那一日的最后一刻」23:59:59（GMT+8）。 */
    private static Date endOfDueDay(Date dueAt) {
        LocalDate day = dueAt.toInstant().atZone(DeadlinePolicy.ZONE).toLocalDate();
        LocalDateTime end = LocalDateTime.of(day, LocalTime.of(23, 59, 59));
        return Date.from(end.atZone(DeadlinePolicy.ZONE).toInstant());
    }
}
