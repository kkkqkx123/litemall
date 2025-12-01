/**
 * 订单数据生成脚本
 * 用于生成测试用的订单数据
 */

// 配置参数
const config = {
    startDate: '2023-01-01',
    endDate: '2024-12-31',
    ordersPerDay: 0.3, // 每天生成订单数（约每3天1个订单）
    userCount: 50, // 用户数量范围
    goodsCount: 100, // 商品数量范围
    addressCount: 100, // 地址数量范围
    minOrderAmount: 10,
    maxOrderAmount: 1000
};

// 订单状态数组
const orderStatus = [101, 102, 103, 201, 202, 203, 301, 302, 303, 401];

// 生成随机数
function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

// 生成随机字符串
function randomString(length) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    for (let i = 0; i < length; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
}

// 格式化日期
function formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

// 生成订单编号
function generateOrderSn() {
    const timestamp = Date.now();
    const random = randomString(6);
    return `${timestamp}${random}`;
}

// 生成订单数据
function generateOrderData() {
    const start = new Date(config.startDate);
    const end = new Date(config.endDate);
    const orders = [];
    
    for (let date = new Date(start); date <= end; date.setDate(date.getDate() + 1)) {
        // 为每一天生成订单（使用概率控制）
        if (Math.random() < config.ordersPerDay) {
            // 每天最多生成1个订单
            const orderTime = new Date(date);
            orderTime.setHours(randomInt(0, 23));
            orderTime.setMinutes(randomInt(0, 59));
            orderTime.setSeconds(randomInt(0, 59));
            
            const userId = randomInt(1, config.userCount);
            const orderSn = generateOrderSn();
            const orderStatus = randomInt(101, 401);
            const consignee = `用户${userId}`;
            const mobile = `138${String(randomInt(10000000, 99999999))}`;
            const address = `测试地址${randomInt(1, 100)}号`;
            const message = `订单备注${randomInt(1, 100)}`;
            const goodsPrice = randomInt(config.minOrderAmount * 100, config.maxOrderAmount * 100) / 100;
            const freightPrice = randomInt(0, 2000) / 100;
            const couponPrice = randomInt(0, 500) / 100;
            const orderPrice = goodsPrice + freightPrice - couponPrice;
            const actualPrice = orderPrice;
            const payId = randomString(32);
            const outTradeNo = randomString(32);
            const shipSn = randomString(20);
            const shipChannel = ['顺丰', '圆通', '中通', '申通', '韵达'][randomInt(0, 4)];
            
            let payTime = null;
            let shipTime = null;
            let confirmTime = null;
            let endTime = null;
            let closeTime = null;
            let commentTime = null;
            
            // 根据订单状态设置时间
            if (orderStatus >= 201) {
                payTime = formatDate(orderTime);
            }
            if (orderStatus >= 301) {
                shipTime = formatDate(new Date(orderTime.getTime() + randomInt(1, 7) * 24 * 60 * 60 * 1000));
            }
            if (orderStatus >= 401) {
                confirmTime = formatDate(new Date(orderTime.getTime() + randomInt(7, 15) * 24 * 60 * 60 * 1000));
                endTime = formatDate(new Date(orderTime.getTime() + randomInt(15, 30) * 24 * 60 * 60 * 1000));
            }
            
            const order = {
                user_id: userId,
                order_sn: `'${orderSn}'`,
                order_status: orderStatus,
                aftersale_status: 0,
                consignee: `'${consignee}'`,
                mobile: `'${mobile}'`,
                address: `'${address}'`,
                message: `'${message}'`,
                goods_price: goodsPrice,
                freight_price: freightPrice,
                coupon_price: couponPrice,
                integral_price: 0,
                groupon_price: 0,
                order_price: orderPrice,
                actual_price: actualPrice,
                pay_id: `'${payId}'`,
                pay_time: payTime ? `'${payTime}'` : 'NULL',
                ship_sn: `'${shipSn}'`,
                ship_channel: `'${shipChannel}'`,
                ship_time: shipTime ? `'${shipTime}'` : 'NULL',
                refund_amount: 0,
                refund_type: 'NULL',
                refund_content: 'NULL',
                refund_time: 'NULL',
                confirm_time: confirmTime ? `'${confirmTime}'` : 'NULL',
                comments: randomInt(0, 5),
                end_time: endTime ? `'${endTime}'` : 'NULL',
                add_time: `'${formatDate(orderTime)}'`,
                update_time: `NOW()`,
                deleted: 0,
                aftersale_status: 0,
                integral_price: 0,
                groupon_price: 0,
                refund_amount: 0,
                refund_type: 'NULL',
                refund_content: 'NULL',
                refund_time: 'NULL'
            };
            
            orders.push(order);
        }
    }
    
    return orders;
}

// 生成订单商品数据
function generateOrderGoodsData(orders) {
    const orderGoods = [];
    
    orders.forEach((order, orderIndex) => {
        const goodsCount = randomInt(1, 5); // 每个订单1-5个商品
        
        for (let i = 0; i < goodsCount; i++) {
            const goodsId = randomInt(1, config.goodsCount);
            const goodsName = `商品${goodsId}`;
            const goodsSn = `SN${goodsId}`;
            const price = randomInt(10, 200);
            const number = randomInt(1, 3);
            const specifications = ['红色', '蓝色', '绿色', '黑色', '白色'][randomInt(0, 4)];
            const picUrl = `http://example.com/pic${goodsId}.jpg`;
            const comment = randomInt(0, 1);
            
            const orderGood = {
                order_id: orderIndex + 1, // 使用订单索引+1作为order_id，假设订单按顺序插入
                goods_id: goodsId,
                goods_name: `'${goodsName}'`,
                goods_sn: `'${goodsSn}'`,
                product_id: goodsId,
                number: number,
                price: price,
                specifications: `'${specifications}'`,
                pic_url: `'${picUrl}'`,
                comment: comment,
                add_time: order.add_time,
                update_time: `NOW()`,
                deleted: 0
            };
            
            orderGoods.push(orderGood);
        }
    });
    
    return orderGoods;
}

// 生成SQL插入语句
function generateSQLInsert(tableName, data) {
    if (data.length === 0) return '';
    
    const columns = Object.keys(data[0]).join(', ');
    const values = data.map(row => {
        return `(${Object.values(row).join(', ')})`;
    }).join(',\n    ');
    
    return `INSERT INTO ${tableName} (${columns}) VALUES\n    ${values};\n`;
}

// 主函数
function main() {
    console.log('-- 订单数据生成脚本');
    console.log('-- 生成时间:', new Date().toISOString());
    console.log('-- 开始日期:', config.startDate);
    console.log('-- 结束日期:', config.endDate);
    console.log('-- 每日订单概率:', config.ordersPerDay);
    console.log('-- 用户数量:', config.userCount);
    console.log('-- 商品数量:', config.goodsCount);
    console.log('-- 地址数量:', config.addressCount);
    console.log('\n-- 开始生成订单数据...\n');
    
    // 生成订单数据
    const orders = generateOrderData();
    console.log(`-- 生成订单数量: ${orders.length}`);
    
    // 生成订单商品数据
    const orderGoods = generateOrderGoodsData(orders);
    console.log(`-- 生成订单商品数量: ${orderGoods.length}`);
    
    // 生成SQL语句
    console.log('\n-- 订单表数据');
    console.log(generateSQLInsert('litemall_order', orders));
    
    console.log('\n-- 订单商品表数据');
    console.log(generateSQLInsert('litemall_order_goods', orderGoods));
    
    console.log('\n-- 数据生成完成');
}

// 运行主函数
main();