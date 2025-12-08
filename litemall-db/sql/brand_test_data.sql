-- 品牌制造商测试数据
-- 用于补充品牌制造商页面展示的数据

-- 插入额外的品牌制造商数据
INSERT INTO litemall_brand (id, name, `desc`, pic_url, sort_order, floor_price, add_time, update_time, deleted) VALUES
(1002000, 'Nike制造商', '严选找到为Nike代工的优质制造商，\n采用相同的技术和材料标准，\n为你提供高性价比的运动装备。', 'https://yanxuan.nosdn.127.net/14967388191253338.png', 10, 99.00, NOW(), NOW(), 0),
(1002001, 'Apple制造商', '严选精选Apple供应链制造商，\n采用相同的工艺和品质标准，\n让科技生活更加触手可及。', 'https://yanxuan.nosdn.127.net/14967388191253339.png', 20, 199.00, NOW(), NOW(), 0),
(1002002, '无印良品制造商', '严选找到为无印良品代工的制造商，\n追求简约设计和实用功能，\n为你打造舒适的生活空间。', 'https://yanxuan.nosdn.127.net/14967388191253340.png', 30, 59.00, NOW(), NOW(), 0),
(1002003, '宜家制造商', '严选精选宜家供应链制造商，\n注重功能性和性价比，\n让好设计成为生活的日常。', 'https://yanxuan.nosdn.127.net/14967388191253341.png', 40, 79.00, NOW(), NOW(), 0),
(1002004, 'Zara制造商', '严选找到为Zara代工的服装制造商，\n紧跟时尚潮流，快速响应，\n让你轻松拥有时尚穿搭。', 'https://yanxuan.nosdn.127.net/14967388191253342.png', 50, 89.00, NOW(), NOW(), 0),
(1002005, 'H&M制造商', '严选精选H&M供应链制造商，\n注重可持续时尚和环保材料，\n为地球和未来做出选择。', 'https://yanxuan.nosdn.127.net/14967388191253343.png', 60, 69.00, NOW(), NOW(), 0),
(1002006, '星巴克制造商', '严选找到为星巴克提供产品的制造商，\n精选优质原料和精湛工艺，\n让品质生活从一杯咖啡开始。', 'https://yanxuan.nosdn.127.net/14967388191253344.png', 70, 39.00, NOW(), NOW(), 0),
(1002007, '乐高制造商', '严选精选乐高玩具制造商，\n采用安全环保材料和精密模具，\n让创意和乐趣陪伴成长。', 'https://yanxuan.nosdn.127.net/14967388191253345.png', 80, 149.00, NOW(), NOW(), 0),
(1002008, '索尼制造商', '严选找到为索尼代工的电子制造商，\n采用先进技术和严格品控，\n为你带来优质的影音体验。', 'https://yanxuan.nosdn.127.net/14967388191253346.png', 90, 299.00, NOW(), NOW(), 0),
(1002009, '宝马制造商', '严选精选宝马汽车配件制造商，\n遵循德国工业标准和精密工艺，\n让品质驾驭每一次出行。', 'https://yanxuan.nosdn.127.net/14967388191253347.png', 100, 399.00, NOW(), NOW(), 0);

-- 验证插入的数据
SELECT '=== 新插入的品牌制造商数据 ===' AS info;
SELECT id, name, `desc`, floor_price FROM litemall_brand WHERE id >= 1002000 AND deleted = 0;

SELECT '=== 品牌制造商总数统计 ===' AS info;
SELECT COUNT(*) as total_brands FROM litemall_brand WHERE deleted = 0;

SELECT '=== 价格区间统计 ===' AS info;
SELECT 
    CASE 
        WHEN floor_price < 50 THEN '0-50元'
        WHEN floor_price < 100 THEN '50-100元'
        WHEN floor_price < 200 THEN '100-200元'
        WHEN floor_price < 300 THEN '200-300元'
        ELSE '300元以上'
    END as price_range,
    COUNT(*) as brand_count
FROM litemall_brand 
WHERE deleted = 0 
GROUP BY price_range 
ORDER BY MIN(floor_price);