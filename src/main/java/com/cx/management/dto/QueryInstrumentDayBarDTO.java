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
public class QueryInstrumentDayBarDTO implements Serializable {

    private String instrumentId;

    private String tradingDay;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QueryInstrumentDayBarDTO that = (QueryInstrumentDayBarDTO) o;
        return Objects.equals(instrumentId, that.instrumentId) && Objects.equals(tradingDay, that.tradingDay);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instrumentId, tradingDay);
    }
}
