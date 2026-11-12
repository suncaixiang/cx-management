-- 已回收账号菜单
SET @assessment_dir_id = (SELECT id FROM sys_permission WHERE path = '/assessment' AND type = 1 LIMIT 1);

INSERT INTO sys_permission (parent_id, name, permission, type, path, component, icon, sort)
SELECT @assessment_dir_id, '已回收账号', 'account:recycled:list', 2, '/assessment/boyi-recycled', 'assessment/boyi-recycled/index', 'Finished', 3
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = 'account:recycled:list');

SET @recycled_menu_id = (SELECT id FROM sys_permission WHERE permission = 'account:recycled:list' LIMIT 1);

INSERT INTO sys_permission (parent_id, name, permission, type, path, component, icon, sort)
SELECT @recycled_menu_id, '查询已回收账号', 'account:recycled:query', 3, NULL, NULL, NULL, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = 'account:recycled:query');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE permission IN ('account:recycled:list', 'account:recycled:query')
AND id NOT IN (SELECT permission_id FROM sys_role_permission WHERE role_id = 1);
