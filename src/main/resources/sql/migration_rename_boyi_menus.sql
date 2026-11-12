-- 菜单名称调整
UPDATE sys_permission SET name = '交易账户' WHERE permission = 'boyi:account:list' AND type = 2;
UPDATE sys_permission SET name = '交易账户回收' WHERE permission = 'account:recycle:list' AND type = 2;
