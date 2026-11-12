package com.cx.management.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AccountRecycleTaskSummaryVO {

    private Long taskId;

    private String status;

    private int totalCount;

    private int processedCount;

    private int successCount;

    private int failCount;

    private String operator;

    private LocalDateTime created;

    private LocalDateTime finishedAt;
}
