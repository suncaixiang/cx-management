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
@TableName("account_score_config")
public class AccountScoreConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String account;

    private String name;

    private LocalDate assessStart;

    private BigDecimal totalScorePct;

    private Integer scoreDays;

    private LocalDate lastSettleDate;

    /** 1启用中 0已停用 */
    private Integer status;

    private String stopReason;

    private LocalDateTime stopTime;

    private String operator;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime created;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime modified;
}
