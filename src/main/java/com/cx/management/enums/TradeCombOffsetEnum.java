package com.cx.management.enums;

import lombok.Getter;

/**
 * @author suncaixiang
 */
@Getter
public enum TradeCombOffsetEnum {

    OPEN("开"),
    CLOSE_TODAY("平今"),
    CLOSE_YESTERDAY("平"),

    ;

    private String code;

    TradeCombOffsetEnum(String code) {
        this.code = code;
    }
}
