package com.cx.management.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class TradingAccountQuickFundRequest {

    @NotBlank(message = "账号不能为空")
    private String account;

    /**
     * RESET_100W / RESET_500W / RESET_1000W / WITHDRAW_ALL
     */
    @NotBlank(message = "快捷操作类型不能为空")
    private String action;
}
