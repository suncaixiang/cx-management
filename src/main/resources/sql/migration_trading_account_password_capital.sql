-- 本地交易账号：补充密码、期初资金
ALTER TABLE trading_account
    ADD COLUMN password VARCHAR(64) DEFAULT NULL COMMENT '交易密码（本地保存）' AFTER name,
    ADD COLUMN base_capital DECIMAL(18,2) DEFAULT NULL COMMENT '期初资金' AFTER `comment`;
