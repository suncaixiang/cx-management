package com.cx.management.enums;

import lombok.Getter;

/**
 * @author suncaixiang
 */
@Getter
public enum TradeDirectionEnum {


    BUY("买"),
    SELL("卖"),
    ;

    private String code;

    TradeDirectionEnum(String code) {
        this.code = code;
    }
}
