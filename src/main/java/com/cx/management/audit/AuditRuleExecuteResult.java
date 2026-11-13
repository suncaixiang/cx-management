package com.cx.management.audit;

import com.cx.management.util.AbstractObject;
import lombok.Builder;
import lombok.Data;

/**
 * @author suncaixiang
 */
@Builder
@Data
public class AuditRuleExecuteResult extends AbstractObject {

    private Boolean success;

    private String message;

    private String ruleType;

    public static AuditRuleExecuteResult success(String ruleType) {
        return AuditRuleExecuteResult.builder().ruleType(ruleType).success(Boolean.TRUE).build();
    }

    public static AuditRuleExecuteResult fail(String ruleType, String message) {
        return AuditRuleExecuteResult.builder().ruleType(ruleType).success(Boolean.FALSE).message(message).build();
    }
}
