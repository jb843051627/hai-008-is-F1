package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TIsHazTask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

/**
 * 隐患整改催办条数据层
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface TIsHazTaskMapper extends BaseMapper<TIsHazTask> {

    /**
     * 原子接单：情形从待派/派不出去翻成目标情形（1已派 / 5转办），接手部门与接手时刻
     * 同一条 SQL 一次写成——两样缺一（更新 0 行）这单就不算派成。
     * 最初真正派出时刻只在为空时落一次，之后永不覆盖。
     * incRound=1 时催办轮次加一（轮次派发）；人工转办不计催办轮次，传 0。
     */
    @Update("UPDATE t_is_haz_task SET "
            + " status = #{toStatus}, "
            + " accept_dept_id = #{deptId}, accept_dept_name = #{deptName}, "
            + " accepted_at = #{at}, "
            + " first_dispatched_at = COALESCE(first_dispatched_at, #{at}), "
            + " urge_rounds = IFNULL(urge_rounds, 0) + #{incRound}, "
            + " update_time = #{at} "
            + " WHERE id = #{id} AND status IN (0, 2) AND del_flag = 0")
    int assign(@Param("id") Long id,
               @Param("deptId") Long deptId,
               @Param("deptName") String deptName,
               @Param("at") Date at,
               @Param("toStatus") int toStatus,
               @Param("incRound") int incRound);

    /** 候选池为空：记为派不出去并催过一轮；情形没到已派，下一轮仍当待办捞出。 */
    @Update("UPDATE t_is_haz_task SET "
            + " status = 2, "
            + " urge_rounds = IFNULL(urge_rounds, 0) + 1, "
            + " update_time = #{at} "
            + " WHERE id = #{id} AND status IN (0, 2) AND del_flag = 0")
    int markStuck(@Param("id") Long id, @Param("at") Date at);

    /** 部门手上压着的活：已派/转办且未销账的催办条。活越多权重越低，手上空的先接。 */
    @Select("SELECT COUNT(1) FROM t_is_haz_task "
            + " WHERE accept_dept_id = #{deptId} AND status IN (1, 5) AND del_flag = 0")
    int countWorkload(@Param("deptId") Long deptId);
}
