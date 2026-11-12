package com.cx.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cx.management.common.PageResult;
import com.cx.management.common.exception.BusinessException;
import com.cx.management.dto.AccountScoreConfigRequest;
import com.cx.management.dto.AccountScoreConfigVO;
import com.cx.management.dto.AccountScoreDailyVO;
import com.cx.management.dto.AccountScoreRefreshResultVO;
import com.cx.management.dto.TradingAccountVO;
import com.cx.management.dto.TradingInvestorListVO;
import com.cx.management.dto.TradingInvestorVO;
import com.cx.management.dto.TradingSettlementVO;
import com.cx.management.entity.AccountScoreConfig;
import com.cx.management.entity.AccountScoreDaily;
import com.cx.management.integration.TradingApiClient;
import com.cx.management.mapper.AccountScoreConfigMapper;
import com.cx.management.mapper.AccountScoreDailyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountScoreService {

    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_STOPPED = 0;
    public static final String STOP_RECYCLE = "RECYCLE";
    public static final String STOP_RESET = "RESET";
    public static final String STOP_MANUAL = "MANUAL";
    /** 交易账户手动停用 */
    public static final String STOP_ACCOUNT_DISABLE = "ACCOUNT_DISABLE";

    private static final BigDecimal SCORE_CAP = new BigDecimal("2");
    private static final int PCT_SCALE = 4;
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId ZONE_SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final String SETTLEMENT_BLOCKED_MESSAGE =
            "当前为交易时段，无法查询结算单，请在非交易时段再刷新（限制：8:55-11:30、13:00-15:15、20:55-2:30）";

    private final AccountScoreConfigMapper configMapper;
    private final AccountScoreDailyMapper dailyMapper;
    private final TradingApiClient tradingApiClient;
    private final BoyiAccountService boyiAccountService;

    public TradingInvestorListVO listCandidates() {
        return boyiAccountService.queryAccounts(null);
    }

    public PageResult<AccountScoreConfigVO> pageConfigs(int current, int size, Integer status,
                                                        String account, BigDecimal minScorePct) {
        Page<AccountScoreConfig> page = new Page<>(current, size);
        LambdaQueryWrapper<AccountScoreConfig> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(AccountScoreConfig::getStatus, status);
        }
        if (StringUtils.hasText(account)) {
            wrapper.like(AccountScoreConfig::getAccount, account.trim());
        }
        if (minScorePct != null) {
            wrapper.ge(AccountScoreConfig::getTotalScorePct, minScorePct);
        }
        wrapper.orderByDesc(AccountScoreConfig::getStatus)
                .orderByDesc(AccountScoreConfig::getAssessStart)
                .orderByDesc(AccountScoreConfig::getId);

        Page<AccountScoreConfig> result = configMapper.selectPage(page, wrapper);
        List<AccountScoreConfigVO> records = result.getRecords().stream()
                .map(this::toConfigVO)
                .collect(Collectors.toList());
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public AccountScoreConfigVO saveOrReset(AccountScoreConfigRequest request, String operator) {
        String account = request.getAccount().trim();
        LocalDate assessStart = request.getAssessStart();
        if (assessStart.isAfter(LocalDate.now(ZONE_SHANGHAI))) {
            throw new BusinessException("考核开始日期不能晚于今天");
        }

        boolean reset = Boolean.TRUE.equals(request.getReset());
        long activeCount = configMapper.selectCount(new LambdaQueryWrapper<AccountScoreConfig>()
                .eq(AccountScoreConfig::getAccount, account)
                .eq(AccountScoreConfig::getStatus, STATUS_ACTIVE));
        if (!reset && activeCount > 0) {
            throw new BusinessException("该账号已存在启用中的考核，请勿重复添加");
        }
        if (reset) {
            disableActiveConfigs(account, STOP_RESET, operator);
        }

        TradingInvestorVO investor = tradingApiClient.queryInvestor(account);
        AccountScoreConfig config = new AccountScoreConfig();
        config.setAccount(account);
        config.setName(investor != null ? investor.getName() : null);
        config.setAssessStart(assessStart);
        config.setTotalScorePct(BigDecimal.ZERO.setScale(PCT_SCALE, RoundingMode.HALF_UP));
        config.setScoreDays(0);
        config.setStatus(STATUS_ACTIVE);
        config.setOperator(operator);
        configMapper.insert(config);
        return toConfigVO(config);
    }

    /** 交易账户启用时：以启用日为考核开始日，加入成绩考核（若已有启用中则先结束再开新期） */
    @Transactional(rollbackFor = Exception.class)
    public void startOnAccountEnable(String account, LocalDate assessStart, String name, String operator) {
        if (!StringUtils.hasText(account) || assessStart == null) {
            return;
        }
        AccountScoreConfigRequest request = new AccountScoreConfigRequest();
        request.setAccount(account.trim());
        request.setAssessStart(assessStart);
        request.setReset(true);
        AccountScoreConfigVO created = saveOrReset(request, operator);
        if (created != null && StringUtils.hasText(name)) {
            AccountScoreConfig config = configMapper.selectById(created.getId());
            if (config != null && !name.equals(config.getName())) {
                config.setName(name);
                configMapper.updateById(config);
            }
        }
    }

    @Transactional
    public void stopConfig(Long configId, String operator) {
        AccountScoreConfig config = requireConfig(configId);
        if (STATUS_STOPPED == config.getStatus()) {
            return;
        }
        config.setStatus(STATUS_STOPPED);
        config.setStopReason(STOP_MANUAL);
        config.setStopTime(LocalDateTime.now());
        if (StringUtils.hasText(operator)) {
            config.setOperator(operator);
        }
        configMapper.updateById(config);
    }

    /** 回收成功后停用该账号所有启用中的成绩统计 */
    @Transactional
    public void disableByAccount(String account, String stopReason) {
        disableByAccount(account, stopReason, null);
    }

    @Transactional
    public void disableByAccount(String account, String stopReason, String operator) {
        if (!StringUtils.hasText(account)) {
            return;
        }
        disableActiveConfigs(account.trim(),
                StringUtils.hasText(stopReason) ? stopReason : STOP_RECYCLE,
                operator);
    }

    public List<AccountScoreRefreshResultVO> refresh(List<Long> configIds) {
        return refreshInternal(configIds, false, false);
    }

    /**
     * 定时任务刷新：补齐到当天（含当日结算），已有日成绩的日期跳过。
     */
    public List<AccountScoreRefreshResultVO> refreshScheduled() {
        return refreshInternal(null, true, true);
    }

    private List<AccountScoreRefreshResultVO> refreshInternal(List<Long> configIds,
                                                              boolean skipExisting,
                                                              boolean includeToday) {
        List<AccountScoreConfig> targets;
        if (CollectionUtils.isEmpty(configIds)) {
            targets = configMapper.selectList(new LambdaQueryWrapper<AccountScoreConfig>()
                    .eq(AccountScoreConfig::getStatus, STATUS_ACTIVE)
                    .orderByAsc(AccountScoreConfig::getId));
        } else {
            targets = configMapper.selectBatchIds(configIds).stream()
                    .filter(c -> STATUS_ACTIVE == c.getStatus())
                    .collect(Collectors.toList());
        }

        List<AccountScoreRefreshResultVO> results = new ArrayList<>();
        for (AccountScoreConfig config : targets) {
            results.add(refreshOne(config, skipExisting, includeToday));
        }
        return results;
    }

    public List<AccountScoreDailyVO> listDaily(Long configId) {
        requireConfig(configId);
        List<AccountScoreDaily> list = dailyMapper.selectList(new LambdaQueryWrapper<AccountScoreDaily>()
                .eq(AccountScoreDaily::getConfigId, configId)
                .orderByDesc(AccountScoreDaily::getSettleDate)
                .orderByDesc(AccountScoreDaily::getId));
        return list.stream().map(this::toDailyVO).collect(Collectors.toList());
    }

    private AccountScoreRefreshResultVO refreshOne(AccountScoreConfig config,
                                                   boolean skipExisting,
                                                   boolean includeToday) {
        AccountScoreRefreshResultVO result = new AccountScoreRefreshResultVO();
        result.setConfigId(config.getId());
        result.setAccount(config.getAccount());

        BigDecimal baseCapital = safeQueryBaseCapital(config.getAccount());
        if (baseCapital == null || baseCapital.compareTo(BigDecimal.ZERO) == 0) {
            result.setMessage("无法获取期初资金或期初为0");
            result.setSkippedDays(0);
            result.setSuccessDays(0);
            result.setAlreadyDoneDays(0);
            return result;
        }

        LocalDate start = config.getAssessStart();
        LocalDate end = includeToday ? LocalDate.now() : LocalDate.now().minusDays(1);
        if (end.isBefore(start)) {
            result.setMessage("尚无已过考核日可结算");
            recalculateTotal(config.getId());
            return result;
        }

        // 考核开始当天：昨日权益 = 期初资金；之后用上一日市值权益
        BigDecimal prevEquity = baseCapital;

        int success = 0;
        int skipped = 0;
        int alreadyDone = 0;
        String blockMessage = null;
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            AccountScoreDaily existing = findDaily(config.getId(), day);
            if (skipExisting && existing != null) {
                if (existing.getMarketEquity() != null) {
                    prevEquity = existing.getMarketEquity();
                }
                alreadyDone++;
                continue;
            }

            BigDecimal todayEquity;
            try {
                todayEquity = queryMarketEquity(config.getAccount(), day);
            } catch (BusinessException ex) {
                if (isSettlementQueryBlocked(ex.getMessage())) {
                    blockMessage = SETTLEMENT_BLOCKED_MESSAGE;
                    break;
                }
                skipped++;
                continue;
            }
            if (todayEquity == null) {
                skipped++;
                continue;
            }

            // 日成绩% = (今日市值权益 - 昨日市值权益) / 期初投入 × 100，保留4位；正收益封顶2%
            BigDecimal rawPct = todayEquity.subtract(prevEquity)
                    .multiply(new BigDecimal("100"))
                    .divide(baseCapital, PCT_SCALE, RoundingMode.HALF_UP);
            BigDecimal scorePct = rawPct.compareTo(BigDecimal.ZERO) > 0
                    ? rawPct.min(SCORE_CAP)
                    : rawPct;
            upsertDaily(config, day, baseCapital, todayEquity, rawPct, scorePct, existing);
            prevEquity = todayEquity;
            success++;
        }

        recalculateTotal(config.getId());
        result.setSuccessDays(success);
        result.setAlreadyDoneDays(alreadyDone);
        result.setSkippedDays(skipped);
        result.setMessage(blockMessage != null ? blockMessage : "刷新完成");
        return result;
    }

    private AccountScoreDaily findDaily(Long configId, LocalDate settleDate) {
        return dailyMapper.selectOne(new LambdaQueryWrapper<AccountScoreDaily>()
                .eq(AccountScoreDaily::getConfigId, configId)
                .eq(AccountScoreDaily::getSettleDate, settleDate)
                .last("LIMIT 1"));
    }

    /** 查询市值权益；交易时段限制会抛 BusinessException，其它失败返回 null */
    private BigDecimal queryMarketEquity(String account, LocalDate day) {
        try {
            TradingSettlementVO settlement = tradingApiClient.querySettlement(
                    account, day.format(DAY_FMT));
            return settlement != null ? settlement.getMarketEquity() : null;
        } catch (BusinessException ex) {
            log.warn("拉取结算单失败 account={} date={}: {}", account, day, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.warn("拉取结算单失败 account={} date={}: {}", account, day, ex.getMessage());
            return null;
        }
    }

    private boolean isSettlementQueryBlocked(String message) {
        if (!StringUtils.hasText(message)) {
            return false;
        }
        return message.contains("不能查询结算单") || message.contains("交易时段");
    }

    private void upsertDaily(AccountScoreConfig config, LocalDate settleDate,
                             BigDecimal baseCapital, BigDecimal marketEquity,
                             BigDecimal rawPct, BigDecimal scorePct,
                             AccountScoreDaily existing) {
        if (existing == null) {
            AccountScoreDaily daily = new AccountScoreDaily();
            daily.setConfigId(config.getId());
            daily.setAccount(config.getAccount());
            daily.setSettleDate(settleDate);
            daily.setBaseCapital(baseCapital);
            daily.setMarketEquity(marketEquity);
            daily.setRawPct(rawPct);
            daily.setScorePct(scorePct);
            dailyMapper.insert(daily);
        } else {
            existing.setBaseCapital(baseCapital);
            existing.setMarketEquity(marketEquity);
            existing.setRawPct(rawPct);
            existing.setScorePct(scorePct);
            dailyMapper.updateById(existing);
        }
    }

    private void recalculateTotal(Long configId) {
        List<AccountScoreDaily> dailies = dailyMapper.selectList(new LambdaQueryWrapper<AccountScoreDaily>()
                .eq(AccountScoreDaily::getConfigId, configId));

        BigDecimal total = BigDecimal.ZERO.setScale(PCT_SCALE, RoundingMode.HALF_UP);
        LocalDate lastDate = null;
        for (AccountScoreDaily daily : dailies) {
            if (daily.getScorePct() != null) {
                total = total.add(daily.getScorePct());
            }
            if (daily.getSettleDate() != null
                    && (lastDate == null || daily.getSettleDate().isAfter(lastDate))) {
                lastDate = daily.getSettleDate();
            }
        }

        AccountScoreConfig config = configMapper.selectById(configId);
        if (config == null) {
            return;
        }
        config.setTotalScorePct(total.setScale(PCT_SCALE, RoundingMode.HALF_UP));
        config.setScoreDays(dailies.size());
        config.setLastSettleDate(lastDate);
        configMapper.updateById(config);
    }

    private void disableActiveConfigs(String account, String stopReason, String operator) {
        List<AccountScoreConfig> actives = configMapper.selectList(new LambdaQueryWrapper<AccountScoreConfig>()
                .eq(AccountScoreConfig::getAccount, account)
                .eq(AccountScoreConfig::getStatus, STATUS_ACTIVE));
        LocalDateTime now = LocalDateTime.now();
        for (AccountScoreConfig config : actives) {
            config.setStatus(STATUS_STOPPED);
            config.setStopReason(stopReason);
            config.setStopTime(now);
            if (StringUtils.hasText(operator)) {
                config.setOperator(operator);
            }
            configMapper.updateById(config);
        }
    }

    private AccountScoreConfig requireConfig(Long configId) {
        AccountScoreConfig config = configMapper.selectById(configId);
        if (config == null) {
            throw new BusinessException("考核配置不存在");
        }
        return config;
    }

    private BigDecimal safeQueryBaseCapital(String account) {
        try {
            TradingAccountVO info = tradingApiClient.queryAccount(account);
            return info != null ? info.getBaseCapital() : null;
        } catch (Exception ex) {
            log.warn("查询期初资金失败 account={}: {}", account, ex.getMessage());
            return null;
        }
    }

    private AccountScoreConfigVO toConfigVO(AccountScoreConfig config) {
        AccountScoreConfigVO vo = new AccountScoreConfigVO();
        vo.setId(config.getId());
        vo.setAccount(config.getAccount());
        vo.setName(config.getName());
        vo.setAssessStart(config.getAssessStart());
        vo.setTotalScorePct(config.getTotalScorePct());
        vo.setScoreDays(config.getScoreDays());
        vo.setLastSettleDate(config.getLastSettleDate());
        vo.setStatus(config.getStatus());
        vo.setStopReason(config.getStopReason());
        vo.setStopTime(config.getStopTime());
        vo.setOperator(config.getOperator());
        vo.setCreated(config.getCreated());
        return vo;
    }

    private AccountScoreDailyVO toDailyVO(AccountScoreDaily daily) {
        AccountScoreDailyVO vo = new AccountScoreDailyVO();
        vo.setId(daily.getId());
        vo.setConfigId(daily.getConfigId());
        vo.setAccount(daily.getAccount());
        vo.setSettleDate(daily.getSettleDate());
        vo.setBaseCapital(daily.getBaseCapital());
        vo.setMarketEquity(daily.getMarketEquity());
        vo.setRawPct(daily.getRawPct());
        vo.setScorePct(daily.getScorePct());
        return vo;
    }
}
