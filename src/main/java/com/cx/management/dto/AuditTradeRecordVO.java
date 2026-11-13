package com.cx.management.dto;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.cx.management.util.AbstractObject;
import lombok.*;

import java.math.BigDecimal;

/**
 * 审核成交记录
 *
 * @author suncaixiang
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AuditTradeRecordVO extends AbstractObject {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @ExcelIgnore
    private Long id;


    /**
     * 审核账号
     */
    @ExcelProperty(value = "账号", index = 0)
    private String accountNo;

    /**
     * 平台
     */
    @ExcelIgnore
    private String platform;

    /**
     * 合约ID
     */
    @ExcelProperty(value = "合约", index = 1)
    private String instrumentId;

    /**
     * 产品ID
     */
    @ExcelProperty(value = "产品", index = 2)
    private String productId;

    /**
     * 交易所
     */
    @ExcelIgnore
    private String exchange;

    /**
     * 交易日
     */
    @ExcelProperty(value = "交易日", index = 3)
    private String tradingDay;

    /**
     * 交易日期
     */
    @ExcelIgnore
    private String tradeDate;

    /**
     * 交易时间
     */
    @ExcelProperty(value = "成交时间", index = 4)
    private String tradeTime;

    /**
     * 成交数量
     */
    @ExcelProperty(value = "手数", index = 5)
    private Integer tradeVolume;

    /**
     * 成交价
     */
    @ExcelProperty(value = "成交价", index = 6)
    private BigDecimal tradePrice;


    /**
     * 买卖
     */
    @ExcelProperty(value = "买卖", index = 7)
    private String tradeDirection;

    /**
     * 开平
     */
    @ExcelProperty(value = "开平", index = 8)
    private String tradeCombOffset;

    /**
     * 平仓收益
     */
    @ExcelProperty(value = "平仓收益", index = 9)
    private BigDecimal closeProfit;

    /**
     * 成交序号
     */
    @ExcelProperty(value = "成交序号", index = 10)
    private String transactionNum;

    /**
     * 分析状态
     */
    @ExcelIgnore
    private Integer status;


    @ExcelProperty(value = "触发详情", index = 11)
    private String triggerDetail;

}