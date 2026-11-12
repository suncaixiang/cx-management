package com.cx.management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("account_recycle")
public class AccountRecycle {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String account;

    private String name;

    private String comment;

    private LocalDateTime recycleTime;

    private String operator;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime created;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime modified;
}
