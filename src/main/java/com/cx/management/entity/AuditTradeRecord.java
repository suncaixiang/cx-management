package com.cx.management.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cx.management.util.AbstractObject;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成交订单 audit_trade_record
 */
@Data
@TableName("audit_trade_record")
public class AuditTradeRecord extends AbstractObject {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 审核记录ID */
    private Long auditRecordId;

    /** 用户账号 */
    private String accountNo;

    /** 平台 */
    private String platform;

    /** 合约ID */
    private String instrumentId;

    /** 合约产品ID */
    private String productId;

    /** 交易所 */
    private String exchange;

    /** 交易日期 */
    private String tradingDay;

    /** 成交日期 */
    private String tradeDate;

    /** 成交时间 */
    private String tradeTime;

    /** 成交手数 */
    private Integer tradeVolume;

    /** 成交价格 */
    private BigDecimal tradePrice;

    /** 买卖 */
    private String tradeDirection;

    /** 开平 */
    private String tradeCombOffset;

    /** 平仓收益 */
    private BigDecimal closeProfit;

    /** 成交序号 */
    private String transactionNum;

    /** 分析状态：0未分析 1分析中 2未通过 3通过 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime created;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime modified;
}
