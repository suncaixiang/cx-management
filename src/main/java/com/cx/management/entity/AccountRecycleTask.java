package com.cx.management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("account_recycle_task")
public class AccountRecycleTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String status;

    private Integer totalCount;

    private Integer processedCount;

    private Integer successCount;

    private Integer failCount;

    private String operator;

    /** 回收备注（写入交易账户 remark） */
    private String remark;

    private LocalDateTime finishedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime created;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime modified;
}
