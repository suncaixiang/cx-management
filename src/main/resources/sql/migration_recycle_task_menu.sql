-- 回收任务菜单（parent_id 请按实际「考核管理」目录 ID 调整，常见为 14）
SET @assessment_dir_id = (SELECT id FROM sys_permission WHERE path = '/assessment' AND type = 1 LIMIT 1);

INSERT INTO sys_permission (parent_id, name, permission, type, path, component, icon, sort)
SELECT @assessment_dir_id, '回收任务', 'account:recycle:task:list', 2, '/assessment/boyi-recycle-tasks', 'assessment/boyi-recycle-tasks/index', 'List', 3
FROM DUAL
WHERE @assessment_dir_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = 'account:recycle:task:list');

SET @task_menu_id = (SELECT id FROM sys_permission WHERE permission = 'account:recycle:task:list' LIMIT 1);

INSERT INTO sys_permission (parent_id, name, permission, type, path, component, icon, sort)
SELECT @task_menu_id, '查询回收任务', 'account:recycle:task:query', 3, NULL, NULL, NULL, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = 'account:recycle:task:query');

UPDATE sys_permission SET sort = 4
WHERE permission = 'account:recycled:list';

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE permission IN ('account:recycle:task:list', 'account:recycle:task:query')
AND id NOT IN (SELECT permission_id FROM sys_role_permission WHERE role_id = 1);
