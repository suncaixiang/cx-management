package com.cx.management.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.cx.management.enums.TradeCombOffsetEnum;
import com.cx.management.util.ContractUtil;

import java.util.List;

/**
 * @author suncaixiang
 */
public class TradeRecordWhNewListener extends AnalysisEventListener<TradeRecordWhNewDTO> {

    private final List<TradeRecordWhNewDTO> tradeRecordDTOList;

    public TradeRecordWhNewListener(List<TradeRecordWhNewDTO> tradeRecordDTOList) {

        this.tradeRecordDTOList = tradeRecordDTOList;
    }

    @Override
    public void invoke(TradeRecordWhNewDTO tradeRecordWhNewDTO, AnalysisContext analysisContext) {
        //如果是开仓
        if (TradeCombOffsetEnum.OPEN.getCode().equals(tradeRecordWhNewDTO.getTradeCombOffset())) {
            tradeRecordWhNewDTO.setCloseProfit("0");
        }
        tradeRecordWhNewDTO.setInstrumentId(ContractUtil.normalize(tradeRecordWhNewDTO.getInstrumentId().toUpperCase()));
        tradeRecordDTOList.add(tradeRecordWhNewDTO);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

    }
}
