-- 回收任务明细：风控原文加长，避免截断导致停用判定丢失
ALTER TABLE account_recycle_task_item
    MODIFY COLUMN message TEXT DEFAULT NULL COMMENT '风控原文/结果说明';
