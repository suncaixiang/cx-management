-- 删除 sys_user 表多余字段
ALTER TABLE sys_user DROP COLUMN nickname;
ALTER TABLE sys_user DROP COLUMN email;
ALTER TABLE sys_user DROP COLUMN phone;
ALTER TABLE sys_user DROP COLUMN avatar;
