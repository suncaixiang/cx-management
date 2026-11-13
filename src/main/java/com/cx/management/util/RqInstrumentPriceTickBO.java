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
public class RqInstrumentPriceTickBO implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * 期货代码
     */
    @JsonProperty("order_book_id")
    private String orderBookId;


    /**
     * tick_size
     */
    @JsonProperty("tick_size")
    private String tickSize;


}
