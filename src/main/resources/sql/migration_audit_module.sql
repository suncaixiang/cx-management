-- =============================================================================
-- 审核模块 PROD 手动执行脚本
-- 库：cx_management
-- 说明：
--   1. 建表使用 IF NOT EXISTS，可重复执行，不会 DROP 已有数据
--   2. 菜单/按钮/角色授权均幂等（NOT EXISTS）
--   3. 默认给 role_id=1（管理员）授权；其他角色请按需追加
--   4. 执行后重新登录，或清权限缓存后再看菜单
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 一、建表（4 张）
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `audit_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_no` varchar(32) NOT NULL COMMENT '用户账号',
  `platform` varchar(64) NOT NULL COMMENT '平台',
  `trade_start_date` varchar(64) NOT NULL COMMENT '开始日期',
  `trade_end_date` varchar(64) NOT NULL COMMENT '结束日期',
  `trade_num` int NOT NULL COMMENT '交易数量',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '审核状态，0-初始化，1-进行中，2-审核完成',
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_account_no` (`account_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='审核记录';

CREATE TABLE IF NOT EXISTS `audit_trade_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `audit_record_id` bigint NOT NULL COMMENT '审核记录ID',
  `account_no` varchar(32) NOT NULL COMMENT '用户账号',
  `platform` varchar(64) NOT NULL COMMENT '平台',
  `instrument_id` varchar(20) NOT NULL COMMENT '合约ID',
  `product_id` varchar(20) DEFAULT NULL COMMENT '合约产品ID',
  `exchange` varchar(32) DEFAULT NULL COMMENT '交易所',
  `trading_day` varchar(10) DEFAULT NULL COMMENT '交易日期',
  `trade_date` varchar(10) DEFAULT NULL COMMENT '成交日期',
  `trade_time` varchar(20) NOT NULL COMMENT '成交时间',
  `trade_volume` int DEFAULT NULL COMMENT '成交手数',
  `trade_price` decimal(12,4) DEFAULT NULL COMMENT '成交价格',
  `trade_direction` varchar(20) NOT NULL COMMENT '买卖',
  `trade_comb_offset` varchar(20) NOT NULL COMMENT '开平',
  `close_profit` decimal(12,4) DEFAULT '0.0000' COMMENT '平仓收益',
  `transaction_num` varchar(200) DEFAULT NULL COMMENT '成交序号',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '分析状态，0-未分析 1-分析中 2-未通过，3-通过',
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_audit_record_id` (`audit_record_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='成交订单';

CREATE TABLE IF NOT EXISTS `audit_fail_result` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `audit_record_id` bigint NOT NULL COMMENT '审核记录ID',
  `trade_record_id` bigint NOT NULL COMMENT '成交记录ID',
  `transaction_num` varchar(200) DEFAULT NULL COMMENT '成交序号',
  `trigger_rule_type` varchar(64) NOT NULL COMMENT '触发规则类型',
  `trigger_detail` varchar(200) NOT NULL COMMENT '触发详情',
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_audit_record_id` (`audit_record_id`) USING BTREE,
  KEY `idx_trade_record_id` (`trade_record_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='分析失败结果';

CREATE TABLE IF NOT EXISTS `audit_disable_product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `platform` varchar(64) NOT NULL COMMENT '平台',
  `product_id` varchar(64) NOT NULL COMMENT '品种ID',
  `product_name` varchar(64) NOT NULL COMMENT '品种名称',
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_platformProduct` (`platform`,`product_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='审核禁用品种';

-- -----------------------------------------------------------------------------
-- 二、菜单与按钮权限
-- -----------------------------------------------------------------------------

-- 一级目录：审核管理
INSERT INTO sys_permission (parent_id, name, permission, type, path, component, icon, sort)
SELECT 0, '审核管理', NULL, 1, '/audit', NULL, 'DocumentChecked', 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE path = '/audit' AND type = 1);

SET @audit_dir_id = (SELECT id FROM sys_permission WHERE path = '/audit' AND type = 1 LIMIT 1);

-- 二级菜单：审核记录 / 禁用产品
INSERT INTO sys_permission (parent_id, name, permission, type, path, component, icon, sort)
SELECT @audit_dir_id, '审核记录', 'audit:record:list', 2, '/audit/record', 'audit/record/index', 'List', 1
FROM DUAL
WHERE @audit_dir_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = 'audit:record:list');

INSERT INTO sys_permission (parent_id, name, permission, type, path, component, icon, sort)
SELECT @audit_dir_id, '禁用产品', 'audit:disable:list', 2, '/audit/disable-product', 'audit/disable-product/index', 'CircleClose', 2
FROM DUAL
WHERE @audit_dir_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = 'audit:disable:list');

SET @audit_record_menu_id = (SELECT id FROM sys_permission WHERE permission = 'audit:record:list' LIMIT 1);
SET @audit_disable_menu_id = (SELECT id FROM sys_permission WHERE permission = 'audit:disable:list' LIMIT 1);

-- 审核记录按钮
INSERT INTO sys_permission (parent_id, name, permission, type, path, component, icon, sort)
SELECT @audit_record_menu_id, '查询审核记录', 'audit:record:query', 3, NULL, NULL, NULL, 1
FROM DUAL
WHERE @audit_record_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = 'audit:record:query');

INSERT INTO sys_permission (parent_id, name, permission, type, path, component, icon, sort)
SELECT @audit_record_menu_id, '新增审核记录', 'audit:record:add', 3, NULL, NULL, NULL, 2
FROM DUAL
WHERE @audit_record_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = 'audit:record:add');

INSERT INTO sys_permission (parent_id, name, permission, type, path, component, icon, sort)
SELECT @audit_record_menu_id, '删除审核记录', 'audit:record:delete', 3, NULL, NULL, NULL, 3
FROM DUAL
WHERE @audit_record_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = 'audit:record:delete');

-- 禁用产品按钮
INSERT INTO sys_permission (parent_id, name, permission, type, path, component, icon, sort)
SELECT @audit_disable_menu_id, '查询禁用产品', 'audit:disable:query', 3, NULL, NULL, NULL, 1
FROM DUAL
WHERE @audit_disable_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = 'audit:disable:query');

INSERT INTO sys_permission (parent_id, name, permission, type, path, component, icon, sort)
SELECT @audit_disable_menu_id, '新增禁用产品', 'audit:disable:add', 3, NULL, NULL, NULL, 2
FROM DUAL
WHERE @audit_disable_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = 'audit:disable:add');

INSERT INTO sys_permission (parent_id, name, permission, type, path, component, icon, sort)
SELECT @audit_disable_menu_id, '删除禁用产品', 'audit:disable:delete', 3, NULL, NULL, NULL, 3
FROM DUAL
WHERE @audit_disable_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = 'audit:disable:delete');

-- -----------------------------------------------------------------------------
-- 三、角色授权（默认管理员 role_id=1；其他角色改 role_id 再跑一遍）
-- -----------------------------------------------------------------------------

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission
WHERE permission IN (
    'audit:record:list', 'audit:record:query', 'audit:record:add', 'audit:record:delete',
    'audit:disable:list', 'audit:disable:query', 'audit:disable:add', 'audit:disable:delete'
)
  AND id NOT IN (SELECT permission_id FROM sys_role_permission WHERE role_id = 1);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, @audit_dir_id FROM DUAL
WHERE @audit_dir_id IS NOT NULL
  AND @audit_dir_id NOT IN (SELECT permission_id FROM sys_role_permission WHERE role_id = 1);

-- -----------------------------------------------------------------------------
-- 四、执行后自检（可选）
-- -----------------------------------------------------------------------------
-- SHOW TABLES LIKE 'audit_%';
-- SELECT id, parent_id, name, permission, type, path FROM sys_permission
-- WHERE path LIKE '/audit%' OR permission LIKE 'audit:%' ORDER BY type, id;
-- SELECT COUNT(*) AS role_perm_cnt FROM sys_role_permission rp
-- JOIN sys_permission p ON p.id = rp.permission_id
-- WHERE rp.role_id = 1 AND (p.permission LIKE 'audit:%' OR p.path = '/audit');
