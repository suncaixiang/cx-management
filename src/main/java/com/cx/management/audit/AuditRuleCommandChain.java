package com.cx.management.audit;

import com.cx.management.common.exception.BusinessException;
import com.cx.management.entity.AuditTradeRecord;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 审核规则执行链
 *
 * @author suncaixiang
 */
@Slf4j
public class AuditRuleCommandChain {

    /**
     * CTP命令列表
     */
    private final List<AuditRuleCommand> commandList = Lists.newArrayList();

    public void add(AuditRuleCommand command) {
        commandList.add(command);
    }

    /**
     * 链条执行，遇见错误中断
     */
    public List<AuditRuleExecuteResult> execute(AuditTradeRecord auditTradeRecordDO) {
        if (CollectionUtils.isEmpty(commandList)) {
            return Lists.newArrayList();
        }
        List<AuditRuleExecuteResult> resultList = Lists.newArrayList();
        commandList.forEach(command -> {
                    try {
                        AuditRuleExecuteResult auditRuleExecuteResult = command.execute(auditTradeRecordDO);
                        resultList.add(auditRuleExecuteResult);
                    } catch (Exception e) {
                        log.error("执行异常，msg:{}", e.getMessage(), e);
                        throw new BusinessException(e);
                    }
                }
        );
        return resultList;
    }
}
