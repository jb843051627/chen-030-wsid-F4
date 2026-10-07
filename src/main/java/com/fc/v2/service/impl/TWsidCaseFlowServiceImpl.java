package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TWsidCaseFlowMapper;
import com.fc.v2.model.auto.TWsidCaseFlow;
import com.fc.v2.service.ITWsidCaseFlowService;

/**
 * 专用标识申领单流转单 Service业务层处理（state-machine 形状：单据流转）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TWsidCaseFlowServiceImpl implements ITWsidCaseFlowService {

    private static final int MAX_STAGE = 3;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_TERMINAL = 2;

    @javax.annotation.Resource
    private TWsidCaseFlowMapper wsidCaseFlowMapper;

    @Override
    public TWsidCaseFlow selectTWsidCaseFlowById(Long id) {
        return this.wsidCaseFlowMapper.selectById(id);
    }

    @Override
    public List<TWsidCaseFlow> selectTWsidCaseFlowList(QueryWrapper<TWsidCaseFlow> queryWrapper) {
        return this.wsidCaseFlowMapper.selectList(queryWrapper);
    }

    @Override
    public TWsidCaseFlow advance(Long id, String remark) {
        TWsidCaseFlow r = this.wsidCaseFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        int st = r.getStage() == null ? 0 : r.getStage();
        r.setStage(Math.min(st + 2, MAX_STAGE));
        r.setStatus(STATUS_ACTIVE);
        r.setLastAction(remark);
        this.wsidCaseFlowMapper.updateById(r);
        return r;
    }

    @Override
    public TWsidCaseFlow rollback(Long id, String remark) {
        TWsidCaseFlow r = this.wsidCaseFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        r.setStage(0);
        r.setStatus(STATUS_ACTIVE);
        r.setLastAction(remark);
        this.wsidCaseFlowMapper.updateById(r);
        return r;
    }

    @Override
    public boolean updateContent(Long id, String remark) {
        TWsidCaseFlow r = this.wsidCaseFlowMapper.selectById(id);
        if (r == null) {
            return false;
        }
        r.setContent(remark);
        return this.wsidCaseFlowMapper.updateById(r) > 0;
    }

    @Override
    public boolean remove(Long id) {
        TWsidCaseFlow r = this.wsidCaseFlowMapper.selectById(id);
        if (r == null) {
            return false;
        }
        return this.wsidCaseFlowMapper.deleteById(id) > 0;
    }

}
