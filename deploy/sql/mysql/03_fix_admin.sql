-- ============================================================
-- 修复管理员（user_id=1）登录
--
-- 背景：
--   1. 登录流程按 user_name 匹配（UserDetailsServiceImpl.selectUserByUserName），
--      而初始 SQL 中 admin 的 user_name='admin'，登录页传入的却是手机号，
--      匹配不上导致“用户不存在”；
--   2. 系统要求密码为手机号后 8 位（Constants.PASSWORD_INTERCEPTION=8，
--      即 88888888），而初始密码是 admin123 的 BCrypt 哈希，导致密码不匹配。
--
-- 本脚本将 admin 的 user_name 改为手机号、密码改为 88888888 的 BCrypt 哈希。
-- 可重复执行。
-- ============================================================

UPDATE sys_user
SET user_name   = '15888888888',
    phonenumber = '15888888888',
    password    = '$2a$10$6UkmtYQlk5SBIsI1XonNv.9RK1BW3Tceun.8gmh0MCkid/jS7J1Ji'
WHERE user_id = 1;
