package com.cx.management.enums;

import lombok.Getter;

/**
 * @author suncaixiang
 */
@Getter
public enum AuditTradeRecordStatusEnum {
    INIT(0,"未分析"),
    DOING(1,"分析中"),
    FAIL(2,"未通过"),
    PASS(3,"通过"),

    ;

    private Integer code;

    private String desc;

    AuditTradeRecordStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String labelOf(Integer code) {
        if (code == null) {
            return "-";
        }
        for (AuditTradeRecordStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status.desc;
            }
        }
        return String.valueOf(code);
    }
}
