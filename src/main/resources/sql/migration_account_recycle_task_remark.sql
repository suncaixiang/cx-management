-- 回收任务增加备注字段
ALTER TABLE account_recycle_task
    ADD COLUMN remark VARCHAR(200) DEFAULT NULL COMMENT '回收备注' AFTER operator;
