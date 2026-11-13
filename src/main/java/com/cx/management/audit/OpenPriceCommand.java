package com.cx.management.audit;

import com.cx.management.entity.AuditTradeRecord;
import com.cx.management.enums.RuleTypeEnum;
import com.cx.management.enums.TradeDirectionEnum;
import com.cx.management.util.HistoryRqDayBarDataBO;
import com.cx.management.util.RqCache;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 是否存在涨跌停板附近开仓的行为
 *
 * @author suncaixiang
 */
@Primary
public class OpenPriceCommand implements AuditRuleCommand {


    private final static Integer DIFFERENCE_PERCENTAGE = 1;

    @Override
    public AuditRuleExecuteResult execute(AuditTradeRecord auditTradeRecordDO) {
        //日线数据
        HistoryRqDayBarDataBO historyRqDayBarDataBO = RqCache.queryOneDayHistoryBarData(auditTradeRecordDO.getInstrumentId(), auditTradeRecordDO.getTradingDay());
        if (TradeDirectionEnum.BUY.getCode().equals(auditTradeRecordDO.getTradeDirection())) {
            double minPriceProportion = 1.0 + DIFFERENCE_PERCENTAGE / 100.0;
            BigDecimal minPrice = new BigDecimal(historyRqDayBarDataBO.getLimitDown()).multiply(new BigDecimal(minPriceProportion)).setScale(4, RoundingMode.HALF_UP);

            //在区间氛围，价格合理
            if (minPrice.compareTo(auditTradeRecordDO.getTradePrice()) >= 0) {
                return AuditRuleExecuteResult.fail(RuleTypeEnum.OPEN_PRICE_LIMIT.getCode(), "跌停板%1附近开仓,交易日跌停价为" + historyRqDayBarDataBO.getLimitDown());
            }

        } else {
            double maxPriceProportion = 1.0 - DIFFERENCE_PERCENTAGE / 100.0;
            BigDecimal maxPrice = new BigDecimal(historyRqDayBarDataBO.getLimitUp()).multiply(new BigDecimal(maxPriceProportion)).setScale(4, RoundingMode.HALF_UP);

            if (maxPrice.compareTo(auditTradeRecordDO.getTradePrice()) <= 0) {
                return AuditRuleExecuteResult.fail(RuleTypeEnum.OPEN_PRICE_LIMIT.getCode(), "涨停板%1附近平仓,交易日涨停价为" + historyRqDayBarDataBO.getLimitUp());
            }
        }

        return AuditRuleExecuteResult.success(RuleTypeEnum.OPEN_PRICE_LIMIT.getCode());

    }
}
