-- 会员足迹测试数据
-- 为会员足迹页面提供测试数据

-- 为用户生成足迹数据
INSERT INTO litemall_footprint (user_id, goods_id, add_time, update_time, deleted) VALUES
-- 用户3 (test_user1) 的足迹
(3, 1025005, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 0),
(3, 1114011, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 0),
(3, 1030002, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 0),
(3, 1023003, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), 0),
(3, 1143016, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 0),

-- 用户4 (test_user2) 的足迹  
(4, 1030003, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 0),
(4, 1113019, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 0),
(4, 1038004, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 0),
(4, 1156006, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), 0),
(4, 1030001, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 0),
(4, 1025005, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), 0),
(4, 1114011, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), 0),

-- 用户5 (test_user3) 的足迹
(5, 1025005, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 0),
(5, 1114011, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 0),
(5, 1030002, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 0),

-- 用户6 (test_user4) 的足迹
(6, 1023003, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 0),
(6, 1143016, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 0),

-- 用户7 (test_user5) 的足迹
(7, 1030003, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 0),
(7, 1113019, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 0),
(7, 1038004, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 0);

-- 验证插入的数据
SELECT '=== 足迹数据统计 ===' AS info;
SELECT COUNT(*) as total_footprints FROM litemall_footprint WHERE deleted = 0;

SELECT '=== 各用户足迹数量统计 ===' AS info;
SELECT u.username, COUNT(*) as footprint_count 
FROM litemall_footprint f 
JOIN litemall_user u ON f.user_id = u.id 
WHERE f.deleted = 0 
GROUP BY u.username 
ORDER BY footprint_count DESC;

SELECT '=== 足迹数据详情（最近10条）===' AS info;
SELECT u.username, g.name as goods_name, f.add_time 
FROM litemall_footprint f 
JOIN litemall_user u ON f.user_id = u.id 
JOIN litemall_goods g ON f.goods_id = g.id 
WHERE f.deleted = 0 
ORDER BY f.add_time DESC 
LIMIT 10;