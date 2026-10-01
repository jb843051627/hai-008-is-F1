package com.fc.v2.service.haz;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TIsDispatchDeptMapper;
import com.fc.v2.mapper.auto.TIsDispatchLogMapper;
import com.fc.v2.mapper.auto.TIsDispatchRoundMapper;
import com.fc.v2.mapper.auto.TIsDispatchWindowMapper;
import com.fc.v2.mapper.auto.TIsHazTaskMapper;
import com.fc.v2.mapper.auto.TIsHazardMapper;
import com.fc.v2.mapper.auto.TIsHolidayMapper;
import com.fc.v2.model.auto.TIsDispatchDept;
import com.fc.v2.model.auto.TIsDispatchLog;
import com.fc.v2.model.auto.TIsDispatchRound;
import com.fc.v2.model.auto.TIsDispatchWindow;
import com.fc.v2.model.auto.TIsHazTask;
import com.fc.v2.model.auto.TIsHazard;
import com.fc.v2.model.auto.TIsHoliday;
import com.fc.v2.service.impl.TIsHazTaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 催办派发链路 H2 集成测试（真实 SQL：唯一键、原子接单 UPDATE、同事务点算）。
 *
 * @author fuce
 * @date 2026-09-30
 */
@SpringJUnitConfig(classes = {HazTestConfig.class, HazDispatchTx.class, TIsHazTaskServiceImpl.class})
class HazDispatchFlowTest {

    static {
        // 库连接与判定都按 GMT+8 墙钟
        TimeZone.setDefault(DeadlineRuler.ZONE);
    }

    @Autowired TIsHazTaskMapper billMapper;
    @Autowired TIsHazardMapper hazardMapper;
    @Autowired TIsDispatchDeptMapper deptMapper;
    @Autowired TIsDispatchWindowMapper windowMapper;
    @Autowired TIsHolidayMapper holidayMapper;
    @Autowired TIsDispatchRoundMapper roundMapper;
    @Autowired TIsDispatchLogMapper logMapper;
    @Autowired HazDispatchTx tx;
    @Autowired TIsHazTaskServiceImpl service;

    private static long seq = 1;

    @BeforeEach
    void clean() {
        // 每个用例独立数据（测试方法本身不带事务回滚，这里显式清表）
        logMapper.delete(new QueryWrapper<TIsDispatchLog>());
        roundMapper.delete(new QueryWrapper<TIsDispatchRound>());
        billMapper.delete(new QueryWrapper<TIsHazTask>());
        hazardMapper.delete(new QueryWrapper<TIsHazard>());
        deptMapper.delete(new QueryWrapper<TIsDispatchDept>());
        windowMapper.delete(new QueryWrapper<TIsDispatchWindow>());
        holidayMapper.delete(new QueryWrapper<TIsHoliday>());
        seedWindowsAndDepts();
    }

    private void seedWindowsAndDepts() {
        TIsDispatchWindow daily = new TIsDispatchWindow();
        daily.setId(1L); daily.setWinType(1); daily.setWinName("日常窗口");
        daily.setOpenHh(8); daily.setCloseHh(20); daily.setStatus(0); daily.setDelFlag(0);
        windowMapper.insert(daily);
        TIsDispatchWindow special = new TIsDispatchWindow();
        special.setId(2L); special.setWinType(2); special.setWinName("专项检查月窗口");
        special.setStartDate(date(2026, 6, 1)); special.setEndDate(date(2026, 6, 15));
        special.setOpenHh(9); special.setCloseHh(11); special.setStatus(0); special.setDelFlag(0);
        windowMapper.insert(special);

        dept(1001L, "危化一科", "HW", 0, 1);
        dept(1002L, "危化二科", "HW", 0, 2);
        dept(1003L, "执法大队", "HW", 0, 3);
        dept(2001L, "企管一所", "QY", 1, 1);
    }

    private void dept(long id, String name, String group, int status, int order) {
        TIsDispatchDept d = new TIsDispatchDept();
        d.setId(id); d.setDeptName(name); d.setDeptGroup(group);
        d.setStatus(status); d.setOrderNum(order); d.setDelFlag(0);
        deptMapper.insert(d);
    }

    private TIsHazard hazard(String no, String group, Long pinDept, int y, int m, int dd, int closed) {
        TIsHazard h = new TIsHazard();
        h.setId(10000L + seq++);
        h.setHazardNo(no); h.setSummary("隐患-" + no); h.setDeptGroup(group);
        h.setPinDeptId(pinDept); h.setRectifyDate(date(y, m, dd));
        h.setClosed(closed); h.setDelFlag(0);
        hazardMapper.insert(h);
        return h;
    }

    /** 直接插一条催办条（绕过生成） */
    private TIsHazTask bill(String no, String group, int y, int m, int dd, int status) {
        TIsHazTask b = new TIsHazTask();
        b.setId(20000L + seq++);
        b.setBillNo(no); b.setItemNo("IT-" + no);
        b.setDueAt(DeadlineRuler.deadlineOf(date(y, m, dd)));
        b.setAmount(BigDecimal.valueOf(3));
        b.setHazardSummary("隐患概况-" + no);
        b.setDeptGroup(group);
        b.setStatus(status); b.setUrgeRounds(0); b.setDelFlag(0);
        billMapper.insert(b);
        return b;
    }

    static Date date(int y, int m, int d) {
        Calendar c = Calendar.getInstance(DeadlineRuler.ZONE);
        c.clear(); c.set(y, m - 1, d);
        return c.getTime();
    }
    static Date ts(int y, int m, int d, int hh, int mi) {
        Calendar c = Calendar.getInstance(DeadlineRuler.ZONE);
        c.clear(); c.set(y, m - 1, d, hh, mi);
        return c.getTime();
    }

    private long countByStatus(int st) {
        return billMapper.selectCount(new QueryWrapper<TIsHazTask>().eq("status", st).eq("del_flag", 0));
    }

    // ---------- 1. 生成即派：待派→已派，接手时刻+初派时刻同写，已派下轮不再捞 ----------

    @Test
    void generateAndDispatchHappyPath() {
        hazard("H1", "HW", 1001L, 2026, 9, 30, 0);
        // 9/28 10:00，距期限 2 日 ≤ 提前 3 日 → 生成并派出
        RoundScreen s = service.runOnce(ts(2026, 9, 28, 10, 0));
        assertEquals(RoundScreen.State.SCANNED, s.getState());
        assertEquals(1, s.getDispatchedCnt());
        assertEquals(0, s.getWaitCnt());

        TIsHazTask b = billMapper.selectList(new QueryWrapper<TIsHazTask>()).get(0);
        assertEquals(HazConst.BILL_DISPATCHED, b.getStatus());
        assertEquals(1001L, b.getAcceptDeptId());
        assertNotNull(b.getAcceptedAt(), "接手时刻必须写上");
        assertNotNull(b.getFirstDispatchedAt(), "初派时刻必须落一次");
        assertEquals(1, b.getUrgeRounds());
        assertEquals(ts(2026, 9, 28, 10, 0), b.getAcceptedAt());

        // 次日同窗口再扫：已派条子不得再当待办捞出 → 本轮无活
        RoundScreen s2 = service.runOnce(ts(2026, 9, 29, 10, 0));
        assertEquals(RoundScreen.State.NO_WORK, s2.getState());
        assertEquals(1L, countByStatus(HazConst.BILL_DISPATCHED));
    }

    // ---------- 2. 本轮无活 ≠ 系统出错 ≠ 窗口关闭 ----------

    @Test
    void noWorkClosedErrorAreDistinct() {
        // 无任何隐患：正常扫到无活
        RoundScreen noWork = service.runOnce(ts(2026, 9, 28, 10, 0));
        assertEquals(RoundScreen.State.NO_WORK, noWork.getState());
        assertTrue(noWork.getMessage().contains("本轮无活"));
        assertEquals(1L, (long) roundMapper.selectCount(new QueryWrapper<TIsDispatchRound>()
                .eq("result_type", HazConst.ROUND_NO_WORK)));

        // 夜里：窗口关闭，不留轮次、不算出错
        RoundScreen closed = service.runOnce(ts(2026, 9, 28, 23, 0));
        assertEquals(RoundScreen.State.CLOSED, closed.getState());
        assertEquals(1L, (long) roundMapper.selectCount(new QueryWrapper<TIsDispatchRound>()),
                "窗口外不写轮次");
    }

    // ---------- 3. 法定节假日整段静默（专项也不派） ----------

    @Test
    void holidaySilencesEverything() {
        hazard("H2", "HW", 1001L, 2026, 6, 2, 0);
        TIsHoliday hol = new TIsHoliday();
        hol.setId(9L); hol.setHolidayDate(date(2026, 6, 2)); hol.setHolidayName("节");
        holidayMapper.insert(hol);

        RoundScreen s = service.runOnce(ts(2026, 6, 2, 10, 0));
        assertEquals(RoundScreen.State.CLOSED, s.getState());
        assertEquals(0L, (long) billMapper.selectCount(new QueryWrapper<TIsHazTask>()),
                "静默期连条子都不生成");
    }

    // ---------- 4. 专项半月单独开窗，不走日常；专项钟点外不回退日常 ----------

    @Test
    void specialWindowIsolated() {
        hazard("H3", "HW", 1001L, 2026, 6, 3, 0);
        // 专项日 15 点：日常本开着，但专项钟点已关，也不回退 → 静默
        assertEquals(RoundScreen.State.CLOSED, service.runOnce(ts(2026, 6, 3, 15, 0)).getState());
        // 专项钟点内：走专项窗口
        RoundScreen s = service.runOnce(ts(2026, 6, 3, 10, 0));
        assertEquals(RoundScreen.State.SCANNED, s.getState());
        assertEquals(HazConst.WIN_SPECIAL, s.getWinType());
    }

    // ---------- 5. 当日同窗口二次进入直接忽略，先前派发原样保留 ----------

    @Test
    void sameWindowTwiceIsIgnited() {
        hazard("H4", "HW", 1001L, 2026, 9, 30, 0);
        RoundScreen first = service.runOnce(ts(2026, 9, 28, 10, 0));
        assertEquals(RoundScreen.State.SCANNED, first.getState());

        RoundScreen second = service.runOnce(ts(2026, 9, 28, 11, 0));
        assertEquals(RoundScreen.State.SKIPPED, second.getState());
        assertEquals(first.getRoundId(), second.getRoundId(), "先前那次原样留着");
        assertEquals(1, second.getDispatchedCnt());
        assertEquals(1L, (long) roundMapper.selectCount(new QueryWrapper<TIsDispatchRound>()),
                "当日同窗口只点算一次");
    }

    // ---------- 6. 权重：压活越多越靠后，空部门先接 ----------

    @Test
    void dispatchFavorsIdleDepartment() {
        // 给二科压 2 条在手里的活
        TIsHazTask load1 = bill("L1", "HW", 2026, 8, 1, HazConst.BILL_DISPATCHED);
        load1.setAcceptDeptId(1002L); billMapper.updateById(load1);
        TIsHazTask load2 = bill("L2", "HW", 2026, 8, 2, HazConst.BILL_DISPATCHED);
        load2.setAcceptDeptId(1002L); billMapper.updateById(load2);

        hazard("W1", "HW", null, 2026, 9, 30, 0);
        hazard("W2", "HW", null, 2026, 9, 30, 0);
        RoundScreen s = service.runOnce(ts(2026, 9, 28, 10, 0));
        assertEquals(2, s.getDispatchedCnt());

        // 权重：二科压 2 活排最后；空的一科、执法大队各接一单（次序 1 < 3）
        List<TIsHazTask> fresh = billMapper.selectList(new QueryWrapper<TIsHazTask>()
                .in("item_no", "W1", "W2"));
        java.util.Map<Long, Integer> perDept = new java.util.HashMap<>();
        for (TIsHazTask x : fresh) {
            perDept.merge(x.getAcceptDeptId(), 1, Integer::sum);
        }
        assertEquals(Integer.valueOf(1), perDept.get(1001L), "危化一科接第一单");
        assertEquals(Integer.valueOf(1), perDept.get(1003L), "执法大队接第二单（摊给空部门）");
        assertNull(perDept.get(1002L), "压活最多的二科一单不接");
    }

    // ---------- 7. 过期限三日派不出去的顶到最前，普通条不得插队 ----------

    @Test
    void overdueStuckBillsJumpToHead() {
        TIsHazTask top = bill("TOP", "HW", 2026, 9, 10, HazConst.BILL_STUCK);  // 逾期且卡单
        TIsHazTask normal = bill("N1", "HW", 2026, 9, 15, HazConst.BILL_WAIT); // 逾期但没卡单
        TIsHazTask later = bill("N2", "HW", 2026, 9, 28, HazConst.BILL_WAIT);
        // 9/28 跑轮：TOP 逾期 18 日且卡单，必须最先派
        RoundScreen s = service.runOnce(ts(2026, 9, 28, 10, 0));
        assertEquals(3, s.getDispatchedCnt());

        List<TIsDispatchLog> dispatches = logMapper.selectList(new QueryWrapper<TIsDispatchLog>()
                .eq("action", HazConst.ACT_DISPATCH).orderByAsc("id"));
        assertEquals(top.getId(), dispatches.get(0).getBillId(), "顶头单排第一");
        assertEquals(normal.getId(), dispatches.get(1).getBillId(), "普通条按期限排队");
        assertEquals(later.getId(), dispatches.get(2).getBillId());
    }

    // ---------- 8. 生成前已销号 / 部门整组撤并：就地挂起写明原由，不拖垮整轮 ----------

    @Test
    void suspendOnGenerateWithReason() {
        hazard("C1", "HW", 1002L, 2026, 9, 30, 1);   // 已销号结案
        hazard("C2", "QY", 2001L, 2026, 9, 30, 0);   // 条线只剩撤并部门
        hazard("C3", "HW", 1003L, 2026, 9, 30, 0);   // 正常

        RoundScreen s = service.runOnce(ts(2026, 9, 28, 10, 0));
        assertEquals(2, s.getSuspendedCnt());
        assertEquals(1, s.getDispatchedCnt());
        assertEquals(0, s.getWaitCnt());

        List<TIsHazTask> suspended = billMapper.selectList(new QueryWrapper<TIsHazTask>()
                .eq("status", HazConst.BILL_SUSPENDED));
        assertEquals(2, suspended.size());
        for (TIsHazTask b : suspended) {
            assertNotNull(b.getSuspendReason());
            assertTrue(b.getSuspendReason().contains("挂起"));
        }
        // 挂起条明细仍翻得到
        assertEquals(3L, service.listBills(null, ts(2026, 9, 28, 10, 0)).size());
    }

    // ---------- 9. 无在岗部门：记派不出去，下一轮仍捞出继续催 ----------

    @Test
    void stuckWithoutDeptGetsRePickedNextRound() {
        // 条子生成后责任条线被整组撤并：候选池空 → 派不出去，下一轮仍捞出继续催
        TIsHazTask b = bill("Z1", "ZZ", 2026, 9, 30, HazConst.BILL_WAIT);
        RoundScreen s1 = service.runOnce(ts(2026, 9, 28, 10, 0));
        assertEquals(0, s1.getDispatchedCnt());
        assertEquals(1, s1.getWaitCnt());
        TIsHazTask stuck = billMapper.selectOne(new QueryWrapper<TIsHazTask>().eq("item_no", "IT-Z1"));
        assertEquals(HazConst.BILL_STUCK, stuck.getStatus());
        assertEquals(1, stuck.getUrgeRounds());

        // 次日再扫：派不出去的仍当待办捞出，轮次+1
        RoundScreen s2 = service.runOnce(ts(2026, 9, 29, 10, 0));
        assertEquals(1, s2.getWaitCnt());
        stuck = billMapper.selectById(stuck.getId());
        assertEquals(2, stuck.getUrgeRounds());
    }

    // ---------- 10. 撤条留痕：期限一刻原样保留；期限没过不许撤 ----------

    @Test
    void revokeKeepsOriginalDeadlineAndAudits() {
        TIsHazTask b = bill("R1", "ZZ", 2026, 9, 10, HazConst.BILL_STUCK); // 无部门 → 卡单
        Date origDue = b.getDueAt();

        // 期限未过（9/5）不许撤
        assertThrows(IllegalStateException.class,
                () -> tx.revoke(b.getId(), "误操作", ts(2026, 9, 5, 9, 0)));

        // 期限过了（10/2）才撤
        TIsHazTask revoked = tx.revoke(b.getId(), "隐患重复立项，撤条", ts(2026, 10, 2, 9, 0));
        assertEquals(HazConst.BILL_REVOKED, revoked.getStatus());
        assertEquals(origDue, revoked.getDueAt(), "期限一刻不许被后到动作覆盖");

        TIsDispatchLog log = logMapper.selectOne(new QueryWrapper<TIsDispatchLog>()
                .eq("action", HazConst.ACT_REVOKE));
        assertNotNull(log);
        assertEquals(origDue, log.getOrigDueAt(), "留痕抄存最初整改日");
        assertTrue(log.getDetail().contains("撤条"));
    }

    // ---------- 11. 转办：原子接单，初派时刻不被覆盖 ----------

    @Test
    void transferAssignsAtomicallyAndKeepsDeadline() {
        TIsHazTask b = bill("T1", "HW", 2026, 9, 10, HazConst.BILL_STUCK);
        Date origDue = b.getDueAt();

        // 转给撤并部门：拒绝
        assertThrows(IllegalArgumentException.class,
                () -> tx.transfer(b.getId(), 2001L, "原部门撤并", ts(2026, 10, 2, 9, 0)));

        TIsHazTask t = tx.transfer(b.getId(), 1002L, "原部门撤并，转二科", ts(2026, 10, 2, 9, 0));
        assertEquals(HazConst.BILL_TRANSFERRED, t.getStatus());
        assertEquals(1002L, t.getAcceptDeptId());
        assertNotNull(t.getAcceptedAt(), "接手时刻必须同写");
        assertEquals(origDue, t.getDueAt());

        TIsDispatchLog log = logMapper.selectOne(new QueryWrapper<TIsDispatchLog>()
                .eq("action", HazConst.ACT_TRANSFER));
        assertEquals(origDue, log.getOrigDueAt());
    }

    // ---------- 12. 只许调"还剩几日"档，期限那一刻动不得；调后按新档露面 ----------

    @Test
    void tuneLeadDaysOnlyNeverMovesDeadline() {
        // 预置一张提前 3 日的待派条：9/24 时还剩 6 日，默认档不露面
        TIsHazTask b = bill("U1", "HW", 2026, 9, 30, HazConst.BILL_WAIT);
        Date origDue = b.getDueAt();
        assertEquals(RoundScreen.State.NO_WORK, service.runOnce(ts(2026, 9, 24, 10, 0)).getState());

        // 只把露面档调到 10 日：期限一刻不动
        TIsHazTask tuned = service.tuneLeadDays(b.getId(), BigDecimal.TEN);
        assertEquals(0, tuned.getAmount().compareTo(BigDecimal.TEN));
        assertEquals(origDue, tuned.getDueAt(), "期限一刻不动");

        assertThrows(IllegalArgumentException.class,
                () -> service.tuneLeadDays(b.getId(), BigDecimal.valueOf(-1)));

        // 调档次日再扫（当日窗口已扫过，只认次日的新轮）：还剩 5 日 ≤ 10 日，露面并派出
        RoundScreen s = service.runOnce(ts(2026, 9, 25, 9, 0));
        assertEquals(RoundScreen.State.SCANNED, s.getState());
        assertEquals(1, s.getDispatchedCnt());
        assertEquals(origDue, billMapper.selectById(b.getId()).getDueAt(), "派发后期限仍不动");

        // 调档留痕可查
        assertEquals(1L, (long) logMapper.selectCount(new QueryWrapper<TIsDispatchLog>()
                .eq("action", HazConst.ACT_TUNE)));
    }

    // ---------- 13. 一屏三个数与库底数同一次点算 ----------

    @Test
    void screenCountsMatchDatabaseSnapshot() {
        hazard("S1", "HW", null, 2026, 9, 30, 1);   // 已销号 → 挂起
        hazard("S2", "HW", null, 2026, 9, 30, 0);   // 派出
        bill("S3", "ZZ", 2026, 9, 30, HazConst.BILL_WAIT); // 条线无在岗部门 → 卡单待派
        RoundScreen screen = service.runOnce(ts(2026, 9, 28, 10, 0));

        TIsDispatchRound round = roundMapper.selectById(screen.getRoundId());
        assertEquals(round.getWaitCnt().intValue(), screen.getWaitCnt());
        assertEquals(round.getDispatchedCnt().intValue(), screen.getDispatchedCnt());
        assertEquals(round.getSuspendedCnt().intValue(), screen.getSuspendedCnt());
        assertEquals(1, screen.getWaitCnt());
        assertEquals(1, screen.getDispatchedCnt());
        assertEquals(1, screen.getSuspendedCnt());
        // 与库底实时计数也对得上
        assertEquals(1L, countByStatus(HazConst.BILL_WAIT) + countByStatus(HazConst.BILL_STUCK));
        assertEquals(1L, countByStatus(HazConst.BILL_DISPATCHED));
        assertEquals(1L, countByStatus(HazConst.BILL_SUSPENDED));
        assertFalse(round.getMessage().isEmpty());
    }

    // ---------- 14. 翻条的剩余几日与派发同一把尺 ----------

    @Test
    void listingDaysLeftUsesSameRuler() {
        hazard("D1", "HW", 1001L, 2026, 9, 30, 0);
        service.runOnce(ts(2026, 9, 28, 10, 0));
        List<TIsHazTask> list = service.listBills(null, ts(2026, 9, 29, 12, 0));
        assertEquals(1, list.size());
        assertEquals(1L, list.get(0).getDaysLeft(), "9/29 看 9/30 期限：还剩 1 日");
    }
}
