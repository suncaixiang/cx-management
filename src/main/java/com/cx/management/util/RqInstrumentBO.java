package com.cx.management.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;

/**
 * 米筐合约信息
 *
 * @author : scx
 * @date : 2025-07-22
 * Description:
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class RqInstrumentBO implements Serializable {
    private static final long serialVersionUID = 7680992109249661591L;


    /**
     * 序号
     */
    @JsonProperty("id")
    private String id;

    /**
     * 期货代码
     */
    @JsonProperty("order_book_id")
    private String orderBookId;


    /**
     * 合约类型，'Future'
     */
    @JsonProperty("type")
    private String type;

    /**
     * 交易所，'DCE' - 大连商品交易所, 'SHFE' - 上海期货交易所，'CFFEX' - 中国金融期货交易所, 'CZCE'- 郑州商品交易所, 'INE' - 上海国际能源交易中心
     */
    @JsonProperty("exchange")
    private String exchange;


    /**
     * 期货的简称，例如'沪深 1005'
     */
    @JsonProperty("symbol")
    private String symbol;


    /**
     * 合约标的名称，例如 IF1005 的合约标的名称为'IF'
     */
    @JsonProperty("underlying_symbol")
    private String underlyingSymbol;


    /**
     * 合约标的代码，目前除股指期货(IH, IF, IC)之外的期货合约，这一字段全部为'null'
     */
    @JsonProperty("underlying_order_book_id")
    private String underlyingOrderBookId;


    /**
     * 合约种类，'Commodity'-商品期货，'Index'-股指期货，'Government'-国债期货
     */
    @JsonProperty("product")
    private String product;


    /**
     * 期货到期日。主力连续合约与指数连续合约都为'0000-00-00'
     */
    @JsonProperty("maturity_date")
    private String maturityDate;



    /**
     * 期货合约的最低保证金率
     */
    @JsonProperty("margin_rate")
    private String marginRate;



    /**
     * 期货全部为 1.0
     */
    @JsonProperty("round_lot")
    private String roundLot;



    /**
     * 期货的上市日期。
     */
    @JsonProperty("listed_date")
    private String listedDate;



    /**
     * 期货的退市日期。
     */
    @JsonProperty("de_listed_date")
    private String deListedDate;



    /**
     * 合约乘数，例如沪深 300 股指期货的乘数为 300.0。
     */
    @JsonProperty("contract_multiplier")
    private String contractMultiplier;


    /**
     * 交易代码。
     */
    @JsonProperty("trading_code")
    private String tradingCode;


    /**
     * 交易制度。'0'表示 T+0，'1'表示 T+1，往后顺推。
     */
    @JsonProperty("market_tplus")
    private String marketTplus;


    /**
     * 行业分类名称。
     */
    @JsonProperty("industry_name")
    private String industryName;


    /**
     * 开始交割日。
     */
    @JsonProperty("start_delivery_date")
    private String startDeliveryDate;



    /**
     * 结束交割日。
     */
    @JsonProperty("end_delivery_date")
    private String endDeliveryDate;



    /**
     * 合约最新的交易时间，如需历史数据请使用get_trading_hours。
     */
    @JsonProperty("trading_hours")
    private String tradingHours;




}
