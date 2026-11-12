package com.cx.management.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AccountScoreConfigVO {

    private Long id;

    private String account;

    private String name;

    private LocalDate assessStart;

    private BigDecimal totalScorePct;

    private Integer scoreDays;

    private LocalDate lastSettleDate;

    private Integer status;

    private String stopReason;

    private LocalDateTime stopTime;

    private String operator;

    private LocalDateTime created;
}
