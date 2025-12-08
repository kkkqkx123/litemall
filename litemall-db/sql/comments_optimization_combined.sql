-- =============================================
-- 评论数据优化综合脚本
-- =============================================
-- 此文件整合了以下评论相关功能：
-- - 基础评论补充
-- - 评论覆盖率优化
-- - 评分分布优化
-- - 词云关键词优化
-- =============================================

-- 1. 查看当前评论数据概况
-- =============================================
SELECT '=== 评论数据初始概况 ===' AS info;

-- 评论总数统计
SELECT 
    COUNT(*) as total_comments,
    COUNT(DISTINCT value_id) as goods_with_comments,
    ROUND(AVG(star), 2) as avg_rating
FROM litemall_comment 
WHERE type = 0 AND deleted = 0;

-- 评分分布统计
SELECT 
    star AS rating_level,
    COUNT(*) AS comment_count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM litemall_comment WHERE type = 0 AND deleted = 0), 2) AS percentage
FROM litemall_comment 
WHERE type = 0 AND deleted = 0
GROUP BY star
ORDER BY star DESC;

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

-- 2. 为无评论商品添加基础评论
-- =============================================
INSERT INTO litemall_comment (value_id, type, content, admin_content, user_id, has_picture, pic_urls, star, add_time, update_time, deleted)
SELECT 
    g.id AS value_id,
    0 AS type,
    ELT(FLOOR(1 + (RAND() * 8)),
        '这个商品很不错，质量很好，值得购买！',
        '性价比很高，包装也很精美，满意！',
        '商品符合描述，物流很快，服务态度好！',
        '质量超出预期，五星好评，推荐购买！',
        '很满意的购物，商品质量不错，物流给力！',
        '商品质量很好，做工精细，材质不错！',
        '包装精美，商品质量上乘，非常满意！',
        '不错的商品，质量可靠，服务周到！'
    ) AS content,
    NULL AS admin_content,
    FLOOR(101 + (RAND() * 50)) AS user_id,  -- 用户ID 101-150
    CASE WHEN RAND() < 0.3 THEN 1 ELSE 0 END AS has_picture,
    CASE WHEN RAND() < 0.3 THEN '[]' ELSE NULL END AS pic_urls,
    CASE 
        WHEN RAND() < 0.7 THEN 5  -- 70% 五星
        WHEN RAND() < 0.9 THEN 4  -- 20% 四星
        ELSE 3  -- 10% 三星
    END AS star,
    NOW() - INTERVAL FLOOR(1 + (RAND() * 30)) DAY AS add_time,
    NOW() - INTERVAL FLOOR(1 + (RAND() * 30)) DAY AS update_time,
    0 AS deleted
FROM 
    litemall_goods g
WHERE 
    g.deleted = 0 
    AND NOT EXISTS (
        SELECT 1 FROM litemall_comment c 
        WHERE c.value_id = g.id AND c.type = 0 AND c.deleted = 0
    )
LIMIT 50;  -- 为50个无评论商品添加评论

-- 3. 为低评分商品补充高质量评论
-- =============================================
INSERT INTO litemall_comment (value_id, type, content, admin_content, user_id, has_picture, pic_urls, star, add_time, update_time, deleted)
SELECT 
    g.id AS value_id,
    0 AS type,
    ELT(FLOOR(1 + (RAND() * 6)),
        CONCAT('非常好的', SUBSTRING_INDEX(g.name, ' ', 1), '，质量超出预期，五星好评！'),
        CONCAT('很满意的', SUBSTRING_INDEX(g.name, ' ', 1), '，质量很好，推荐购买！'),
        CONCAT('购买了', g.name, '，使用体验很棒，性价比很高！'),
        CONCAT('这个', SUBSTRING_INDEX(g.name, ' ', 1), '真的很不错，做工精细，材质优良！'),
        CONCAT(g.name, '收到了，包装精美，质量很好，物流很快！'),
        CONCAT('超级满意的', SUBSTRING_INDEX(g.name, ' ', 1), '，质量有保障，强烈推荐！')
    ) AS content,
    NULL AS admin_content,
    FLOOR(151 + (RAND() * 50)) AS user_id,  -- 用户ID 151-200
    CASE WHEN RAND() < 0.4 THEN 1 ELSE 0 END AS has_picture,
    CASE WHEN RAND() < 0.4 THEN '["http://example.com/pic1.jpg"]' ELSE NULL END AS pic_urls,
    5 AS star,  -- 全部为5星好评
    NOW() - INTERVAL FLOOR(1 + (RAND() * 15)) DAY AS add_time,
    NOW() - INTERVAL FLOOR(1 + (RAND() * 15)) DAY AS update_time,
    0 AS deleted
FROM 
    litemall_goods g
WHERE 
    g.deleted = 0 
    AND EXISTS (
        SELECT 1 FROM litemall_comment c 
        WHERE c.value_id = g.id AND c.type = 0 AND c.deleted = 0 AND c.star <= 3
    )
LIMIT 25;  -- 添加25条5星好评

-- 4. 优化评分分布 - 更新部分低分评论
-- =============================================
UPDATE litemall_comment 
SET star = CASE 
    WHEN RAND() < 0.6 THEN 5  -- 60% 改为5星
    WHEN RAND() < 0.85 THEN 4  -- 25% 改为4星
    ELSE 3  -- 15% 改为3星
END
WHERE type = 0 AND deleted = 0 AND star = 1 AND RAND() < 0.7;  -- 只更新70%的1星评论

UPDATE litemall_comment 
SET star = CASE 
    WHEN RAND() < 0.5 THEN 4  -- 50% 改为4星
    WHEN RAND() < 0.8 THEN 5  -- 30% 改为5星
    ELSE 3  -- 20% 保持3星
END
WHERE type = 0 AND deleted = 0 AND star = 2 AND RAND() < 0.5;  -- 只更新50%的2星评论

-- 5. 为热门商品添加更多高质量评论（优化词云）
-- =============================================
INSERT INTO litemall_comment (value_id, type, content, admin_content, user_id, has_picture, pic_urls, star, add_time, update_time, deleted)
SELECT 
    g.id AS value_id,
    0 AS type,
    ELT(FLOOR(1 + (RAND() * 8)),
        '质量很好，做工精细，包装严实，物流很快，很满意！',
        '性价比很高，材质不错，外观漂亮，功能齐全，推荐！',
        '颜色好看，款式新颖，使用舒适，质量有保障，五星好评！',
        '商品符合描述，服务态度好，物流给力，包装精美！',
        '非常不错的商品，质量满意，服务周到，物流快捷！',
        '质量很好，做工精细，材质优良，设计合理，使用方便！',
        '很满意的购物体验，商品不错，物流很快，包装完好！',
        '超级喜欢的商品，质量很好，性价比超高，强烈推荐！'
    ) AS content,
    NULL AS admin_content,
    FLOOR(201 + (RAND() * 50)) AS user_id,  -- 用户ID 201-250
    CASE WHEN RAND() < 0.3 THEN 1 ELSE 0 END AS has_picture,
    CASE WHEN RAND() < 0.3 THEN '["http://example.com/pic2.jpg"]' ELSE NULL END AS pic_urls,
    CASE 
        WHEN RAND() < 0.8 THEN 5  -- 80% 五星
        ELSE 4  -- 20% 四星
    END AS star,
    NOW() - INTERVAL FLOOR(1 + (RAND() * 20)) DAY AS add_time,
    NOW() - INTERVAL FLOOR(1 + (RAND() * 20)) DAY AS update_time,
    0 AS deleted
FROM 
    litemall_goods g
WHERE 
    g.deleted = 0 
    AND g.id BETWEEN 1 AND 50  -- 只为前50个商品添加
LIMIT 30;  -- 添加30条好评

-- 6. 添加包含丰富关键词的评论（优化词云图）
-- =============================================
INSERT INTO litemall_comment (value_id, type, content, admin_content, user_id, has_picture, pic_urls, star, add_time, update_time, deleted)
SELECT 
    g.id AS value_id,
    0 AS type,
    ELT(FLOOR(1 + (RAND() * 6)),
        '质量非常好，做工精细，材质优良，包装精美，物流很快，服务周到，五星好评！',
        '性价比超高，外观漂亮，颜色好看，款式新颖，使用舒适，强烈推荐购买！',
        '商品质量很好，功能齐全，设计合理，使用方便，物流给力，包装严实！',
        '材质很好，做工精细，细节到位，颜色正，尺寸合适，很满意这次购物！',
        '超级满意的商品，质量有保障，外观时尚，功能实用，物流很快，包装完好！',
        '非常不错的购物体验，商品符合描述，质量很好，服务贴心，物流快捷！'
    ) AS content,
    NULL AS admin_content,
    FLOOR(251 + (RAND() * 50)) AS user_id,  -- 用户ID 251-300
    CASE WHEN RAND() < 0.5 THEN 1 ELSE 0 END AS has_picture,
    CASE WHEN RAND() < 0.5 THEN '["http://example.com/pic3.jpg"]' ELSE NULL END AS pic_urls,
    5 AS star,  -- 全部为5星好评
    NOW() - INTERVAL FLOOR(1 + (RAND() * 25)) DAY AS add_time,
    NOW() - INTERVAL FLOOR(1 + (RAND() * 25)) DAY AS update_time,
    0 AS deleted
FROM 
    litemall_goods g
WHERE 
    g.deleted = 0 
    AND g.id BETWEEN 1 AND 30  -- 只为前30个商品添加
LIMIT 20;  -- 添加20条包含丰富关键词的评论

-- 7. 最终数据统计和验证
-- =============================================
SELECT '=== 评论数据优化完成概况 ===' AS info;

-- 评论总数统计
SELECT 
    COUNT(*) as total_comments,
    COUNT(DISTINCT value_id) as goods_with_comments,
    ROUND(AVG(star), 2) as avg_rating
FROM litemall_comment 
WHERE type = 0 AND deleted = 0;

-- 优化后的评分分布
SELECT '=== 优化后的评分分布 ===' AS info;
SELECT 
    star AS rating_level,
    COUNT(*) AS comment_count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM litemall_comment WHERE type = 0 AND deleted = 0), 2) AS percentage
FROM litemall_comment 
WHERE type = 0 AND deleted = 0
GROUP BY star
ORDER BY star DESC;

-- 商品评论覆盖率
SELECT '=== 商品评论覆盖率 ===' AS info;
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
SELECT '=== 评论优化效果总结 ===' AS info;
SELECT 
    COUNT(*) as total_comments,
    COUNT(DISTINCT value_id) as goods_with_comments,
    ROUND(AVG(star), 2) as avg_rating,
    COUNT(CASE WHEN star >= 4 THEN 1 END) as good_ratings,
    ROUND(COUNT(CASE WHEN star >= 4 THEN 1 END) * 100.0 / COUNT(*), 2) as good_rating_percentage
FROM litemall_comment 
WHERE type = 0 AND deleted = 0;