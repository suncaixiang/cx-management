package com.cx.management.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分析失败结果 audit_fail_result
 */
@Data
@TableName("audit_fail_result")
public class AuditFailResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 审核记录ID */
    private Long auditRecordId;

    /** 成交记录ID */
    private Long tradeRecordId;

    /** 成交序号 */
    private String transactionNum;

    /** 触发规则类型 */
    private String triggerRuleType;

    /** 触发详情 */
    private String triggerDetail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime created;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime modified;
}
