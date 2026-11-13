package com.cx.management.dto;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author suncaixiang
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class QueryMainInstrumentDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String productId;

    private String tradingDay;


    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QueryMainInstrumentDTO that = (QueryMainInstrumentDTO) o;
        return Objects.equals(productId, that.productId) && Objects.equals(tradingDay, that.tradingDay);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, tradingDay);
    }
}
