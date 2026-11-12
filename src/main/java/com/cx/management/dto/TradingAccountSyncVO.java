package com.cx.management.dto;

import lombok.Data;

@Data
public class TradingAccountSyncVO {

    /** 本次同步写入/更新的账号数量 */
    private int total;
}
