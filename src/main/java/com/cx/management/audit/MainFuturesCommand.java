package com.cx.management.audit;

import com.cx.management.entity.AuditTradeRecord;
import com.cx.management.enums.RuleTypeEnum;
import com.cx.management.enums.TradeCombOffsetEnum;
import com.cx.management.util.RqCache;

import java.util.List;

/**
 * 非主力合约交易行为
 *
 * @author suncaixiang
 */
public class MainFuturesCommand implements AuditRuleCommand {


    @Override
    public AuditRuleExecuteResult execute(AuditTradeRecord auditTradeRecordDO) {
        if (!TradeCombOffsetEnum.OPEN.getCode().equals(auditTradeRecordDO.getTradeCombOffset())) {
            return AuditRuleExecuteResult.success(RuleTypeEnum.MAIN_FUTURES_LIMIT.getCode());
        }
        //查询主力、次主力合约
        List<String> mainInstrumentIdList = RqCache.queryMainInstrumentIdList(auditTradeRecordDO.getProductId(), auditTradeRecordDO.getTradingDay());
        if (!mainInstrumentIdList.contains(auditTradeRecordDO.getInstrumentId())) {
            return AuditRuleExecuteResult.fail(RuleTypeEnum.MAIN_FUTURES_LIMIT.getCode(), "非主力/次主力合约");
        }
        return AuditRuleExecuteResult.success(RuleTypeEnum.MAIN_FUTURES_LIMIT.getCode());
    }
}
