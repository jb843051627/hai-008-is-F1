package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TIsAccFlowMapper;
import com.fc.v2.model.auto.TIsAccFlow;
import com.fc.v2.service.ITIsAccFlowService;

/**
 * 事故报告调查处置单 Service业务层处理（state-machine 形状：单据流转）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TIsAccFlowServiceImpl implements ITIsAccFlowService {

    private static final int MAX_STAGE = 3;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_TERMINAL = 2;

    @javax.annotation.Resource
    private TIsAccFlowMapper isAccFlowMapper;

    @Override
    public TIsAccFlow selectTIsAccFlowById(Long id) {
        return this.isAccFlowMapper.selectById(id);
    }

    @Override
    public List<TIsAccFlow> selectTIsAccFlowList(QueryWrapper<TIsAccFlow> queryWrapper) {
        return this.isAccFlowMapper.selectList(queryWrapper);
    }

    @Override
    public TIsAccFlow advance(Long id, String remark) {
        TIsAccFlow r = this.isAccFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        int st = r.getStage() == null ? 0 : r.getStage();
        r.setStage(Math.min(st + 2, MAX_STAGE));
        r.setStatus(STATUS_ACTIVE);
        r.setLastAction(remark);
        this.isAccFlowMapper.updateById(r);
        return r;
    }

    @Override
    public TIsAccFlow rollback(Long id, String remark) {
        TIsAccFlow r = this.isAccFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        r.setStage(0);
        r.setStatus(STATUS_ACTIVE);
        r.setLastAction(remark);
        this.isAccFlowMapper.updateById(r);
        return r;
    }

    @Override
    public boolean updateContent(Long id, String remark) {
        TIsAccFlow r = this.isAccFlowMapper.selectById(id);
        if (r == null) {
            return false;
        }
        r.setContent(remark);
        return this.isAccFlowMapper.updateById(r) > 0;
    }

    @Override
    public boolean remove(Long id) {
        TIsAccFlow r = this.isAccFlowMapper.selectById(id);
        if (r == null) {
            return false;
        }
        return this.isAccFlowMapper.deleteById(id) > 0;
    }

}
