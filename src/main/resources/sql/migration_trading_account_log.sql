-- 交易账号变动记录（首次入库不记；仅状态变化时写入）
CREATE TABLE IF NOT EXISTS trading_account_log (
    id         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    account    VARCHAR(32)   NOT NULL COMMENT '交易账号',
    remark     VARCHAR(1000) NOT NULL COMMENT '从何状态到何状态、原因；风控原文可附在此',
    operator   VARCHAR(50)   DEFAULT NULL COMMENT '操作人；同步可为 system',
    created    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_account_created (account, created),
    KEY idx_created (created)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易账号变动记录';
