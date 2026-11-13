package com.cx.management.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审核禁用品种 audit_disable_product
 */
@Data
@TableName("audit_disable_product")
public class AuditDisableProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 平台 */
    private String platform;

    /** 品种ID */
    private String productId;

    /** 品种名称 */
    private String productName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime created;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime modified;
}
