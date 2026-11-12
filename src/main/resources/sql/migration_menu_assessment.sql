-- 菜单重命名：考核管理 → 博易账号回收
UPDATE sys_permission SET name = '考核管理', path = '/assessment', icon = 'DataBoard'
WHERE type = 1 AND path = '/account';

UPDATE sys_permission SET name = '博易账号回收', path = '/assessment/boyi-recycle', component = 'assessment/boyi-recycle/index'
WHERE permission = 'account:recycle:list';
