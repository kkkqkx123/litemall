-- 商品评论数据补充SQL脚本
-- 为确保词云图功能正常展示，为商品添加一些真实有效的评论数据

-- 插入测试评论数据
INSERT INTO litemall_comment (value_id, type, content, user_id, has_picture, star, add_time, update_time, deleted)
VALUES
-- 为热销商品添加多条评论
(1, 0, '质量非常好，做工精细，包装也很严实，很满意这次的购买，下次还会再来的！', 1, 0, 5, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 10 DAY, 0),
(1, 0, '商品与描述一致，材质不错，使用起来很方便，价格也很实惠，值得推荐给朋友。', 2, 0, 4, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 8 DAY, 0),
(1, 0, '收到货后很惊喜，超出预期，颜色好看，款式新颖，物流也很快，客服态度很好。', 3, 1, 5, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY, 0),

-- 为不同类别商品添加评论
(2, 0, '整体来说还可以，就是有点小瑕疵，希望商家能够改进，不过价格在那里，性价比还是不错的。', 4, 0, 3, NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 15 DAY, 0),
(2, 0, '第二次购买了，质量稳定，使用体验很好，会继续支持的！', 5, 0, 5, NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 7 DAY, 0),

(3, 0, '商品外观漂亮，功能齐全，操作简单，适合各种场景使用，非常满意。', 6, 0, 5, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY, 0),
(3, 0, '价格便宜，但是质量一般，希望能够提高一些，不过作为入门级产品还是可以的。', 7, 0, 3, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY, 0),

(4, 0, '物流很快，包装完好，商品质量很好，使用效果超出预期，五星好评！', 8, 1, 5, NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 12 DAY, 0),
(4, 0, '商品不错，但是售后服务需要提升，希望能够改进，其他方面都很好。', 9, 0, 4, NOW() - INTERVAL 9 DAY, NOW() - INTERVAL 9 DAY, 0),

(5, 0, '材质很好，做工精细，细节处理到位，很喜欢这个设计，会推荐给朋友的。', 10, 0, 5, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY, 0),
(5, 0, '商品符合描述，就是稍微有点贵，不过一分钱一分货，质量还是有保障的。', 11, 0, 4, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY, 0),

-- 为更多商品添加评论，确保各类别都有数据
(6, 0, '性价比很高，功能实用，操作简单，适合家庭使用，很满意这次购物。', 12, 0, 5, NOW() - INTERVAL 14 DAY, NOW() - INTERVAL 14 DAY, 0),
(7, 0, '商品质量不错，物流也很快，包装很用心，客服态度很好，整体体验很好。', 13, 0, 5, NOW() - INTERVAL 11 DAY, NOW() - INTERVAL 11 DAY, 0),
(8, 0, '收到货后很失望，质量不如预期，与描述有差距，希望商家能够改进。', 14, 0, 2, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY, 0),

-- 生成一些含有特定高频词汇的评论，以便词云图展示效果更好
(9, 0, '非常好的商品，质量满意，服务周到，物流快捷，强烈推荐大家购买！', 15, 0, 5, NOW() - INTERVAL 13 DAY, NOW() - INTERVAL 13 DAY, 0),
(9, 0, '质量很好，做工精细，材质优良，设计合理，使用方便，性价比高。', 16, 0, 5, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 10 DAY, 0),
(10, 0, '不错的商品，外观时尚，功能强大，价格合理，值得购买。', 17, 0, 4, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 8 DAY, 0),
(10, 0, '很喜欢这个商品，颜色好看，款式新颖，使用舒适，物流很快。', 18, 0, 5, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY, 0);

-- 批量为更多商品添加随机评论的SQL（可根据实际需要调整）
INSERT INTO litemall_comment (value_id, type, content, user_id, has_picture, star, add_time, update_time, deleted)
SELECT 
    id AS value_id,
    0 AS type,
    -- 随机选择评论内容模板
    ELT(FLOOR(1 + (RAND() * 8)),
        '商品质量很好，与描述一致，物流很快，很满意。',
        '包装严实，做工精细，价格合理，值得购买。',
        '使用体验不错，功能齐全，操作简单，推荐。',
        '性价比很高，质量有保障，会再次购买。',
        '颜色好看，款式新颖，材质不错，物流迅速。',
        '商品符合预期，服务态度好，物流给力，好评！',
        '产品质量稳定，使用方便，价格实惠，满意。',
        '收到货很惊喜，超出预期，强烈推荐购买。'
    ) AS content,
    -- 随机用户ID（假设系统中有100个用户）
    FLOOR(1 + (RAND() * 100)) AS user_id,
    -- 20%概率有图片
    CASE WHEN RAND() < 0.2 THEN 1 ELSE 0 END AS has_picture,
    -- 随机评分（3-5分）
    FLOOR(3 + (RAND() * 3)) AS star,
    -- 随机时间（过去30天内）
    NOW() - INTERVAL FLOOR(1 + (RAND() * 30)) DAY AS add_time,
    NOW() - INTERVAL FLOOR(1 + (RAND() * 30)) DAY AS update_time,
    0 AS deleted
FROM 
    litemall_goods
WHERE 
    id > 10 AND id <= 50
    -- 只为每个商品生成一条评论
    AND NOT EXISTS (SELECT 1 FROM litemall_comment WHERE value_id = litemall_goods.id AND type = 0)
LIMIT 40; -- 限制生成40条评论

-- 查询生成的评论数据
SELECT 
    c.id,
    g.name AS goods_name,
    c.content,
    c.star,
    c.add_time
FROM 
    litemall_comment c
JOIN 
    litemall_goods g ON c.value_id = g.id
WHERE 
    c.type = 0
ORDER BY 
    c.add_time DESC;

-- 统计每个商品的评论数量
SELECT 
    g.id,
    g.name AS goods_name,
    COUNT(c.id) AS comment_count
FROM 
    litemall_goods g
LEFT JOIN 
    litemall_comment c ON g.id = c.value_id AND c.type = 0 AND c.deleted = 0
GROUP BY 
    g.id, g.name
ORDER BY 
    comment_count DESC;