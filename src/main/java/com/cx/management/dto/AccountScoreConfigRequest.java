package com.cx.management.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class AccountScoreConfigRequest {

    @NotBlank(message = "账号不能为空")
    private String account;

    @NotNull(message = "考核开始日期不能为空")
    private LocalDate assessStart;

    /** true=重设日期（结束旧期并开新期）；false/空=新增，若已有启用中则拒绝 */
    private Boolean reset;
}
