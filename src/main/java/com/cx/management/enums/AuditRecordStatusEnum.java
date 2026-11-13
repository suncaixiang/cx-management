package com.cx.management.enums;

import lombok.Getter;

/**
 * @author suncaixiang
 */
@Getter
public enum AuditRecordStatusEnum {
    INIT(0,"初始化"),
    DOING(1,"进行中"),
    DONE(2,"审核完成"),
    ;

    private Integer code;

    private String desc;

    AuditRecordStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String labelOf(Integer code) {
        if (code == null) {
            return "-";
        }
        for (AuditRecordStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status.desc;
            }
        }
        return String.valueOf(code);
    }
}
