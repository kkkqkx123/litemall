-- =============================================
-- 综合测试数据脚本
-- =============================================
-- 此文件整合了以下测试数据：
-- - 品牌制造商测试数据
-- - 用户收藏测试数据  
-- - 会员足迹测试数据
-- =============================================

-- 1. 品牌制造商测试数据
-- =============================================
-- 为不同的用户添加品牌制造商关注和收藏

-- 用户3 (test_user1) 的品牌关注
INSERT INTO litemall_collect (user_id, value_id, type, add_time, update_time, deleted) VALUES 
(3, 1001001, 1, NOW(), NOW(), 0),
(3, 1001002, 1, NOW(), NOW(), 0);

-- 用户4 (test_user2) 的品牌关注
INSERT INTO litemall_collect (user_id, value_id, type, add_time, update_time, deleted) VALUES 
(4, 1001003, 1, NOW(), NOW(), 0),
(4, 1001004, 1, NOW(), NOW(), 0),
(4, 1001005, 1, NOW(), NOW(), 0);

-- 用户5 (test_user3) 的品牌关注
INSERT INTO litemall_collect (user_id, value_id, type, add_time, update_time, deleted) VALUES 
(5, 1001006, 1, NOW(), NOW(), 0),
(5, 1001007, 1, NOW(), NOW(), 0);

-- 用户6 (test_user4) 的品牌关注
INSERT INTO litemall_collect (user_id, value_id, type, add_time, update_time, deleted) VALUES 
(6, 1001008, 1, NOW(), NOW(), 0),
(6, 1001009, 1, NOW(), NOW(), 0),
(6, 1001010, 1, NOW(), NOW(), 0);

-- 用户7 (test_user5) 的品牌关注
INSERT INTO litemall_collect (user_id, value_id, type, add_time, update_time, deleted) VALUES 
(7, 1001001, 1, NOW(), NOW(), 0);

-- 2. 用户商品收藏测试数据
-- =============================================
-- 为不同的用户添加商品收藏

-- 用户3 (test_user1) 的商品收藏
INSERT INTO litemall_collect (user_id, value_id, type, add_time, update_time, deleted) VALUES 
(3, 1025005, 0, NOW(), NOW(), 0),
(3, 1114011, 0, NOW(), NOW(), 0),
(3, 1030002, 0, NOW(), NOW(), 0);

-- 用户4 (test_user2) 的商品收藏
INSERT INTO litemall_collect (user_id, value_id, type, add_time, update_time, deleted) VALUES 
(4, 1023003, 0, NOW(), NOW(), 0),
(4, 1143016, 0, NOW(), NOW(), 0),
(4, 1030003, 0, NOW(), NOW(), 0),
(4, 1113019, 0, NOW(), NOW(), 0);

-- 用户5 (test_user3) 的商品收藏
INSERT INTO litemall_collect (user_id, value_id, type, add_time, update_time, deleted) VALUES 
(5, 1038004, 0, NOW(), NOW(), 0),
(5, 1156006, 0, NOW(), NOW(), 0);

-- 用户6 (test_user4) 的商品收藏
INSERT INTO litemall_collect (user_id, value_id, type, add_time, update_time, deleted) VALUES 
(6, 1030001, 0, NOW(), NOW(), 0),
(6, 1025005, 0, NOW(), NOW(), 0),
(6, 1114011, 0, NOW(), NOW(), 0);

-- 用户7 (test_user5) 的商品收藏
INSERT INTO litemall_collect (user_id, value_id, type, add_time, update_time, deleted) VALUES 
(7, 1023003, 0, NOW(), NOW(), 0);

-- 3. 会员足迹测试数据
-- =============================================
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

-- 4. 数据验证和统计
-- =============================================

-- 收藏数据统计
SELECT '=== 收藏数据统计 ===' AS info;
SELECT 
    '品牌关注' as collect_type,
    COUNT(*) as total_count 
FROM litemall_collect 
WHERE deleted = 0 AND type = 1;

SELECT 
    '商品收藏' as collect_type,
    COUNT(*) as total_count 
FROM litemall_collect 
WHERE deleted = 0 AND type = 0;

SELECT '=== 各用户收藏数量统计 ===' AS info;
SELECT 
    u.username, 
    COUNT(CASE WHEN c.type = 0 THEN 1 END) as goods_collects,
    COUNT(CASE WHEN c.type = 1 THEN 1 END) as brand_follows,
    COUNT(*) as total_collects
FROM litemall_collect c 
JOIN litemall_user u ON c.user_id = u.id 
WHERE c.deleted = 0 
GROUP BY u.username 
ORDER BY total_collects DESC;

-- 足迹数据统计
SELECT '=== 足迹数据统计 ===' AS info;
SELECT COUNT(*) as total_footprints FROM litemall_footprint WHERE deleted = 0;

SELECT '=== 各用户足迹数量统计 ===' AS info;
SELECT 
    u.username, 
    COUNT(*) as footprint_count 
FROM litemall_footprint f 
JOIN litemall_user u ON f.user_id = u.id 
WHERE f.deleted = 0 
GROUP BY u.username 
ORDER BY footprint_count DESC;

-- 最近足迹详情
SELECT '=== 最近足迹详情（前10条）===' AS info;
SELECT 
    u.username, 
    g.name as goods_name, 
    f.add_time 
FROM litemall_footprint f 
JOIN litemall_user u ON f.user_id = u.id 
JOIN litemall_goods g ON f.goods_id = g.id 
WHERE f.deleted = 0 
ORDER BY f.add_time DESC 
LIMIT 10;