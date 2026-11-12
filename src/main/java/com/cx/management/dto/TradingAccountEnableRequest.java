package com.cx.management.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class TradingAccountEnableRequest {

    @NotBlank(message = "账号不能为空")
    private String account;

    /** 名称，可空串表示清空 */
    private String name;

    /** 空或不传表示不改密码 */
    private String password;

    /** 备注，可空串表示清空 */
    private String comment;

    /** 分组 ID，可选 */
    private String groupId;

    /** 目标期初资金（可高于或低于当前值） */
    @NotNull(message = "期初资金不能为空")
    private BigDecimal baseCapital;
}
