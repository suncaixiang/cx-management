package com.cx.management.excel;


import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.fastjson.JSON;
import com.cx.management.common.exception.BusinessException;
import com.cx.management.dto.AddAuditRecordDTO;
import com.cx.management.dto.TradeRecordDTO;
import com.cx.management.util.AuditDateUtil;
import com.cx.management.util.HistoryRqDayBarDataBO;
import com.cx.management.util.RqCache;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 审核记录文件解析
 *
 * @author suncaixiang
 */
@Slf4j
public abstract class PlatformFileParse<T> {


    /**
     * 读取文件数据
     **/
    public List<TradeRecordDTO> readExcel(AddAuditRecordDTO dto) {

        List<T> dataList = readExcel(dto.getMultipartFile());

        Set<String> instrumentIdSet = instrumentIdList(dataList);

        //获取米筐日线数据
        Map<String, List<HistoryRqDayBarDataBO>> instrumentGroupDayBar = queryRqDayBar(instrumentIdSet, dto.getTradeStartDate(), dto.getTradeEndDate());
        if (CollectionUtils.isEmpty(instrumentGroupDayBar)) {

        }
        return convert(dataList, instrumentGroupDayBar);
    }


    /**
     * 读取Excel数据
     */
    private List<T> readExcel(MultipartFile multipartFile) {
        //读取Excel文件数据
        List<T> dataList = Lists.newArrayList();
        try (ExcelReader reader = reader(multipartFile.getInputStream(), dataList)) {
            // 获取所有sheet
            List<ReadSheet> sheetList = reader.excelExecutor().sheetList();
            for (ReadSheet sheet : sheetList) {
                ReadSheet readSheet = EasyExcel.readSheet(sheet.getSheetNo()).build();
                reader.read(readSheet);
            }
        } catch (Exception e) {
            log.info("数据读取异常,msg:{}", e.getMessage(), e);
            throw new BusinessException("请检查文件格式是否正确");
        }
        return dataList;
    }


    /**
     * 读取文件数据
     */
    public abstract ExcelReader reader(InputStream inputStream, List<T> dataList);

    /**
     * 合约列表
     */
    public abstract Set<String> instrumentIdList(List<T> dataList);

    /**
     * 数据转换
     */
    public abstract List<TradeRecordDTO> convert(List<T> dataList, Map<String, List<HistoryRqDayBarDataBO>> instrumentGroupDayBar);


    private Map<String, List<HistoryRqDayBarDataBO>> queryRqDayBar(Collection<String> instrumentIdList, String startDate, String endDate) {
        List<HistoryRqDayBarDataBO> historyRqDayBarDataBOList = RqCache.queryDayHistoryBarData(instrumentIdList, startDate, endDate);
        if (CollectionUtils.isEmpty(historyRqDayBarDataBOList)) {
            log.error("未查询到日线数据，InstrumentIdList:{},startDate:{},endDate:{}", JSON.toJSONString(instrumentIdList), startDate, endDate);
            throw new BusinessException("根据合约、开始日期、结束日期未查询到日线数据");
        }
        return historyRqDayBarDataBOList.stream()
                .peek(s -> s.setDatetime(s.getDatetime().replace("-", "")))
                .sorted(Comparator.comparing(HistoryRqDayBarDataBO::getDatetime))
                .collect(Collectors.groupingBy(HistoryRqDayBarDataBO::getOrderBookId));

    }


    static String tradingDay(String tradeTime, String tradeDate, List<HistoryRqDayBarDataBO> historyRqDayBarDataBOList) {
        //数据格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(AuditDateUtil.DATE_TIME_FORMAT);

        //超过15点，是下个交易日
        LocalTime nightStartLocalTime = LocalTime.parse(AuditDateUtil.NIGHT_START_TIME, formatter);
        LocalTime tradeLocalTime = LocalTime.parse(tradeTime, formatter);

        //夜盘开启之后，下个交易日
        if (!tradeLocalTime.isBefore(nightStartLocalTime)) {
            return nextTradingDay(tradeDate, historyRqDayBarDataBOList);
        }

        // 夜盘凌晨
        LocalTime dayStartLocalTime = LocalTime.parse(AuditDateUtil.DAY_START_TIME, formatter);
        if (tradeLocalTime.isBefore(dayStartLocalTime)) {
            LocalDate localDate = LocalDate.parse(tradeDate, DateTimeFormatter.ofPattern(AuditDateUtil.DATE_FORMAT));
            return nextTradingDay(localDate.minusDays(1).format(DateTimeFormatter.ofPattern(AuditDateUtil.DATE_FORMAT)), historyRqDayBarDataBOList);
        }

        return tradeDate;
    }


    static String nextTradingDay(String tradeDate, List<HistoryRqDayBarDataBO> historyRqDayBarDataBOList) {

        for (int i = 0; i < historyRqDayBarDataBOList.size(); i++) {
            if (historyRqDayBarDataBOList.get(i).getDatetime().equals(tradeDate)
                    && i + 1 < historyRqDayBarDataBOList.size()) {
                return historyRqDayBarDataBOList.get(i + 1).getDatetime();
            }
        }
        return tradeDate;
    }
}

