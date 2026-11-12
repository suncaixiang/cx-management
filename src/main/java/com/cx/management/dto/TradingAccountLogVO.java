package com.cx.management.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TradingAccountLogVO {

    private Long id;

    private String account;

    private String remark;

    private String operator;

    private LocalDateTime created;
}
