package com.cx.management.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class TradingAccountUpdateRequest {

    @NotBlank(message = "账号不能为空")
    private String account;

    /** null 表示不改名称；空串表示清空名称 */
    private String name;

    /** 空或不传表示不改密码 */
    private String password;

    /** null 表示不改备注；空串表示清空备注 */
    private String comment;

    /** null 或不传表示不改分组 */
    private String groupId;

    /**
     * 考核日期。null 表示不改；空串表示清空；yyyy-MM-dd 表示设为该日。
     * 设为新日期（相对原值有变动）时，成绩统计会新开一条考核记录。
     */
    private String assessDate;
}
