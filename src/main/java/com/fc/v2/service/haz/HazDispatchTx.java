package com.fc.v2.service.haz;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TIsDispatchDeptMapper;
import com.fc.v2.mapper.auto.TIsDispatchLogMapper;
import com.fc.v2.mapper.auto.TIsDispatchRoundMapper;
import com.fc.v2.mapper.auto.TIsHazTaskMapper;
import com.fc.v2.mapper.auto.TIsHazardMapper;
import com.fc.v2.model.auto.TIsDispatchDept;
import com.fc.v2.model.auto.TIsDispatchLog;
import com.fc.v2.model.auto.TIsDispatchRound;
import com.fc.v2.model.auto.TIsDispatchWindow;
import com.fc.v2.model.auto.TIsHazTask;
import com.fc.v2.model.auto.TIsHazard;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 催办派发一轮的事务执行器。
 *
 * 单开一个 Bean：让 {@link Transactional} 经代理生效（同类自调不生效）。
 * 一轮里的全部落库——当日幂等、生成/挂起、接单、留痕、轮次点算——在同一事务内，
 * 任何一条抛错整轮回滚；唯独"单条失败跳过继续"的场景在循环内就地兜住，不连累整轮。
 *
 * @author fuce
 * @date 2026-09-30
 */
@Component
public class HazDispatchTx {

    @Resource
    private TIsHazTaskMapper billMapper;
    @Resource
    private TIsHazardMapper hazardMapper;
    @Resource
    private TIsDispatchDeptMapper deptMapper;
    @Resource
    private TIsDispatchRoundMapper roundMapper;
    @Resource
    private TIsDispatchLogMapper logMapper;

    /**
     * 窗口已由外层判过，这里在事务内执行完整一轮。
     *
     * @return SCANNED / NO_WORK / SKIPPED 三态之一（CLOSED、ERROR 在外层处理）
     */
    @Transactional(rollbackFor = Exception.class)
    public RoundScreen execute(Date at, TIsDispatchWindow win) {
        Date day = DeadlineRuler.startOfDay(at);

        // 1) 当日该窗口只派一次：二次进来直接忽略，先前那次原样留着。
        TIsDispatchRound prior = findRound(day, win.getWinType());
        if (prior != null) {
            RoundScreen s = new RoundScreen();
            s.setState(RoundScreen.State.SKIPPED);
            s.setMessage("当日[" + win.getWinName() + "]已扫过一轮，二次进入直接忽略，先前派发原样保留");
            s.setWinType(prior.getWinType());
            s.setWinName(prior.getWinName());
            s.setStartedAt(prior.getStartedAt());
            s.setFinishedAt(prior.getFinishedAt());
            s.setWaitCnt(nz(prior.getWaitCnt()));
            s.setDispatchedCnt(nz(prior.getDispatchedCnt()));
            s.setSuspendedCnt(nz(prior.getSuspendedCnt()));
            s.setRoundId(prior.getId());
            return s;
        }

        // 2) 轮次先建档（同一事务，保证屏与库同一次点算）。
        TIsDispatchRound round = new TIsDispatchRound();
        round.setRoundDate(day);
        round.setWinType(win.getWinType());
        round.setWinName(win.getWinName());
        round.setStartedAt(at);
        round.setWaitCnt(0);
        round.setDispatchedCnt(0);
        round.setSuspendedCnt(0);
        roundMapper.insert(round);

        // 3) 生成新条：到露面档的隐患开催办条；已销号/部门撤并的就地挂起。
        List<TIsHazTask> created = generate(at, round.getId());
        int suspended = 0;
        for (TIsHazTask b : created) {
            if (nz(b.getStatus()) == HazConst.BILL_SUSPENDED) {
                suspended++;
            }
        }

        // 4) 捞本轮候选：待派 / 派不出去、到露面档的条子。这一轮派过（已派）的不会再进。
        List<TIsHazTask> candidates = billMapper.selectList(new QueryWrapper<TIsHazTask>()
                .eq("del_flag", 0)
                .in("status", HazConst.BILL_WAIT, HazConst.BILL_STUCK));
        List<TIsHazTask> due = new ArrayList<>();
        for (TIsHazTask b : candidates) {
            if (DeadlineRuler.visible(b.getAmount(), b.getDueAt(), at)) {
                due.add(b);
            }
        }

        RoundScreen screen = new RoundScreen();
        screen.setStartedAt(at);
        screen.setWinType(win.getWinType());
        screen.setWinName(win.getWinName());
        screen.setRoundId(round.getId());
        screen.setSuspendedCnt(suspended);

        // 挂起数先回写轮次：哪怕本轮无活可派，屏与库也必须同一次点算、一个不差。
        round.setSuspendedCnt(suspended);

        if (due.isEmpty()) {
            // 本轮确实没有到期的活——正常结果，绝不能跟"系统出错"混成一个意思。
            round.setResultType(HazConst.ROUND_NO_WORK);
            round.setFinishedAt(at);
            String msg = "本轮无活：当前窗口内没有到期露面的催办条";
            round.setMessage(msg);
            roundMapper.updateById(round);
            screen.setState(RoundScreen.State.NO_WORK);
            screen.setMessage(msg);
            screen.setFinishedAt(at);
            return screen;
        }

        // 5) 排序：过期限三日派不出去的顶到最前头；同档内期限早的先，普通条子不得插队。
        Collections.sort(due, new Comparator<TIsHazTask>() {
            @Override
            public int compare(TIsHazTask a, TIsHazTask b) {
                boolean topA = DeadlineRuler.overdueAtLeast(a.getDueAt(), at, HazConst.OVERDUE_TOP_DAYS)
                        && nz(a.getStatus()) == HazConst.BILL_STUCK;
                boolean topB = DeadlineRuler.overdueAtLeast(b.getDueAt(), at, HazConst.OVERDUE_TOP_DAYS)
                        && nz(b.getStatus()) == HazConst.BILL_STUCK;
                if (topA != topB) {
                    return topA ? -1 : 1;
                }
                int byDue = a.getDueAt().compareTo(b.getDueAt());
                if (byDue != 0) {
                    return byDue;
                }
                return Long.compare(a.getId(), b.getId());
            }
        });

        int dispatched = 0;
        int left = 0;
        // 本轮已派的在手活计数跨条累计，让多条同科时也能摊开给空部门。
        Map<Long, Integer> roundLoad = new HashMap<>();
        for (TIsHazTask b : due) {
            try {
                if (dispatchOne(b, at, round.getId(), roundLoad)) {
                    dispatched++;
                } else {
                    left++;
                }
            } catch (RuntimeException ex) {
                // 卡一条不拖垮整轮：本条留待派/派不出去，下一轮再捞，整轮其余照走。
                left++;
                log(b, round.getId(), HazConst.ACT_URGE, nz(b.getStatus()), nz(b.getStatus()),
                        null, null, "本轮派出受阻，留待下一轮：" + ex.getMessage(), at);
            }
        }

        // 6) 同一次点算回写轮次（屏上三个数就是库里这三个数）。
        round.setDispatchedCnt(dispatched);
        round.setSuspendedCnt(suspended);
        round.setWaitCnt(left);
        round.setResultType(HazConst.ROUND_SCANNED);
        round.setFinishedAt(at);
        String msg = "本轮扫描完成：待派 " + left + " 条、已派 " + dispatched + " 条、挂起 " + suspended + " 条";
        round.setMessage(msg);
        roundMapper.updateById(round);

        screen.setState(RoundScreen.State.SCANNED);
        screen.setMessage(msg);
        screen.setDispatchedCnt(dispatched);
        screen.setWaitCnt(left);
        screen.setFinishedAt(at);
        return screen;
    }

    /** 只生成一轮条子（手工"生成催办条"入口），独立事务，不写轮次点算。 */
    @Transactional(rollbackFor = Exception.class)
    public List<TIsHazTask> generateOnly(Date at) {
        return generate(at, null);
    }

    /**
     * 撤条：期限过了仍派不出去的唯一出路之一。留痕——最初整改日、最初真正派出时刻
     * 原样抄进日志，条上的 due_at / first_dispatched_at 一个都不许被后到动作覆盖。
     */
    @Transactional(rollbackFor = Exception.class)
    public TIsHazTask revoke(Long id, String reason, Date at) {
        TIsHazTask b = mustGet(id);
        int st = nz(b.getStatus());
        if (st != HazConst.BILL_WAIT && st != HazConst.BILL_STUCK) {
            throw new IllegalStateException("只有待派/派不出去的条子能撤，当前情形：" + HazConst.statusText(st));
        }
        if (!DeadlineRuler.isDue(b.getDueAt(), at)) {
            throw new IllegalStateException("期限未过，不允许撤条（期限过了仍派不出去才可撤）");
        }
        b.setStatus(HazConst.BILL_REVOKED);
        b.setUpdateTime(at);
        billMapper.updateById(b);
        log(b, null, HazConst.ACT_REVOKE, st, HazConst.BILL_REVOKED,
                b.getAcceptDeptId(), b.getAcceptDeptName(),
                "撤条留痕，原由：" + reason + "；最初整改期限与初派时刻原样保留", at);
        return b;
    }

    /** 转办：期限过了仍派不出去的另一条出路，原子指派给指定在岗部门（情形转办）。 */
    @Transactional(rollbackFor = Exception.class)
    public TIsHazTask transfer(Long id, Long toDeptId, String reason, Date at) {
        TIsHazTask b = mustGet(id);
        int st = nz(b.getStatus());
        if (st != HazConst.BILL_WAIT && st != HazConst.BILL_STUCK) {
            throw new IllegalStateException("只有待派/派不出去的条子能转办，当前情形：" + HazConst.statusText(st));
        }
        if (!DeadlineRuler.isDue(b.getDueAt(), at)) {
            throw new IllegalStateException("期限未过，不允许转办（期限过了仍派不出去才可转）");
        }
        TIsDispatchDept dept = deptMapper.selectById(toDeptId);
        if (dept == null || nz(dept.getStatus()) == 1 || nz(dept.getDelFlag()) == 1) {
            throw new IllegalArgumentException("目标部门不存在或已撤并，不能转办");
        }
        int rows = billMapper.assign(b.getId(), dept.getId(), dept.getDeptName(),
                at, HazConst.BILL_TRANSFERRED, 0);
        if (rows == 0) {
            throw new IllegalStateException("转办未生效：条子已被别的动作先动过");
        }
        b = mustGet(id);
        log(b, null, HazConst.ACT_TRANSFER, st, HazConst.BILL_TRANSFERRED,
                dept.getId(), dept.getDeptName(),
                "转办原由：" + reason + "；最初整改期限与初派时刻原样保留", at);
        return b;
    }

    /** 调档：只许动"还剩几日露面"（amount）；期限那一刻（due_at）动不得。 */
    @Transactional(rollbackFor = Exception.class)
    public TIsHazTask tuneLeadDays(Long id, BigDecimal leadDays, Date at) {
        if (leadDays == null || leadDays.signum() < 0) {
            throw new IllegalArgumentException("提前露面日数必须是不小于 0 的数");
        }
        TIsHazTask b = mustGet(id);
        BigDecimal old = b.getAmount();
        b.setAmount(leadDays);
        // 显式只回写 amount：哪怕实体别处被带脏，due_at 也绝不在更新列里。
        int rows = billMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<TIsHazTask>()
                .eq("id", id)
                .set("amount", leadDays)
                .set("update_time", at));
        if (rows == 0) {
            throw new IllegalStateException("调档未生效：条子不存在");
        }
        log(b, null, HazConst.ACT_TUNE, nz(b.getStatus()), nz(b.getStatus()),
                null, null, "还剩几日露面档：" + old + " → " + leadDays + "（期限一刻未动）", at);
        return mustGet(id);
    }

    private TIsHazTask mustGet(Long id) {
        TIsHazTask b = billMapper.selectById(id);
        if (b == null || nz(b.getDelFlag()) == 1) {
            throw new IllegalArgumentException("催办条不存在：" + id);
        }
        return b;
    }

    /** 生成条子（含挂起）。返回本次生成的条子；挂起条的情形为 3。单条异常就地兜住。 */
    private List<TIsHazTask> generate(Date at, Long roundId) {
        List<TIsHazTask> created = new ArrayList<>();
        List<TIsHazard> hazards = hazardMapper.selectList(new QueryWrapper<TIsHazard>()
                .eq("del_flag", 0));
        Set<String> billed = new HashSet<>();
        for (TIsHazTask b : billMapper.selectList(new QueryWrapper<TIsHazTask>()
                .eq("del_flag", 0)
                .select("DISTINCT item_no"))) {
            if (b.getItemNo() != null) {
                billed.add(b.getItemNo());
            }
        }

        int seq = (int) billMapper.selectCount(new QueryWrapper<TIsHazTask>()
                .likeRight("bill_no", "CB" + new SimpleDateFormat("yyyyMMdd").format(at)));
        for (TIsHazard h : hazards) {
            if (h.getRectifyDate() == null || billed.contains(h.getHazardNo())) {
                continue;
            }
            Date deadline = DeadlineRuler.deadlineOf(h.getRectifyDate());
            if (!DeadlineRuler.visible(BigDecimal.valueOf(HazConst.DEFAULT_LEAD_DAYS), deadline, at)) {
                continue;
            }
            try {
                TIsHazTask b = new TIsHazTask();
                b.setItemNo(h.getHazardNo());
                b.setBillNo(nextBillNo(at, ++seq));
                b.setDueAt(deadline);
                b.setAmount(BigDecimal.valueOf(HazConst.DEFAULT_LEAD_DAYS));
                b.setHazardSummary(h.getSummary());
                b.setDeptGroup(h.getDeptGroup());
                b.setUrgeRounds(0);
                b.setCreateTime(at);
                b.setUpdateTime(at);

                TIsDispatchDept pin = h.getPinDeptId() == null ? null
                        : deptMapper.selectById(h.getPinDeptId());
                if (pin != null) {
                    b.setPinDeptId(pin.getId());
                    b.setPinDeptName(pin.getDeptName());
                }
                boolean closed = nz(h.getClosed()) == 1;
                // "没了着落"只认整条线再无在岗部门：原定部门没了、但同条线还有在岗科的，照常按权重派。
                boolean noActiveDept = activeDepts(h.getDeptGroup()).isEmpty();
                if (closed || noActiveDept) {
                    String reason;
                    if (closed) {
                        reason = "条子生成前隐患已销号结案，就地挂起";
                    } else {
                        reason = "责任条线已无在岗部门（原定部门"
                                + (pin == null ? "无记载" : pin.getDeptName()) + "，已整组撤并），没了着落，就地挂起";
                    }
                    b.setStatus(HazConst.BILL_SUSPENDED);
                    b.setSuspendReason(reason);
                    billMapper.insert(b);
                    log(b, roundId, HazConst.ACT_SUSPEND, null, HazConst.BILL_SUSPENDED,
                            h.getPinDeptId(), pin == null ? null : pin.getDeptName(), reason, at);
                    created.add(b);
                } else {
                    b.setStatus(HazConst.BILL_WAIT);
                    b.setContent(h.getSummary());
                    billMapper.insert(b);
                    log(b, roundId, HazConst.ACT_CREATE, null, HazConst.BILL_WAIT,
                            pin.getId(), pin.getDeptName(), "按限期整改要求生成催办条", at);
                    created.add(b);
                }
            } catch (RuntimeException ex) {
                // 单条生成失败不拖垮整批/整轮。
                TIsDispatchLog l = baseLog(roundId, null, null, HazConst.ACT_CREATE, at);
                l.setDetail("催办条生成失败已跳过：" + ex.getMessage());
                logMapper.insert(l);
            }
        }
        return created;
    }

    /**
     * 派一条：在手活最少的在岗部门先接（活越多权重越低）；同活数次序小的先。
     * 接单走原子 SQL，情形翻已派与接手时刻同写，缺一（0 行）不算派成。
     *
     * @return true 已派出；false 候选池空，已记派不出去
     */
    private boolean dispatchOne(TIsHazTask bill, Date at, Long roundId, Map<Long, Integer> roundLoad) {
        List<TIsDispatchDept> pool = activeDepts(bill.getDeptGroup());
        if (pool.isEmpty()) {
            billMapper.markStuck(bill.getId(), at);
            log(bill, roundId, HazConst.ACT_URGE, nz(bill.getStatus()), HazConst.BILL_STUCK,
                    null, null, "责任条线无在岗部门，派不出去，顶档待派", at);
            return false;
        }
        // 在手活 = 库里既有压活 + 本轮已派给该科的条数。
        TIsDispatchDept picked = null;
        int minLoad = Integer.MAX_VALUE;
        for (TIsDispatchDept d : pool) {
            int load = billMapper.countWorkload(d.getId())
                    + (roundLoad.containsKey(d.getId()) ? roundLoad.get(d.getId()) : 0);
            if (load < minLoad
                    || (load == minLoad && picked != null && nz(d.getOrderNum()) < nz(picked.getOrderNum()))) {
                minLoad = load;
                picked = d;
            }
        }
        int from = nz(bill.getStatus());
        int rows = billMapper.assign(bill.getId(), picked.getId(), picked.getDeptName(),
                at, HazConst.BILL_DISPATCHED, 1);
        if (rows == 0) {
            // 被并发轮次/人工先动过：这单不算派成。
            return false;
        }
        roundLoad.put(picked.getId(), (roundLoad.containsKey(picked.getId()) ? roundLoad.get(picked.getId()) : 0) + 1);
        log(bill, roundId, HazConst.ACT_DISPATCH, from, HazConst.BILL_DISPATCHED,
                picked.getId(), picked.getDeptName(),
                "接单部门=" + picked.getDeptName() + "，在手活=" + minLoad + "，权重最低先接", at);
        return true;
    }

    private List<TIsDispatchDept> activeDepts(String group) {
        if (group == null) {
            return new ArrayList<>();
        }
        List<TIsDispatchDept> list = deptMapper.selectList(new QueryWrapper<TIsDispatchDept>()
                .eq("del_flag", 0)
                .eq("status", 0)
                .eq("dept_group", group)
                .orderByAsc("order_num", "id"));
        return list == null ? new ArrayList<TIsDispatchDept>() : list;
    }

    private TIsDispatchRound findRound(Date day, int winType) {
        Date next = new Date(day.getTime() + 24L * 60 * 60 * 1000);
        List<TIsDispatchRound> list = roundMapper.selectList(new QueryWrapper<TIsDispatchRound>()
                .eq("win_type", winType)
                .ge("round_date", day)
                .lt("round_date", next)
                .last("limit 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    private String nextBillNo(Date at, int seq) {
        return "CB" + new SimpleDateFormat("yyyyMMdd").format(at)
                + "-" + String.format("%04d", seq);
    }

    private void log(TIsHazTask b, Long roundId, String action, Integer from, Integer to,
                     Long deptId, String deptName, String detail, Date at) {
        TIsDispatchLog l = baseLog(roundId, b.getId(), b.getBillNo(), action, at);
        l.setFromStatus(from);
        l.setToStatus(to);
        l.setDeptId(deptId);
        l.setDeptName(deptName);
        l.setDetail(detail);
        // 撤条留痕的口径平时也照抄：期限与初派时刻从条上原样带出，绝不改写。
        l.setOrigDueAt(b.getDueAt());
        l.setFirstDispatchedAt(b.getFirstDispatchedAt());
        logMapper.insert(l);
    }

    private TIsDispatchLog baseLog(Long roundId, Long billId, String billNo, String action, Date at) {
        TIsDispatchLog l = new TIsDispatchLog();
        l.setRoundId(roundId);
        l.setBillId(billId);
        l.setBillNo(billNo);
        l.setAction(action);
        l.setCreateBy("Task");
        l.setCreateTime(at);
        return l;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
