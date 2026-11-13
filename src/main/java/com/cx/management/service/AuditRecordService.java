package com.cx.management.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cx.management.entity.AuditRecord;

/**
 * @author suncaixiang
 */
public interface AuditRecordService extends IService<AuditRecord> {

    /**
     * 更新状态
     */
    boolean updateStatus(Long id, Integer nowStatus, Integer newStatus);
}
