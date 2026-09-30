package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TIsHazCard;

import java.util.List;

/**
 * 重大危险源档案卡 Service接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITIsHazCardService {

    /** 按主键查询 */
    TIsHazCard selectTIsHazCardById(Long id);

    /** 按条件查询列表（分页由调用方统一处理） */
    List<TIsHazCard> selectTIsHazCardList(Wrapper<TIsHazCard> queryWrapper);

    /** 新增 */
    int insertTIsHazCard(TIsHazCard record);

    /** 修改 */
    int updateTIsHazCard(TIsHazCard record);

    /** 批量删除 */
    int deleteTIsHazCardByIds(String ids);

    /** 按主键删除 */
    int deleteTIsHazCardById(Long id);
}
