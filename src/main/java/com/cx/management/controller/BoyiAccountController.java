package com.cx.management.controller;

import com.cx.management.annotation.OperationLog;
import com.cx.management.common.Result;
import com.cx.management.dto.TradingAccountCapitalVO;
import com.cx.management.dto.TradingAccountDepositRequest;
import com.cx.management.dto.TradingAccountDisableRequest;
import com.cx.management.dto.TradingAccountEnableRequest;
import com.cx.management.dto.TradingAccountLogVO;
import com.cx.management.dto.TradingAccountPasswordVO;
import com.cx.management.dto.TradingAccountQuickFundRequest;
import com.cx.management.dto.TradingAccountResetMaxBalanceRequest;
import com.cx.management.dto.TradingAccountSyncVO;
import com.cx.management.dto.TradingAccountUpdateRequest;
import com.cx.management.dto.TradingAccountVO;
import com.cx.management.dto.TradingGroupVO;
import com.cx.management.dto.TradingInvestorListVO;
import com.cx.management.security.SecurityUtils;
import com.cx.management.service.BoyiAccountService;
import com.cx.management.service.TradingAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/boyi-accounts")
@RequiredArgsConstructor
public class BoyiAccountController {

    private final BoyiAccountService boyiAccountService;
    private final TradingAccountService tradingAccountService;

    @GetMapping("/groups")
    @PreAuthorize("hasAuthority('boyi:account:query')")
    public Result<List<TradingGroupVO>> groups() {
        return Result.success(boyiAccountService.listGroups());
    }

    @GetMapping("/funds")
    @PreAuthorize("hasAuthority('boyi:account:query')")
    public Result<TradingAccountVO> funds(@RequestParam String account) {
        return Result.success(tradingAccountService.refreshFunds(account));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('boyi:account:query')")
    public Result<TradingInvestorListVO> list(@RequestParam(required = false) String groupId) {
        return Result.success(boyiAccountService.queryAccounts(groupId));
    }

    @PostMapping("/sync")
    public Result<TradingAccountSyncVO> sync() {
        return Result.success(boyiAccountService.syncAccounts());
    }

    @PostMapping("/capitals")
    @PreAuthorize("hasAuthority('boyi:account:query')")
    public Result<TradingAccountCapitalVO> capitals(@RequestBody List<String> accounts) {
        return Result.success(boyiAccountService.queryCapitals(accounts));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('boyi:account:query')")
    @OperationLog(module = "交易账户", operation = "修改账号资料")
    public Result<Void> update(@Validated @RequestBody TradingAccountUpdateRequest request) {
        tradingAccountService.updateAccount(request, SecurityUtils.getUsername());
        return Result.success();
    }

    @PostMapping("/enable")
    @PreAuthorize("hasAuthority('boyi:account:query')")
    @OperationLog(module = "交易账户", operation = "启用账号")
    public Result<TradingAccountVO> enable(@Validated @RequestBody TradingAccountEnableRequest request) {
        return Result.success(tradingAccountService.enableAccount(request, SecurityUtils.getUsername()));
    }

    @PostMapping("/disable")
    @PreAuthorize("hasAuthority('boyi:account:query')")
    @OperationLog(module = "交易账户", operation = "停用账号")
    public Result<Void> disable(@Validated @RequestBody TradingAccountDisableRequest request) {
        tradingAccountService.disableAccount(request, SecurityUtils.getUsername());
        return Result.success();
    }

    @PostMapping("/deposit")
    @PreAuthorize("hasAuthority('boyi:account:query')")
    @OperationLog(module = "交易账户", operation = "出入金")
    public Result<TradingAccountVO> deposit(@Validated @RequestBody TradingAccountDepositRequest request) {
        return Result.success(tradingAccountService.deposit(request, SecurityUtils.getUsername()));
    }

    @PostMapping("/reset-max-balance")
    @PreAuthorize("hasAuthority('boyi:account:query')")
    @OperationLog(module = "交易账户", operation = "复位最高权益")
    public Result<TradingAccountVO> resetMaxBalance(
            @Validated @RequestBody TradingAccountResetMaxBalanceRequest request) {
        return Result.success(tradingAccountService.resetMaxBalance(request, SecurityUtils.getUsername()));
    }

    @PostMapping("/quick-fund")
    @PreAuthorize("hasAuthority('boyi:account:query')")
    @OperationLog(module = "交易账户", operation = "快捷资金操作")
    public Result<TradingAccountVO> quickFund(@Validated @RequestBody TradingAccountQuickFundRequest request) {
        return Result.success(tradingAccountService.quickFund(request, SecurityUtils.getUsername()));
    }

    @GetMapping("/{account}/password")
    @PreAuthorize("hasAuthority('boyi:account:query')")
    @OperationLog(module = "交易账户", operation = "查看密码")
    public Result<TradingAccountPasswordVO> password(@PathVariable String account) {
        return Result.success(tradingAccountService.revealPassword(account, SecurityUtils.getUsername()));
    }

    @GetMapping("/{account}/logs")
    @PreAuthorize("hasAuthority('boyi:account:query')")
    public Result<List<TradingAccountLogVO>> logs(@PathVariable String account) {
        return Result.success(tradingAccountService.listLogs(account));
    }
}
