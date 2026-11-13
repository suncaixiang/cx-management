package com.cx.management.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.cx.management.util.AbstractObject;
import lombok.Data;

/**
 * 文华新成交记录
 *
 * @author suncaixiang
 */
@Data
public class TradeRecordWhNewDTO extends AbstractObject {
    private static final long serialVersionUID = 1L;


    @ExcelProperty(value = "合约")
    private String instrumentId;


    /**
     * 交易日期
     */
    @ExcelProperty(value = "交易日")
    private String tradingDay;

    /**
     * 交易时间
     */
    @ExcelProperty(value = "时间")
    private String tradeTime;

    /**
     * 成交数量
     */
    @ExcelProperty(value = "成交量")
    private String tradeVolume;

    /**
     * 成交价
     */
    @ExcelProperty(value = "成交价")
    private String tradePrice;



    /**
     * 买卖
     */
    @ExcelProperty(value = "买卖")
    private String tradeDirection;

    /**
     * 开平
     */
    @ExcelProperty(value = "开平")
    private String tradeCombOffset;

    /**
     * 平仓收益
     */
    @ExcelProperty(value = "平仓盈亏(逐笔)")
    private String closeProfit;

    /**
     * 成交序号
     */
    @ExcelProperty(value = "主场号")
    private String transactionNum;
}
