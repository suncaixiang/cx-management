package com.cx.management.enums;

import lombok.Getter;

/**
 * @author suncaixiang
 */
@Getter
public enum RuleTypeEnum {

    OPEN_PRICE_LIMIT("OPEN_PRICE_LIMIT","涨跌停板附近行为"),
    MAIN_FUTURES_LIMIT("MAIN_FUTURES_LIMIT","非主力合约交易行为"),
    SUPPORT_PRODUCT_LIMIT("SUPPORT_PRODUCT_LIMIT","交易不支持品种行为"),
    TRADE_TIME_LIMIT("TRADE_TIME_LIMIT","非交易时间成交行为"),
    CLOSE_PRICE_LIMIT("CLOSE_PRICE_LIMIT","刷单行为"),

    ;

    private String code;

    private String desc;


    RuleTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
