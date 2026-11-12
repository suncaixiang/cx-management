package com.cx.management.controller;

import com.cx.management.annotation.OperationLog;
import com.cx.management.common.Result;
import com.cx.management.common.PageResult;
import com.cx.management.dto.AccountRecycleRequestDTO;
import com.cx.management.dto.AccountRecycleTaskStatusVO;
import com.cx.management.dto.AccountRecycleTaskSubmitVO;
import com.cx.management.dto.AccountRecycleTaskSummaryVO;
import com.cx.management.dto.RiskLogQueryDTO;
import com.cx.management.dto.RiskLogQueryResultVO;
import com.cx.management.dto.TradingInvestorListVO;
import com.cx.management.security.SecurityUtils;
import com.cx.management.service.AccountRecycleService;
import com.cx.management.service.AccountRecycleTaskService;
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

@RestController
@RequestMapping("/api/account-recycle")
@RequiredArgsConstructor
public class AccountRecycleController {

    private final AccountRecycleService accountRecycleService;
    private final AccountRecycleTaskService accountRecycleTaskService;

    @GetMapping("/risk-logs")
    @PreAuthorize("hasAuthority('account:recycle:query')")
    public Result<RiskLogQueryResultVO> queryRiskLogs(@Validated RiskLogQueryDTO dto) {
        return Result.success(accountRecycleService.queryRiskLogs(dto));
    }

    @PostMapping("/recycle")
    @PreAuthorize("hasAuthority('account:recycle:query')")
    @OperationLog(module = "交易账户回收", operation = "账号回收")
    public Result<AccountRecycleTaskSubmitVO> submitRecycleTask(@Validated @RequestBody AccountRecycleRequestDTO dto) {
        String operator = SecurityUtils.getUsername();
        return Result.success(accountRecycleTaskService.submitTask(dto, operator));
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasAuthority('account:recycle:query')")
    public Result<PageResult<AccountRecycleTaskSummaryVO>> listRecycleTasks(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return Result.success(accountRecycleTaskService.listTasks(current, size));
    }

    @GetMapping("/tasks/{taskId}")
    @PreAuthorize("hasAuthority('account:recycle:query')")
    public Result<AccountRecycleTaskStatusVO> getRecycleTaskStatus(@PathVariable Long taskId) {
        return Result.success(accountRecycleTaskService.getTaskStatus(taskId));
    }

    @GetMapping("/recycled-accounts")
    @PreAuthorize("hasAuthority('account:recycled:query')")
    public Result<TradingInvestorListVO> queryRecycledAccounts() {
        return Result.success(accountRecycleService.queryRecycledAccounts());
    }
}
