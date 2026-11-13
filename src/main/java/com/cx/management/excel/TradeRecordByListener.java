package com.cx.management.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.cx.management.util.ContractUtil;

import java.util.List;

/**
 * @author suncaixiang
 */
public class TradeRecordByListener extends AnalysisEventListener<TradeRecordByDTO> {

    private final List<TradeRecordByDTO> tradeRecordDTOList;

    public TradeRecordByListener(List<TradeRecordByDTO> tradeRecordDTOList) {
        this.tradeRecordDTOList = tradeRecordDTOList;
    }

    @Override
    public void invoke(TradeRecordByDTO tradeRecordByDTO, AnalysisContext analysisContext) {
        tradeRecordByDTO.setInstrumentId(ContractUtil.normalize(tradeRecordByDTO.getInstrumentId().toUpperCase()));
        tradeRecordDTOList.add(tradeRecordByDTO);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

    }

}
