-- 成绩统计：考核期配置 + 每日明细
CREATE TABLE IF NOT EXISTS account_score_config (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    account          VARCHAR(32)  NOT NULL COMMENT '交易账号',
    name             VARCHAR(100) DEFAULT NULL COMMENT '名称快照',
    assess_start     DATE         NOT NULL COMMENT '本期考核开始日期',
    total_score_pct  DECIMAL(12,4) NOT NULL DEFAULT 0 COMMENT '累计成绩%=SUM(daily.score_pct)',
    score_days       INT          NOT NULL DEFAULT 0 COMMENT '有效结算天数',
    last_settle_date DATE         DEFAULT NULL COMMENT '最近结算日',
    status           TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用中 0已停用',
    stop_reason      VARCHAR(32)  DEFAULT NULL COMMENT 'RECYCLE/RESET/MANUAL',
    stop_time        DATETIME     DEFAULT NULL COMMENT '停用时间',
    operator         VARCHAR(50)  DEFAULT NULL COMMENT '操作人',
    created          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_account_status (account, status),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩统计-账号考核期';

CREATE TABLE IF NOT EXISTS account_score_daily (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    config_id      BIGINT        NOT NULL COMMENT '所属考核期',
    account        VARCHAR(32)   NOT NULL COMMENT '交易账号',
    settle_date    DATE          NOT NULL COMMENT '结算交易日',
    base_capital   DECIMAL(18,2) NOT NULL COMMENT '计算时使用的期初资金',
    market_equity  DECIMAL(18,2) NOT NULL COMMENT '市值权益',
    raw_pct        DECIMAL(10,4) NOT NULL COMMENT '原始收益率%',
    score_pct      DECIMAL(10,4) NOT NULL COMMENT '当日成绩%',
    created        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_date (config_id, settle_date),
    KEY idx_account_date (account, settle_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩统计-每日明细';
