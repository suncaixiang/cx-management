-- 系统管理菜单排到最后
UPDATE sys_permission SET sort = 1 WHERE parent_id = 0 AND path = '/assessment';
UPDATE sys_permission SET sort = 2 WHERE parent_id = 0 AND path = '/system';
