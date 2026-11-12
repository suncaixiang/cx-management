-- 本地交易账号当前态
CREATE TABLE IF NOT EXISTS trading_account (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    account      VARCHAR(32)  NOT NULL COMMENT '交易账号',
    name         VARCHAR(100) DEFAULT NULL COMMENT '名称',
    comment      VARCHAR(200) DEFAULT NULL COMMENT '备注',
    assess_date  DATE         DEFAULT NULL COMMENT '考核日期（备注解析）',
    group_id     VARCHAR(64)  DEFAULT NULL COMMENT '分组ID',
    group_name   VARCHAR(100) DEFAULT NULL COMMENT '分组名称',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0待使用 1已启用 2已停用',
    disable_time DATETIME     DEFAULT NULL COMMENT '停用时间',
    created      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_account (account),
    KEY idx_status (status),
    KEY idx_group_id (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地交易账号';
