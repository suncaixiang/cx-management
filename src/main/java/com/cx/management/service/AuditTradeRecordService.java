package com.cx.management.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cx.management.dto.QueryAuditTradeRecordDTO;
import com.cx.management.entity.AuditTradeRecord;

import java.util.Collection;
import java.util.List;

/**
 * @author suncaixiang
 */
public interface AuditTradeRecordService extends IService<AuditTradeRecord> {


    /**
     * 更新状态
     */
    Boolean updateStatus(Long id, Integer nowStatus, Integer newStatus);


    /**
     * 统计
     */
    Long countByStatus(Long auditRecordId, Collection<Integer> statusList);

    /**
     * 分页查询
     */
    Page<AuditTradeRecord> queryPage(QueryAuditTradeRecordDTO dto);

    /**
     * 按条件查询全部（导出用）
     */
    List<AuditTradeRecord> queryList(QueryAuditTradeRecordDTO dto);

}
