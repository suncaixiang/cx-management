package com.cx.management.job;

import com.cx.management.dto.AccountScoreRefreshResultVO;
import com.cx.management.service.AccountScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 成绩统计定时任务：每天 17:30（上海时区）刷新启用中的考核期。
 * 已有日成绩的日期跳过，不重复计算。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountScoreScheduleJob {

    private final AccountScoreService accountScoreService;

    @Scheduled(cron = "0 30 17 * * ?", zone = "Asia/Shanghai")
    public void runDailyScore() {
        log.info("成绩统计定时任务开始");
        try {
            List<AccountScoreRefreshResultVO> results = accountScoreService.refreshScheduled();
            int success = 0;
            int already = 0;
            int skipped = 0;
            for (AccountScoreRefreshResultVO item : results) {
                success += item.getSuccessDays();
                already += item.getAlreadyDoneDays();
                skipped += item.getSkippedDays();
            }
            log.info("成绩统计定时任务结束: configs={}, 新增/更新={}天, 已跑过跳过={}天, 无结算跳过={}天",
                    results.size(), success, already, skipped);
        } catch (Exception ex) {
            log.error("成绩统计定时任务失败", ex);
        }
    }
}
