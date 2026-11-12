package com.cx.management.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AccountRecycleTaskStatusVO {

    private Long taskId;

    /** PENDING / RUNNING / COMPLETED */
    private String status;

    private int totalCount;

    private int processedCount;

    private int successCount;

    private int failCount;

    private LocalDateTime finishedAt;

    private List<AccountRecycleItemResultVO> results = new ArrayList<>();
}
