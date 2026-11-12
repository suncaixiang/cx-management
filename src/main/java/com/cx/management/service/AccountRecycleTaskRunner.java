package com.cx.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cx.management.dto.TradingInvestorVO;
import com.cx.management.entity.AccountRecycleTask;
import com.cx.management.entity.AccountRecycleTaskItem;
import com.cx.management.mapper.AccountRecycleTaskItemMapper;
import com.cx.management.mapper.AccountRecycleTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountRecycleTaskRunner {

    private final AccountRecycleTaskMapper taskMapper;
    private final AccountRecycleTaskItemMapper taskItemMapper;
    private final AccountRecycleService accountRecycleService;
    private final TradingAccountService tradingAccountService;

    public void run(Long taskId) {
        AccountRecycleTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }

        updateTaskStatus(taskId, AccountRecycleTaskService.STATUS_RUNNING);

        Map<String, TradingInvestorVO> investorMap = accountRecycleService.loadInvestorMap();
        String operator = task.getOperator();
        String remark = task.getRemark();

        List<AccountRecycleTaskItem> items = taskItemMapper.selectList(
                new LambdaQueryWrapper<AccountRecycleTaskItem>()
                        .eq(AccountRecycleTaskItem::getTaskId, taskId)
                        .orderByAsc(AccountRecycleTaskItem::getId));

        int successCount = 0;
        int failCount = 0;

        for (AccountRecycleTaskItem item : items) {
            String riskContent = item.getMessage();
            updateItemStatus(item.getId(), AccountRecycleTaskService.ITEM_RUNNING, riskContent);
            try {
                accountRecycleService.recycleSingleAccount(item.getAccount(), operator, investorMap, remark);
                tradingAccountService.onRecycleSuccess(item.getAccount(), riskContent, operator, remark);
                updateItemStatus(item.getId(), AccountRecycleTaskService.ITEM_SUCCESS, "回收成功");
                successCount++;
            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : "回收失败";
                updateItemStatus(item.getId(), AccountRecycleTaskService.ITEM_FAILED, message);
                failCount++;
            }
            updateTaskProgress(taskId, successCount + failCount, successCount, failCount);
        }

        markCompleted(taskId);
    }

    public void markCompleted(Long taskId) {
        AccountRecycleTask update = new AccountRecycleTask();
        update.setId(taskId);
        update.setStatus(AccountRecycleTaskService.STATUS_COMPLETED);
        update.setFinishedAt(LocalDateTime.now());
        taskMapper.updateById(update);
    }

    private void updateTaskStatus(Long taskId, String status) {
        AccountRecycleTask update = new AccountRecycleTask();
        update.setId(taskId);
        update.setStatus(status);
        taskMapper.updateById(update);
    }

    private void updateTaskProgress(Long taskId, int processed, int success, int fail) {
        AccountRecycleTask update = new AccountRecycleTask();
        update.setId(taskId);
        update.setProcessedCount(processed);
        update.setSuccessCount(success);
        update.setFailCount(fail);
        taskMapper.updateById(update);
    }

    private void updateItemStatus(Long itemId, String status, String message) {
        AccountRecycleTaskItem update = new AccountRecycleTaskItem();
        update.setId(itemId);
        update.setStatus(status);
        update.setMessage(message);
        taskItemMapper.updateById(update);
    }
}
