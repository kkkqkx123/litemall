-- 用户收藏测试数据
-- 为不同的用户添加商品收藏

-- 用户3 (test_user1) 的收藏
INSERT INTO litemall_collect (user_id, value_id, type, add_time, update_time, deleted) VALUES 
(3, 1025005, 0, NOW(), NOW(), 0),
(3, 1114011, 0, NOW(), NOW(), 0),
(3, 1030002, 0, NOW(), NOW(), 0);

-- 用户4 (test_user2) 的收藏
INSERT INTO litemall_collect (user_id, value_id, type, add_time, update_time, deleted) VALUES 
(4, 1023003, 0, NOW(), NOW(), 0),
(4, 1143016, 0, NOW(), NOW(), 0),
(4, 1030003, 0, NOW(), NOW(), 0),
(4, 1113019, 0, NOW(), NOW(), 0);

-- 用户5 (test_user3) 的收藏
INSERT INTO litemall_collect (user_id, value_id, type, add_time, update_time, deleted) VALUES 
(5, 1038004, 0, NOW(), NOW(), 0),
(5, 1156006, 0, NOW(), NOW(), 0);

-- 用户6 (test_user4) 的收藏
INSERT INTO litemall_collect (user_id, value_id, type, add_time, update_time, deleted) VALUES 
(6, 1030001, 0, NOW(), NOW(), 0),
(6, 1025005, 0, NOW(), NOW(), 0),
(6, 1114011, 0, NOW(), NOW(), 0);

-- 用户7 (test_user5) 的收藏
INSERT INTO litemall_collect (user_id, value_id, type, add_time, update_time, deleted) VALUES 
(7, 1023003, 0, NOW(), NOW(), 0);

-- 查看插入结果
SELECT '收藏数据统计' as info;
SELECT COUNT(*) as total_collects FROM litemall_collect WHERE deleted = 0;
SELECT user_id, COUNT(*) as collect_count FROM litemall_collect WHERE deleted = 0 GROUP BY user_id;
SELECT '收藏数据详情' as info;
SELECT c.id, u.username, g.name as goods_name, c.add_time 
FROM litemall_collect c 
JOIN litemall_user u ON c.user_id = u.id 
JOIN litemall_goods g ON c.value_id = g.id 
WHERE c.deleted = 0 AND c.type = 0 
ORDER BY c.user_id, c.add_time;