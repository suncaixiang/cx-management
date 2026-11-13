package com.cx.management.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.cx.management.dto.TradeRecordDTO;
import com.cx.management.util.HistoryRqDayBarDataBO;
import org.springframework.util.CollectionUtils;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author suncaixiang
 */
public class ByFileParse extends PlatformFileParse<TradeRecordByDTO> {


    @Override
    public ExcelReader reader(InputStream inputStream, List<TradeRecordByDTO> dataList) {
        return EasyExcel.read(inputStream, TradeRecordByDTO.class, new TradeRecordByListener(dataList)).build();
    }

    @Override
    public Set<String> instrumentIdList(List<TradeRecordByDTO> dataList) {
        if (CollectionUtils.isEmpty(dataList)) {
            return Collections.emptySet();
        }
        return dataList.stream().map(TradeRecordByDTO::getInstrumentId).collect(Collectors.toSet());
    }

    @Override
    public List<TradeRecordDTO> convert(List<TradeRecordByDTO> dataList, Map<String, List<HistoryRqDayBarDataBO>> instrumentGroupDayBar) {
        if (CollectionUtils.isEmpty(dataList)) {
            return Collections.emptyList();
        }
        return dataList.stream().map(s -> {
            TradeRecordDTO tradeRecordDTO = s.clone(TradeRecordDTO.class);
            tradeRecordDTO.setTradingDay(tradingDay(s.getTradeTime(), s.getTradeDate(), instrumentGroupDayBar.get(s.getInstrumentId())));
            return tradeRecordDTO;
        }).collect(Collectors.toList());
    }
}
