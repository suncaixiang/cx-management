package com.cx.management.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AccountScoreDailyVO {

    private Long id;

    private Long configId;

    private String account;

    private LocalDate settleDate;

    private BigDecimal baseCapital;

    private BigDecimal marketEquity;

    private BigDecimal rawPct;

    private BigDecimal scorePct;
}
