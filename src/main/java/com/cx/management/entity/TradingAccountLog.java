package com.cx.management.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trading_account_log")
public class TradingAccountLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String account;

    private String remark;

    private String operator;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime created;
}
