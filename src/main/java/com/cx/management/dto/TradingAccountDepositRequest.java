package com.cx.management.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class TradingAccountDepositRequest {

    @NotBlank(message = "账号不能为空")
    private String account;

    /** IN 入金 / OUT 出金 */
    @NotBlank(message = "类型不能为空")
    private String type;

    /** 金额，须为正数 */
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;
}
