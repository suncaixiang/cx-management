package com.cx.management.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审核记录 audit_record
 */
@Data
@TableName("audit_record")
public class AuditRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户账号 */
    private String accountNo;

    /** 平台 */
    private String platform;

    /** 开始日期 yyyyMMdd */
    private String tradeStartDate;

    /** 结束日期 yyyyMMdd */
    private String tradeEndDate;

    /** 交易数量 */
    private Integer tradeNum;

    /** 审核状态：0初始化 1进行中 2审核完成 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime created;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime modified;
}
