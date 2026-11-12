-- CX Management System Database Schema
CREATE DATABASE IF NOT EXISTS cx_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cx_management;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT '密码',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_name   VARCHAR(50)  NOT NULL COMMENT '角色名称',
    role_code   VARCHAR(50)  NOT NULL COMMENT '角色编码',
    description VARCHAR(200) DEFAULT NULL COMMENT '描述',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 权限/菜单表
CREATE TABLE IF NOT EXISTS sys_permission (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父级ID',
    name        VARCHAR(50)  NOT NULL COMMENT '权限名称',
    permission  VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    type        TINYINT      NOT NULL COMMENT '类型 1-目录 2-菜单 3-按钮',
    path        VARCHAR(200) DEFAULT NULL COMMENT '路由路径',
    component   VARCHAR(200) DEFAULT NULL COMMENT '组件路径',
    icon        VARCHAR(50)  DEFAULT NULL COMMENT '图标',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限菜单表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    role_id     BIGINT   NOT NULL COMMENT '角色ID',
    created     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_id       BIGINT   NOT NULL COMMENT '角色ID',
    permission_id BIGINT   NOT NULL COMMENT '权限ID',
    created       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 系统日志表
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT        DEFAULT NULL COMMENT '操作用户ID',
    username    VARCHAR(50)   DEFAULT NULL COMMENT '操作用户名',
    module      VARCHAR(50)   DEFAULT NULL COMMENT '模块',
    operation   VARCHAR(100)  DEFAULT NULL COMMENT '操作描述',
    method      VARCHAR(200)  DEFAULT NULL COMMENT '请求方法',
    params      TEXT          DEFAULT NULL COMMENT '请求参数',
    ip          VARCHAR(50)   DEFAULT NULL COMMENT 'IP地址',
    created     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_created (created)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统日志表';

-- 账号回收记录表
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

-- 初始化数据
INSERT INTO sys_user (username, password, status) VALUES
('admin', '$2a$10$ivmXszg44uyjgBXYlEFE7Ok7u2zVlUqEqkfr5Jmyfjrh5oyv2aF6y', 1);

INSERT INTO sys_role (role_name, role_code, description) VALUES
('超级管理员', 'SUPER_ADMIN', '拥有所有权限'),
('普通用户', 'USER', '普通用户角色');

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

INSERT INTO sys_permission (parent_id, name, permission, type, path, component, icon, sort) VALUES
(0, '系统管理', NULL, 1, '/system', NULL, 'Setting', 2),
(1, '用户管理', 'system:user:list', 2, '/system/user', 'system/user/index', 'User', 1),
(1, '角色管理', 'system:role:list', 2, '/system/role', 'system/role/index', 'UserFilled', 2),
(1, '系统日志', 'system:log:list', 2, '/system/log', 'system/log/index', 'Document', 3),
(0, '考核管理', NULL, 1, '/assessment', NULL, 'DataBoard', 1),
(5, '博易', NULL, 1, '/assessment/boyi', NULL, 'Wallet', 1),
(6, '交易账户', 'boyi:account:list', 2, '/assessment/boyi-account', 'assessment/boyi-account/index', 'Wallet', 1),
(6, '交易账户回收', 'account:recycle:list', 2, '/assessment/boyi-recycle', 'assessment/boyi-recycle/index', 'Refresh', 2),
(6, '回收任务', 'account:recycle:task:list', 2, '/assessment/boyi-recycle-tasks', 'assessment/boyi-recycle-tasks/index', 'List', 3),
(6, '已回收账号', 'account:recycled:list', 2, '/assessment/boyi-recycled', 'assessment/boyi-recycled/index', 'Finished', 4),
(2, '用户查询', 'system:user:query', 3, NULL, NULL, NULL, 1),
(2, '用户新增', 'system:user:add', 3, NULL, NULL, NULL, 2),
(2, '用户修改', 'system:user:edit', 3, NULL, NULL, NULL, 3),
(2, '用户删除', 'system:user:delete', 3, NULL, NULL, NULL, 4),
(3, '角色查询', 'system:role:query', 3, NULL, NULL, NULL, 1),
(3, '角色新增', 'system:role:add', 3, NULL, NULL, NULL, 2),
(3, '角色修改', 'system:role:edit', 3, NULL, NULL, NULL, 3),
(3, '角色删除', 'system:role:delete', 3, NULL, NULL, NULL, 4),
(7, '查询交易账户', 'boyi:account:query', 3, NULL, NULL, NULL, 1),
(8, '查询风控日志', 'account:recycle:query', 3, NULL, NULL, NULL, 1),
(9, '查询回收任务', 'account:recycle:task:query', 3, NULL, NULL, NULL, 1),
(10, '查询已回收账号', 'account:recycled:query', 3, NULL, NULL, NULL, 1);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;
