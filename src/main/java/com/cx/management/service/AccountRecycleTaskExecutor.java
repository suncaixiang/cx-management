package com.cx.management.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountRecycleTaskExecutor {

    private final AccountRecycleTaskRunner taskRunner;

    @Async("recycleTaskExecutor")
    public void executeAsync(Long taskId) {
        try {
            taskRunner.run(taskId);
        } catch (Exception e) {
            log.error("回收任务执行异常: taskId={}", taskId, e);
            taskRunner.markCompleted(taskId);
        }
    }
}
