-- 博易账号菜单：查询所有交易账户
SET @assessment_dir_id = (SELECT id FROM sys_permission WHERE path = '/assessment' AND type = 1 LIMIT 1);

INSERT INTO sys_permission (parent_id, name, permission, type, path, component, icon, sort) VALUES
(@assessment_dir_id, '博易账号', 'boyi:account:list', 2, '/assessment/boyi-account', 'assessment/boyi-account/index', 'Wallet', 1);

SET @boyi_account_menu_id = LAST_INSERT_ID();

INSERT INTO sys_permission (parent_id, name, permission, type, path, component, icon, sort) VALUES
(@boyi_account_menu_id, '查询交易账户', 'boyi:account:query', 3, NULL, NULL, NULL, 1);

UPDATE sys_permission SET sort = 2
WHERE path = '/assessment/boyi-recycle' AND type = 2;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE permission IN ('boyi:account:list', 'boyi:account:query');
