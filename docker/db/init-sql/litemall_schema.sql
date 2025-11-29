drop database if exists litemall;
drop user if exists 'admin'@'%';
drop user if exists 'kkkqkx'@'%';
drop user if exists 'kkkqkx'@'localhost';
-- 支持emoji：需要mysql数据库参数： character_set_server=utf8mb4
create database litemall default character set utf8mb4 collate utf8mb4_unicode_ci;
use litemall;
create user 'admin'@'%' identified by 'admin123';
create user 'kkkqkx'@'%' identified by '1234567kk';
create user 'kkkqkx'@'localhost' identified by '1234567kk';
grant all privileges on litemall.* to 'admin'@'%';
grant all privileges on litemall.* to 'kkkqkx'@'%';
grant all privileges on litemall.* to 'kkkqkx'@'localhost';
flush privileges;