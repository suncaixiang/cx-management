package com.cx.management.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TradingInvestorVO {

    private String account;

    private String name;

    private String comment;

    private String groupId;

    private String groupName;

    /** 从备注解析的创建/重置/回收时间，格式 yyyy-MM-dd */
    private String accountDate;

    private BigDecimal baseCapital;

    /** 当前权益 */
    private BigDecimal balance;

    /** 回收操作人 */
    private String operator;

    /** 本地状态：0待使用 1已启用 2已停用 */
    private Integer status;

    /** 本地是否已保存密码 */
    private Boolean hasPassword;

    /** 交易密码明文（本地保存） */
    private String password;

    /** 脱敏密码（兼容旧前端） */
    private String passwordMasked;
}
