package com.cx.management.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class TradingAccountCapitalVO {

    private Map<String, BigDecimal> capitals;

    /** 当前权益（Balance） */
    private Map<String, BigDecimal> balances;
}
