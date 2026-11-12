package com.cx.management.dto;

import lombok.Data;

@Data
public class AccountScoreRefreshResultVO {

    private Long configId;

    private String account;

    /** 成功写入/更新的天数 */
    private int successDays;

    /** 已存在跳过的天数 */
    private int alreadyDoneDays;

    /** 跳过（无结算/失败）天数 */
    private int skippedDays;

    private String message;
}
