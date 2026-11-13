package com.cx.management.enums;

import lombok.Getter;

@Getter
public enum AuditPlatform {

    B_Y("B_Y", "博易"),
    W_H_NEW("W_H_NEW", "文华(新)");

    private final String code;
    private final String label;

    AuditPlatform(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static AuditPlatform fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AuditPlatform platform : values()) {
            if (platform.code.equalsIgnoreCase(code.trim())) {
                return platform;
            }
        }
        return null;
    }

    public static boolean isValid(String code) {
        return fromCode(code) != null;
    }
}
