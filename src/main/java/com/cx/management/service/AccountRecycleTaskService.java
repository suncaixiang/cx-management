package com.cx.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cx.management.common.PageResult;
import com.cx.management.common.exception.BusinessException;
import com.cx.management.dto.AccountRecycleItemResultVO;
import com.cx.management.dto.AccountRecycleRequestDTO;
import com.cx.management.dto.AccountRecycleTaskStatusVO;
import com.cx.management.dto.AccountRecycleTaskSubmitVO;
import com.cx.management.dto.AccountRecycleTaskSummaryVO;
import com.cx.management.entity.AccountRecycleTask;
import com.cx.management.entity.AccountRecycleTaskItem;
import com.cx.management.mapper.AccountRecycleTaskItemMapper;
import com.cx.management.mapper.AccountRecycleTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountRecycleTaskService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String ITEM_PENDING = "PENDING";
    public static final String ITEM_RUNNING = "RUNNING";
    public static final String ITEM_SUCCESS = "SUCCESS";
    public static final String ITEM_FAILED = "FAILED";

    private final AccountRecycleTaskMapper taskMapper;
    private final AccountRecycleTaskItemMapper taskItemMapper;
    private final AccountRecycleTaskExecutor taskExecutor;

    @Transactional(rollbackFor = Exception.class)
    public AccountRecycleTaskSubmitVO submitTask(AccountRecycleRequestDTO dto, String operator) {
        List<String> accounts = normalizeAccounts(dto.getAccounts());
        if (accounts.isEmpty()) {
            throw new BusinessException("账号列表不能为空");
        }

        AccountRecycleTask task = new AccountRecycleTask();
        task.setStatus(STATUS_PENDING);
        task.setTotalCount(accounts.size());
        task.setProcessedCount(0);
        task.setSuccessCount(0);
        task.setFailCount(0);
        task.setOperator(operator);
        String remark = StringUtils.hasText(dto.getRemark())
                ? dto.getRemark().trim()
                : AccountRecycleService.defaultRecycleRemark();
        task.setRemark(remark);
        taskMapper.insert(task);

        Map<String, String> riskContents = dto.getRiskContents();
        for (String account : accounts) {
            AccountRecycleTaskItem item = new AccountRecycleTaskItem();
            item.setTaskId(task.getId());
            item.setAccount(account);
            item.setStatus(ITEM_PENDING);
            // 暂存风控原文到 message；停用类加标记，防止列截断后漏判
            if (riskContents != null && StringUtils.hasText(riskContents.get(account))) {
                String content = riskContents.get(account).trim();
                if (TradingAccountService.isDisableRisk(content)) {
                    item.setMessage(TradingAccountService.DISABLE_RISK_MARK + content);
                } else {
                    item.setMessage(content);
                }
            }
            taskItemMapper.insert(item);
        }

        taskExecutor.executeAsync(task.getId());

        AccountRecycleTaskSubmitVO vo = new AccountRecycleTaskSubmitVO();
        vo.setTaskId(task.getId());
        return vo;
    }

    public AccountRecycleTaskStatusVO getTaskStatus(Long taskId) {
        AccountRecycleTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("回收任务不存在");
        }

        List<AccountRecycleTaskItem> items = taskItemMapper.selectList(
                new LambdaQueryWrapper<AccountRecycleTaskItem>()
                        .eq(AccountRecycleTaskItem::getTaskId, taskId)
                        .orderByAsc(AccountRecycleTaskItem::getId));

        AccountRecycleTaskStatusVO vo = new AccountRecycleTaskStatusVO();
        vo.setTaskId(task.getId());
        vo.setStatus(task.getStatus());
        vo.setTotalCount(defaultZero(task.getTotalCount()));
        vo.setProcessedCount(defaultZero(task.getProcessedCount()));
        vo.setSuccessCount(defaultZero(task.getSuccessCount()));
        vo.setFailCount(defaultZero(task.getFailCount()));
        vo.setFinishedAt(task.getFinishedAt());

        List<AccountRecycleItemResultVO> results = new ArrayList<>();
        for (AccountRecycleTaskItem item : items) {
            if (ITEM_PENDING.equals(item.getStatus()) || ITEM_RUNNING.equals(item.getStatus())) {
                continue;
            }
            AccountRecycleItemResultVO result = new AccountRecycleItemResultVO();
            result.setAccount(item.getAccount());
            result.setSuccess(ITEM_SUCCESS.equals(item.getStatus()));
            result.setMessage(item.getMessage());
            results.add(result);
        }
        vo.setResults(results);
        return vo;
    }

    public PageResult<AccountRecycleTaskSummaryVO> listTasks(long current, long size) {
        Page<AccountRecycleTask> page = taskMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<AccountRecycleTask>()
                        .orderByDesc(AccountRecycleTask::getCreated)
                        .orderByDesc(AccountRecycleTask::getId));

        List<AccountRecycleTaskSummaryVO> records = page.getRecords().stream()
                .map(this::toSummaryVO)
                .collect(Collectors.toList());
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private AccountRecycleTaskSummaryVO toSummaryVO(AccountRecycleTask task) {
        AccountRecycleTaskSummaryVO vo = new AccountRecycleTaskSummaryVO();
        vo.setTaskId(task.getId());
        vo.setStatus(task.getStatus());
        vo.setTotalCount(defaultZero(task.getTotalCount()));
        vo.setProcessedCount(defaultZero(task.getProcessedCount()));
        vo.setSuccessCount(defaultZero(task.getSuccessCount()));
        vo.setFailCount(defaultZero(task.getFailCount()));
        vo.setOperator(task.getOperator());
        vo.setCreated(task.getCreated());
        vo.setFinishedAt(task.getFinishedAt());
        return vo;
    }

    private List<String> normalizeAccounts(List<String> accounts) {
        if (accounts == null) {
            return new ArrayList<>();
        }
        return accounts.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }
}
