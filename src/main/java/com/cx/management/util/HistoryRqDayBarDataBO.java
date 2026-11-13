package com.cx.management.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;

/**
 * 米筐日bar数据
 * https://www.ricequant.com/doc/rqdata/python/generic-api.html#get-price-%E8%8E%B7%E5%8F%96%E5%90%88%E7%BA%A6%E5%8E%86%E5%8F%B2%E8%A1%8C%E6%83%85%E6%95%B0%E6%8D%AE
 * @author suncaixiang
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class HistoryRqDayBarDataBO implements Serializable {

    private static final long serialVersionUID = 6446562143071483485L;
    /**
     * 合约ID
     */
    @JsonProperty("order_book_id")
    private String orderBookId;

    /**
     * 日期
     */
    @JsonProperty("datetime")
    private String datetime;


    /**
     * 日盘开盘价（仅限期货期权日线数据）
     */
    @JsonProperty("day_session_open")
    private String daySessionOpen;


    /**
     * 开盘价
     */
    @JsonProperty("open")
    private String open;

    /**
     * 收盘价
     */
    @JsonProperty("close")
    private String close;


    /**
     * 最高价
     */
    @JsonProperty("high")
    private String high;

    /**
     * 最低价
     */
    @JsonProperty("low")
    private String low;


    /**
     * 涨停价
     */
    @JsonProperty("limit_up")
    private String limitUp;


    /**
     * 跌停价
     */
    @JsonProperty("limit_down")
    private String limitDown;


    /**
     * 累计持仓量（期货期权专用）
     */
    @JsonProperty("open_interest")
    private String openInterest;

    /**
     * 昨日收盘价 （交易所披露的原始昨收价，复权方法对该字段无效）
     */
    @JsonProperty("prev_close")
    private String prevClose;


    /**
     * 成交额
     */
    @JsonProperty("total_turnover")
    private String totalTurnover;


    /**
     * 成交量
     */
    @JsonProperty("volume")
    private String volume;

    /**
     * 结算价 （仅限期货期权日线数据）
     */
    @JsonProperty("settlement")
    private String settlement;

    /**
     * 昨日结算价（仅限期货期权日线数据）
     */
    @JsonProperty("prev_settlement")
    private String prevSettlement;
}
