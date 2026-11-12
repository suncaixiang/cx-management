package com.cx.management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("account_recycle_task_item")
public class AccountRecycleTaskItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String account;

    private String status;

    private String message;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime created;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime modified;
}
