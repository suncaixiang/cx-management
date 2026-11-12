package com.cx.management.service;

import com.cx.management.common.exception.BusinessException;
import com.cx.management.dto.TradingAccountCapitalVO;
import com.cx.management.dto.TradingAccountSyncVO;
import com.cx.management.dto.TradingAccountVO;
import com.cx.management.dto.TradingGroupVO;
import com.cx.management.dto.TradingInvestorListVO;
import com.cx.management.dto.TradingInvestorVO;
import com.cx.management.entity.TradingAccount;
import com.cx.management.integration.TradingApiClient;
import com.cx.management.util.TradingAccountHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoyiAccountService {

    private static final int MAX_CAPITAL_BATCH_SIZE = 200;
    private static final int CAPITAL_FETCH_THREADS = 24;

    private final TradingApiClient tradingApiClient;
    private final TradingAccountService tradingAccountService;

    public List<TradingGroupVO> listGroups() {
        return filterGroups(tradingApiClient.queryGroups("0"));
    }

    public TradingInvestorListVO queryAccounts(String groupId) {
        List<TradingGroupVO> groups = listGroups();
        List<TradingInvestorVO> accounts = tradingAccountService.listLocalAccounts(groupId);

        TradingInvestorListVO result = new TradingInvestorListVO();
        result.setAccounts(accounts);
        result.setTotal(accounts.size());
        result.setGroups(groups);
        return result;
    }

    /**
     * 全量同步：一次拉齐平台数据并写入本地
     * account / name / comment / base_capital / group_id / group_name / status。
     * 不同步覆盖本地 password。
     */
    public TradingAccountSyncVO syncAccounts() {
        List<TradingGroupVO> allGroups = tradingApiClient.queryGroups("0");
        Map<String, String> groupNameMap = TradingAccountHelper.buildGroupNameMap(allGroups);

        TradingInvestorListVO result = tradingApiClient.queryInvestors("0");
        List<TradingInvestorVO> accounts = result.getAccounts();
        if (accounts == null) {
            accounts = new ArrayList<>();
        }

        List<String> accountNos = accounts.stream()
                .filter(a -> a != null && StringUtils.hasText(a.getAccount()))
                .map(a -> a.getAccount().trim())
                .distinct()
                .collect(Collectors.toList());
        Map<String, BigDecimal> capitals = fetchCapitalsMap(accountNos);

        for (TradingInvestorVO account : accounts) {
            if (account == null || !StringUtils.hasText(account.getAccount())) {
                continue;
            }
            TradingAccountHelper.enrichAccount(account, groupNameMap);
            BigDecimal capital = capitals.get(account.getAccount().trim());
            if (capital != null) {
                account.setBaseCapital(capital);
            }
        }

        Map<String, TradingAccount> synced = tradingAccountService.syncFromPlatform(accounts);
        TradingAccountSyncVO vo = new TradingAccountSyncVO();
        vo.setTotal(synced.size());
        return vo;
    }

    public TradingAccountVO queryAccountFunds(String account) {
        if (!StringUtils.hasText(account)) {
            throw new BusinessException("账号不能为空");
        }
        TradingAccountVO accountInfo = tradingApiClient.queryAccount(account.trim());
        if (accountInfo == null) {
            throw new BusinessException("无法获取账号资金信息");
        }
        return accountInfo;
    }

    public TradingAccountCapitalVO queryCapitals(List<String> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            throw new BusinessException("账号列表不能为空");
        }
        if (accounts.size() > MAX_CAPITAL_BATCH_SIZE) {
            throw new BusinessException("单次最多查询 " + MAX_CAPITAL_BATCH_SIZE + " 个账号的期初资金");
        }

        List<String> distinctAccounts = accounts.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());

        Map<String, BigDecimal> capitals = new LinkedHashMap<>();
        Map<String, BigDecimal> balances = new LinkedHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(CAPITAL_FETCH_THREADS);
        try {
            List<CompletableFuture<Void>> futures = distinctAccounts.stream()
                    .map(account -> CompletableFuture.runAsync(() -> {
                        TradingAccountVO accountInfo = tradingApiClient.queryAccount(account);
                        synchronized (capitals) {
                            capitals.put(account, accountInfo != null ? accountInfo.getBaseCapital() : null);
                            balances.put(account, accountInfo != null ? accountInfo.getBalance() : null);
                        }
                    }, executor))
                    .collect(Collectors.toList());
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            executor.shutdown();
        }

        TradingAccountCapitalVO result = new TradingAccountCapitalVO();
        result.setCapitals(capitals);
        result.setBalances(balances);
        return result;
    }

    /** 批量拉取期初资金（同步用，可超过单次接口上限） */
    private Map<String, BigDecimal> fetchCapitalsMap(List<String> accounts) {
        Map<String, BigDecimal> capitals = new LinkedHashMap<>();
        if (accounts == null || accounts.isEmpty()) {
            return capitals;
        }
        List<String> distinctAccounts = accounts.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        if (distinctAccounts.isEmpty()) {
            return capitals;
        }

        ExecutorService executor = Executors.newFixedThreadPool(CAPITAL_FETCH_THREADS);
        try {
            List<CompletableFuture<Void>> futures = distinctAccounts.stream()
                    .map(account -> CompletableFuture.runAsync(() -> {
                        try {
                            TradingAccountVO accountInfo = tradingApiClient.queryAccount(account);
                            synchronized (capitals) {
                                capitals.put(account, accountInfo != null ? accountInfo.getBaseCapital() : null);
                            }
                        } catch (Exception ignored) {
                            synchronized (capitals) {
                                capitals.put(account, null);
                            }
                        }
                    }, executor))
                    .collect(Collectors.toList());
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            executor.shutdown();
        }
        return capitals;
    }

    private List<TradingGroupVO> filterGroups(List<TradingGroupVO> groups) {
        if (groups == null) {
            return new ArrayList<>();
        }
        return groups.stream()
                .filter(group -> group != null && StringUtils.hasText(group.getGroupId()))
                .filter(group -> !"00".equals(group.getGroupId()))
                .filter(group -> !"佰正咨询".equals(group.getGroupName()))
                .collect(Collectors.toList());
    }
}
