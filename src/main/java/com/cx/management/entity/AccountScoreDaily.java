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
@TableName("account_score_daily")
public class AccountScoreDaily {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long configId;

    private String account;

    private LocalDate settleDate;

    private BigDecimal baseCapital;

    private BigDecimal marketEquity;

    private BigDecimal rawPct;

    private BigDecimal scorePct;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime created;
}
