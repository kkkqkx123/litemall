-- =============================================
-- 商品评分统计测试数据生成脚本
-- =============================================
-- 此脚本用于生成商品评分统计功能的测试数据
-- 确保商品评分统计功能能够正常显示和排序
-- =============================================

-- 1. 查看当前商品和评论数据概况
-- =============================================
SELECT '=== 当前数据概况 ===' AS info;

-- 商品总数统计
SELECT 
    COUNT(*) as total_goods,
    COUNT(CASE WHEN c.id IS NOT NULL THEN 1 END) as goods_with_comments,
    ROUND(COUNT(CASE WHEN c.id IS NOT NULL THEN 1 END) * 100.0 / COUNT(*), 2) as coverage_percentage
FROM litemall_goods g
LEFT JOIN litemall_comment c ON g.id = c.value_id AND c.type = 0 AND c.deleted = 0
WHERE g.deleted = 0;

-- 评论评分分布统计
SELECT 
    star AS rating_level,
    COUNT(*) AS comment_count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM litemall_comment WHERE type = 0 AND deleted = 0), 2) AS percentage
FROM litemall_comment 
WHERE type = 0 AND deleted = 0
GROUP BY star
ORDER BY star DESC;

-- 2. 清理现有测试数据（可选）
-- =============================================
-- 注意：取消下面注释将删除所有现有评论数据，请谨慎操作
-- DELETE FROM litemall_comment WHERE type = 0;

-- 3. 为所有商品生成基础评论数据
-- =============================================
INSERT INTO litemall_comment (value_id, type, content, admin_content, user_id, has_picture, pic_urls, star, add_time, update_time, deleted)
SELECT 
    g.id AS value_id,
    0 AS type,
    ELT(FLOOR(1 + (RAND() * 10)),
        '这个商品很不错，质量很好，值得购买！',
        '性价比很高，包装也很精美，满意！',
        '商品符合描述，物流很快，服务态度好！',
        '质量超出预期，五星好评，推荐购买！',
        '很满意的购物，商品质量不错，物流给力！',
        '商品质量很好，做工精细，材质不错！',
        '包装精美，商品质量上乘，非常满意！',
        '不错的商品，质量可靠，服务周到！',
        '商品收到了，使用体验很好，推荐！',
        '质量不错，价格合理，物流快速！'
    ) AS content,
    NULL AS admin_content,
    FLOOR(101 + (RAND() * 200)) AS user_id,  -- 用户ID 101-300
    CASE WHEN RAND() < 0.3 THEN 1 ELSE 0 END AS has_picture,
    CASE WHEN RAND() < 0.3 THEN '[]' ELSE NULL END AS pic_urls,
    -- 评分分布：40%五星，30%四星，20%三星，8%二星，2%一星
    CASE 
        WHEN RAND() < 0.4 THEN 5
        WHEN RAND() < 0.7 THEN 4
        WHEN RAND() < 0.9 THEN 3
        WHEN RAND() < 0.98 THEN 2
        ELSE 1
    END AS star,
    NOW() - INTERVAL FLOOR(1 + (RAND() * 60)) DAY AS add_time,
    NOW() - INTERVAL FLOOR(1 + (RAND() * 60)) DAY AS update_time,
    0 AS deleted
FROM 
    litemall_goods g
WHERE 
    g.deleted = 0 
    AND NOT EXISTS (
        SELECT 1 FROM litemall_comment c 
        WHERE c.value_id = g.id AND c.type = 0 AND c.deleted = 0
    );

-- 4. 为部分商品添加更多评论，确保有足够的评论数据用于统计
-- =============================================
INSERT INTO litemall_comment (value_id, type, content, admin_content, user_id, has_picture, pic_urls, star, add_time, update_time, deleted)
SELECT 
    g.id AS value_id,
    0 AS type,
    ELT(FLOOR(1 + (RAND() * 8)),
        '质量非常好，做工精细，材质优良，包装精美！',
        '性价比超高，外观漂亮，颜色好看，款式新颖！',
        '商品质量很好，功能齐全，设计合理，使用方便！',
        '材质很好，做工精细，细节到位，颜色正，尺寸合适！',
        '超级满意的商品，质量有保障，外观时尚，功能实用！',
        '非常不错的购物体验，商品符合描述，质量很好！',
        '质量很好，做工精细，材质优良，设计合理，使用方便！',
        '很满意的购物体验，商品不错，物流很快，包装完好！'
    ) AS content,
    NULL AS admin_content,
    FLOOR(301 + (RAND() * 200)) AS user_id,  -- 用户ID 301-500
    CASE WHEN RAND() < 0.4 THEN 1 ELSE 0 END AS has_picture,
    CASE WHEN RAND() < 0.4 THEN '["http://example.com/pic.jpg"]' ELSE NULL END AS pic_urls,
    -- 高质量评论评分分布：70%五星，25%四星，5%三星
    CASE 
        WHEN RAND() < 0.7 THEN 5
        WHEN RAND() < 0.95 THEN 4
        ELSE 3
    END AS star,
    NOW() - INTERVAL FLOOR(1 + (RAND() * 30)) DAY AS add_time,
    NOW() - INTERVAL FLOOR(1 + (RAND() * 30)) DAY AS update_time,
    0 AS deleted
FROM 
    litemall_goods g
WHERE 
    g.deleted = 0 
    AND g.id <= 100  -- 为前100个商品添加更多评论
    AND EXISTS (
        SELECT 1 FROM litemall_comment c 
        WHERE c.value_id = g.id AND c.type = 0 AND c.deleted = 0
    )
    AND (SELECT COUNT(*) FROM litemall_comment c2 
         WHERE c2.value_id = g.id AND c2.type = 0 AND c2.deleted = 0) < 5  -- 只为评论少于5条的商品添加
LIMIT 200;  -- 最多添加200条评论

-- 5. 为特定商品生成特定评分，确保评分分布更加合理
-- =============================================
-- 为前20个商品生成高分评论
INSERT INTO litemall_comment (value_id, type, content, admin_content, user_id, has_picture, pic_urls, star, add_time, update_time, deleted)
SELECT 
    g.id AS value_id,
    0 AS type,
    CONCAT('超级满意的', g.name, '，质量非常好，强烈推荐！') AS content,
    NULL AS admin_content,
    FLOOR(501 + (RAND() * 100)) AS user_id,  -- 用户ID 501-600
    CASE WHEN RAND() < 0.5 THEN 1 ELSE 0 END AS has_picture,
    CASE WHEN RAND() < 0.5 THEN '["http://example.com/high_rating.jpg"]' ELSE NULL END AS pic_urls,
    5 AS star,  -- 全部为5星好评
    NOW() - INTERVAL FLOOR(1 + (RAND() * 15)) DAY AS add_time,
    NOW() - INTERVAL FLOOR(1 + (RAND() * 15)) DAY AS update_time,
    0 AS deleted
FROM 
    litemall_goods g
WHERE 
    g.deleted = 0 
    AND g.id <= 20
LIMIT 20;  -- 为前20个商品添加一条5星好评

-- 为中间20个商品生成中等评分评论
INSERT INTO litemall_comment (value_id, type, content, admin_content, user_id, has_picture, pic_urls, star, add_time, update_time, deleted)
SELECT 
    g.id AS value_id,
    0 AS type,
    CONCAT(g.name, '，质量一般，价格合理，可以购买！') AS content,
    NULL AS admin_content,
    FLOOR(601 + (RAND() * 100)) AS user_id,  -- 用户ID 601-700
    CASE WHEN RAND() < 0.3 THEN 1 ELSE 0 END AS has_picture,
    CASE WHEN RAND() < 0.3 THEN '["http://example.com/medium_rating.jpg"]' ELSE NULL END AS pic_urls,
    3 AS star,  -- 全部为3星评价
    NOW() - INTERVAL FLOOR(1 + (RAND() * 15)) DAY AS add_time,
    NOW() - INTERVAL FLOOR(1 + (RAND() * 15)) DAY AS update_time,
    0 AS deleted
FROM 
    litemall_goods g
WHERE 
    g.deleted = 0 
    AND g.id BETWEEN 21 AND 40
LIMIT 20;  -- 为中间20个商品添加一条3星评价

-- 6. 数据验证和统计
-- =============================================
SELECT '=== 数据生成后概况 ===' AS info;

-- 商品评论覆盖率
SELECT 
    CASE 
        WHEN comment_count > 0 THEN '有评论'
        ELSE '无评论'
    END AS has_comments,
    COUNT(*) as goods_count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM litemall_goods WHERE deleted = 0), 2) as percentage
FROM (
    SELECT 
        g.id,
        COUNT(c.id) as comment_count
    FROM litemall_goods g
    LEFT JOIN litemall_comment c ON g.id = c.value_id AND c.type = 0 AND c.deleted = 0
    WHERE g.deleted = 0
    GROUP BY g.id
) t
GROUP BY has_comments;

-- 评分分布统计
SELECT 
    star AS rating_level,
    COUNT(*) AS comment_count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM litemall_comment WHERE type = 0 AND deleted = 0), 2) AS percentage
FROM litemall_comment 
WHERE type = 0 AND deleted = 0
GROUP BY star
ORDER BY star DESC;

-- 商品评分统计（前20名）
SELECT '=== 商品评分统计（前20名）===' AS info;
SELECT 
    g.id,
    g.name AS goods_name,
    ROUND(IFNULL(AVG(c.star), 0), 2) AS avg_rating,
    COUNT(c.id) AS rating_count,
    CASE 
        WHEN COUNT(c.id) = 0 THEN '暂无评论'
        WHEN AVG(c.star) >= 4.5 THEN '好评如潮'
        WHEN AVG(c.star) >= 4.0 THEN '评价不错'
        WHEN AVG(c.star) >= 3.0 THEN '评价一般'
        ELSE '评价较差'
    END AS rating_status
FROM 
    litemall_goods g
LEFT JOIN 
    litemall_comment c ON g.id = c.value_id AND c.type = 0 AND c.deleted = 0
WHERE 
    g.deleted = 0
GROUP BY 
    g.id, g.name
HAVING 
    COUNT(c.id) > 0  -- 只显示有评论的商品
ORDER BY 
    avg_rating DESC, rating_count DESC
LIMIT 20;

-- 最终优化效果总结
SELECT '=== 评论数据生成效果总结 ===' AS info;
SELECT 
    COUNT(*) as total_comments,
    COUNT(DISTINCT value_id) as goods_with_comments,
    ROUND(AVG(star), 2) as avg_rating,
    COUNT(CASE WHEN star >= 4 THEN 1 END) as good_ratings,
    ROUND(COUNT(CASE WHEN star >= 4 THEN 1 END) * 100.0 / COUNT(*), 2) as good_rating_percentage
FROM litemall_comment 
WHERE type = 0 AND deleted = 0;