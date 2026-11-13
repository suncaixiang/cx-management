package com.cx.management.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cx.management.entity.AuditRecord;
import com.cx.management.mapper.AuditRecordMapper;
import org.springframework.stereotype.Service;

/**
 * @author suncaixiang
 */
@Service
public class AuditRecordServiceImpl extends ServiceImpl<AuditRecordMapper, AuditRecord> implements AuditRecordService {


    @Override
    public boolean updateStatus(Long id, Integer nowStatus, Integer newStatus) {
        LambdaUpdateWrapper<AuditRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper
                .eq(AuditRecord::getStatus, nowStatus)
                .eq(AuditRecord::getId, id)
                .set(AuditRecord::getStatus, newStatus);
        return baseMapper.update(updateWrapper) > 0;
    }
}
