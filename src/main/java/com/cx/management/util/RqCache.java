package com.cx.management.util;


import com.alibaba.fastjson.JSON;
import com.cx.management.dto.QueryInstrumentDayBarDTO;
import com.cx.management.dto.QueryMainInstrumentDTO;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author suncaixiang
 */
@Slf4j
public class RqCache {
    /**
     * 使用guava cache
     * 获取token信息
     */
    private final static LoadingCache<String, String> TOKEN_CACHE =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(1, TimeUnit.HOURS)
                    .build(new CacheLoader<String, String>() {
                        @Override
                        public String load(String day) {
                            return RqApi.getToken();
                        }
                    });

    //priceTick 缓存
    private final static LoadingCache<String, RqInstrumentPriceTickBO> PRICE_TICK_CACHE =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .build(new CacheLoader<String, RqInstrumentPriceTickBO>() {
                        @Override
                        public RqInstrumentPriceTickBO load(String instrumentId) {
                            return RqApi.getOnePriceTick(instrumentId, getTodayRqToken());
                        }
                    });


    //获取主力、次主力合约缓存
    private final static LoadingCache<QueryMainInstrumentDTO, List<String>> MAIN_INSTRUMENT =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .build(new CacheLoader<QueryMainInstrumentDTO, List<String>>() {
                        @Override
                        public List<String> load(QueryMainInstrumentDTO dto) {
                            return RqApi.queryMainInstrument(dto.getProductId(), dto.getTradingDay(), getTodayRqToken());
                        }
                    });

    /**
     * 查询历史日线数据
     */
    private final static LoadingCache<QueryInstrumentDayBarDTO, HistoryRqDayBarDataBO> BAR_DAT_CACHE =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .build(new CacheLoader<QueryInstrumentDayBarDTO, HistoryRqDayBarDataBO>() {
                        @Override
                        public HistoryRqDayBarDataBO load(QueryInstrumentDayBarDTO dto) {
                            return RqApi.queryOneDayHistoryBarData(dto.getInstrumentId(), dto.getTradingDay(), getTodayRqToken());
                        }
                    });

    /**
     * 查询历史日线数据
     */
    private final static LoadingCache<String, RqInstrumentBO> INSTRUMENT_CACHE =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .build(new CacheLoader<String, RqInstrumentBO>() {
                        @Override
                        public RqInstrumentBO load(String instrumentId) {
                            return RqApi.queryOne(instrumentId, getTodayRqToken());
                        }
                    });


    public static String getTodayRqToken() {
        return getRqToken(LocalDate.now().toString());
    }

    private static String getRqToken(String day) {
        try {
            return TOKEN_CACHE.get(day);
        } catch (Exception e) {
            return RqApi.getToken();
        }
    }


    /**
     * 获取priceTick
     */
    public static RqInstrumentPriceTickBO getPriceTickCache(String instrumentId) {
        try {
            return PRICE_TICK_CACHE.get(instrumentId);
        } catch (Exception e) {
            return RqApi.getOnePriceTick(instrumentId, getTodayRqToken());
        }
    }

    /**
     * 获取主力、次主力合约
     */
    public static List<String> queryMainInstrumentIdList(String productId, String tradingDay) {
        try {
            return MAIN_INSTRUMENT.get(QueryMainInstrumentDTO.builder().productId(productId).tradingDay(tradingDay).build());
        } catch (Exception e) {
            return RqApi.queryMainInstrument(productId, tradingDay, getTodayRqToken());
        }
    }


    public static List<HistoryRqDayBarDataBO> queryDayHistoryBarData(Collection<String> orderBookIdList,
                                                                     String startDate,
                                                                     String endDate) {
        List<HistoryRqDayBarDataBO> historyRqDayBarDataBOList = RqApi.queryDayHistoryBarData(orderBookIdList, startDate, endDate, getTodayRqToken());
        log.info("查询到日线数据:{},合约{},开始时间:{},结束时间:{}", JSON.toJSONString(historyRqDayBarDataBOList), JSON.toJSONString(orderBookIdList), startDate, endDate);
        //缓存下来
        historyRqDayBarDataBOList.forEach(s -> {
            BAR_DAT_CACHE.put(QueryInstrumentDayBarDTO.builder().instrumentId(s.getOrderBookId()).tradingDay(s.getDatetime()).build(), s);
        });
        return historyRqDayBarDataBOList.stream()
                .sorted(Comparator.comparing(HistoryRqDayBarDataBO::getDatetime)).collect(Collectors.toList());
    }


    public static HistoryRqDayBarDataBO queryOneDayHistoryBarData(String instrumentId, String tradingDay) {
        try {
            return BAR_DAT_CACHE.get(QueryInstrumentDayBarDTO.builder().instrumentId(instrumentId).tradingDay(tradingDay).build());
        } catch (Exception e) {
            return RqApi.queryOneDayHistoryBarData(instrumentId, tradingDay, getTodayRqToken());
        }
    }


    public static List<RqInstrumentBO> queryInstrumentList(Collection<String> instrumentIdList) {
        List<RqInstrumentBO> instrumentBOList = RqApi.queryInstrument(instrumentIdList, getTodayRqToken());

        //缓存下来
        instrumentBOList.forEach(s -> {
            INSTRUMENT_CACHE.put(s.getOrderBookId(), s);
        });
        return instrumentBOList;
    }


    public static RqInstrumentBO queryOneInstrument(String instrumentId) {
        try {
            return INSTRUMENT_CACHE.get(instrumentId);
        } catch (Exception e) {
            return RqApi.queryOne(instrumentId, getTodayRqToken());
        }
    }


}
