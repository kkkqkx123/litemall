/**
 * 通用测试数据生成脚本
 * 用于生成litemall商城系统的各类测试数据
 * 
 * 使用方法:
 * node generate_test_data.js [数据类型] [数量]
 * 
 * 参数说明:
 * 数据类型: collect(收藏), footprint(足迹), comment(评论), cart(购物车)
 * 数量: 要生成的数据条数，默认为10
 * 
 * 示例:
 * node generate_test_data.js collect 20    # 生成20条收藏数据
 * node generate_test_data.js footprint 15  # 生成15条足迹数据
 * node generate_test_data.js comment 30    # 生成30条评论数据
 */

const fs = require('fs');
const path = require('path');

// 配置参数
const config = {
    // 用户ID范围
    userIdRange: { min: 3, max: 10 },
    
    // 商品ID范围
    goodsIdRange: { min: 1023001, max: 1186041 },
    
    // 品牌ID范围
    brandIdRange: { min: 1001001, max: 1001020 },
    
    // 评分范围
    ratingRange: { min: 3, max: 5 },
    
    // 日期范围（天数）
    dateRange: { min: 1, max: 30 },
    
    // 输出目录
    outputDir: __dirname,
    
    // 是否包含图片
    includePicture: 0.3, // 30%的概率包含图片
    
    // 好评率
    goodRatingRate: 0.8 // 80%的好评率
};

// 评论内容模板
const commentTemplates = {
    positive: [
        '这个商品质量很好，做工精细，值得推荐！',
        '性价比很高，包装精美，物流很快，满意！',
        '商品符合描述，质量不错，服务态度好！',
        '很满意的购物，质量很好，五星好评！',
        '超级喜欢的商品，质量有保障，强烈推荐！',
        '非常不错的购物体验，商品质量很好！',
        '质量很好，材质不错，外观漂亮，功能齐全！',
        '颜色好看，款式新颖，使用舒适，质量有保障！'
    ],
    neutral: [
        '商品还可以，质量一般，价格合适。',
        '整体感觉还行，符合预期，可以用。',
        '收到货了，包装一般，商品质量还可以。',
        '性价比一般，质量中等，能用。',
        '商品质量还行，没有特别惊喜。'
    ],
    keywords: [
        '质量非常好，做工精细，材质优良，包装精美，物流很快，服务周到，五星好评！',
        '性价比超高，外观漂亮，颜色好看，款式新颖，使用舒适，强烈推荐购买！',
        '商品质量很好，功能齐全，设计合理，使用方便，物流给力，包装严实！',
        '材质很好，做工精细，细节到位，颜色正，尺寸合适，很满意这次购物！',
        '超级满意的商品，质量有保障，外观时尚，功能实用，物流很快，包装完好！',
        '非常不错的购物体验，商品符合描述，质量很好，服务贴心，物流快捷！'
    ]
};

// 生成随机数
function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomFloat(min, max) {
    return Math.random() * (max - min) + min;
}

// 生成随机日期
function randomDate(daysAgo) {
    const date = new Date();
    date.setDate(date.getDate() - randomInt(1, daysAgo));
    return date.toISOString().slice(0, 19).replace('T', ' ');
}

// 生成收藏数据
function generateCollectData(count) {
    const inserts = [];
    const statistics = { goods: 0, brand: 0 };
    
    for (let i = 0; i < count; i++) {
        const userId = randomInt(config.userIdRange.min, config.userIdRange.max);
        const type = Math.random() > 0.5 ? 0 : 1; // 0:商品收藏, 1:品牌关注
        const valueId = type === 0 ? 
            randomInt(config.goodsIdRange.min, config.goodsIdRange.max) :
            randomInt(config.brandIdRange.min, config.brandIdRange.max);
        
        const addTime = randomDate(config.dateRange.max);
        
        inserts.push(`(${userId}, ${valueId}, ${type}, '${addTime}', '${addTime}', 0)`);
        
        if (type === 0) statistics.goods++;
        else statistics.brand++;
    }
    
    const sql = `-- 生成的收藏测试数据 (${count}条)
-- 商品收藏: ${statistics.goods}条, 品牌关注: ${statistics.brand}条
-- 生成时间: ${new Date().toLocaleString()}

INSERT INTO litemall_collect (user_id, value_id, type, add_time, update_time, deleted) VALUES 
${inserts.join(',\n')};

-- 数据统计
SELECT '=== 生成的收藏数据统计 ===' AS info;
SELECT 
    '商品收藏' as collect_type,
    COUNT(*) as total_count 
FROM litemall_collect 
WHERE deleted = 0 AND type = 0;

SELECT 
    '品牌关注' as collect_type,
    COUNT(*) as total_count 
FROM litemall_collect 
WHERE deleted = 0 AND type = 1;

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
ORDER BY total_collects DESC;`;
    
    return sql;
}

// 生成足迹数据
function generateFootprintData(count) {
    const inserts = [];
    const userStats = {};
    
    for (let i = 0; i < count; i++) {
        const userId = randomInt(config.userIdRange.min, config.userIdRange.max);
        const goodsId = randomInt(config.goodsIdRange.min, config.goodsIdRange.max);
        const addTime = randomDate(config.dateRange.max);
        
        inserts.push(`(${userId}, ${goodsId}, '${addTime}', '${addTime}', 0)`);
        
        userStats[userId] = (userStats[userId] || 0) + 1;
    }
    
    const sql = `-- 生成的足迹测试数据 (${count}条)
-- 涉及用户数: ${Object.keys(userStats).length}
-- 生成时间: ${new Date().toLocaleString()}

INSERT INTO litemall_footprint (user_id, goods_id, add_time, update_time, deleted) VALUES 
${inserts.join(',\n')};

-- 数据统计
SELECT '=== 生成的足迹数据统计 ===' AS info;
SELECT COUNT(*) as total_footprints FROM litemall_footprint WHERE deleted = 0;

SELECT '=== 各用户足迹数量统计 ===' AS info;
SELECT 
    u.username, 
    COUNT(*) as footprint_count 
FROM litemall_footprint f 
JOIN litemall_user u ON f.user_id = u.id 
WHERE f.deleted = 0 
GROUP BY u.username 
ORDER BY footprint_count DESC;`;
    
    return sql;
}

// 生成评论数据
function generateCommentData(count) {
    const inserts = [];
    const ratingStats = { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 };
    
    for (let i = 0; i < count; i++) {
        const userId = randomInt(config.userIdRange.min, config.userIdRange.max);
        const goodsId = randomInt(config.goodsIdRange.min, config.goodsIdRange.max);
        
        // 确定评分
        let star;
        const rand = Math.random();
        if (rand < config.goodRatingRate) {
            star = randomInt(4, 5); // 好评
        } else {
            star = randomInt(3, 4); // 中评
        }
        
        // 确定评论内容
        let content;
        if (Math.random() < 0.3) {
            content = commentTemplates.keywords[randomInt(0, commentTemplates.keywords.length - 1)];
        } else if (star >= 4) {
            content = commentTemplates.positive[randomInt(0, commentTemplates.positive.length - 1)];
        } else {
            content = commentTemplates.neutral[randomInt(0, commentTemplates.neutral.length - 1)];
        }
        
        const hasPicture = Math.random() < config.includePicture ? 1 : 0;
        const picUrls = hasPicture ? '["http://example.com/pic1.jpg"]' : 'NULL';
        const addTime = randomDate(config.dateRange.max);
        
        inserts.push(`(${goodsId}, 0, '${content}', NULL, ${userId}, ${hasPicture}, ${picUrls}, ${star}, '${addTime}', '${addTime}', 0)`);
        
        ratingStats[star]++;
    }
    
    const sql = `-- 生成的评论测试数据 (${count}条)
-- 好评(4-5星): ${ratingStats[4] + ratingStats[5]}条, 中评(3星): ${ratingStats[3]}条
-- 生成时间: ${new Date().toLocaleString()}

INSERT INTO litemall_comment (value_id, type, content, admin_content, user_id, has_picture, pic_urls, star, add_time, update_time, deleted) VALUES 
${inserts.join(',\n')};

-- 数据统计
SELECT '=== 生成的评论数据统计 ===' AS info;
SELECT 
    COUNT(*) as total_comments,
    COUNT(DISTINCT value_id) as goods_with_comments,
    ROUND(AVG(star), 2) as avg_rating
FROM litemall_comment 
WHERE type = 0 AND deleted = 0;

SELECT '=== 评分分布统计 ===' AS info;
SELECT 
    star AS rating_level,
    COUNT(*) AS comment_count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM litemall_comment WHERE type = 0 AND deleted = 0), 2) AS percentage
FROM litemall_comment 
WHERE type = 0 AND deleted = 0
GROUP BY star
ORDER BY star DESC;`;
    
    return sql;
}

// 生成购物车数据
function generateCartData(count) {
    const inserts = [];
    
    for (let i = 0; i < count; i++) {
        const userId = randomInt(config.userIdRange.min, config.userIdRange.max);
        const goodsId = randomInt(config.goodsIdRange.min, config.goodsIdRange.max);
        const productId = goodsId + 10000; // 假设商品ID和产品ID有关联
        const number = randomInt(1, 5);
        const addTime = randomDate(config.dateRange.max);
        const checked = Math.random() > 0.3 ? 1 : 0; // 70%概率选中
        
        inserts.push(`(${userId}, ${goodsId}, ${productId}, '${number}', ${checked}, '${addTime}', '${addTime}', 0)`);
    }
    
    const sql = `-- 生成的购物车测试数据 (${count}条)
-- 生成时间: ${new Date().toLocaleString()}

INSERT INTO litemall_cart (user_id, goods_id, product_id, number, checked, add_time, update_time, deleted) VALUES 
${inserts.join(',\n')};

-- 数据统计
SELECT '=== 生成的购物车数据统计 ===' AS info;
SELECT 
    COUNT(*) as total_items,
    COUNT(DISTINCT user_id) as users_with_items,
    SUM(number) as total_goods
FROM litemall_cart 
WHERE deleted = 0;

SELECT '=== 各用户购物车统计 ===' AS info;
SELECT 
    u.username, 
    COUNT(*) as item_count,
    SUM(c.number) as total_quantity
FROM litemall_cart c 
JOIN litemall_user u ON c.user_id = u.id 
WHERE c.deleted = 0 
GROUP BY u.username 
ORDER BY item_count DESC;`;
    
    return sql;
}

// 主函数
function main() {
    const args = process.argv.slice(2);
    const dataType = args[0] || 'collect';
    const count = parseInt(args[1]) || 10;
    
    console.log(`开始生成 ${dataType} 测试数据，数量: ${count}`);
    
    let sql;
    let filename;
    
    switch (dataType.toLowerCase()) {
        case 'collect':
            sql = generateCollectData(count);
            filename = `generated_collect_data_${count}.sql`;
            break;
        case 'footprint':
            sql = generateFootprintData(count);
            filename = `generated_footprint_data_${count}.sql`;
            break;
        case 'comment':
            sql = generateCommentData(count);
            filename = `generated_comment_data_${count}.sql`;
            break;
        case 'cart':
            sql = generateCartData(count);
            filename = `generated_cart_data_${count}.sql`;
            break;
        default:
            console.error(`不支持的数据类型: ${dataType}`);
            console.log('支持的数据类型: collect, footprint, comment, cart');
            process.exit(1);
    }
    
    const outputPath = path.join(config.outputDir, filename);
    fs.writeFileSync(outputPath, sql, 'utf8');
    
    console.log(`✅ 测试数据生成完成！`);
    console.log(`📄 SQL文件已保存: ${outputPath}`);
    console.log(`📊 生成数据: ${count}条 ${dataType} 数据`);
    console.log('');
    console.log('使用说明:');
    console.log(`1. 执行SQL文件: mysql -uroot -proot litemall < ${filename}`);
    console.log('2. 或者在MySQL客户端中执行SQL文件内容');
    console.log('');
    console.log('参数说明:');
    console.log('- 用户ID范围: 3-10');
    console.log(`- 商品ID范围: ${config.goodsIdRange.min}-${config.goodsIdRange.max}`);
    console.log(`- 品牌ID范围: ${config.brandIdRange.min}-${config.brandIdRange.max}`);
    console.log(`- 日期范围: 最近${config.dateRange.max}天`);
}

// 执行主函数
if (require.main === module) {
    main();
}

module.exports = {
    generateCollectData,
    generateFootprintData,
    generateCommentData,
    generateCartData,
    config
};