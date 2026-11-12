-- 考核管理下新增「博易」目录，并将相关菜单归入其下
SET @assessment_dir_id = (SELECT id FROM sys_permission WHERE path = '/assessment' AND type = 1 LIMIT 1);

INSERT INTO sys_permission (parent_id, name, permission, type, path, component, icon, sort)
SELECT @assessment_dir_id, '博易', NULL, 1, '/assessment/boyi', NULL, 'Wallet', 1
FROM DUAL
WHERE @assessment_dir_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE path = '/assessment/boyi' AND type = 1);

SET @boyi_dir_id = (SELECT id FROM sys_permission WHERE path = '/assessment/boyi' AND type = 1 LIMIT 1);

UPDATE sys_permission SET parent_id = @boyi_dir_id, sort = 1
WHERE permission = 'boyi:account:list' AND @boyi_dir_id IS NOT NULL;

UPDATE sys_permission SET parent_id = @boyi_dir_id, sort = 2
WHERE permission = 'account:recycle:list' AND @boyi_dir_id IS NOT NULL;

UPDATE sys_permission SET parent_id = @boyi_dir_id, sort = 3
WHERE permission = 'account:recycle:task:list' AND @boyi_dir_id IS NOT NULL;

UPDATE sys_permission SET parent_id = @boyi_dir_id, sort = 4
WHERE permission = 'account:recycled:list' AND @boyi_dir_id IS NOT NULL;
