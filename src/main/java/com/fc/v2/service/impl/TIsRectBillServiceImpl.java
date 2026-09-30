package com.fc.v2.service.impl;

import org.springframework.stereotype.Service;

import com.fc.v2.mapper.auto.TIsRectBillMapper;
import com.fc.v2.model.auto.TIsRectBill;
import com.fc.v2.service.ITIsRectBillService;

/**
 * 重大隐患整改处置签核单 Service业务层处理（approval-chain 形状：多阶段签批）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TIsRectBillServiceImpl implements ITIsRectBillService {

    private static final int MAX_NODE = 2;
    private static final int MODE_OR = 0;
    private static final int MODE_AND = 1;
    private static final int STATUS_RUNNING = 0;
    private static final int STATUS_PASS = 1;
    private static final int STATUS_VETO = 2;

    @javax.annotation.Resource
    private TIsRectBillMapper isRectBillMapper;

    @Override
    public TIsRectBill selectTIsRectBillById(Long id) {
        return this.isRectBillMapper.selectById(id);
    }

    @Override
    public TIsRectBill approve(Long id, String approver, String comment) {
        TIsRectBill r = this.isRectBillMapper.selectById(id);
        if (r == null || approver == null || approver.trim().isEmpty()) {
            return null;
        }
        r.setNodeNo(Integer.valueOf((r.getNodeNo() == null ? 0 : r.getNodeNo()) + 1));
        r.setStatus(Integer.valueOf(r.getNodeNo() >= MAX_NODE ? STATUS_PASS : STATUS_RUNNING));
        this.isRectBillMapper.updateById(r);
        return r;
    }

    @Override
    public TIsRectBill reject(Long id, String approver, String comment) {
        TIsRectBill r = this.isRectBillMapper.selectById(id);
        if (r == null) {
            return null;
        }
        this.isRectBillMapper.updateById(r);
        return r;
    }

    @Override
    public TIsRectBill rollback(Long id, String comment) {
        TIsRectBill r = this.isRectBillMapper.selectById(id);
        if (r == null) {
            return null;
        }
        int node = r.getNodeNo() == null ? 0 : r.getNodeNo();
        r.setNodeNo(Integer.valueOf(Math.max(0, node - 1)));
        this.isRectBillMapper.updateById(r);
        return r;
    }
}
