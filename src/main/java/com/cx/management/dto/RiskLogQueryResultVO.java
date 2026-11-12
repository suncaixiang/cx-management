package com.cx.management.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RiskLogQueryResultVO {

    /** 风控日志列表 */
    private List<RiskLogVO> logs = new ArrayList<>();

    /** 触发风控的子账号（去重） */
    private List<String> accounts = new ArrayList<>();
}
