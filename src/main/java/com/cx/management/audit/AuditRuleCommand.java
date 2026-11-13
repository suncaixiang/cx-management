package com.cx.management.audit;

import com.cx.management.entity.AuditTradeRecord;

/**
 * @author suncaixiang
 */
public interface AuditRuleCommand {


    /**
     * 审核规则执行
     *
     * @param auditTradeRecordDO 成交记录
     * @return 执行结果
     */
    AuditRuleExecuteResult execute(AuditTradeRecord auditTradeRecordDO);

}
