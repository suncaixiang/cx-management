package com.cx.management.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("trading_account")
public class TradingAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String account;

    private String name;

    /** 交易密码（本地保存） */
    private String password;

    @TableField("`comment`")
    private String comment;

    /** 期初资金 */
    private BigDecimal baseCapital;

    private LocalDate assessDate;

    private String groupId;

    private String groupName;

    /** 0待使用 1已启用 2已停用 */
    private Integer status;

    private LocalDateTime disableTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime created;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime modified;
}
