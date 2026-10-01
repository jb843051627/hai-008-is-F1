package com.fc.v2.service;

import com.fc.v2.model.auto.TIsDispatchLog;
import com.fc.v2.model.auto.TIsDispatchRound;
import com.fc.v2.model.auto.TIsHazTask;
import com.fc.v2.service.haz.RoundScreen;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 隐患整改催办条 Service 接口（scheduling-job 形状：周期执行 + 少量处置入口）。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITIsHazTaskService {

    /** 按主键回查条目（翻条，剩余几日由 DeadlineRuler 统一算） */
    TIsHazTask selectTIsHazTaskById(Long id, Date at);

    /** 明细台账（挂起/已撤等各情形都翻得到；status 为空取全部） */
    List<TIsHazTask> listBills(Integer status, Date at);

    /**
     * 把已到露面档、还没生成催办条的隐患生成条子。
     * 生成前已销号结案、或原定部门撤并没着落的：就地挂起并写明原由。
     * 单条失败不拖垮整批。返回本次生成（含挂起）的条子。
     */
    List<TIsHazTask> generateDueBills(Date at);

    /**
     * 跑一轮派发。
     * 窗口外 → CLOSED（条子原地不动，不留轮次）；当日同窗口二次进来 → SKIPPED（原样留着）；
     * 无到期活 → NO_WORK（正常结果，不是出错）；正常扫过 → SCANNED 并亮一屏三个同次点算的数。
     */
    RoundScreen runOnce(Date at);

    /** 期限过了仍派不出去：撤条留痕（期限一刻、初派时刻原样留存，永不覆盖） */
    TIsHazTask revoke(Long id, String reason);

    /** 期限过了仍派不出去：转办给指定在岗部门（原子接单） */
    TIsHazTask transfer(Long id, Long toDeptId, String reason);

    /** 只许调"还剩几日露面"这一档；期限那一刻动不得 */
    TIsHazTask tuneLeadDays(Long id, BigDecimal leadDays);

    /** 一条催办条的全过程留痕（生成/挂起/派出/催办/撤条/转办/调档），谁接手、催过几轮一翻便知 */
    java.util.List<TIsDispatchLog> listLogs(Long billId);

    /** 最近一轮点算（明细页亮屏用；无则 null） */
    TIsDispatchRound latestRound();
}
