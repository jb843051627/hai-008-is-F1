package com.fc.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TIsDispatchLogMapper;
import com.fc.v2.mapper.auto.TIsDispatchRoundMapper;
import com.fc.v2.mapper.auto.TIsDispatchWindowMapper;
import com.fc.v2.mapper.auto.TIsHazTaskMapper;
import com.fc.v2.mapper.auto.TIsHolidayMapper;
import com.fc.v2.model.auto.TIsDispatchLog;
import com.fc.v2.model.auto.TIsDispatchRound;
import com.fc.v2.model.auto.TIsDispatchWindow;
import com.fc.v2.model.auto.TIsHazTask;
import com.fc.v2.model.auto.TIsHoliday;
import com.fc.v2.service.ITIsHazTaskService;
import com.fc.v2.service.haz.DeadlineRuler;
import com.fc.v2.service.haz.DispatchWindows;
import com.fc.v2.service.haz.HazDispatchTx;
import com.fc.v2.service.haz.HazConst;
import com.fc.v2.service.haz.RoundScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 隐患整改催办条 Service 实现（scheduling-job 形状：窗口判定 + 周期派发）。
 *
 * 职责切分：
 * 本类只做"几点派、派不派"（窗口/节假日）与异常兜底、翻条；落库的派发过程在
 * {@link HazDispatchTx} 的单一事务里跑，保证一屏三个数与库底数同一次点算。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TIsHazTaskServiceImpl implements ITIsHazTaskService {

    private static final Logger log = LoggerFactory.getLogger(TIsHazTaskServiceImpl.class);

    @Resource
    private TIsHazTaskMapper isHazTaskMapper;
    @Resource
    private TIsDispatchWindowMapper windowMapper;
    @Resource
    private TIsHolidayMapper holidayMapper;
    @Resource
    private TIsDispatchRoundMapper roundMapper;
    @Resource
    private TIsDispatchLogMapper logMapper;
    @Resource
    private HazDispatchTx dispatchTx;

    @Override
    public TIsHazTask selectTIsHazTaskById(Long id, Date at) {
        TIsHazTask b = isHazTaskMapper.selectById(id);
        if (b != null) {
            fillDaysLeft(b, at == null ? new Date() : at);
        }
        return b;
    }

    @Override
    public List<TIsHazTask> listBills(Integer status, Date at) {
        QueryWrapper<TIsHazTask> qw = new QueryWrapper<TIsHazTask>().eq("del_flag", 0);
        if (status != null) {
            qw.eq("status", status);
        }
        qw.orderByDesc("due_at");
        List<TIsHazTask> list = isHazTaskMapper.selectList(qw);
        Date now = at == null ? new Date() : at;
        for (TIsHazTask b : list) {
            fillDaysLeft(b, now);
        }
        return list;
    }

    /** 翻条的"还剩几日"与回写派发同走 DeadlineRuler，不许各算各的。 */
    private void fillDaysLeft(TIsHazTask b, Date at) {
        if (b.getDueAt() != null) {
            b.setDaysLeft(DeadlineRuler.daysLeft(b.getDueAt(), at));
        }
    }

    @Override
    public List<TIsHazTask> generateDueBills(Date at) {
        return dispatchTx.generateOnly(at == null ? new Date() : at);
    }

    @Override
    public RoundScreen runOnce(Date at) {
        Date now = at == null ? new Date() : at;
        TIsDispatchWindow win;
        try {
            List<TIsDispatchWindow> windows = windowMapper.selectList(new QueryWrapper<TIsDispatchWindow>()
                    .eq("del_flag", 0)
                    .eq("status", 0));
            List<Date> holidays = new ArrayList<>();
            for (TIsHoliday h : holidayMapper.selectList(new QueryWrapper<TIsHoliday>())) {
                holidays.add(h.getHolidayDate());
            }
            win = DispatchWindows.resolve(now, holidays, windows);
        } catch (RuntimeException ex) {
            log.error("催办派发：窗口/节假日配置读取失败", ex);
            RoundScreen s = new RoundScreen();
            s.setState(RoundScreen.State.ERROR);
            s.setMessage("系统出错：窗口配置读取失败——" + ex.getMessage());
            s.setStartedAt(now);
            s.setFinishedAt(now);
            return s;
        }

        if (win == null) {
            // 夜间 / 法定节假日 / 窗口外：哪怕有条子到期也原地不动，等下一轮窗口。不留轮次、不顶错误。
            return RoundScreen.closed("当前不在派发窗口内（夜间或法定节假日静默），到期条子原地不动，等下一轮窗口再派");
        }

        try {
            return dispatchTx.execute(now, win);
        } catch (RuntimeException ex) {
            // 事务已整轮回滚；这才是真正的"系统出错"，与"本轮无活"严格区分。
            log.error("催办派发：第 " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(now)
                    + " 轮执行失败，已整轮回滚", ex);
            RoundScreen s = new RoundScreen();
            s.setState(RoundScreen.State.ERROR);
            s.setMessage("系统出错：本轮派发异常已整轮回滚——" + ex.getMessage());
            s.setStartedAt(now);
            s.setFinishedAt(new Date());
            s.setWinType(win.getWinType());
            s.setWinName(win.getWinName());
            return s;
        }
    }

    @Override
    public TIsHazTask revoke(Long id, String reason) {
        return dispatchTx.revoke(id, reason, new Date());
    }

    @Override
    public TIsHazTask transfer(Long id, Long toDeptId, String reason) {
        return dispatchTx.transfer(id, toDeptId, reason, new Date());
    }

    @Override
    public TIsHazTask tuneLeadDays(Long id, BigDecimal leadDays) {
        return dispatchTx.tuneLeadDays(id, leadDays, new Date());
    }

    /** 最近一轮点算（明细页亮屏用）。 */
    @Override
    public TIsDispatchRound latestRound() {
        List<TIsDispatchRound> list = roundMapper.selectList(new QueryWrapper<TIsDispatchRound>()
                .orderByDesc("round_date", "win_type")
                .last("limit 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<TIsDispatchLog> listLogs(Long billId) {
        return logMapper.selectList(new QueryWrapper<TIsDispatchLog>()
                .eq("bill_id", billId)
                .orderByAsc("create_time", "id"));
    }

    /** 状态文案，供页面/接口使用。 */
    public static String statusText(Integer status) {
        return HazConst.statusText(status);
    }
}
