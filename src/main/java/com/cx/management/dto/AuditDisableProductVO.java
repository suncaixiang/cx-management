package com.cx.management.dto;

import lombok.Data;

@Data
public class AuditDisableProductVO {

    private Long id;

    private String platform;

    private String platformLabel;

    private String productId;

    private String productName;
}
