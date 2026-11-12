-- 成绩统计菜单（挂在「博易」目录下）
SET @boyi_dir_id = (SELECT id FROM sys_permission WHERE path = '/assessment/boyi' AND type = 1 LIMIT 1);

INSERT INTO sys_permission (parent_id, name, permission, type, path, component, icon, sort)
SELECT @boyi_dir_id, '成绩统计', 'boyi:score:list', 2, '/assessment/boyi-score', 'assessment/boyi-score/index', 'DataAnalysis', 5
FROM DUAL
WHERE @boyi_dir_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = 'boyi:score:list');

SET @score_menu_id = (SELECT id FROM sys_permission WHERE permission = 'boyi:score:list' LIMIT 1);

INSERT INTO sys_permission (parent_id, name, permission, type, path, component, icon, sort)
SELECT @score_menu_id, '查询成绩', 'boyi:score:query', 3, NULL, NULL, NULL, 1
FROM DUAL
WHERE @score_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = 'boyi:score:query');

INSERT INTO sys_permission (parent_id, name, permission, type, path, component, icon, sort)
SELECT @score_menu_id, '编辑成绩', 'boyi:score:edit', 3, NULL, NULL, NULL, 2
FROM DUAL
WHERE @score_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission = 'boyi:score:edit');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission
WHERE permission IN ('boyi:score:list', 'boyi:score:query', 'boyi:score:edit')
  AND id NOT IN (SELECT permission_id FROM sys_role_permission WHERE role_id = 1);
