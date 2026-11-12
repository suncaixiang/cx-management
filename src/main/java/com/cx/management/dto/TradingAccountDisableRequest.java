package com.cx.management.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class TradingAccountDisableRequest {

    @NotBlank(message = "账号不能为空")
    private String account;
}
