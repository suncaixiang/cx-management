package com.cx.management.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class TradingAccountResetMaxBalanceRequest {

    @NotBlank(message = "账号不能为空")
    private String account;

    /**
     * EVER_MAX 历史最高动态权益
     * DAY_MAX 当日最高动态权益
     * SETTLE_MAX 历史最高结算权益
     */
    @NotBlank(message = "复位类型不能为空")
    private String type;
}
