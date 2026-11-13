package com.cx.management.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cx.management.entity.AuditFailResult;

import java.util.Collection;
import java.util.List;

/**
 * @author suncaixiang
 */
public interface AuditFailResultService extends IService<AuditFailResult> {


    /**
     * 查询审核失败记录
     */
    List<AuditFailResult> query(Collection<Long> auditTradeRecordIdList, Collection<String> ruleTypeList);

}
