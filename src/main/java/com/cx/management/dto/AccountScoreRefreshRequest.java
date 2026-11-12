package com.cx.management.dto;

import lombok.Data;

import java.util.List;

@Data
public class AccountScoreRefreshRequest {

    /** 指定考核期 ID；为空则刷新全部启用中 */
    private List<Long> configIds;
}
