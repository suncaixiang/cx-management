package com.cx.management.dto;

import lombok.Data;

@Data
public class AuditPlatformVO {

    private String code;

    private String label;

    public AuditPlatformVO() {
    }

    public AuditPlatformVO(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
