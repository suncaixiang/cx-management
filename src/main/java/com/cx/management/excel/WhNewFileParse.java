package com.cx.management.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.cx.management.dto.TradeRecordDTO;
import com.cx.management.util.HistoryRqDayBarDataBO;
import com.cx.management.util.ObjectUtils;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author suncaixiang
 */
public class WhNewFileParse extends PlatformFileParse<TradeRecordWhNewDTO> {


    @Override
    public ExcelReader reader(InputStream inputStream, List<TradeRecordWhNewDTO> dataList) {
        return EasyExcel.read(inputStream, TradeRecordWhNewDTO.class, new TradeRecordWhNewListener(dataList)).build();
    }

    @Override
    public Set<String> instrumentIdList(List<TradeRecordWhNewDTO> dataList) {
        return dataList.stream().map(TradeRecordWhNewDTO::getInstrumentId).collect(Collectors.toSet());
    }

    @Override
    public List<TradeRecordDTO> convert(List<TradeRecordWhNewDTO> dataList, Map<String, List<HistoryRqDayBarDataBO>> instrumentGroupDayBar) {
        return ObjectUtils.convertList(dataList, TradeRecordDTO.class);
    }
}
