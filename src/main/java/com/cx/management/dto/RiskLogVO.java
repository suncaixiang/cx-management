package com.cx.management.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class RiskLogVO {

    /** 子账号 */
    private String account;

    /** 日志时间 */
    private String time;

    /** 日志内容 */
    private String content;
}
