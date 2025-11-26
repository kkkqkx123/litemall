-- 商品表索引优化脚本
-- 为提升问答查询性能，在商品表上添加复合索引

-- 商品表索引优化
ALTER TABLE `litemall_goods` 
ADD INDEX IF NOT EXISTS `idx_name_category` (`name`, `category_id`),
ADD INDEX IF NOT EXISTS `idx_price_sale` (`retail_price`, `is_on_sale`),
ADD INDEX IF NOT EXISTS `idx_category_brand` (`category_id`, `brand_id`),
ADD INDEX IF NOT EXISTS `idx_keywords` (`keywords`),
ADD FULLTEXT INDEX IF NOT EXISTS `ft_name_brief` (`name`, `brief`);

-- 库存查询优化索引
ALTER TABLE `litemall_goods_product` 
ADD INDEX IF NOT EXISTS `idx_goods_number` (`goods_id`, `number`);

-- 分类表索引
ALTER TABLE `litemall_category` 
ADD INDEX IF NOT EXISTS `idx_name_level` (`name`, `level`),
ADD INDEX IF NOT EXISTS `idx_pid` (`pid`);

-- 品牌表索引
ALTER TABLE `litemall_brand` 
ADD INDEX IF NOT EXISTS `idx_name` (`name`);

-- 添加注释说明
-- idx_name_category: 支持名称+分类的联合查询
-- idx_price_sale: 支持价格范围+上架状态的查询  
-- idx_category_brand: 支持分类+品牌的联合筛选
-- idx_keywords: 支持关键词搜索
-- ft_name_brief: 支持商品名称和简介的全文搜索