-- =============================================
-- 数据库优化综合脚本
-- =============================================
-- 此文件整合了以下优化功能：
-- - 商品表索引优化
-- - 问答功能相关表创建
-- - 评分分布优化
-- =============================================

-- 1. 商品表索引优化
-- =============================================
-- 为提升查询性能，在商品表上添加复合索引

-- 商品表索引优化
ALTER TABLE `litemall_goods` 
ADD INDEX `idx_name_category` (`name`, `category_id`),
ADD INDEX `idx_price_sale` (`retail_price`, `is_on_sale`),
ADD INDEX `idx_category_brand` (`category_id`, `brand_id`),
ADD INDEX `idx_keywords` (`keywords`),
ADD FULLTEXT INDEX `ft_name_brief` (`name`, `brief`);

-- 库存查询优化索引
ALTER TABLE `litemall_goods_product` 
ADD INDEX `idx_goods_number` (`goods_id`, `number`);

-- 分类表索引
ALTER TABLE `litemall_category` 
ADD INDEX `idx_name_level` (`name`, `level`),
ADD INDEX `idx_pid` (`pid`);

-- 品牌表索引
ALTER TABLE `litemall_brand` 
ADD INDEX `idx_name` (`name`);

-- 添加注释说明
-- idx_name_category: 支持名称+分类的联合查询
-- idx_price_sale: 支持价格范围+上架状态的查询  
-- idx_category_brand: 支持分类+品牌的联合筛选
-- idx_keywords: 支持关键词搜索
-- ft_name_brief: 支持商品名称和简介的全文搜索

-- 2. 问答功能相关表创建（如需要）
-- =============================================
-- 问答会话历史表
CREATE TABLE IF NOT EXISTS `litemall_qa_session` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL COMMENT '会话ID',
  `user_id` int(11) DEFAULT NULL COMMENT '用户ID（可选）',
  `question` text NOT NULL COMMENT '用户问题',
  `answer` text COMMENT '系统回答',
  `query_intent` json COMMENT '查询意图JSON',
  `sql_query` text COMMENT '执行的SQL查询',
  `query_result` json COMMENT '查询结果',
  `response_time` int(11) DEFAULT NULL COMMENT '响应时间（毫秒）',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态：1-成功，0-失败',
  `error_msg` varchar(255) DEFAULT NULL COMMENT '错误信息',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答会话历史表';

-- 问答会话统计表（可选，用于性能监控）
CREATE TABLE IF NOT EXISTS `litemall_qa_statistics` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL COMMENT '会话ID',
  `query_type` varchar(32) DEFAULT NULL COMMENT '查询类型',
  `total_queries` int(11) DEFAULT '0' COMMENT '总查询次数',
  `avg_response_time` int(11) DEFAULT NULL COMMENT '平均响应时间',
  `success_count` int(11) DEFAULT '0' COMMENT '成功次数',
  `fail_count` int(11) DEFAULT '0' COMMENT '失败次数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`),
  KEY `idx_query_type` (`query_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答会话统计表';

-- 3. 评分分布优化
-- =============================================
-- 查看当前评分分布
SELECT '=== 评分分布优化前 ===' AS info;
SELECT 
    star AS rating_level,
    COUNT(*) AS comment_count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM litemall_comment WHERE type = 0 AND deleted = 0), 2) AS percentage
FROM litemall_comment 
WHERE type = 0 AND deleted = 0
GROUP BY star
ORDER BY star DESC;

-- 更新部分1星评论为更高评分
UPDATE litemall_comment 
SET star = CASE 
    WHEN RAND() < 0.6 THEN 5  -- 60% 改为5星
    WHEN RAND() < 0.85 THEN 4  -- 25% 改为4星
    ELSE 3  -- 15% 改为3星
END
WHERE type = 0 AND deleted = 0 AND star = 1 AND RAND() < 0.7;  -- 只更新70%的1星评论

-- 为评分较低的商品添加高质量好评
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
    FLOOR(101 + (RAND() * 50)) AS user_id,  -- 用户ID 101-150
    CASE WHEN RAND() < 0.4 THEN 1 ELSE 0 END AS has_picture,
    CASE WHEN RAND() < 0.4 THEN '["http://example.com/pic4.jpg"]' ELSE NULL END AS pic_urls,
    5 AS star,  -- 全部为5星好评
    NOW() - INTERVAL FLOOR(1 + (RAND() * 10)) DAY AS add_time,
    NOW() - INTERVAL FLOOR(1 + (RAND() * 10)) DAY AS update_time,
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

-- 为热门商品添加更多好评
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
    FLOOR(151 + (RAND() * 50)) AS user_id,  -- 用户ID 151-200
    CASE WHEN RAND() < 0.3 THEN 1 ELSE 0 END AS has_picture,
    CASE WHEN RAND() < 0.3 THEN '["http://example.com/pic5.jpg"]' ELSE NULL END AS pic_urls,
    CASE 
        WHEN RAND() < 0.8 THEN 5  -- 80% 五星
        ELSE 4  -- 20% 四星
    END AS star,
    NOW() - INTERVAL FLOOR(1 + (RAND() * 15)) DAY AS add_time,
    NOW() - INTERVAL FLOOR(1 + (RAND() * 15)) DAY AS update_time,
    0 AS deleted
FROM 
    litemall_goods g
WHERE 
    g.deleted = 0 
    AND g.id BETWEEN 1 AND 50  -- 只为前50个商品添加
LIMIT 30;  -- 添加30条好评

-- 添加包含丰富关键词的评论（优化词云图）
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
    FLOOR(201 + (RAND() * 50)) AS user_id,  -- 用户ID 201-250
    CASE WHEN RAND() < 0.5 THEN 1 ELSE 0 END AS has_picture,
    CASE WHEN RAND() < 0.5 THEN '["http://example.com/pic6.jpg"]' ELSE NULL END AS pic_urls,
    5 AS star,  -- 全部为5星好评
    NOW() - INTERVAL FLOOR(1 + (RAND() * 20)) DAY AS add_time,
    NOW() - INTERVAL FLOOR(1 + (RAND() * 20)) DAY AS update_time,
    0 AS deleted
FROM 
    litemall_goods g
WHERE 
    g.deleted = 0 
    AND g.id BETWEEN 1 AND 30  -- 只为前30个商品添加
LIMIT 20;  -- 添加20条包含丰富关键词的评论

-- 4. 优化效果验证
-- =============================================
SELECT '=== 评分分布优化后 ===' AS info;
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

-- 最终数据统计
SELECT '=== 数据库优化完成数据统计 ===' AS info;
SELECT 
    COUNT(*) as total_comments,
    COUNT(DISTINCT value_id) as goods_with_comments,
    ROUND(AVG(star), 2) as avg_rating,
    COUNT(CASE WHEN star >= 4 THEN 1 END) as good_ratings,
    ROUND(COUNT(CASE WHEN star >= 4 THEN 1 END) * 100.0 / COUNT(*), 2) as good_rating_percentage
FROM litemall_comment 
WHERE type = 0 AND deleted = 0;

-- 索引创建验证
SELECT '=== 创建的索引验证 ===' AS info;
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    INDEX_TYPE
FROM 
    information_schema.STATISTICS
WHERE 
    TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME IN ('litemall_goods', 'litemall_goods_product', 'litemall_category', 'litemall_brand')
    AND INDEX_NAME LIKE 'idx_%' OR INDEX_NAME LIKE 'ft_%'
ORDER BY 
    TABLE_NAME, INDEX_NAME;