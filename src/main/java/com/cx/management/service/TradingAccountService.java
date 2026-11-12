package com.cx.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cx.management.common.exception.BusinessException;
import com.cx.management.config.TradingApiProperties;
import com.cx.management.dto.TradingAccountDepositRequest;
import com.cx.management.dto.TradingAccountDisableRequest;
import com.cx.management.dto.TradingAccountEnableRequest;
import com.cx.management.dto.TradingAccountLogVO;
import com.cx.management.dto.TradingAccountPasswordVO;
import com.cx.management.dto.TradingAccountQuickFundRequest;
import com.cx.management.dto.TradingAccountResetMaxBalanceRequest;
import com.cx.management.dto.TradingAccountUpdateRequest;
import com.cx.management.dto.TradingAccountVO;
import com.cx.management.dto.TradingGroupVO;
import com.cx.management.dto.TradingInvestorVO;
import com.cx.management.entity.TradingAccount;
import com.cx.management.entity.TradingAccountLog;
import com.cx.management.integration.TradingApiClient;
import com.cx.management.mapper.TradingAccountLogMapper;
import com.cx.management.mapper.TradingAccountMapper;
import com.cx.management.util.TradingAccountHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TradingAccountService {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 2;

    private static final String OPERATOR_SYSTEM = "system";
    private static final int REMARK_MAX_LEN = 1000;
    private static final int MONEY_SCALE = 2;
    private static final ZoneId ZONE_SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final TradingAccountMapper tradingAccountMapper;
    private final TradingAccountLogMapper tradingAccountLogMapper;
    private final TradingApiClient tradingApiClient;
    private final TradingApiProperties tradingApiProperties;
    /** 用 ObjectProvider 避免与成绩服务形成循环依赖 */
    private final ObjectProvider<AccountScoreService> accountScoreServiceProvider;

    /**
     * 从平台投资者列表一次写入本地账号快照：
     * account / name / comment / base_capital / group_id / group_name / status。
     * 首次入库不写 log；仅状态变化时写 log。不同步覆盖本地 password。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, TradingAccount> syncFromPlatform(List<TradingInvestorVO> investors) {
        Map<String, TradingAccount> localByAccount = new HashMap<>();
        if (investors == null || investors.isEmpty()) {
            return localByAccount;
        }
        for (TradingInvestorVO investor : investors) {
            if (investor == null || !StringUtils.hasText(investor.getAccount())) {
                continue;
            }
            String account = investor.getAccount().trim();
            TradingAccount existing = findByAccount(account);
            int targetStatus = resolveSyncStatus(existing, investor.getName());

            if (existing == null) {
                TradingAccount created = new TradingAccount();
                fillSnapshot(created, investor, false);
                created.setStatus(targetStatus);
                if (targetStatus == STATUS_DISABLED) {
                    created.setDisableTime(LocalDateTime.now());
                }
                tradingAccountMapper.insert(created);
                localByAccount.put(account, created);
            } else {
                Integer fromStatus = existing.getStatus();
                String keptPassword = existing.getPassword();
                BigDecimal keptCapital = existing.getBaseCapital();
                fillSnapshot(existing, investor, false);
                existing.setPassword(keptPassword);
                // 平台未拉到期初时保留原值，避免偶发失败清空
                if (existing.getBaseCapital() == null && keptCapital != null) {
                    existing.setBaseCapital(keptCapital);
                }
                existing.setStatus(targetStatus);
                if (targetStatus == STATUS_DISABLED) {
                    if (existing.getDisableTime() == null) {
                        existing.setDisableTime(LocalDateTime.now());
                    }
                } else if (fromStatus != null && fromStatus == STATUS_DISABLED && targetStatus != STATUS_DISABLED) {
                    existing.setDisableTime(null);
                }
                tradingAccountMapper.updateById(existing);
                if (fromStatus == null || fromStatus != targetStatus) {
                    writeLog(account,
                            buildSyncRemark(fromStatus, targetStatus, investor.getName()),
                            OPERATOR_SYSTEM);
                }
                localByAccount.put(account, existing);
            }
        }
        return localByAccount;
    }

    /**
     * 列表查询：只读本地 trading_account（同步才拉交易后台）。
     */
    public List<TradingInvestorVO> listLocalAccounts(String groupId) {
        LambdaQueryWrapper<TradingAccount> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(groupId)) {
            wrapper.eq(TradingAccount::getGroupId, groupId.trim());
        }
        // 考核日期倒序，无日期排最后；同日期再按账号
        wrapper.last("ORDER BY assess_date IS NULL ASC, assess_date DESC, account ASC");
        List<TradingAccount> locals = tradingAccountMapper.selectList(wrapper);
        List<TradingInvestorVO> result = new ArrayList<>();
        for (TradingAccount local : locals) {
            if (local == null || !StringUtils.hasText(local.getAccount())) {
                continue;
            }
            result.add(toInvestorVo(local));
        }
        return result;
    }

    private TradingInvestorVO toInvestorVo(TradingAccount local) {
        TradingInvestorVO vo = new TradingInvestorVO();
        vo.setAccount(local.getAccount());
        vo.setName(local.getName());
        vo.setComment(local.getComment());
        vo.setGroupId(local.getGroupId());
        vo.setGroupName(local.getGroupName());
        vo.setBaseCapital(local.getBaseCapital());
        vo.setStatus(local.getStatus());
        vo.setHasPassword(StringUtils.hasText(local.getPassword()));
        vo.setPassword(local.getPassword());
        vo.setPasswordMasked(maskPassword(local.getPassword()));
        if (local.getAssessDate() != null) {
            vo.setAccountDate(local.getAssessDate().format(DATE_FORMATTER));
        }
        return vo;
    }

    public void enrichInvestorVos(List<TradingInvestorVO> investors, Map<String, TradingAccount> localByAccount) {
        if (investors == null) {
            return;
        }
        Map<String, TradingAccount> map = localByAccount;
        if (map == null) {
            map = loadLocalMap(investors);
        }
        for (TradingInvestorVO vo : investors) {
            if (vo == null || !StringUtils.hasText(vo.getAccount())) {
                continue;
            }
            TradingAccount local = map.get(vo.getAccount().trim());
            if (local == null) {
                continue;
            }
            vo.setStatus(local.getStatus());
            vo.setBaseCapital(local.getBaseCapital());
            vo.setHasPassword(StringUtils.hasText(local.getPassword()));
            vo.setPassword(local.getPassword());
            vo.setPasswordMasked(maskPassword(local.getPassword()));
            if (StringUtils.hasText(local.getGroupName())) {
                vo.setGroupName(local.getGroupName());
            }
            // 启用/重启日期作为考核日期优先展示
            if (local.getAssessDate() != null) {
                vo.setAccountDate(local.getAssessDate().format(DATE_FORMATTER));
            }
        }
    }

    /** 只读加载本地账号，不写入 */
    public Map<String, TradingAccount> loadLocalMap(List<TradingInvestorVO> investors) {
        Map<String, TradingAccount> result = new HashMap<>();
        if (investors == null || investors.isEmpty()) {
            return result;
        }
        List<String> accounts = investors.stream()
                .filter(v -> v != null && StringUtils.hasText(v.getAccount()))
                .map(v -> v.getAccount().trim())
                .distinct()
                .collect(Collectors.toList());
        if (accounts.isEmpty()) {
            return result;
        }
        List<TradingAccount> locals = tradingAccountMapper.selectList(
                new LambdaQueryWrapper<TradingAccount>().in(TradingAccount::getAccount, accounts));
        for (TradingAccount local : locals) {
            if (local != null && StringUtils.hasText(local.getAccount())) {
                result.put(local.getAccount(), local);
            }
        }
        return result;
    }

    /**
     * 确保本地有账号记录；不存在则按平台信息首次入库（不写 log）。
     */
    @Transactional(rollbackFor = Exception.class)
    public TradingAccount ensureLocal(String account) {
        String trimmed = account.trim();
        TradingAccount local = findByAccount(trimmed);
        if (local != null) {
            return local;
        }
        TradingInvestorVO investor = tradingApiClient.queryInvestor(trimmed);
        local = new TradingAccount();
        local.setAccount(trimmed);
        if (investor != null) {
            fillSnapshot(local, investor, false);
            local.setStatus(resolveSyncStatus(null, investor.getName()));
        } else {
            local.setStatus(STATUS_PENDING);
        }
        tradingAccountMapper.insert(local);
        return local;
    }

    /**
     * 修改名称/密码/备注/分组/考核日期：先调交易后台，再更新本地并写变更记录。
     * 考核日期有变动且新值非空时，成绩统计新开一条考核记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateAccount(TradingAccountUpdateRequest request, String operator) {
        String account = request.getAccount().trim();
        TradingAccount local = ensureLocal(account);

        boolean changeName = request.getName() != null;
        boolean changePassword = StringUtils.hasText(request.getPassword());
        boolean changeComment = request.getComment() != null;
        boolean changeGroup = request.getGroupId() != null;
        boolean changeAssessDate = request.getAssessDate() != null;
        if (!changeName && !changePassword && !changeComment && !changeGroup && !changeAssessDate) {
            throw new BusinessException("请至少修改名称、密码、备注、分组或考核日期中的一项");
        }

        String newName = changeName ? trimToEmpty(request.getName()) : null;
        String newPassword = changePassword ? request.getPassword().trim() : null;
        String newComment = changeComment ? trimToEmpty(request.getComment()) : null;

        String apiName = changeName ? newName : null;
        String apiPassword = changePassword ? newPassword : null;
        // remark != null 才会下发；空串表示清空
        String apiRemark = changeComment ? newComment : null;
        if (changeName || changePassword || changeComment) {
            tradingApiClient.changeAccount(account, apiPassword, apiName, apiRemark);
        }

        List<String> parts = new ArrayList<>();
        if (changeName) {
            parts.add("名称：" + nullToDash(local.getName()) + " → " + nullToDash(newName));
            local.setName(StringUtils.hasText(newName) ? newName : null);
            int newStatus = resolveSyncStatus(local, local.getName());
            if (local.getStatus() == null || local.getStatus() != newStatus) {
                Integer from = local.getStatus();
                local.setStatus(newStatus);
                if (newStatus == STATUS_DISABLED) {
                    local.setDisableTime(LocalDateTime.now());
                } else {
                    local.setDisableTime(null);
                }
                parts.add("状态：" + statusLabel(from) + " → " + statusLabel(newStatus));
            }
        }
        if (changePassword) {
            parts.add("密码：" + nullToDash(local.getPassword()) + " → " + nullToDash(newPassword));
            local.setPassword(newPassword);
        }
        if (changeComment) {
            parts.add("备注：" + nullToDash(local.getComment()) + " → " + nullToDash(newComment));
            local.setComment(StringUtils.hasText(newComment) ? newComment : null);
        }
        if (changeGroup) {
            applyGroupChange(account, local, request.getGroupId().trim(), parts);
        }

        LocalDate newAssessDate = null;
        boolean assessDateChanged = false;
        if (changeAssessDate) {
            String raw = request.getAssessDate().trim();
            LocalDate oldAssessDate = local.getAssessDate();
            if (!StringUtils.hasText(raw)) {
                newAssessDate = null;
            } else {
                try {
                    newAssessDate = LocalDate.parse(raw, DATE_FORMATTER);
                } catch (Exception ex) {
                    throw new BusinessException("考核日期格式不正确，应为 yyyy-MM-dd");
                }
                if (newAssessDate.isAfter(LocalDate.now(ZONE_SHANGHAI))) {
                    throw new BusinessException("考核日期不能晚于今天");
                }
            }
            assessDateChanged = !Objects.equals(oldAssessDate, newAssessDate);
            if (assessDateChanged) {
                parts.add("考核日期："
                        + (oldAssessDate == null ? "-" : oldAssessDate.format(DATE_FORMATTER))
                        + " → "
                        + (newAssessDate == null ? "-" : newAssessDate.format(DATE_FORMATTER)));
                local.setAssessDate(newAssessDate);
            }
        }

        tradingAccountMapper.updateById(local);
        if (assessDateChanged && newAssessDate == null) {
            // updateById 默认忽略 null，需单独清空考核日期
            tradingAccountMapper.update(null, new LambdaUpdateWrapper<TradingAccount>()
                    .eq(TradingAccount::getId, local.getId())
                    .set(TradingAccount::getAssessDate, null));
        }

        if (assessDateChanged && newAssessDate != null) {
            AccountScoreService accountScoreService = accountScoreServiceProvider.getObject();
            accountScoreService.startOnAccountEnable(account, newAssessDate, local.getName(), operator);
            parts.add("已加入成绩考核");
        }

        if (parts.isEmpty()) {
            throw new BusinessException("未检测到有效变更");
        }
        writeLog(account, "修改账号资料：" + String.join("；", parts), operator);
    }

    private static final BigDecimal FUND_100W = new BigDecimal("1000000");
    private static final BigDecimal FUND_500W = new BigDecimal("5000000");
    private static final BigDecimal FUND_1000W = new BigDecimal("10000000");

    /**
     * 出入金：type=IN 入金 / OUT 出金，amount 为正数。
     */
    @Transactional(rollbackFor = Exception.class)
    public TradingAccountVO deposit(TradingAccountDepositRequest request, String operator) {
        String account = request.getAccount().trim();
        String type = request.getType() == null ? "" : request.getType().trim().toUpperCase();
        if (!"IN".equals(type) && !"OUT".equals(type)) {
            throw new BusinessException("类型必须为入金(IN)或出金(OUT)");
        }
        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("金额必须为正数");
        }
        amount = amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        ensureLocal(account);

        boolean isIn = "IN".equals(type);
        BigDecimal signed = isIn ? amount : amount.negate();
        String label = isIn ? "入金" : "出金";
        tradingApiClient.deposit(account, signed, BigDecimal.ZERO, label);

        TradingAccountVO funds = tradingApiClient.queryAccount(account);
        StringBuilder logRemark = new StringBuilder()
                .append(label)
                .append("：")
                .append(amount.toPlainString());
        if (funds != null && funds.getBalance() != null) {
            logRemark.append("，当前权益：").append(funds.getBalance().toPlainString());
        }
        writeLog(account, logRemark.toString(), operator);
        return funds;
    }

    /**
     * 快捷资金操作：
     * RESET_100W / RESET_500W / RESET_1000W 将权益与期初重置到目标金额；
     * WITHDRAW_ALL 一键出清当前权益。
     */
    @Transactional(rollbackFor = Exception.class)
    public TradingAccountVO quickFund(TradingAccountQuickFundRequest request, String operator) {
        String account = request.getAccount().trim();
        String action = request.getAction() == null ? "" : request.getAction().trim().toUpperCase();
        ensureLocal(account);

        TradingAccountVO before = tradingApiClient.queryAccount(account);
        if (before == null || before.getBalance() == null) {
            throw new BusinessException("无法获取账号资金信息");
        }
        BigDecimal balance = before.getBalance();
        TradingAccountVO funds;

        switch (action) {
            case "RESET_100W":
                funds = resetFundToTarget(account, before, FUND_100W, "100万资金重置");
                break;
            case "RESET_500W":
                funds = resetFundToTarget(account, before, FUND_500W, "500万资金重置");
                break;
            case "RESET_1000W":
                funds = resetFundToTarget(account, before, FUND_1000W, "1000万资金重置");
                break;
            case "WITHDRAW_ALL":
                funds = withdrawAll(account, balance);
                break;
            default:
                throw new BusinessException("快捷操作类型无效");
        }

        TradingAccount local = findByAccount(account);
        if (local != null && funds != null && funds.getBaseCapital() != null) {
            local.setBaseCapital(funds.getBaseCapital());
            tradingAccountMapper.updateById(local);
        }
        writeLog(account, buildQuickFundRemark(action, before, funds), operator);
        return funds;
    }

    /**
     * 将权益与期初重置到目标金额，并复位历史最高/当日最高/历史最高结算权益。
     * 交易后台 deposit 的 amount / credit / basecapital 均为差额，且须同号（或为 0）；
     * 因此权益与期初分开两次下发，避免符号冲突。
     */
    private TradingAccountVO resetFundToTarget(String account, TradingAccountVO before,
                                               BigDecimal target, String remark) {
        BigDecimal currentBalance = before.getBalance() != null ? before.getBalance() : BigDecimal.ZERO;
        BigDecimal currentBase = before.getBaseCapital() != null ? before.getBaseCapital() : BigDecimal.ZERO;
        BigDecimal amountDelta = target.subtract(currentBalance).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal baseDelta = target.subtract(currentBase).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        if (amountDelta.compareTo(BigDecimal.ZERO) != 0) {
            tradingApiClient.deposit(account, amountDelta, BigDecimal.ZERO, remark);
        }
        if (baseDelta.compareTo(BigDecimal.ZERO) != 0) {
            tradingApiClient.deposit(account, BigDecimal.ZERO, BigDecimal.ZERO, baseDelta, remark);
        }
        // 1=历史最高动态权益 2=当日最高动态权益 3=历史最高结算权益
        tradingApiClient.resetMaxBalance(account, 1);
        tradingApiClient.resetMaxBalance(account, 2);
        tradingApiClient.resetMaxBalance(account, 3);
        return tradingApiClient.queryAccount(account);
    }

    private TradingAccountVO withdrawAll(String account, BigDecimal balance) {
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("当前无可出金权益");
        }
        BigDecimal amount = balance.setScale(MONEY_SCALE, RoundingMode.HALF_UP).negate();
        tradingApiClient.deposit(account, amount, BigDecimal.ZERO, "一键出金");
        return tradingApiClient.queryAccount(account);
    }

    private String buildQuickFundRemark(String action, TradingAccountVO before, TradingAccountVO after) {
        String label;
        switch (action) {
            case "RESET_100W":
                label = "100万资金重置";
                break;
            case "RESET_500W":
                label = "500万资金重置";
                break;
            case "RESET_1000W":
                label = "1000万资金重置";
                break;
            case "WITHDRAW_ALL":
                label = "一键出金";
                break;
            default:
                label = "快捷资金操作";
        }
        return label + "：权益 "
                + moneyText(before != null ? before.getBalance() : null)
                + " → "
                + moneyText(after != null ? after.getBalance() : null)
                + "，期初 "
                + moneyText(before != null ? before.getBaseCapital() : null)
                + " → "
                + moneyText(after != null ? after.getBaseCapital() : null)
                + (isFundResetAction(action) ? "；已复位历史最高/当日最高/历史最高结算权益" : "");
    }

    private boolean isFundResetAction(String action) {
        return "RESET_100W".equals(action) || "RESET_500W".equals(action) || "RESET_1000W".equals(action);
    }

    /**
     * 复位最高权益：EVER_MAX / DAY_MAX / SETTLE_MAX。
     */
    @Transactional(rollbackFor = Exception.class)
    public TradingAccountVO resetMaxBalance(TradingAccountResetMaxBalanceRequest request, String operator) {
        String account = request.getAccount().trim();
        String type = request.getType() == null ? "" : request.getType().trim().toUpperCase();
        int item;
        String label;
        switch (type) {
            case "EVER_MAX":
                item = 1;
                label = "历史最高动态权益";
                break;
            case "DAY_MAX":
                item = 2;
                label = "当日最高动态权益";
                break;
            case "SETTLE_MAX":
                item = 3;
                label = "历史最高结算权益";
                break;
            default:
                throw new BusinessException("复位类型无效");
        }
        ensureLocal(account);
        TradingAccountVO before = tradingApiClient.queryAccount(account);
        tradingApiClient.resetMaxBalance(account, item);
        TradingAccountVO after = tradingApiClient.queryAccount(account);

        String beforeVal = maxBalanceText(before, type);
        String afterVal = maxBalanceText(after, type);
        writeLog(account, "复位" + label + "：" + beforeVal + " → " + afterVal, operator);
        return after;
    }

    private String maxBalanceText(TradingAccountVO funds, String type) {
        if (funds == null) {
            return "（空）";
        }
        BigDecimal value;
        switch (type) {
            case "EVER_MAX":
                value = funds.getEverMaxBalance();
                break;
            case "DAY_MAX":
                value = funds.getMaxBalance();
                break;
            case "SETTLE_MAX":
                value = funds.getSettleMaxBalance();
                break;
            default:
                value = null;
        }
        return moneyText(value);
    }

    /**
     * 启用账号（仅待使用/已停用）：可同时改名称/密码/备注/分组/期初资金，并置为已启用。
     */
    @Transactional(rollbackFor = Exception.class)
    public TradingAccountVO enableAccount(TradingAccountEnableRequest request, String operator) {
        String account = request.getAccount().trim();
        TradingAccount local = ensureLocal(account);

        Integer status = local.getStatus();
        if (status == null || (status != STATUS_PENDING && status != STATUS_DISABLED)) {
            throw new BusinessException("仅待使用或已停用账号可启用");
        }

        BigDecimal newCapital = request.getBaseCapital().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (newCapital.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("期初资金不能为负数");
        }

        List<String> parts = new ArrayList<>();
        Integer fromStatus = local.getStatus();

        boolean changePassword = StringUtils.hasText(request.getPassword());
        String newName = request.getName() != null ? trimToEmpty(request.getName()) : trimToEmpty(local.getName());
        String newComment = request.getComment() != null ? trimToEmpty(request.getComment()) : trimToEmpty(local.getComment());
        String newPassword = changePassword ? request.getPassword().trim() : null;

        boolean nameChanged = !Objects.equals(nullToEmpty(local.getName()), newName);
        boolean commentChanged = !Objects.equals(nullToEmpty(local.getComment()), newComment);

        if (nameChanged || changePassword || commentChanged) {
            tradingApiClient.changeAccount(
                    account,
                    newPassword,
                    nameChanged ? newName : null,
                    commentChanged ? newComment : null);
        }
        if (nameChanged) {
            parts.add("名称：" + nullToDash(local.getName()) + " → " + nullToDash(newName));
            local.setName(StringUtils.hasText(newName) ? newName : null);
        }
        if (changePassword) {
            parts.add("密码：" + nullToDash(local.getPassword()) + " → " + nullToDash(newPassword));
            local.setPassword(newPassword);
        }
        if (commentChanged) {
            parts.add("备注：" + nullToDash(local.getComment()) + " → " + nullToDash(newComment));
            local.setComment(StringUtils.hasText(newComment) ? newComment : null);
        }

        if (StringUtils.hasText(request.getGroupId())) {
            applyGroupChange(account, local, request.getGroupId().trim(), parts);
        }

        TradingAccountVO beforeFunds = tradingApiClient.queryAccount(account);
        BigDecimal oldCapital = beforeFunds != null && beforeFunds.getBaseCapital() != null
                ? beforeFunds.getBaseCapital()
                : local.getBaseCapital();
        if (oldCapital == null) {
            oldCapital = BigDecimal.ZERO;
        }
        boolean capitalChanged = oldCapital.compareTo(newCapital) != 0;
        if (capitalChanged) {
            BigDecimal delta = newCapital.subtract(oldCapital).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            tradingApiClient.deposit(account, BigDecimal.ZERO, BigDecimal.ZERO, delta, "启用调整期初资金");
        }

        TradingAccountVO funds = tradingApiClient.queryAccount(account);
        BigDecimal actualCapital = funds != null && funds.getBaseCapital() != null
                ? funds.getBaseCapital()
                : newCapital;

        LocalDate assessDate = LocalDate.now(ZONE_SHANGHAI);
        local.setBaseCapital(actualCapital);
        local.setStatus(STATUS_ENABLED);
        local.setDisableTime(null);
        local.setAssessDate(assessDate);
        tradingAccountMapper.updateById(local);

        AccountScoreService accountScoreService = accountScoreServiceProvider.getObject();
        accountScoreService.startOnAccountEnable(account, assessDate, local.getName(), operator);

        parts.add(0, "状态：" + statusLabel(fromStatus) + " → " + statusLabel(STATUS_ENABLED));
        parts.add("期初资金：" + moneyText(oldCapital) + " → " + moneyText(actualCapital));
        parts.add("考核日期：" + assessDate.format(DATE_FORMATTER));
        parts.add("已加入成绩考核");
        writeLog(account, "启用账号：" + String.join("；", parts), operator);
        return funds;
    }

    /**
     * 停用账号（仅已启用）：置为已停用，并停止该账号启用中的成绩统计。
     */
    @Transactional(rollbackFor = Exception.class)
    public void disableAccount(TradingAccountDisableRequest request, String operator) {
        String account = request.getAccount().trim();
        TradingAccount local = ensureLocal(account);

        Integer status = local.getStatus();
        if (status == null || status != STATUS_ENABLED) {
            throw new BusinessException("仅已启用账号可停用");
        }

        LocalDateTime now = LocalDateTime.now();
        local.setStatus(STATUS_DISABLED);
        local.setDisableTime(now);
        tradingAccountMapper.updateById(local);

        AccountScoreService accountScoreService = accountScoreServiceProvider.getObject();
        accountScoreService.disableByAccount(account, AccountScoreService.STOP_ACCOUNT_DISABLE, operator);

        writeLog(account,
                "停用账号：状态：" + statusLabel(STATUS_ENABLED) + " → " + statusLabel(STATUS_DISABLED)
                        + "；已停止成绩统计",
                operator);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void applyGroupChange(String account, TradingAccount local, String newGroupId, List<String> parts) {
        TradingInvestorVO investor = tradingApiClient.queryInvestor(account);
        String oldGroupId = investor != null ? investor.getGroupId() : local.getGroupId();
        List<TradingGroupVO> groups = tradingApiClient.queryGroups("0");
        Map<String, String> groupNameMap = TradingAccountHelper.buildGroupNameMap(groups);
        String oldGroupName = TradingAccountHelper.resolveGroupName(oldGroupId, groupNameMap);

        if (!StringUtils.hasText(newGroupId)) {
            throw new BusinessException("目标分组不能为空");
        }
        if (newGroupId.equals(oldGroupId == null ? "" : oldGroupId)) {
            return;
        }
        String targetGroupName = groupNameMap.get(newGroupId);
        if (!StringUtils.hasText(targetGroupName)) {
            throw new BusinessException("目标分组不存在: " + newGroupId);
        }
        tradingApiClient.changeAccountGroup(account, targetGroupName);
        TradingInvestorVO afterInvestor = tradingApiClient.queryInvestor(account);
        String actualGi = afterInvestor != null ? afterInvestor.getGroupId() : null;
        if (!newGroupId.equals(actualGi)) {
            throw new BusinessException("分组修改未生效，当前仍为「"
                    + TradingAccountHelper.resolveGroupName(actualGi, groupNameMap)
                    + "」。交易后台通常要求结算后才能改分组");
        }
        String newGroupName = TradingAccountHelper.resolveGroupName(newGroupId, groupNameMap);
        local.setGroupId(newGroupId);
        local.setGroupName(newGroupName);
        parts.add("分组：" + nullToDash(oldGroupName != null ? oldGroupName : oldGroupId)
                + " → " + nullToDash(newGroupName != null ? newGroupName : newGroupId));
    }

    public List<TradingAccountLogVO> listLogs(String account) {
        if (!StringUtils.hasText(account)) {
            throw new BusinessException("账号不能为空");
        }
        List<TradingAccountLog> logs = tradingAccountLogMapper.selectList(
                new LambdaQueryWrapper<TradingAccountLog>()
                        .eq(TradingAccountLog::getAccount, account.trim())
                        .orderByDesc(TradingAccountLog::getCreated)
                        .orderByDesc(TradingAccountLog::getId)
                        .last("LIMIT 200"));
        return logs.stream().map(this::toLogVO).collect(Collectors.toList());
    }

    /**
     * 查看明文密码，并写入变动记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public TradingAccountPasswordVO revealPassword(String account, String operator) {
        if (!StringUtils.hasText(account)) {
            throw new BusinessException("账号不能为空");
        }
        String trimmed = account.trim();
        TradingAccount local = findByAccount(trimmed);
        if (local == null || !StringUtils.hasText(local.getPassword())) {
            throw new BusinessException("该账号未保存密码");
        }
        writeLog(trimmed, "查看密码", operator);
        TradingAccountPasswordVO vo = new TradingAccountPasswordVO();
        vo.setAccount(trimmed);
        vo.setPassword(local.getPassword());
        return vo;
    }

    /** 刷新并回写本地期初资金 */
    @Transactional(rollbackFor = Exception.class)
    public TradingAccountVO refreshFunds(String account) {
        if (!StringUtils.hasText(account)) {
            throw new BusinessException("账号不能为空");
        }
        String trimmed = account.trim();
        TradingAccountVO funds = tradingApiClient.queryAccount(trimmed);
        TradingAccount local = findByAccount(trimmed);
        if (local != null && funds != null && funds.getBaseCapital() != null) {
            local.setBaseCapital(funds.getBaseCapital());
            tradingAccountMapper.updateById(local);
        }
        return funds;
    }

    /**
     * 回收成功后更新本地：清空名称、写入回收备注、密码改为回收密码，并按风控更新状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public void onRecycleSuccess(String account, String riskContent, String operator, String remark) {
        if (!StringUtils.hasText(account)) {
            return;
        }
        String trimmed = account.trim();
        TradingAccount existing = findByAccount(trimmed);
        int fromStatus = existing != null && existing.getStatus() != null
                ? existing.getStatus()
                : STATUS_PENDING;
        boolean disableRisk = isDisableRisk(riskContent);
        boolean alreadyDisabled = existing != null
                && existing.getStatus() != null
                && existing.getStatus() == STATUS_DISABLED;
        // 命中停用风控 → 已停用；已停用不降级为待使用
        int toStatus = (disableRisk || alreadyDisabled) ? STATUS_DISABLED : STATUS_PENDING;
        String recyclePassword = tradingApiProperties.getRecyclePassword();
        String recycleName = tradingApiProperties.getRecycleName();
        // 与交易后台一致：默认清空名称
        String nameToStore = StringUtils.hasText(recycleName) ? recycleName.trim() : null;
        String finalRemark = StringUtils.hasText(remark)
                ? remark.trim()
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "回收";
        LocalDateTime now = LocalDateTime.now();

        if (existing == null) {
            existing = new TradingAccount();
            existing.setAccount(trimmed);
            existing.setName(nameToStore);
            existing.setComment(finalRemark);
            existing.setStatus(toStatus);
            if (StringUtils.hasText(recyclePassword)) {
                existing.setPassword(recyclePassword);
            }
            if (toStatus == STATUS_DISABLED) {
                existing.setDisableTime(now);
            }
            tradingAccountMapper.insert(existing);
        } else {
            // 必须用 UpdateWrapper：updateById 默认忽略 null，名称清空会写不进库
            LambdaUpdateWrapper<TradingAccount> updater = new LambdaUpdateWrapper<TradingAccount>()
                    .eq(TradingAccount::getId, existing.getId())
                    .set(TradingAccount::getName, nameToStore)
                    .set(TradingAccount::getComment, finalRemark)
                    .set(TradingAccount::getStatus, toStatus)
                    .set(TradingAccount::getDisableTime, toStatus == STATUS_DISABLED ? now : null);
            if (StringUtils.hasText(recyclePassword)) {
                updater.set(TradingAccount::getPassword, recyclePassword);
            }
            tradingAccountMapper.update(null, updater);
            existing.setName(nameToStore);
            existing.setComment(finalRemark);
            existing.setStatus(toStatus);
            existing.setDisableTime(toStatus == STATUS_DISABLED ? now : null);
            if (StringUtils.hasText(recyclePassword)) {
                existing.setPassword(recyclePassword);
            }
        }

        String op = StringUtils.hasText(operator) ? operator.trim() : OPERATOR_SYSTEM;
        // 状态变化或回收本身都写入风控原文，避免「非指定风控」看不到具体内容
        writeLog(trimmed, buildRecycleRemark(fromStatus, toStatus, disableRisk, riskContent, finalRemark), op);
    }

    public static String statusLabel(Integer status) {
        if (status == null) {
            return "-";
        }
        switch (status) {
            case STATUS_PENDING:
                return "待使用";
            case STATUS_ENABLED:
                return "已启用";
            case STATUS_DISABLED:
                return "已停用";
            default:
                return String.valueOf(status);
        }
    }

    /** 提交回收任务时写入 message 前缀，避免 VARCHAR 截断后丢失停用判定 */
    public static final String DISABLE_RISK_MARK = "[DISABLE_RISK]";

    /**
     * 是否属于应置「已停用」的风控文案。
     * 兼容空格差异，以及任务提交时写入的 {@link #DISABLE_RISK_MARK} 标记。
     */
    public static boolean isDisableRisk(String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }
        if (content.contains(DISABLE_RISK_MARK)) {
            return true;
        }
        // 去掉空白，避免「超过 6%」等写法漏判
        String compact = content.replaceAll("\\s+", "");
        if (compact.contains("历史最大市值权益")) {
            return true;
        }
        if (compact.contains("回撤") && compact.contains("超过6%")) {
            return true;
        }
        return compact.contains("小于期初投入") && compact.contains("的95%");
    }

    private int resolveSyncStatus(TradingAccount existing, String name) {
        boolean emptyName = !StringUtils.hasText(name);
        // 已停用：名称为空则保持停用，绝不降为待使用；有名称则视为重新启用
        if (existing != null
                && existing.getStatus() != null
                && existing.getStatus() == STATUS_DISABLED) {
            return emptyName ? STATUS_DISABLED : STATUS_ENABLED;
        }
        return emptyName ? STATUS_PENDING : STATUS_ENABLED;
    }

    private void fillSnapshot(TradingAccount entity, TradingInvestorVO investor, boolean overwritePassword) {
        entity.setAccount(investor.getAccount().trim());
        entity.setName(StringUtils.hasText(investor.getName()) ? investor.getName().trim() : null);
        entity.setComment(StringUtils.hasText(investor.getComment()) ? investor.getComment().trim() : null);
        // 考核日期仅启用时写入，同步不从备注补齐、也不覆盖已有值
        entity.setGroupId(StringUtils.hasText(investor.getGroupId()) ? investor.getGroupId().trim() : null);
        String groupName = investor.getGroupName();
        if (StringUtils.hasText(groupName) && !"-".equals(groupName.trim())) {
            entity.setGroupName(groupName.trim());
        } else {
            entity.setGroupName(null);
        }
        if (investor.getBaseCapital() != null) {
            entity.setBaseCapital(investor.getBaseCapital());
        }
        if (overwritePassword) {
            entity.setPassword(null);
        }
    }

    private TradingAccount findByAccount(String account) {
        return tradingAccountMapper.selectOne(new LambdaQueryWrapper<TradingAccount>()
                .eq(TradingAccount::getAccount, account)
                .last("LIMIT 1"));
    }

    private void writeLog(String account, String remark, String operator) {
        TradingAccountLog log = new TradingAccountLog();
        log.setAccount(account);
        log.setRemark(truncate(remark, REMARK_MAX_LEN));
        log.setOperator(StringUtils.hasText(operator) ? operator.trim() : OPERATOR_SYSTEM);
        tradingAccountLogMapper.insert(log);
    }

    private String maskPassword(String password) {
        if (!StringUtils.hasText(password)) {
            return null;
        }
        int len = password.length();
        if (len == 1) {
            return "*";
        }
        if (len == 2) {
            return password.charAt(0) + "*";
        }
        return password.charAt(0) + "***" + password.charAt(len - 1);
    }

    private TradingAccountLogVO toLogVO(TradingAccountLog log) {
        TradingAccountLogVO vo = new TradingAccountLogVO();
        vo.setId(log.getId());
        vo.setAccount(log.getAccount());
        vo.setRemark(log.getRemark());
        vo.setOperator(log.getOperator());
        vo.setCreated(log.getCreated());
        return vo;
    }

    private String buildSyncRemark(Integer fromStatus, int toStatus, String name) {
        String reason = StringUtils.hasText(name) ? "名称非空" : "名称为空";
        if (fromStatus == null) {
            return "同步更新：状态设为" + statusLabel(toStatus) + "（" + reason + "）";
        }
        return "同步更新：" + statusLabel(fromStatus) + " → " + statusLabel(toStatus)
                + "（" + reason + "）";
    }

    private String buildRecycleRemark(int fromStatus, int toStatus, boolean disableRisk,
                                       String riskContent, String finalRemark) {
        StringBuilder header = new StringBuilder();
        if (fromStatus != toStatus) {
            if (disableRisk) {
                header.append("风控回收：")
                        .append(statusLabel(fromStatus))
                        .append(" → ")
                        .append(statusLabel(toStatus))
                        .append("（")
                        .append(describeDisableReason(riskContent))
                        .append("）");
            } else {
                header.append("账号回收：")
                        .append(statusLabel(fromStatus))
                        .append(" → ")
                        .append(statusLabel(toStatus))
                        .append("（非指定风控）");
            }
        } else {
            header.append("账号回收：状态保持")
                    .append(statusLabel(toStatus))
                    .append("，名称清空，备注改为「")
                    .append(finalRemark)
                    .append("」");
            if (disableRisk) {
                header.append("（")
                        .append(describeDisableReason(riskContent))
                        .append("）");
            } else {
                header.append("（非指定风控）");
            }
        }
        String raw = normalizeRiskContentForLog(riskContent);
        String riskPart = StringUtils.hasText(raw) ? ("原文：" + raw) : "原文：（无）";
        return joinRemarkWithRisk(header.toString(), riskPart);
    }

    /** 去掉内部标记，仅用于日志展示 */
    private String normalizeRiskContentForLog(String riskContent) {
        if (!StringUtils.hasText(riskContent)) {
            return null;
        }
        String s = riskContent.trim();
        if (s.startsWith(DISABLE_RISK_MARK)) {
            s = s.substring(DISABLE_RISK_MARK.length()).trim();
        }
        return StringUtils.hasText(s) ? s : null;
    }

    /** 保证状态说明 + 风控原文尽量都写入（优先保留原文） */
    private String joinRemarkWithRisk(String header, String riskPart) {
        String sep = "。";
        int max = REMARK_MAX_LEN;
        if (header.length() + sep.length() + riskPart.length() <= max) {
            return header + sep + riskPart;
        }
        int remain = max - header.length() - sep.length();
        if (remain <= 8) {
            return truncate(header + sep + riskPart, max);
        }
        String clipped = riskPart.length() > remain
                ? riskPart.substring(0, remain - 1) + "…"
                : riskPart;
        return header + sep + clipped;
    }

    private String describeDisableReason(String content) {
        String raw = normalizeRiskContentForLog(content);
        String compact = StringUtils.hasText(raw) ? raw.replaceAll("\\s+", "") : "";
        if (compact.contains("小于期初投入") && compact.contains("的95%")) {
            return "市值权益小于期初投入的95%";
        }
        if (compact.contains("历史最大市值权益")
                || (compact.contains("回撤") && compact.contains("超过6%"))) {
            return "历史最大市值权益回撤超过6%";
        }
        return "指定风控";
    }

    private String moneyText(BigDecimal value) {
        return value == null ? "（空）" : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP).toPlainString();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String nullToDash(String value) {
        return StringUtils.hasText(value) ? value : "（空）";
    }

    private String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen - 1) + "…";
    }
}
