package com.cx.management.integration;

import com.cx.management.common.exception.BusinessException;
import com.cx.management.config.TradingApiProperties;
import com.cx.management.dto.RiskLogQueryResultVO;
import com.cx.management.dto.RiskLogVO;
import com.cx.management.dto.TradingAccountVO;
import com.cx.management.dto.TradingGroupVO;
import com.cx.management.dto.TradingInvestorListVO;
import com.cx.management.dto.TradingInvestorVO;
import com.cx.management.dto.TradingSettlementVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class TradingApiClient {

    private static final Pattern RECORD_BLOCK_PATTERN = Pattern.compile(
            "<Record>([\\s\\S]*?)</Record>", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_TAG_PATTERN = Pattern.compile(
            "<Time>([^<]+)</Time>", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTENT_TAG_PATTERN = Pattern.compile(
            "<Content>([^<]*)</Content>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile(
            "<Code>([^<]+)</Code>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ERROR_MSG_PATTERN = Pattern.compile(
            "<Message>([^<]*)</Message>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACCOUNT_ZT_PATTERN = Pattern.compile("ZT:(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern INVESTOR_BLOCK_PATTERN = Pattern.compile(
            "<Investor>([\\s\\S]*?)</Investor>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACCOUNT_TAG_PATTERN = Pattern.compile(
            "<Account>([^<]+)</Account>", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME_TAG_PATTERN = Pattern.compile(
            "<Name>([^<]*)</Name>", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMENT_TAG_PATTERN = Pattern.compile(
            "<Comment>([^<]*)</Comment>", Pattern.CASE_INSENSITIVE);
    private static final Pattern GI_TAG_PATTERN = Pattern.compile(
            "<GI>([^<]*)</GI>", Pattern.CASE_INSENSITIVE);
    private static final Pattern GROUP_BLOCK_PATTERN = Pattern.compile(
            "<Group>([\\s\\S]*?)</Group>", Pattern.CASE_INSENSITIVE);
    private static final Pattern BASE_CAPITAL_TAG_PATTERN = Pattern.compile(
            "<BaseCapital>([^<]+)</BaseCapital>", Pattern.CASE_INSENSITIVE);
    private static final Pattern BALANCE_TAG_PATTERN = Pattern.compile(
            "<Balance>([^<]+)</Balance>", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREDIT_TAG_PATTERN = Pattern.compile(
            "<Credit>([^<]+)</Credit>", Pattern.CASE_INSENSITIVE);
    private static final Pattern MAX_BALANCE_TAG_PATTERN = Pattern.compile(
            "<MaxBalance>([^<]+)</MaxBalance>", Pattern.CASE_INSENSITIVE);
    private static final Pattern EVER_MAX_BALANCE_TAG_PATTERN = Pattern.compile(
            "<EverMaxBalance>([^<]+)</EverMaxBalance>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SETTLE_MAX_BALANCE_TAG_PATTERN = Pattern.compile(
            "<SettleMaxBalance>([^<]+)</SettleMaxBalance>", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTENTS_TAG_PATTERN = Pattern.compile(
            "<Contents>([\\s\\S]*?)</Contents>", Pattern.CASE_INSENSITIVE);
    /** 市值权益 Market value(equity)：金额（收盘市值权益行因后缀不同不会误匹配） */
    private static final Pattern MARKET_EQUITY_PATTERN = Pattern.compile(
            "市值权益\\s*Market value\\(equity\\)\\s*[：:]\\s*([\\d,]+(?:\\.\\d+)?)",
            Pattern.CASE_INSENSITIVE);

    private static final Charset TRADING_GBK = Charset.forName("GBK");

    private final TradingHttpClient httpClient;
    private final TradingApiProperties properties;

    public TradingApiClient(TradingHttpClient httpClient, TradingApiProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    public RiskLogQueryResultVO queryRiskLogs(String timeFrom, String timeTo) {
        validateConfig();
        String url = buildQueryLogUrl(timeFrom, timeTo);
        log.info("查询风控日志: {}", maskPassword(url));

        String responseBody = executeGet(url);
        return parseQueryLogResponse(responseBody);
    }

    public TradingInvestorListVO queryInvestors(String date) {
        validateConfig();
        String url = buildQueryInvestorUrl(date);
        log.info("查询交易账户: {}", maskPassword(url));

        String responseBody = executeGet(url);
        return parseQueryInvestorResponse(responseBody);
    }

    public TradingInvestorVO queryInvestor(String account) {
        if (!StringUtils.hasText(account)) {
            return null;
        }
        String targetAccount = account.trim();
        TradingInvestorListVO result = queryInvestors("0");
        if (result.getAccounts() == null) {
            return null;
        }
        return result.getAccounts().stream()
                .filter(item -> targetAccount.equals(item.getAccount()))
                .findFirst()
                .orElse(null);
    }

    public List<TradingGroupVO> queryGroups(String date) {
        validateConfig();
        String url = buildQueryGroupUrl(date);
        log.info("查询分组列表: {}", maskPassword(url));

        String responseBody = executeGet(url);
        return parseQueryGroupResponse(responseBody);
    }

    public BigDecimal queryBaseCapital(String account) {
        TradingAccountVO accountInfo = queryAccount(account);
        return accountInfo == null ? null : accountInfo.getBaseCapital();
    }

    public TradingAccountVO queryAccount(String account) {
        validateConfig();
        if (!StringUtils.hasText(account)) {
            return null;
        }
        String url = buildQueryAccountUrl(account.trim());
        String responseBody = executeGet(url);
        checkApiError(responseBody);
        return parseQueryAccountResponse(account.trim(), responseBody);
    }

    /**
     * 查询结算单。date 格式 YYYYMMDD。
     * 未出结算或无市值权益时返回 marketEquity=null（不抛错，由业务层跳过）。
     */
    public TradingSettlementVO querySettlement(String account, String dateYyyyMmDd) {
        validateConfig();
        if (!StringUtils.hasText(account) || !StringUtils.hasText(dateYyyyMmDd)) {
            throw new BusinessException("结算单查询参数不能为空");
        }
        String trimmedAccount = account.trim();
        String date = dateYyyyMmDd.trim();
        String url = buildQuerySettlementUrl(trimmedAccount, date);
        log.info("查询结算单: {}", maskPassword(url));

        String responseBody = executeGet(url);
        checkApiError(responseBody);
        return parseQuerySettlementResponse(trimmedAccount, date, responseBody);
    }

    public void deposit(String account, BigDecimal amount, BigDecimal credit, String remark) {
        deposit(account, amount, credit, null, remark);
    }

    /**
     * 出入金 / 设置期初资金。
     * amount、credit、baseCapital 均为差额（累加）；baseCapital 非空时写入交易后台 basecapital。
     * 注意：交易后台要求可用(amount)、优先(credit)、期初(basecapital) 同为正、同为负或为 0。
     */
    public void deposit(String account, BigDecimal amount, BigDecimal credit,
                        BigDecimal baseCapital, String remark) {
        validateConfig();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("account", account.trim());
        params.put("amount", amount != null ? amount.toPlainString() : "0");
        params.put("credit", credit != null ? credit.toPlainString() : "0");
        if (baseCapital != null) {
            params.put("basecapital", baseCapital.toPlainString());
        }
        if (StringUtils.hasText(remark)) {
            params.put("remark", remark);
        }
        String responseBody = executeOperation("deposit", params, false);
        checkApiError(responseBody);
    }

    /**
     * 更改账户。交易后台备注字段为 remark（查询接口返回仍为 Comment）。
     * name != null 时下发名称（空串会转成空格以清空）；null 表示不改名称。
     */
    public void changeAccount(String account, String password, String name, String remark) {
        validateConfig();
        String trimmedAccount = account.trim();
        boolean changePasswordOrName = StringUtils.hasText(password) || name != null;
        boolean changeRemark = remark != null;
        if (!changePasswordOrName && !changeRemark) {
            return;
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("account", trimmedAccount);
        if (StringUtils.hasText(password)) {
            params.put("password", password);
        }
        if (name != null) {
            // 交易后台忽略空 name；传单个空格可把名称置空
            params.put("name", name.isEmpty() ? " " : name);
        }
        if (changeRemark) {
            // 空备注传空格，避免被交易后台忽略
            params.put("remark", remark.trim().isEmpty() ? " " : remark.trim());
        }
        params.put("charset", "utf8");
        String responseBody = executeOperation("changeaccount", params, true);
        checkApiError(responseBody);
    }

    /**
     * 修改账号分组。交易后台参数为 group=分组名称（非 GI），且通常需在结算后才能修改。
     * URL 按 UTF-8 编码中文组名；响应按接口声明（多为 GBK）解码。
     */
    public void changeAccountGroup(String account, String groupName) {
        validateConfig();
        if (!StringUtils.hasText(account) || !StringUtils.hasText(groupName)) {
            throw new BusinessException("账号和分组不能为空");
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("account", account.trim());
        params.put("group", groupName.trim());
        params.put("charset", "utf8");
        // 优先操作端口，失败再试查询端口（查询端口对「结算后改分组」提示更明确）
        String responseBody;
        try {
            responseBody = executeGet(buildOperationUrl("changeaccount", properties.getPort1(), true, params), false);
            checkApiError(responseBody);
        } catch (BusinessException ex) {
            log.warn("修改分组端口1失败，尝试查询端口: {}", ex.getMessage());
            responseBody = executeGet(buildOperationUrl("changeaccount", properties.getPort(), true, params), false);
            checkApiError(responseBody);
        }
    }

    public void resetMaxBalance(String account, int item) {
        validateConfig();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("account", account.trim());
        params.put("item", String.valueOf(item));
        String responseBody = executeGet(buildOperationUrl("resetmaxbalance", properties.getPort(), false, params));
        checkApiError(responseBody);
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.getHost())) {
            throw new BusinessException("交易后台地址未配置，请在 application.yml 中设置 trading.api.host");
        }
        if (!StringUtils.hasText(properties.getSa()) || !StringUtils.hasText(properties.getSapass())) {
            throw new BusinessException("交易后台管理员账号未配置，请设置 trading.api.sa 和 trading.api.sapass");
        }
    }

    private String buildQueryLogUrl(String timeFrom, String timeTo) {
        String scheme = properties.isSslEnabled() ? "https" : "http";
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(String.format("%s://%s:%d/querylog", scheme, properties.getHost(), properties.getPort()))
                .queryParam("requestid", System.currentTimeMillis())
                .queryParam("sa", properties.getSa())
                .queryParam("sapass", properties.getSapass())
                .queryParam("timefrom", timeFrom)
                .queryParam("timeto", timeTo);

        return builder.encode(StandardCharsets.UTF_8).build().toUriString();
    }

    private String buildQueryInvestorUrl(String date) {
        String scheme = properties.isSslEnabled() ? "https" : "http";
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(String.format("%s://%s:%d/queryinvestor", scheme, properties.getHost(), properties.getPort()))
                .queryParam("requestid", System.currentTimeMillis())
                .queryParam("sa", properties.getSa())
                .queryParam("sapass", properties.getSapass())
                .queryParam("date", date);

        return builder.encode(StandardCharsets.UTF_8).build().toUriString();
    }

    private String buildQueryGroupUrl(String date) {
        String scheme = properties.isSslEnabled() ? "https" : "http";
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(String.format("%s://%s:%d/querygroup", scheme, properties.getHost(), properties.getPort()))
                .queryParam("requestid", System.currentTimeMillis())
                .queryParam("sa", properties.getSa())
                .queryParam("sapass", properties.getSapass())
                .queryParam("date", date);

        return builder.encode(StandardCharsets.UTF_8).build().toUriString();
    }

    private String buildQueryAccountUrl(String account) {
        String scheme = properties.isSslEnabled() ? "https" : "http";
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(String.format("%s://%s:%d/queryaccount", scheme, properties.getHost(), properties.getPort()))
                .queryParam("requestid", System.currentTimeMillis())
                .queryParam("sa", properties.getSa())
                .queryParam("sapass", properties.getSapass())
                .queryParam("account", account);

        return builder.encode(StandardCharsets.UTF_8).build().toUriString();
    }

    private String buildQuerySettlementUrl(String account, String date) {
        String scheme = properties.isSslEnabled() ? "https" : "http";
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(String.format("%s://%s:%d/querysettlement", scheme, properties.getHost(), properties.getPort()))
                .queryParam("requestid", System.currentTimeMillis())
                .queryParam("sa", properties.getSa())
                .queryParam("sapass", properties.getSapass())
                .queryParam("account", account)
                .queryParam("date", date);

        return builder.encode(StandardCharsets.UTF_8).build().toUriString();
    }

    private String executeGet(String url) {
        return executeGet(url, false);
    }

    private String executeGet(String url, boolean useUtf8) {
        return httpClient.get(url, useUtf8);
    }

    private String executeOperation(String path, Map<String, String> params, boolean useUtf8) {
        try {
            return executeGet(buildOperationUrl(path, properties.getPort1(), useUtf8, params), useUtf8);
        } catch (BusinessException ex) {
            log.warn("交易操作端口1调用失败，尝试查询端口: path={}, error={}", path, ex.getMessage());
            return executeGet(buildOperationUrl(path, properties.getPort(), useUtf8, params), useUtf8);
        }
    }

    private String buildOperationUrl(String path, int port, boolean useUtf8, Map<String, String> extraParams) {
        String scheme = properties.isSslEnabled() ? "https" : "http";
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(String.format("%s://%s:%d/%s", scheme, properties.getHost(), port, path))
                .queryParam("requestid", System.currentTimeMillis())
                .queryParam("sa", properties.getSa())
                .queryParam("sapass", properties.getSapass());
        if (extraParams != null) {
            extraParams.forEach(builder::queryParam);
        }
        Charset charset = useUtf8 ? StandardCharsets.UTF_8 : TRADING_GBK;
        return builder.encode(charset).build().toUriString();
    }

    private TradingAccountVO parseQueryAccountResponse(String account, String responseBody) {
        TradingAccountVO vo = new TradingAccountVO();
        vo.setAccount(account);
        vo.setBalance(parseDecimal(BALANCE_TAG_PATTERN, responseBody));
        vo.setBaseCapital(parseDecimal(BASE_CAPITAL_TAG_PATTERN, responseBody));
        vo.setCredit(parseDecimal(CREDIT_TAG_PATTERN, responseBody));
        vo.setMaxBalance(parseDecimal(MAX_BALANCE_TAG_PATTERN, responseBody));
        vo.setEverMaxBalance(parseDecimal(EVER_MAX_BALANCE_TAG_PATTERN, responseBody));
        vo.setSettleMaxBalance(parseDecimal(SETTLE_MAX_BALANCE_TAG_PATTERN, responseBody));
        return vo;
    }

    TradingSettlementVO parseQuerySettlementResponse(String account, String date, String responseBody) {
        TradingSettlementVO vo = new TradingSettlementVO();
        vo.setAccount(account);
        vo.setDate(date);
        String contents = extractFirst(CONTENTS_TAG_PATTERN, responseBody);
        vo.setContents(contents);
        if (StringUtils.hasText(contents)) {
            vo.setMarketEquity(parseMarketEquity(contents));
        }
        return vo;
    }

    BigDecimal parseMarketEquity(String contents) {
        if (!StringUtils.hasText(contents)) {
            return null;
        }
        Matcher matcher = MARKET_EQUITY_PATTERN.matcher(contents);
        if (!matcher.find()) {
            return null;
        }
        String raw = matcher.group(1).replace(",", "").trim();
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseDecimal(Pattern pattern, String responseBody) {
        String value = extractFirst(pattern, responseBody);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    RiskLogQueryResultVO parseQueryLogResponse(String responseBody) {
        checkApiError(responseBody);

        RiskLogQueryResultVO result = new RiskLogQueryResultVO();
        List<RiskLogVO> logs = new ArrayList<>();
        Set<String> accounts = new LinkedHashSet<>();

        Matcher recordMatcher = RECORD_BLOCK_PATTERN.matcher(responseBody);
        while (recordMatcher.find()) {
            RiskLogVO logItem = parseRecordBlock(recordMatcher.group(1));
            if (logItem != null) {
                logs.add(logItem);
                if (StringUtils.hasText(logItem.getAccount())) {
                    accounts.add(logItem.getAccount());
                }
            }
        }

        result.setLogs(logs);
        result.setAccounts(new ArrayList<>(accounts));
        return result;
    }

    TradingInvestorListVO parseQueryInvestorResponse(String responseBody) {
        checkApiError(responseBody);

        TradingInvestorListVO result = new TradingInvestorListVO();
        List<TradingInvestorVO> accounts = new ArrayList<>();

        Matcher investorMatcher = INVESTOR_BLOCK_PATTERN.matcher(responseBody);
        while (investorMatcher.find()) {
            TradingInvestorVO investor = parseInvestorBlock(investorMatcher.group(1));
            if (investor != null) {
                accounts.add(investor);
            }
        }

        result.setAccounts(accounts);
        result.setTotal(accounts.size());
        return result;
    }

    private TradingInvestorVO parseInvestorBlock(String block) {
        String account = extractFirst(ACCOUNT_TAG_PATTERN, block);
        if (!StringUtils.hasText(account)) {
            return null;
        }

        TradingInvestorVO vo = new TradingInvestorVO();
        vo.setAccount(account);
        vo.setName(extractFirst(NAME_TAG_PATTERN, block));
        vo.setComment(extractFirst(COMMENT_TAG_PATTERN, block));
        vo.setGroupId(extractFirst(GI_TAG_PATTERN, block));
        return vo;
    }

    List<TradingGroupVO> parseQueryGroupResponse(String responseBody) {
        checkApiError(responseBody);

        List<TradingGroupVO> groups = new ArrayList<>();
        Matcher groupMatcher = GROUP_BLOCK_PATTERN.matcher(responseBody);
        while (groupMatcher.find()) {
            TradingGroupVO group = parseGroupBlock(groupMatcher.group(1));
            if (group != null) {
                groups.add(group);
            }
        }
        return groups;
    }

    private TradingGroupVO parseGroupBlock(String block) {
        String groupId = extractFirst(GI_TAG_PATTERN, block);
        if (!StringUtils.hasText(groupId)) {
            return null;
        }
        TradingGroupVO vo = new TradingGroupVO();
        vo.setGroupId(groupId);
        vo.setGroupName(extractFirst(NAME_TAG_PATTERN, block));
        return vo;
    }

    private void checkApiError(String responseBody) {
        Matcher codeMatcher = ERROR_CODE_PATTERN.matcher(responseBody);
        if (codeMatcher.find()) {
            String code = codeMatcher.group(1).trim();
            if (!"0".equals(code)) {
                String message = extractFirst(ERROR_MSG_PATTERN, responseBody);
                throw new BusinessException("交易后台返回错误[" + code + "]: " +
                        (StringUtils.hasText(message) ? message : "未知错误"));
            }
        }
    }

    private RiskLogVO parseRecordBlock(String block) {
        String time = extractFirst(TIME_TAG_PATTERN, block);
        String content = extractFirst(CONTENT_TAG_PATTERN, block);
        String account = extractAccountFromContent(content);

        if (!StringUtils.hasText(account) && !StringUtils.hasText(content) && !StringUtils.hasText(time)) {
            return null;
        }

        RiskLogVO vo = new RiskLogVO();
        vo.setAccount(account);
        vo.setTime(formatLogTime(time));
        vo.setContent(content);
        return vo;
    }

    private String formatLogTime(String time) {
        if (!StringUtils.hasText(time)) {
            return time;
        }
        String normalized = time;
        int dotIndex = time.indexOf('.');
        if (dotIndex > 0) {
            normalized = time.substring(0, dotIndex);
        }
        return normalized.length() >= 16 ? normalized.substring(0, 16) : normalized;
    }

    private String extractAccountFromContent(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        Matcher matcher = ACCOUNT_ZT_PATTERN.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractFirst(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String maskPassword(String url) {
        return url.replaceAll("sapass=[^&]*", "sapass=***");
    }
}
