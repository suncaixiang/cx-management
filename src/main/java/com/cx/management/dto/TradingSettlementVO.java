package com.cx.management.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TradingSettlementVO {

    private String account;

    /** 交易日 YYYYMMDD */
    private String date;

    /** 市值权益 */
    private BigDecimal marketEquity;

    /** 原始 Contents 文本（调试用，可不返回前端） */
    private String contents;
}
