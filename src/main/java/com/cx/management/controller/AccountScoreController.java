package com.cx.management.controller;

import com.cx.management.annotation.OperationLog;
import com.cx.management.common.PageResult;
import com.cx.management.common.Result;
import com.cx.management.dto.AccountScoreConfigRequest;
import com.cx.management.dto.AccountScoreConfigVO;
import com.cx.management.dto.AccountScoreDailyVO;
import com.cx.management.dto.AccountScoreRefreshRequest;
import com.cx.management.dto.AccountScoreRefreshResultVO;
import com.cx.management.dto.TradingInvestorListVO;
import com.cx.management.security.SecurityUtils;
import com.cx.management.service.AccountScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/account-score")
@RequiredArgsConstructor
public class AccountScoreController {

    private final AccountScoreService accountScoreService;

    @GetMapping("/candidates")
    @PreAuthorize("hasAuthority('boyi:score:query')")
    public Result<TradingInvestorListVO> candidates() {
        return Result.success(accountScoreService.listCandidates());
    }

    @GetMapping("/configs")
    @PreAuthorize("hasAuthority('boyi:score:query')")
    public Result<PageResult<AccountScoreConfigVO>> configs(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String account,
            @RequestParam(required = false) BigDecimal minScorePct) {
        return Result.success(accountScoreService.pageConfigs(current, size, status, account, minScorePct));
    }

    @PostMapping("/configs")
    @PreAuthorize("hasAuthority('boyi:score:edit')")
    @OperationLog(module = "成绩统计", operation = "设置考核日期")
    public Result<AccountScoreConfigVO> saveConfig(@Validated @RequestBody AccountScoreConfigRequest request) {
        return Result.success(accountScoreService.saveOrReset(request, SecurityUtils.getUsername()));
    }

    @PostMapping("/configs/{id}/stop")
    @PreAuthorize("hasAuthority('boyi:score:edit')")
    @OperationLog(module = "成绩统计", operation = "停用考核")
    public Result<Void> stop(@PathVariable Long id) {
        accountScoreService.stopConfig(id, SecurityUtils.getUsername());
        return Result.success();
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasAuthority('boyi:score:edit')")
    @OperationLog(module = "成绩统计", operation = "刷新成绩")
    public Result<List<AccountScoreRefreshResultVO>> refresh(
            @RequestBody(required = false) AccountScoreRefreshRequest request) {
        List<Long> configIds = request != null ? request.getConfigIds() : null;
        return Result.success(accountScoreService.refresh(configIds));
    }

    @GetMapping("/daily")
    @PreAuthorize("hasAuthority('boyi:score:query')")
    public Result<List<AccountScoreDailyVO>> daily(@RequestParam Long configId) {
        return Result.success(accountScoreService.listDaily(configId));
    }
}
