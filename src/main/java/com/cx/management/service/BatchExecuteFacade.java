package com.cx.management.service;

import com.cx.management.entity.AuditTradeRecord;

import java.util.List;

/**
 * @author suncaixiang
 */
public interface BatchExecuteFacade {


    /**
     * 分析
     *
     * @param auditTradeRecordDOList 成交记录
     */
    void analysis(List<AuditTradeRecord> auditTradeRecordDOList);

}
