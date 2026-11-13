package com.cx.management.dto;

import com.cx.management.util.AbstractObject;
import lombok.Data;

/**
 * 成交记录参数
 *
 * @author suncaixiang
 */
@Data
public class TradeRecordDTO extends AbstractObject {


    private static final long serialVersionUID = 1L;


    private String instrumentId;

    private String productId;

    private String exchange;

    private String tradingDay;


    /**
     * 交易日期
     */
    private String tradeDate;

    /**
     * 交易时间
     */
    private String tradeTime;

    /**
     * 成交数量
     */
    private String tradeVolume;

    /**
     * 成交价
     */
    private String tradePrice;

    /**
     * 买卖
     */
    private String tradeDirection;

    /**
     * 开平
     */
    private String tradeCombOffset;

    /**
     * 平仓收益
     */
    private String closeProfit;

    /**
     * 成交序号
     */
    private String transactionNum;
}
