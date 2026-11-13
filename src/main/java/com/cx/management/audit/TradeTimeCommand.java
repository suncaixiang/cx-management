package com.cx.management.audit;

import com.cx.management.dto.TimeRangeBO;
import com.cx.management.entity.AuditTradeRecord;
import com.cx.management.enums.RuleTypeEnum;
import com.cx.management.util.RqCache;
import com.cx.management.util.RqInstrumentBO;
import com.google.common.collect.Lists;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 交易时间段
 *
 * @author suncaixiang
 */
public class TradeTimeCommand implements AuditRuleCommand {


    @Override
    public AuditRuleExecuteResult execute(AuditTradeRecord auditTradeRecordDO) {
        RqInstrumentBO rqInstrumentBO = RqCache.queryOneInstrument(auditTradeRecordDO.getInstrumentId());
        if (trading(rqInstrumentBO.getTradingHours(), auditTradeRecordDO.getTradeTime())) {
            return AuditRuleExecuteResult.success(RuleTypeEnum.TRADE_TIME_LIMIT.getCode());
        }
        return AuditRuleExecuteResult.fail(RuleTypeEnum.TRADE_TIME_LIMIT.getCode(), "非交易时间" + auditTradeRecordDO.getTradeTime());
    }


    /**
     * 判断是否交易时间
     *
     * @param tradingTime 交易时间
     * @param tradeTime   成交时间
     * @return true：是
     */
    public static Boolean trading(String tradingTime, String tradeTime) {
        List<TimeRangeBO> timeRangeBOList = parse(tradingTime);
        if (CollectionUtils.isEmpty(timeRangeBOList)) {
            return Boolean.FALSE;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm:ss");
        LocalTime nowTime = LocalTime.parse(tradeTime, formatter);
        for (TimeRangeBO timeRangeBO : timeRangeBOList) {
            if (timeRangeBO.contains(nowTime)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }


    /**
     * tradingTime格式如下
     * 21:00-02:30,09:00-10:15,10:30-11:30,13:30-15:00
     * <p>
     * 米筐给的交易时间有问题，开始时间都多加了1分钟
     *
     * @param tradingTime 交易时间
     * @return 交易范围
     */
    public static List<TimeRangeBO> parse(String tradingTime) {
        if (!StringUtils.hasText(tradingTime)) {
            return Lists.newArrayList();
        }
        List<TimeRangeBO> list = Lists.newArrayList();
        String[] times = tradingTime.split(",");
        for (String time : times) {
            String[] arr = time.split("-");
            list.add(TimeRangeBO.of(arr[0], arr[1]));
        }
        return list;
    }

}


