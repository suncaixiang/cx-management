package com.cx.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cx.management.entity.AuditFailResult;
import com.cx.management.mapper.AuditFailResultMapper;
import com.google.common.collect.Lists;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;

/**
 * @author suncaixiang
 */
@Service
public class AuditFailResultServiceImpl extends ServiceImpl<AuditFailResultMapper, AuditFailResult> implements AuditFailResultService {
    @Override
    public List<AuditFailResult> query(Collection<Long> auditTradeRecordIdList, Collection<String> ruleTypeList) {
        if (CollectionUtils.isEmpty(auditTradeRecordIdList)) {
            return Lists.newArrayList();
        }
        LambdaQueryWrapper<AuditFailResult> queryWrapper = new LambdaQueryWrapper<AuditFailResult>()
                .in(!CollectionUtils.isEmpty(auditTradeRecordIdList), AuditFailResult::getTradeRecordId, auditTradeRecordIdList)
                .in(!CollectionUtils.isEmpty(ruleTypeList), AuditFailResult::getTriggerRuleType, ruleTypeList);
        return baseMapper.selectList(queryWrapper);
    }

}
