package com.cx.management.service;

import com.cx.management.common.PageResult;
import com.cx.management.dto.AuditTradeRecordVO;
import com.cx.management.dto.QueryAuditTradeRecordDTO;
import com.cx.management.entity.AuditTradeRecord;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author suncaixiang
 */
public interface AuditTradeRecordFacade {


    /**
     * 分页交易记录
     */
    void analysis(AuditTradeRecord auditTradeRecordDO);


    PageResult<AuditTradeRecordVO> queryPageList(QueryAuditTradeRecordDTO dto);

    /**
     * 按当前查询条件导出全部成交明细 CSV
     */
    void exportCsv(QueryAuditTradeRecordDTO dto, HttpServletResponse response);
}
