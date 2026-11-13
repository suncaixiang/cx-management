package com.cx.management.util;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.cx.management.common.exception.BusinessException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * https://www.ricequant.com/doc/rqdata/python/generic-api.html#get-price-%E8%8E%B7%E5%8F%96%E5%90%88%E7%BA%A6%E5%8E%86%E5%8F%B2%E8%A1%8C%E6%83%85%E6%95%B0%E6%8D%AE
 *
 * @author : scx
 * @date : 2025-07-18
 * Description: 米筐远程调用
 */
@Slf4j
public class RqApi {
    private static String RETURN_VALID = "invalid";


    /**
     * 获取api调用token
     *
     * @return token
     */
    public static String getToken() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("user_name", RqHttpConstant.R_Q_USER_NAME);
        paramMap.put("password", RqHttpConstant.R_Q_PASSWORD);
        return HttpUtil.post(RqHttpConstant.R_Q_AUTH_URL, JSON.toJSONString(paramMap));
    }


    public static RqInstrumentPriceTickBO getOnePriceTick(String orderBookId, String token) {
        List<RqInstrumentPriceTickBO> priceTickList = getPriceTick(Lists.newArrayList(orderBookId), token);
        return CollectionUtils.isEmpty(priceTickList) ? null : priceTickList.get(0);
    }

    /**
     * 获取合约变动最小单位
     *
     * @param orderBookIdList 合约ID
     * @param token           认证token
     * @return 合约详情
     */
    public static List<RqInstrumentPriceTickBO> getPriceTick(List<String> orderBookIdList, String token) {
        Map<String, Object> apiParamMap = Maps.newHashMap();
        apiParamMap.put("method", "get_tick_size");
        apiParamMap.put("order_book_ids", orderBookIdList);
        String result = RqApi.getRqApiResult(apiParamMap, token);
        return CsvUtil.convertCsvToBean(result, RqInstrumentPriceTickBO.class);

    }

    /**
     * 获取合约ID详情
     *
     * @param orderBookId 合约ID
     * @param token       认证token
     * @return 合约详情
     */
    public static RqInstrumentBO queryOne(String orderBookId, String token) {
        List<RqInstrumentBO> rqInstrumentBOList = queryInstrument(Lists.newArrayList(orderBookId), token);
        return CollectionUtils.isEmpty(rqInstrumentBOList) ? null : rqInstrumentBOList.get(0);
    }


    /**
     * 获取合约ID详情
     *
     * @param orderBookIdList 合约ID
     * @param token           认证token
     * @return 合约详情
     */
    public static List<RqInstrumentBO> queryInstrument(Collection<String> orderBookIdList, String token) {
        Map<String, Object> apiParamMap = Maps.newHashMap();
        apiParamMap.put("method", "instruments");
        apiParamMap.put("order_book_ids", orderBookIdList);
        String result = RqApi.getRqApiResult(apiParamMap, token);
        return CsvUtil.convertCsvToBean(result, RqInstrumentBO.class);
    }


    public static HistoryRqDayBarDataBO queryOneDayHistoryBarData(String orderBookId, String queryDate, String token) {
        List<HistoryRqDayBarDataBO> historyRqDayBarDataBOList = queryDayHistoryBarData(Lists.newArrayList(orderBookId), queryDate, queryDate, token);
        return CollectionUtils.isEmpty(historyRqDayBarDataBOList) ? null : historyRqDayBarDataBOList.get(0);
    }

    /**
     * 查询历史bar数据
     *
     * @param orderBookIdList 合约ID
     * @param startDate       开始日期
     * @param endDate         结束日期
     * @param token           认证token
     * @return bar数据字符串，CSV文本
     */
    public static List<HistoryRqDayBarDataBO> queryDayHistoryBarData(Collection<String> orderBookIdList, String startDate, String endDate, String token) {
        Map<String, Object> apiParamMap = Maps.newHashMap();
        apiParamMap.put("method", "get_price");
        apiParamMap.put("frequency", "1d");
        apiParamMap.put("adjust_type", "none");
        apiParamMap.put("order_book_ids", orderBookIdList);
        apiParamMap.put("start_date", startDate);
        apiParamMap.put("end_date", endDate);
        String historyRqDayBar = RqApi.getRqApiResult(apiParamMap, token);

        //判断数据是否合法
        if (!StringUtils.hasText(historyRqDayBar) || historyRqDayBar.contains(RETURN_VALID)) {
            log.error("{}未查询到日期:{}到{}的历史数据,返回数据:{}", JSON.toJSONString(orderBookIdList), startDate, endDate, historyRqDayBar);
            return Lists.newArrayList();
        }
        return CsvUtil.convertCsvToBean(historyRqDayBar, HistoryRqDayBarDataBO.class)
                .stream()
                .peek(s -> s.setDatetime(s.getDatetime().replace("-", "")))
                .collect(Collectors.toList());
    }

    /**
     * 查询主力+次主力合约
     */
    public static List<String> queryMainInstrument(String productId, String queryDate, String token) {

        //主力
        RqDomainInstrumentBO main = queryDomainFutures(productId, queryDate, 1, token);

        //次主力
        RqDomainInstrumentBO secondMain = queryDomainFutures(productId, queryDate, 2, token);

        return Lists.newArrayList(main, secondMain).stream().map(RqDomainInstrumentBO::getDominant).collect(Collectors.toList());
    }

    /**
     * 查询某一品种的主力、次主力合约
     */
    public static RqDomainInstrumentBO queryDomainFutures(String productId, String tradeDate, Integer rank, String token) {
        Map<String, Object> apiParamMap = Maps.newHashMap();
        apiParamMap.put("method", "futures.get_dominant");
        apiParamMap.put("underlying_symbol", productId);
        apiParamMap.put("start_date", tradeDate);
        apiParamMap.put("end_date", tradeDate);
        apiParamMap.put("rule", 1);
        apiParamMap.put("rank", rank);
        String result = RqApi.getRqApiResult(apiParamMap, token);
        if (!StringUtils.hasText(result)) {
            throw new BusinessException("未查询到主力或次主力合约");
        }
        return CsvUtil.convertCsvToBean(result, RqDomainInstrumentBO.class).get(0);
    }

    /**
     * 调用米筐api接口
     *
     * @param param 请求参数
     * @param token api token
     * @return bar数据，csv文本数据格式
     */
    private static String getRqApiResult(Object param, String token) {
        return HttpUtil.createPost(RqHttpConstant.R_Q_API_URL)
                .header("token", token)
                //米筐没有返回Accept-Encoding，禁用压缩，不处理
                .header("Accept-Encoding", "identity")
                .body(JSON.toJSONString(param))
                .execute()
                .body();

    }


    public static void main(String[] args) {
        String token = getToken();
//        List<RqInstrumentPriceTickBO> tickBOList = getPriceTick(Lists.newArrayList("AU2604", "AG2604"), token);
//        System.out.println(JSON.toJSONString(tickBOList));
//
//        List<RqInstrumentBO> instrumentBOList = queryInstrument(Lists.newArrayList("PF2604"), token);
//        System.out.println(JSON.toJSONString(instrumentBOList));

        List<HistoryRqDayBarDataBO> historyRqDayBarDataBOList = queryDayHistoryBarData(Lists.newArrayList("AU2605"), "20260325", "20260325", token);
        System.out.println(JSON.toJSONString(historyRqDayBarDataBOList));

//        RqDomainInstrumentBO rqDomainInstrumentBO = queryDomainFutures("AU", "20260305", 2, token);
//        System.out.println(JSON.toJSONString(rqDomainInstrumentBO));


//        List<String> queryMainInstrument = queryMainInstrument("SC","20260307", token);
//        System.out.println(JSON.toJSONString(queryMainInstrument));
    }

}
