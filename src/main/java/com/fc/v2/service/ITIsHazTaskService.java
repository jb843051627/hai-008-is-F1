package com.fc.v2.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TIsHazTask;
import com.fc.v2.model.auto.TIsHazTaskLog;
import com.fc.v2.service.dispatch.RoundResult;

/**
 * 隐患整改催办条 Service接口（scheduling-job 形状：周期窗口扫描派发，无前台增删改查入口）。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITIsHazTaskService {

    /** 按主键回查条目 */
    TIsHazTask selectTIsHazTaskById(Long id);

    /** 明细台账：含挂起条，按 wrapper 过滤（挂起条不藏，在明细页仍能翻到） */
    List<TIsHazTask> selectList(Wrapper<TIsHazTask> wrapper);

    /** 一条子的全部派发处置留痕（谁接手、催过几轮、撤转原由） */
    List<TIsHazTaskLog> listLogs(Long taskId);

    /**
     * 已到露面档（到期或进入提前天数）且尚未派出的待派条（含派不出去待再试的）。
     * 只按期限口径筛，不判窗口；是否在窗口内由 {@link #runOnce} 先判，窗外一律不捞不派。
     * 已派/挂起/撤条不捞。
     */
    List<TIsHazTask> listDue(Date at);

    /**
     * 生成催办条：期限统一规范成「期限那一日最后一刻」；生成前隐患已销号或责任部门
     * 撤并没着落的，就地挂起并写明原由。期限那一刻落定后任何入口不得再改。
     */
    TIsHazTask generate(TIsHazTask bill);

    /**
     * 跑一个派发轮次。窗外静默、本轮无活与系统出错由 {@link RoundResult} 区分；
     * 单条失败跳过不拖垮整轮。
     */
    RoundResult runOnce(Date at);

    /**
     * 只许调「还剩几日」这一档（提前露面天数）；期限那一刻动不得。
     */
    boolean adjustLeadDays(Long id, BigDecimal leadDays);

    /** 撤条：软删留痕；最初整改期限与首派时刻快照进留痕，不被覆盖。 */
    boolean cancel(Long id, String reason);

    /** 转办：派不出去/逾期后改挂另一责任部门；首派时刻原样保留。 */
    TIsHazTask transfer(Long id, Long toDeptId, String reason);
}
