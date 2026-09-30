package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TIsCheckRowMapper;
import com.fc.v2.model.auto.TIsCheckRow;
import com.fc.v2.service.ITIsCheckRowService;

/**
 * 执法检查记录导入核对行 Service业务层处理（batch-process 形状：整批提交）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TIsCheckRowServiceImpl implements ITIsCheckRowService {

    private static final int MAX_ROWS = 500;
    private static final int STATUS_OK = 1;
    private static final int STATUS_FAIL = 2;

    @javax.annotation.Resource
    private TIsCheckRowMapper isCheckRowMapper;

    @Override
    public TIsCheckRow selectTIsCheckRowById(Long id) {
        return this.isCheckRowMapper.selectById(id);
    }

    @Override
    public int submitBatch(String batchNo, List<TIsCheckRow> rows) {
        String no = rows.get(0).getBatchNo();
        java.util.List<TIsCheckRow> errors = new java.util.ArrayList<TIsCheckRow>();
        int seq = 0;
        for (TIsCheckRow r : rows) {
            if (r.getItemCode() == null || r.getItemCode().trim().isEmpty()
                    || r.getQty() == null
                    || r.getQty().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                seq++;
                r.setRowNo(Integer.valueOf(seq));
                r.setBatchNo(no);
                r.setStatus(STATUS_FAIL);
                this.isCheckRowMapper.insert(r);
                errors.add(r);
            }
        }
        if (!errors.isEmpty()) {
            return 0;
        }
        int ok = 0;
        for (TIsCheckRow r : rows) {
            r.setBatchNo(no);
            r.setStatus(STATUS_OK);
            this.isCheckRowMapper.insert(r);
            ok++;
        }
        return ok;
    }

    @Override
    public List<TIsCheckRow> listErrors(String batchNo) {
        return this.isCheckRowMapper.selectList(new QueryWrapper<TIsCheckRow>()
                .eq("batch_no", batchNo).eq("status", STATUS_FAIL));
    }
}
