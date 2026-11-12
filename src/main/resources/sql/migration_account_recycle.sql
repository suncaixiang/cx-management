-- 账号回收记录表
DROP TABLE IF EXISTS boyi_account_recycle;

CREATE TABLE IF NOT EXISTS account_recycle (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    account      VARCHAR(20)  NOT NULL COMMENT '子账号',
    name         VARCHAR(100) DEFAULT NULL COMMENT '名称',
    comment      VARCHAR(200) DEFAULT NULL COMMENT '备注',
    recycle_time DATETIME     NOT NULL COMMENT '回收时间',
    operator     VARCHAR(50)  DEFAULT NULL COMMENT '操作人',
    created      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_account (account),
    KEY idx_recycle_time (recycle_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号回收记录';
