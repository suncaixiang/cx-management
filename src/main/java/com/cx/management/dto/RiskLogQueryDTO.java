package com.cx.management.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RiskLogQueryDTO {

    @NotBlank(message = "开始时间不能为空")
    private String timeFrom;

    @NotBlank(message = "截止时间不能为空")
    private String timeTo;
}
