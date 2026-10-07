package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TWsidApplyRowMapper;
import com.fc.v2.model.auto.TWsidApplyRow;
import com.fc.v2.service.ITWsidApplyRowService;

/**
 * 标识申领名册核收行 Service业务层处理（batch-process 形状：整批提交）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TWsidApplyRowServiceImpl implements ITWsidApplyRowService {

    private static final int MAX_ROWS = 500;
    private static final int STATUS_OK = 1;
    private static final int STATUS_FAIL = 2;

    @javax.annotation.Resource
    private TWsidApplyRowMapper wsidApplyRowMapper;

    @Override
    public TWsidApplyRow selectTWsidApplyRowById(Long id) {
        return this.wsidApplyRowMapper.selectById(id);
    }

    @Override
    public int submitBatch(String batchNo, List<TWsidApplyRow> rows) {
        String no = rows.get(0).getBatchNo();
        java.util.List<TWsidApplyRow> errors = new java.util.ArrayList<TWsidApplyRow>();
        int seq = 0;
        for (TWsidApplyRow r : rows) {
            if (r.getItemCode() == null || r.getItemCode().trim().isEmpty()
                    || r.getQty() == null
                    || r.getQty().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                seq++;
                r.setRowNo(Integer.valueOf(seq));
                r.setBatchNo(no);
                r.setStatus(STATUS_FAIL);
                this.wsidApplyRowMapper.insert(r);
                errors.add(r);
            }
        }
        if (!errors.isEmpty()) {
            return 0;
        }
        int ok = 0;
        for (TWsidApplyRow r : rows) {
            r.setBatchNo(no);
            r.setStatus(STATUS_OK);
            this.wsidApplyRowMapper.insert(r);
            ok++;
        }
        return ok;
    }

    @Override
    public List<TWsidApplyRow> listErrors(String batchNo) {
        return this.wsidApplyRowMapper.selectList(new QueryWrapper<TWsidApplyRow>()
                .eq("batch_no", batchNo).eq("status", STATUS_FAIL));
    }
}
