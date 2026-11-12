package com.cx.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cx.management.common.exception.BusinessException;
import com.cx.management.config.TradingApiProperties;
import com.cx.management.dto.RiskLogQueryDTO;
import com.cx.management.dto.RiskLogQueryResultVO;
import com.cx.management.dto.TradingAccountVO;
import com.cx.management.dto.TradingInvestorListVO;
import com.cx.management.dto.TradingInvestorVO;
import com.cx.management.entity.AccountRecycle;
import com.cx.management.integration.TradingApiClient;
import com.cx.management.mapper.AccountRecycleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountRecycleService {

    private static final DateTimeFormatter INPUT_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter INPUT_SECOND_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter API_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter REMARK_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long MAX_QUERY_DAYS = 7;
    private static final int MONEY_SCALE = 2;

    private final TradingApiClient tradingApiClient;
    private final AccountRecycleMapper accountRecycleMapper;
    private final TradingApiProperties tradingApiProperties;
    private final PlatformTransactionManager transactionManager;
    private final AccountScoreService accountScoreService;

    public RiskLogQueryResultVO queryRiskLogs(RiskLogQueryDTO dto) {
        LocalDateTime timeFrom = parseDateTime(dto.getTimeFrom(), "开始时间");
        LocalDateTime timeTo = parseDateTime(dto.getTimeTo(), "结束时间");

        if (timeTo.isBefore(timeFrom)) {
            throw new BusinessException("结束时间不能早于开始时间");
        }
        if (ChronoUnit.DAYS.between(timeFrom, timeTo) > MAX_QUERY_DAYS) {
            throw new BusinessException("查询时间范围不能超过7天");
        }

        String apiTimeFrom = toApiTime(timeFrom, true);
        String apiTimeTo = toApiTime(timeTo, false);

        return tradingApiClient.queryRiskLogs(apiTimeFrom, apiTimeTo);
    }

    public Map<String, TradingInvestorVO> loadInvestorMap() {
        TradingInvestorListVO result = tradingApiClient.queryInvestors("0");
        Map<String, TradingInvestorVO> investorMap = new HashMap<>();
        if (result.getAccounts() != null) {
            for (TradingInvestorVO investor : result.getAccounts()) {
                if (investor != null && StringUtils.hasText(investor.getAccount())) {
                    investorMap.put(investor.getAccount(), investor);
                }
            }
        }
        return investorMap;
    }

    public void recycleSingleAccount(String account, String operator, Map<String, TradingInvestorVO> investorMap) {
        recycleSingleAccount(account, operator, investorMap, null);
    }

    public void recycleSingleAccount(String account, String operator,
                                     Map<String, TradingInvestorVO> investorMap, String remark) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status ->
                doRecycleSingleAccount(account, operator, investorMap, remark));
    }

    private void doRecycleSingleAccount(String account, String operator,
                                        Map<String, TradingInvestorVO> investorMap, String remark) {
        TradingInvestorVO investor = investorMap != null ? investorMap.get(account) : null;
        String originalName = investor != null ? investor.getName() : null;
        String originalComment = investor != null ? investor.getComment() : null;

        TradingAccountVO accountInfo = tradingApiClient.queryAccount(account);
        if (accountInfo == null || accountInfo.getBaseCapital() == null) {
            throw new BusinessException("无法获取账号期初资金");
        }

        LocalDateTime recycleTime = LocalDateTime.now();
        AccountRecycle record = new AccountRecycle();
        record.setAccount(account);
        record.setName(originalName);
        record.setComment(originalComment);
        record.setRecycleTime(recycleTime);
        record.setOperator(operator);
        accountRecycleMapper.insert(record);

        accountScoreService.disableByAccount(account, AccountScoreService.STOP_RECYCLE);

        resetBalanceToBaseCapital(account, accountInfo);
        resetMaxBalances(account);
        String finalRemark = resolveRecycleRemark(remark, recycleTime);
        tradingApiClient.changeAccount(
                account,
                tradingApiProperties.getRecyclePassword(),
                tradingApiProperties.getRecycleName(),
                finalRemark);
    }

    public static String defaultRecycleRemark() {
        return LocalDateTime.now().format(REMARK_DATE_FORMATTER) + "回收";
    }

    private String resolveRecycleRemark(String remark, LocalDateTime recycleTime) {
        if (StringUtils.hasText(remark)) {
            return remark.trim();
        }
        return recycleTime.format(REMARK_DATE_FORMATTER) + "回收";
    }

    private void resetBalanceToBaseCapital(String account, TradingAccountVO accountInfo) {
        BigDecimal balance = defaultZero(accountInfo.getBalance());
        BigDecimal baseCapital = accountInfo.getBaseCapital();
        BigDecimal credit = defaultZero(accountInfo.getCredit());
        BigDecimal amountDiff = baseCapital.subtract(balance).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal creditDiff = BigDecimal.ZERO.subtract(credit).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (amountDiff.compareTo(BigDecimal.ZERO) == 0 && creditDiff.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        tradingApiClient.deposit(account, amountDiff, creditDiff, "回收复位");
    }

    private void resetMaxBalances(String account) {
        tradingApiClient.resetMaxBalance(account, 1);
        tradingApiClient.resetMaxBalance(account, 2);
        tradingApiClient.resetMaxBalance(account, 3);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public TradingInvestorListVO queryRecycledAccounts() {
        List<AccountRecycle> records = accountRecycleMapper.selectList(
                new LambdaQueryWrapper<AccountRecycle>()
                        .orderByDesc(AccountRecycle::getRecycleTime)
                        .orderByDesc(AccountRecycle::getId));

        List<TradingInvestorVO> accounts = records.stream()
                .map(this::toInvestorVO)
                .collect(Collectors.toList());

        TradingInvestorListVO result = new TradingInvestorListVO();
        result.setAccounts(accounts);
        result.setTotal(accounts.size());
        result.setGroups(null);
        return result;
    }

    private TradingInvestorVO toInvestorVO(AccountRecycle record) {
        TradingInvestorVO vo = new TradingInvestorVO();
        vo.setAccount(record.getAccount());
        vo.setName(record.getName());
        vo.setComment(record.getComment());
        vo.setOperator(record.getOperator());
        if (record.getRecycleTime() != null) {
            vo.setAccountDate(record.getRecycleTime().format(DISPLAY_DATETIME_FORMATTER));
        }
        return vo;
    }

    private LocalDateTime parseDateTime(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(fieldName + "不能为空");
        }
        String trimmed = value.trim();
        try {
            if (trimmed.length() == 16) {
                return LocalDateTime.parse(trimmed, INPUT_MINUTE_FORMATTER);
            }
            return LocalDateTime.parse(trimmed, INPUT_SECOND_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BusinessException(fieldName + "格式错误，应为 yyyy-MM-dd HH:mm");
        }
    }

    /** 页面传分钟时：开始补 :00，结束补 :59，覆盖该分钟内日志 */
    private String toApiTime(LocalDateTime dateTime, boolean isStart) {
        if (dateTime.getSecond() == 0 && dateTime.getNano() == 0) {
            LocalDateTime adjusted = isStart
                    ? dateTime.withSecond(0)
                    : dateTime.withSecond(59);
            return adjusted.format(API_FORMATTER);
        }
        return dateTime.format(API_FORMATTER);
    }
}
