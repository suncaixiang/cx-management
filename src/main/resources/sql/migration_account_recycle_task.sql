-- 账号回收异步任务
CREATE TABLE IF NOT EXISTS account_recycle_task (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    status          VARCHAR(20)  NOT NULL COMMENT 'PENDING/RUNNING/COMPLETED',
    total_count     INT          NOT NULL DEFAULT 0,
    processed_count INT          NOT NULL DEFAULT 0,
    success_count   INT          NOT NULL DEFAULT 0,
    fail_count      INT          NOT NULL DEFAULT 0,
    operator        VARCHAR(64)  DEFAULT NULL COMMENT '操作人',
    remark          VARCHAR(200) DEFAULT NULL COMMENT '回收备注',
    created         DATETIME     DEFAULT NULL,
    modified        DATETIME     DEFAULT NULL,
    finished_at     DATETIME     DEFAULT NULL COMMENT '完成时间',
    KEY idx_status (status),
    KEY idx_created (created)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号回收任务';

CREATE TABLE IF NOT EXISTS account_recycle_task_item (
    id       BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    task_id  BIGINT       NOT NULL,
    account  VARCHAR(32)  NOT NULL,
    status   VARCHAR(20)  NOT NULL COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
    message  TEXT         DEFAULT NULL COMMENT '风控原文/结果说明',
    created  DATETIME     DEFAULT NULL,
    modified DATETIME     DEFAULT NULL,
    KEY idx_task_id (task_id),
    KEY idx_account (account)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号回收任务明细';
