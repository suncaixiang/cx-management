package com.cx.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cx.management.dto.QueryAuditTradeRecordDTO;
import com.cx.management.entity.AuditRecord;
import com.cx.management.entity.AuditTradeRecord;
import com.cx.management.mapper.AuditTradeRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;

/**
 * 
 *
 * @author suncaixiang
 */
@Service
public class AuditTradeRecordServiceImpl extends ServiceImpl<AuditTradeRecordMapper, AuditTradeRecord> implements AuditTradeRecordService{
    @Override
    public Boolean updateStatus(Long id, Integer nowStatus, Integer newStatus) {
        LambdaUpdateWrapper<AuditTradeRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper
                .eq(AuditTradeRecord::getStatus, nowStatus)
                .eq(AuditTradeRecord::getId, id)
                .set(AuditTradeRecord::getStatus, newStatus);
        return baseMapper.update(updateWrapper) > 0;
    }

    @Override
    public Long countByStatus(Long auditRecordId, Collection<Integer> statusList) {
        LambdaQueryWrapper<AuditTradeRecord> queryWrapper = new LambdaQueryWrapper<AuditTradeRecord>()
                .eq(AuditTradeRecord::getAuditRecordId, auditRecordId)
                .in(!CollectionUtils.isEmpty(statusList), AuditTradeRecord::getStatus, statusList);
        return baseMapper.selectCount(queryWrapper);
    }

    @Override
    public Page<AuditTradeRecord> queryPage(QueryAuditTradeRecordDTO dto) {
        LambdaQueryWrapper<AuditTradeRecord> queryWrapper = buildQueryWrapper(dto);
        Page<AuditTradeRecord> page = new Page<>(dto.getCurrent(), dto.getSize());
        return baseMapper.selectPage(page, queryWrapper);
    }

    @Override
    public List<AuditTradeRecord> queryList(QueryAuditTradeRecordDTO dto) {
        return baseMapper.selectList(buildQueryWrapper(dto));
    }

    private LambdaQueryWrapper<AuditTradeRecord> buildQueryWrapper(QueryAuditTradeRecordDTO dto) {
        return new LambdaQueryWrapper<AuditTradeRecord>()
                .eq(AuditTradeRecord::getAuditRecordId, dto.getAuditRecordId())
                .eq(dto.getStatus() != null, AuditTradeRecord::getStatus, dto.getStatus())
                .orderByAsc(AuditTradeRecord::getId);
    }
}

