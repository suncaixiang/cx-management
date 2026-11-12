-- 日志合并迁移脚本
USE cx_management;

-- 1. 操作日志表增加 log_type 字段
ALTER TABLE sys_operation_log
    ADD COLUMN IF NOT EXISTS log_type TINYINT NOT NULL DEFAULT 2 COMMENT '日志类型 1-登录 2-操作' AFTER username;

-- MySQL 8.0 不支持 IF NOT EXISTS for column, use procedure or ignore error
-- 若字段已存在可跳过上一句，执行下面索引
ALTER TABLE sys_operation_log ADD INDEX idx_log_type (log_type);

-- 2. 迁移登录日志历史数据（如存在）
INSERT INTO sys_operation_log (user_id, username, log_type, module, operation, method, params, ip, duration, status, error_msg, create_time)
SELECT NULL, username, 1, '认证管理', IF(status = 1, '用户登录', '用户登录'), 'POST /api/auth/login',
       CONCAT('{"username":"', username, '"}'), ip, 0, status, message, login_time
FROM sys_login_log
WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'cx_management' AND table_name = 'sys_login_log');

-- 3. 删除登录日志表
DROP TABLE IF EXISTS sys_login_log;

-- 4. 合并菜单权限
UPDATE sys_permission SET deleted = 1 WHERE permission IN ('system:loginlog:list', 'system:operlog:list');

UPDATE sys_permission
SET name = '系统日志', permission = 'system:log:list', path = '/system/log',
    component = 'system/log/index', icon = 'Document', sort = 3
WHERE permission = 'system:loginlog:list' OR id = (
    SELECT id FROM (SELECT MIN(id) AS id FROM sys_permission WHERE permission IN ('system:loginlog:list', 'system:operlog:list')) t
);

DELETE FROM sys_role_permission WHERE permission_id IN (
    SELECT id FROM sys_permission WHERE permission = 'system:operlog:list' AND deleted = 1
);

-- 为超级管理员补充新权限（如不存在）
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE permission = 'system:log:list';
