package com.cx.management.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AuditDisableProductRequest {

    @NotBlank(message = "请选择平台")
    private String platform;

    @NotBlank(message = "请输入品种代码")
    private String productId;

    @NotBlank(message = "请输入品种名称")
    private String productName;
}
