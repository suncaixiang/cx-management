package com.cx.management.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;

/**
 * 米筐主力合约
 * https://www.ricequant.com/doc/rqdata/python/generic-api.html#get-price-%E8%8E%B7%E5%8F%96%E5%90%88%E7%BA%A6%E5%8E%86%E5%8F%B2%E8%A1%8C%E6%83%85%E6%95%B0%E6%8D%AE
 *
 * @author suncaixiang
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class RqDomainInstrumentBO implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 合约ID
     */
    @JsonProperty("dominant")
    private String dominant;

    /**
     * 日期
     */
    @JsonProperty("date")
    private String date;


}
