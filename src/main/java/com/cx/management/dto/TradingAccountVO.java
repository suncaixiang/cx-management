package com.cx.management.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TradingAccountVO {

    private String account;

    private BigDecimal balance;

    private BigDecimal baseCapital;

    private BigDecimal credit;

    /** 当日最高动态权益 MaxBalance */
    private BigDecimal maxBalance;

    /** 历史最高动态权益 EverMaxBalance */
    private BigDecimal everMaxBalance;

    /** 历史最高结算权益 SettleMaxBalance */
    private BigDecimal settleMaxBalance;
}
