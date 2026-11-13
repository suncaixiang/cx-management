package com.cx.management.audit;

import com.cx.management.entity.AuditTradeRecord;
import com.cx.management.enums.RuleTypeEnum;
import com.cx.management.enums.TradeCombOffsetEnum;
import com.cx.management.util.RqCache;
import com.cx.management.util.RqInstrumentBO;
import com.cx.management.util.RqInstrumentPriceTickBO;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 是否存在刷单行为；（需要品种每跳的Excel）
 * <p>
 * 平仓金额大于0
 * 平仓金额/手数，看每T价格，是否在4之内
 *
 * @author suncaixiang
 */
public class ClosePriceCommand implements AuditRuleCommand {


    private final static Integer PRICE_FLUCTUATION_RAGE = 4;

    private final static String TIPS = "平仓价在%sT以内，每T价格:%s,合约乘数为:%s,每手收益应在%s以上，当前每手收益:%s";

    @Override
    public AuditRuleExecuteResult execute(AuditTradeRecord auditTradeRecordDO) {

        //开仓的不看
        if (TradeCombOffsetEnum.OPEN.getCode().equals(auditTradeRecordDO.getTradeCombOffset())) {
            return AuditRuleExecuteResult.success(RuleTypeEnum.CLOSE_PRICE_LIMIT.getCode());
        }

        //收益小于0，不作处理
        if (auditTradeRecordDO.getCloseProfit().compareTo(BigDecimal.ZERO) <= 0) {
            return AuditRuleExecuteResult.success(RuleTypeEnum.CLOSE_PRICE_LIMIT.getCode());
        }

        //计算每手收益，在计算多少T可以达到这个收益，如果收益在规定的T之内，则无效
        RqInstrumentPriceTickBO rqInstrumentPriceTickBO = RqCache.getPriceTickCache(auditTradeRecordDO.getInstrumentId());
        RqInstrumentBO rqInstrumentBO = RqCache.queryOneInstrument(auditTradeRecordDO.getInstrumentId());

        //保留4位小数
        BigDecimal perVolumeProfit = auditTradeRecordDO.getCloseProfit().divide(new BigDecimal(auditTradeRecordDO.getTradeVolume()), 4, RoundingMode.HALF_UP);

        BigDecimal rulePerVolumeProfit = new BigDecimal(PRICE_FLUCTUATION_RAGE).multiply(new BigDecimal(rqInstrumentBO.getContractMultiplier())).multiply(new BigDecimal(rqInstrumentPriceTickBO.getTickSize()));
        if (perVolumeProfit.compareTo(rulePerVolumeProfit) > 0) {
            return AuditRuleExecuteResult.success(RuleTypeEnum.CLOSE_PRICE_LIMIT.getCode());
        }

        return AuditRuleExecuteResult.fail(RuleTypeEnum.CLOSE_PRICE_LIMIT.getCode(), String.format(TIPS, PRICE_FLUCTUATION_RAGE,
                rqInstrumentPriceTickBO.getTickSize(),
                rqInstrumentBO.getContractMultiplier(),
                rulePerVolumeProfit,
                perVolumeProfit));
    }
}
