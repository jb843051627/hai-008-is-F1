package com.fc.v2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSession;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.util.ReflectionTestUtils;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fc.v2.mapper.auto.TIsHazTaskLogMapper;
import com.fc.v2.mapper.auto.TIsHazTaskMapper;
import com.fc.v2.mapper.auto.TSysDepartmentMapper;
import com.fc.v2.model.auto.TIsHazTask;
import com.fc.v2.model.auto.TIsHazTaskLog;
import com.fc.v2.service.dispatch.DeadlinePolicy;
import com.fc.v2.service.dispatch.DispatchCalendar;
import com.fc.v2.service.dispatch.DispatchWindow;
import com.fc.v2.service.dispatch.HazardClosurePort;
import com.fc.v2.service.dispatch.RoundResult;
import com.fc.v2.service.impl.HazTaskDispatchHandler;
import com.fc.v2.service.impl.TIsHazTaskServiceImpl;

/**
 * 催办条派发全链路测试（H2 MySQL 兼容模式），不起整个 Spring 容器。
 *
 * @author fuce
 * @date 2026-09-30
 */
class HazTaskDispatchTest {

    private static final ZoneId ZONE = ZoneId.of("GMT+8");

    private DataSource dataSource;
    private SqlSession session;
    private TIsHazTaskMapper taskMapper;
    private TIsHazTaskLogMapper logMapper;
    private TSysDepartmentMapper departmentMapper;

    private DispatchWindow window;
    private TIsHazTaskServiceImpl svc;

    @BeforeAll
    static void forceZone() {
        // 期限口径按 GMT+8 自然日算，测试固定时区，避免随宿主跑歪
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));
    }

    @AfterAll
    static void restoreZone() {
        TimeZone.setDefault(null);
    }

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:haztask-" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        this.dataSource = ds;
        ScriptUtils.executeSqlScript(ds.getConnection(),
                new ClassPathResource("schema-h2.sql"));

        MybatisConfiguration cfg = new MybatisConfiguration();
        cfg.setMapUnderscoreToCamelCase(true);
        cfg.addMapper(TIsHazTaskMapper.class);
        cfg.addMapper(TIsHazTaskLogMapper.class);
        cfg.addMapper(TSysDepartmentMapper.class);

        MybatisSqlSessionFactoryBean fb = new MybatisSqlSessionFactoryBean();
        fb.setDataSource(ds);
        fb.setConfiguration(cfg);
        fb.setGlobalConfig(new GlobalConfig());
        this.session = fb.getObject().openSession(true);
        this.taskMapper = session.getMapper(TIsHazTaskMapper.class);
        this.logMapper = session.getMapper(TIsHazTaskLogMapper.class);
        this.departmentMapper = session.getMapper(TSysDepartmentMapper.class);

        HashSet<LocalDate> holidays = new HashSet<>();
        holidays.add(LocalDate.of(2026, 10, 1)); // 国庆
        // 专项检查月那半个月：6/16–6/30
        this.window = new DispatchWindow(holidays,
                LocalDate.of(2026, 6, 16), LocalDate.of(2026, 6, 30));

        this.svc = newService(window, HazardClosurePort.NONE);
    }

    private TIsHazTaskServiceImpl newService(DispatchWindow win, HazardClosurePort port) {
        HazTaskDispatchHandler handler = new HazTaskDispatchHandler();
        ReflectionTestUtils.setField(handler, "taskMapper", taskMapper);
        ReflectionTestUtils.setField(handler, "logMapper", logMapper);

        TIsHazTaskServiceImpl s = new TIsHazTaskServiceImpl();
        ReflectionTestUtils.setField(s, "isHazTaskMapper", taskMapper);
        ReflectionTestUtils.setField(s, "isHazTaskLogMapper", logMapper);
        ReflectionTestUtils.setField(s, "departmentMapper", departmentMapper);
        ReflectionTestUtils.setField(s, "dispatchHandler", handler);
        ReflectionTestUtils.setField(s, "dispatchWindow", win);
        ReflectionTestUtils.setField(s, "hazardClosurePort",
                port == null ? HazardClosurePort.NONE : port);
        return s;
    }

    // ---------- 工具 ----------

    private static Date dt(int y, int mo, int d, int h, int mi) {
        return Date.from(LocalDateTime.of(y, mo, d, h, mi).atZone(ZONE).toInstant());
    }

    /** 期限那一日的最后一刻 23:59:59（与生产归一口径一致） */
    private static Date endOf(int y, int mo, int d) {
        return Date.from(LocalDateTime.of(LocalDate.of(y, mo, d), LocalTime.of(23, 59, 59))
                .atZone(ZONE).toInstant());
    }

    private TIsHazTask newTask(String no, Date dueAt, BigDecimal leadDays, Long deptId) {
        TIsHazTask r = new TIsHazTask();
        r.setItemNo(no);
        r.setDueAt(dueAt);
        r.setAmount(leadDays);
        r.setContent("隐患-" + no);
        r.setDeptId(deptId);
        return r;
    }

    private long logCount(Long taskId, int action) {
        return logMapper.selectCount(new QueryWrapper<TIsHazTaskLog>()
                .eq("task_id", taskId).eq("action", action));
    }

    private void setDeptStatus(long id, int status) throws Exception {
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            st.execute("UPDATE t_sys_department SET status = " + status + " WHERE id = " + id);
        }
    }

    // ---------- 1. 期限口径：一把尺子 ----------

    @Test
    void deadlineRuler_sameYardstickForDueAndDaysLeft() {
        Date due = endOf(2026, 10, 10);

        // 到期日白天还没到最后一刻：不算到期，还剩 0 日
        assertFalse(DeadlinePolicy.isDue(due, dt(2026, 10, 10, 12, 0)));
        assertEquals(0, DeadlinePolicy.daysLeft(due, dt(2026, 10, 10, 12, 0)));

        // 恰好走到最后一刻：到期
        assertTrue(DeadlinePolicy.isDue(due, due));
        assertEquals(0, DeadlinePolicy.daysLeft(due, due));

        // 次日：逾期 1 日
        assertTrue(DeadlinePolicy.isDue(due, dt(2026, 10, 11, 0, 30)));
        assertEquals(-1, DeadlinePolicy.daysLeft(due, dt(2026, 10, 11, 0, 30)));
        assertEquals(1, DeadlinePolicy.overdueDays(due, dt(2026, 10, 11, 0, 30)));

        // 前一天：还剩 1 日
        assertEquals(1, DeadlinePolicy.daysLeft(due, dt(2026, 10, 9, 8, 0)));
    }

    @Test
    void shouldSurface_leadDaysOnlyGovernsAppearance() {
        Date due = endOf(2026, 10, 10);
        BigDecimal lead3 = new BigDecimal("3");

        // 还剩 3 日：露面
        assertTrue(DeadlinePolicy.shouldSurface(due, lead3, dt(2026, 10, 7, 9, 0)));
        // 还剩 4 日：不露面
        assertFalse(DeadlinePolicy.shouldSurface(due, lead3, dt(2026, 10, 6, 9, 0)));
        // 逾期恒露面
        assertTrue(DeadlinePolicy.shouldSurface(due, lead3, dt(2026, 10, 12, 9, 0)));
        // 提前档 0：未到期不露面
        assertFalse(DeadlinePolicy.shouldSurface(due, BigDecimal.ZERO, dt(2026, 10, 9, 9, 0)));
        assertTrue(DeadlinePolicy.shouldSurface(due, BigDecimal.ZERO, due));
    }

    // ---------- 2. 窗口：白天/夜里/节假日/专项 ----------

    @Test
    void window_dailyNightHoliday() {
        assertEquals(DispatchWindow.DAILY, window.activeWindow(dt(2026, 10, 8, 10, 0)));
        assertNull(window.activeWindow(dt(2026, 10, 8, 7, 30)), "窗外早晨静默");
        assertNull(window.activeWindow(dt(2026, 10, 8, 19, 0)), "夜里静默");
        assertNull(window.activeWindow(dt(2026, 10, 1, 10, 0)), "法定节假日整段静默");
    }

    @Test
    void window_specialHalfMonthIsSeparate() {
        // 专项期间 19 点：专项窗开到 20 点，不走日常窗
        assertEquals(DispatchWindow.SPECIAL, window.activeWindow(dt(2026, 6, 20, 19, 0)));
        // 专项窗也关门：夜里仍静默
        assertNull(window.activeWindow(dt(2026, 6, 20, 20, 30)));
        // 专项结束回到日常窗
        assertEquals(DispatchWindow.DAILY, window.activeWindow(dt(2026, 7, 1, 10, 0)));
    }

    @Test
    void roundNo_sameWindowSameDayIsOneKey() {
        assertEquals("DAILY:2026-10-08", window.roundNo(dt(2026, 10, 8, 8, 0)));
        assertEquals("DAILY:2026-10-08", window.roundNo(dt(2026, 10, 8, 17, 59)));
        assertEquals("SPECIAL:2026-06-20", window.roundNo(dt(2026, 6, 20, 9, 0)));
        assertNull(window.roundNo(dt(2026, 10, 8, 22, 0)));
    }

    // ---------- 3. 生成：挂起与期限归一 ----------

    @Test
    void generate_suspendWhenHazardAlreadyClosed() {
        TIsHazTaskServiceImpl s = newService(window, new HazardClosurePort() {
            @Override
            public boolean isClosed(String itemNo) {
                return "HZ-CLOSED".equals(itemNo);
            }
        });
        TIsHazTask saved = s.generate(newTask("HZ-CLOSED", endOf(2026, 10, 9), BigDecimal.ZERO, 1L));
        assertEquals(3, saved.getStatus());
        assertNotNull(saved.getSuspendReason());
        assertTrue(saved.getSuspendReason().contains("销号"));
        assertEquals(1, logMapper.selectCount(new QueryWrapper<TIsHazTaskLog>()
                .eq("task_id", saved.getId()).eq("action", 1)));
    }

    @Test
    void generate_suspendWhenDeptMergedAwayOrMissing() throws Exception {
        TIsHazTask disabled = svc.generate(newTask("HZ-1", endOf(2026, 10, 9), BigDecimal.ZERO, 9L));
        assertEquals(3, disabled.getStatus());
        assertTrue(disabled.getSuspendReason().contains("撤并"));

        TIsHazTask missing = svc.generate(newTask("HZ-2", endOf(2026, 10, 9), BigDecimal.ZERO, 999L));
        assertEquals(3, missing.getStatus());

        // 先给活再撤并部门：模拟生成后部门被撤
        TIsHazTask later = svc.generate(newTask("HZ-3", endOf(2026, 10, 9), BigDecimal.ZERO, 1L));
        assertEquals(0, later.getStatus());
        setDeptStatus(1, 0);
        TIsHazTask again = svc.generate(newTask("HZ-4", endOf(2026, 10, 9), BigDecimal.ZERO, 1L));
        assertEquals(3, again.getStatus());
        assertTrue(again.getSuspendReason().contains("撤并"));

        TIsHazTask noDept = svc.generate(newTask("HZ-5", endOf(2026, 10, 9), BigDecimal.ZERO, null));
        assertEquals(3, noDept.getStatus());
        assertTrue(noDept.getSuspendReason().contains("责任部门"));
    }

    @Test
    void generate_normalizesDeadlineToLastMomentOfDay() {
        TIsHazTask saved = svc.generate(newTask("HZ-N", dt(2026, 10, 9, 10, 0), BigDecimal.ZERO, 1L));
        assertEquals(0, saved.getStatus());
        // 传进几点都规整成期限当日 23:59:59
        assertEquals(endOf(2026, 10, 9), saved.getDueAt());
        assertEquals(0, saved.getUrgeCount());
        assertNull(saved.getAcceptedAt());
    }

    // ---------- 4. 负载派发 + 接单双写 + 当日幂等 ----------

    @Test
    void run_dispatchesToFreestDeptsWithAtomicAcceptance() {
        svc.generate(newTask("A", endOf(2026, 10, 7), new BigDecimal("3"), 1L));
        svc.generate(newTask("B", endOf(2026, 10, 7), new BigDecimal("3"), 2L));
        svc.generate(newTask("C", endOf(2026, 10, 7), new BigDecimal("3"), 3L));

        Date at = dt(2026, 10, 8, 10, 0);
        RoundResult r = svc.runOnce(at);

        assertEquals(3, r.getRoundDispatched());
        java.util.Set<Long> taken = new HashSet<>();
        for (TIsHazTask t : taskMapper.selectList(new QueryWrapper<TIsHazTask>())) {
            assertEquals(1, t.getStatus(), "状态必须翻成已派");
            assertNotNull(t.getAcceptedAt(), "接单时刻必须写上");
            assertEquals(at, t.getAcceptedAt());
            taken.add(t.getDeptId());
        }
        // 三个部门各接一条（活一样少时摊开）
        assertEquals(new HashSet<>(java.util.Arrays.asList(1L, 2L, 3L)), taken);
    }

    @Test
    void run_busierDeptGetsLowerWeight() {
        // 部门 1 手上已压 2 条已派活
        for (int i = 0; i < 2; i++) {
            TIsHazTask old = svc.generate(newTask("OLD-" + i, endOf(2026, 9, 1), BigDecimal.ZERO, 1L));
            old.setStatus(1);
            old.setAcceptedAt(dt(2026, 9, 1, 10, 0));
            taskMapper.updateById(old);
        }
        svc.generate(newTask("FREE", endOf(2026, 10, 7), new BigDecimal("3"), 1L));

        svc.runOnce(dt(2026, 10, 8, 10, 0));

        TIsHazTask fresh = taskMapper.selectOne(new QueryWrapper<TIsHazTask>()
                .eq("item_no", "FREE"));
        assertEquals(1, fresh.getStatus());
        // 手上空的部门（2 或 3）接走，不派给压着活的 1
        assertTrue(fresh.getDeptId() == 2L || fresh.getDeptId() == 3L,
                "活多的部门权重应更低，实际落到 dept=" + fresh.getDeptId());
    }

    @Test
    void run_secondScanSameWindowIsIdempotent() {
        TIsHazTask a = svc.generate(newTask("A", endOf(2026, 10, 7), new BigDecimal("3"), 1L));

        Date firstAt = dt(2026, 10, 8, 9, 0);
        RoundResult first = svc.runOnce(firstAt);
        assertEquals(1, first.getRoundDispatched());
        Date acceptedAt = taskMapper.selectById(a.getId()).getAcceptedAt();
        assertEquals(firstAt, acceptedAt);

        // 当日二次进来（同窗口同轮次）：直接忽略，先前那次原样留着
        RoundResult second = svc.runOnce(dt(2026, 10, 8, 15, 0));
        assertEquals(0, second.getRoundDispatched());
        assertEquals(acceptedAt, taskMapper.selectById(a.getId()).getAcceptedAt(),
                "首派时刻不得被二次扫描覆盖");
        assertEquals(1, logCount(a.getId(), 2), "同窗口派出留痕只能有一条");
    }

    @Test
    void run_nextWindowDayFindsNothing() {
        svc.generate(newTask("A", endOf(2026, 10, 7), new BigDecimal("3"), 1L));
        svc.runOnce(dt(2026, 10, 8, 9, 0));

        // 次日新窗口再扫：本轮无活（不是系统出错）
        RoundResult r = svc.runOnce(dt(2026, 10, 9, 9, 0));
        assertFalse(r.isWindowClosed());
        assertFalse(r.isHasWork());
        assertEquals(0, r.getRoundDispatched());
    }

    @Test
    void run_nightWindowStaysFrozen() {
        TIsHazTask a = svc.generate(newTask("A", endOf(2026, 10, 7), new BigDecimal("3"), 1L));
        RoundResult r = svc.runOnce(dt(2026, 10, 8, 23, 0));
        assertTrue(r.isWindowClosed());
        assertEquals(0, taskMapper.selectById(a.getId()).getStatus(), "窗外到期条原地不动");

        // 节假日同样不动
        TIsHazTask b = svc.generate(newTask("B", endOf(2026, 9, 30), BigDecimal.ZERO, 1L));
        RoundResult holiday = svc.runOnce(dt(2026, 10, 1, 10, 0));
        assertTrue(holiday.isWindowClosed());
        assertEquals(0, taskMapper.selectById(b.getId()).getStatus());
    }

    // ---------- 5. 逾期三天置顶 + 派不出去 ----------

    @Test
    void run_overdueThreeDaysPinnedAndMarkedStuckWhenNoDept() throws Exception {
        TIsHazTask overdue = svc.generate(newTask("OVERDUE", endOf(2026, 10, 4), BigDecimal.ZERO, 1L));
        TIsHazTask fresh = svc.generate(newTask("FRESH", endOf(2026, 10, 7), BigDecimal.ZERO, 1L));
        // 生成后部门全撤：谁也派不出去
        setDeptStatus(1, 0);
        setDeptStatus(2, 0);
        setDeptStatus(3, 0);

        RoundResult r = svc.runOnce(dt(2026, 10, 8, 10, 0));
        assertEquals(0, r.getRoundDispatched());
        assertEquals(1, r.getRoundStuck());
        assertEquals(2, taskMapper.selectById(overdue.getId()).getStatus(), "逾期满3天落派不出去");
        assertEquals(1, taskMapper.selectById(fresh.getId()).getUrgeCount(), "普通条催办一轮但不插队");
        assertEquals(1, taskMapper.selectById(overdue.getId()).getUrgeCount());

        // 当日二次扫描不重复催
        svc.runOnce(dt(2026, 10, 8, 11, 0));
        assertEquals(1, taskMapper.selectById(fresh.getId()).getUrgeCount());
    }

    @Test
    void run_stuckTaskDispatchedFirstOnceDeptFreesUp() throws Exception {
        TIsHazTask overdue = svc.generate(newTask("OVERDUE", endOf(2026, 10, 4), BigDecimal.ZERO, 1L));
        TIsHazTask later = svc.generate(newTask("LATER", endOf(2026, 10, 8), BigDecimal.ZERO, 1L));
        setDeptStatus(1, 0);
        setDeptStatus(2, 0);
        setDeptStatus(3, 0);
        svc.runOnce(dt(2026, 10, 8, 10, 0));
        assertEquals(2, taskMapper.selectById(overdue.getId()).getStatus());

        // 次日只有部门 1 空出来：逾期置顶的先派出（只有一个启用部门，两条都落它）
        setDeptStatus(1, 1);
        RoundResult r = svc.runOnce(dt(2026, 10, 9, 10, 0));
        assertEquals(2, r.getRoundDispatched());
        assertEquals(1, taskMapper.selectById(overdue.getId()).getStatus());
        assertEquals(1, taskMapper.selectById(later.getId()).getStatus());

        // 留痕先后：逾期条的派出记录先落（雪花 id 随插入序递增）
        List<TIsHazTaskLog> overLogs = svc.listLogs(overdue.getId());
        List<TIsHazTaskLog> laterLogs = svc.listLogs(later.getId());
        long dispatchOver = overLogs.stream().filter(l -> l.getAction() == 2).findFirst().get().getId();
        long dispatchLater = laterLogs.stream().filter(l -> l.getAction() == 2).findFirst().get().getId();
        assertTrue(dispatchOver < dispatchLater, "逾期置顶条必须先派");
    }

    // ---------- 6. 撤条 / 转办 / 期限不动 ----------

    @Test
    void cancel_keepsTracesAndOriginalDeadline() {
        TIsHazTask a = svc.generate(newTask("A", endOf(2026, 10, 9), BigDecimal.ONE, 1L));
        svc.runOnce(dt(2026, 10, 8, 10, 0)); // 还剩1日，露面即派
        Date dueBefore = taskMapper.selectById(a.getId()).getDueAt();
        Date acceptedBefore = taskMapper.selectById(a.getId()).getAcceptedAt();

        assertTrue(svc.cancel(a.getId(), "线下已闭环"));
        TIsHazTask row = taskMapper.selectById(a.getId());
        assertEquals(1, row.getDelFlag());
        assertEquals(dueBefore, row.getDueAt(), "撤条不得覆盖最初期限");
        assertEquals(acceptedBefore, row.getAcceptedAt(), "撤条不得覆盖首派时刻");
        assertEquals(1, logCount(a.getId(), 3));
        TIsHazTaskLog cancelLog = logMapper.selectList(new QueryWrapper<TIsHazTaskLog>()
                .eq("task_id", a.getId()).eq("action", 3)).get(0);
        assertEquals(dueBefore, cancelLog.getDueAt());
        assertEquals(acceptedBefore, cancelLog.getAcceptedAt());

        // 撤过不能再撤，扫描不再捞
        assertFalse(svc.cancel(a.getId(), "再撤一次"));
        RoundResult r = svc.runOnce(dt(2026, 10, 9, 10, 0));
        assertEquals(0, r.getRoundDispatched());
    }

    @Test
    void transfer_movesStuckTaskAndKeepsFirstAcceptedAt() throws Exception {
        TIsHazTask stuck = svc.generate(newTask("S", endOf(2026, 10, 4), BigDecimal.ZERO, 1L));
        setDeptStatus(1, 0);
        setDeptStatus(2, 0);
        setDeptStatus(3, 0);
        svc.runOnce(dt(2026, 10, 8, 10, 0));
        assertEquals(2, taskMapper.selectById(stuck.getId()).getStatus());

        setDeptStatus(2, 1);
        Date dueBefore = taskMapper.selectById(stuck.getId()).getDueAt();
        TIsHazTask moved = svc.transfer(stuck.getId(), 2L, "改由危化科接");
        assertNotNull(moved);
        assertEquals(1, moved.getStatus());
        assertEquals(2L, moved.getDeptId());
        assertNotNull(moved.getAcceptedAt());
        assertEquals(dueBefore, moved.getDueAt());
        assertEquals(1, logCount(stuck.getId(), 4));

        // 已派条不能再走转办口；撤并部门不能接
        assertNull(svc.transfer(stuck.getId(), 3L, "再转"));
        assertNull(svc.transfer(stuck.getId(), 9L, "转给撤并部门"));
    }

    @Test
    void adjustLeadDays_onlyThatDialMoves() {
        TIsHazTask a = svc.generate(newTask("A", endOf(2026, 10, 20), BigDecimal.ZERO, 1L));
        Date dueBefore = taskMapper.selectById(a.getId()).getDueAt();

        assertTrue(svc.adjustLeadDays(a.getId(), new BigDecimal("5")));
        assertEquals(0, new BigDecimal("5").compareTo(taskMapper.selectById(a.getId()).getAmount()));
        assertEquals(dueBefore, taskMapper.selectById(a.getId()).getDueAt(), "期限那一刻动不得");

        assertFalse(svc.adjustLeadDays(a.getId(), new BigDecimal("-1")), "负数不合法");

        // 调了提前档：还剩 5 日就露面被派
        RoundResult r = svc.runOnce(dt(2026, 10, 15, 10, 0));
        assertEquals(1, r.getRoundDispatched());
    }

    // ---------- 7. 一屏三数同次点算 ----------

    @Test
    void inventoryTally_oneCountAndThreeNumbers() {
        // 2 待派（未到露面档）+ 1 派不出去 + 2 已派 + 1 挂起 + 1 撤条
        svc.generate(newTask("W1", endOf(2026, 10, 30), BigDecimal.ZERO, 1L));
        svc.generate(newTask("W2", endOf(2026, 10, 30), BigDecimal.ZERO, 1L));
        TIsHazTask stuck = svc.generate(newTask("ST", endOf(2026, 10, 30), BigDecimal.ZERO, 1L));
        stuck.setStatus(2);
        taskMapper.updateById(stuck);
        for (String no : new String[]{"D1", "D2"}) {
            TIsHazTask d = svc.generate(newTask(no, endOf(2026, 9, 1), BigDecimal.ZERO, 1L));
            d.setStatus(1);
            d.setAcceptedAt(dt(2026, 9, 1, 10, 0));
            taskMapper.updateById(d);
        }
        svc.generate(newTask("SUS", endOf(2026, 10, 30), BigDecimal.ZERO, 9L)); // 部门撤并→挂起
        TIsHazTask gone = svc.generate(newTask("GONE", endOf(2026, 10, 30), BigDecimal.ZERO, 1L));
        svc.cancel(gone.getId(), "撤销");

        RoundResult r = svc.runOnce(dt(2026, 10, 8, 10, 0)); // 待派条都还没到露面档
        assertFalse(r.isHasWork(), "本轮无活");
        assertEquals(3, r.getWaitingCount(), "待派含派不出去(2+1)");
        assertEquals(2, r.getDispatchedCount());
        assertEquals(1, r.getSuspendedCount());
    }

    // ---------- 8. 卡一条不拖垮整轮 ----------

    @Test
    void run_oneFailingBillDoesNotBreakRound() {
        final TIsHazTask bad = svc.generate(newTask("BAD", endOf(2026, 10, 7), new BigDecimal("3"), 1L));
        svc.generate(newTask("OK", endOf(2026, 10, 7), new BigDecimal("3"), 1L));
        svc.generate(newTask("OK2", endOf(2026, 10, 7), new BigDecimal("3"), 1L));

        // 给处理器塞一个只在查 BAD 时炸的 mapper 动态代理，模拟该条落库异常
        final TIsHazTaskMapper real = taskMapper;
        TIsHazTaskMapper failing = (TIsHazTaskMapper) Proxy.newProxyInstance(
                TIsHazTaskMapper.class.getClassLoader(),
                new Class<?>[]{TIsHazTaskMapper.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("selectById".equals(method.getName())
                                && args != null && args.length == 1 && bad.getId().equals(args[0])) {
                            throw new RuntimeException("模拟落库异常");
                        }
                        return method.invoke(real, args);
                    }
                });
        HazTaskDispatchHandler handler2 = new HazTaskDispatchHandler();
        ReflectionTestUtils.setField(handler2, "taskMapper", failing);
        ReflectionTestUtils.setField(handler2, "logMapper", logMapper);
        ReflectionTestUtils.setField(svc, "dispatchHandler", handler2);

        RoundResult r = svc.runOnce(dt(2026, 10, 8, 10, 0));
        assertEquals(2, r.getRoundDispatched(), "坏条跳过，其余照派");
        assertEquals(0, taskMapper.selectById(bad.getId()).getStatus(), "坏条不留半截状态");
        assertNotNull(r.getMessage());
        assertTrue(r.getMessage().contains("BAD"));
    }

    // ---------- 9. 挂起条在明细页仍可翻到 ----------

    @Test
    void suspendedBillsRemainVisibleInDetail() {
        svc.generate(newTask("SUS1", endOf(2026, 10, 30), BigDecimal.ZERO, 9L));
        List<TIsHazTask> all = svc.selectList(new QueryWrapper<TIsHazTask>());
        assertEquals(1, all.size());
        assertEquals(3, all.get(0).getStatus());
        assertNotNull(svc.listLogs(all.get(0).getId()));
    }

    // ---------- 10. 专项窗口独立派发 ----------

    @Test
    void specialWindowDispatchesOnItsOwn() {
        TIsHazTask a = svc.generate(newTask("SP", endOf(2026, 6, 20), new BigDecimal("3"), 1L));
        // 6/17 还剩 3 日，19 点属于专项窗（开到 20 点）
        RoundResult r = svc.runOnce(dt(2026, 6, 17, 19, 0));
        assertEquals(DispatchWindow.SPECIAL, r.getWindow());
        assertEquals(1, r.getRoundDispatched());
        assertEquals(1, taskMapper.selectById(a.getId()).getStatus());

        // 同一专项窗口当日晚些再扫：忽略
        RoundResult again = svc.runOnce(dt(2026, 6, 17, 19, 30));
        assertEquals(0, again.getRoundDispatched());

        // 次日是另一轮次，可正常空扫
        RoundResult next = svc.runOnce(dt(2026, 6, 18, 19, 0));
        assertFalse(next.isHasWork());
    }

    @Test
    void holidayWindowEvenInsideSpecialStaysSilent() {
        DispatchWindow win = new DispatchWindow(
                Collections.singleton(LocalDate.of(2026, 6, 18)),
                LocalDate.of(2026, 6, 16), LocalDate.of(2026, 6, 30));
        TIsHazTaskServiceImpl s = newService(win, HazardClosurePort.NONE);
        s.generate(newTask("H", endOf(2026, 6, 17), BigDecimal.ZERO, 1L));
        RoundResult r = s.runOnce(dt(2026, 6, 18, 10, 0));
        assertTrue(r.isWindowClosed(), "专项期间撞上法定节假日仍整段静默");
    }

    @Test
    void productionCalendar_parsesConfig() {
        DispatchCalendar cal = new DispatchCalendar(
                "09:00", "17:30", "08:30", "19:45",
                "2026-10-01,2026-10-02", "2026-06-16", "2026-06-30");
        assertEquals(DispatchWindow.DAILY, cal.activeWindow(dt(2026, 10, 8, 10, 0)));
        assertNull(cal.activeWindow(dt(2026, 10, 8, 18, 0)), "17:30 后窗外静默");
        assertEquals(DispatchWindow.SPECIAL, cal.activeWindow(dt(2026, 6, 20, 19, 0)));
        assertNull(cal.activeWindow(dt(2026, 10, 1, 12, 0)), "节假日静默");
    }
}
