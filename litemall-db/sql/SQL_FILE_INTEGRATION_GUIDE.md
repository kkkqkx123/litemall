# litemall-db SQL文件整合说明

## 概述

为了提高SQL文件的管理效率，我们对`litemall-db/sql`目录中的SQL文件进行了整合优化。将功能相似的文件合并，减少文件数量，同时保持数据量大的文件独立。

## 整合后的文件结构

### 核心文件（保持独立）
这些文件包含基础的数据库结构和数据，建议保持独立：

- `litemall_schema.sql` - 数据库创建和用户权限设置
- `litemall_table.sql` - 所有表结构定义（837行）
- `litemall_data.sql` - 基础数据插入（368行）
- `order_test_data.sql` - 订单测试数据（774行，数据量大）
- `generate_order_data.js` - 订单数据生成脚本

### 整合后的文件

#### 1. 综合测试数据 (`test_data_combined.sql`)
**整合了以下文件：**
- `brand_test_data.sql` - 品牌制造商测试数据
- `collect_test_data.sql` - 用户收藏测试数据
- `footprint_test_data.sql` - 会员足迹测试数据

**功能：**
- 用户品牌关注数据
- 用户商品收藏数据
- 用户浏览足迹数据
- 完整的数据统计和验证

#### 2. 评论数据优化 (`comments_optimization_combined.sql`)
**整合了以下文件：**
- `supplement_comments.sql` - 基础评论补充
- `supplement_comments_final.sql` - 评论覆盖率优化
- `supplement_comments_improved.sql` - 改进版评论生成
- `optimize_rating_distribution.sql` - 评分分布优化

**功能：**
- 为无评论商品添加基础评论
- 优化评论覆盖率
- 改善评分分布（提高好评比例）
- 优化词云图关键词
- 完整的数据统计和验证

#### 3. 数据库优化 (`database_optimization_combined.sql`)
**整合了以下文件：**
- `qa_optimization_indexes_fixed.sql` - 索引优化
- `qa_optimization_tables.sql` - 表结构优化

**功能：**
- 商品表索引优化（支持多种查询场景）
- 库存查询优化索引
- 分类表和品牌表索引
- 问答功能相关表创建（可选）
- 评分分布优化

#### 4. 通用测试数据生成器 (`generate_test_data.js`)
**新增功能：**
- 支持生成收藏数据
- 支持生成足迹数据
- 支持生成评论数据
- 支持生成购物车数据
- 可配置参数（用户范围、商品范围、好评率等）

### 其他文件
- `create_database.sql` - 数据库创建脚本
- `insert_test_orders.sql` - 订单插入脚本
- 各种文档文件（`.md`文件）

## 使用指南

### 基础部署
1. 执行核心文件：
   ```sql
   mysql -uroot -proot < litemall_schema.sql
   mysql -uroot -proot litemall < litemall_table.sql
   mysql -uroot -proot litemall < litemall_data.sql
   ```

### 测试数据部署

#### 使用整合文件（推荐）
```sql
-- 部署综合测试数据
mysql -uroot -proot litemall < test_data_combined.sql

-- 优化评论数据
mysql -uroot -proot litemall < comments_optimization_combined.sql

-- 数据库性能优化
mysql -uroot -proot litemall < database_optimization_combined.sql

-- 订单测试数据（数据量大，单独执行）
mysql -uroot -proot litemall < order_test_data.sql
```

#### 使用JS生成器生成自定义数据
```bash
# 生成20条收藏数据
node generate_test_data.js collect 20

# 生成15条足迹数据
node generate_test_data.js footprint 15

# 生成30条评论数据
node generate_test_data.js comment 30

# 生成购物车数据
node generate_test_data.js cart 25
```

执行生成的SQL文件：
```sql
mysql -uroot -proot litemall < generated_collect_data_20.sql
```

### 验证数据

每个整合文件都包含了完整的数据统计和验证SQL，执行后会显示：
- 数据总量统计
- 按用户分组统计
- 数据详情查询
- 优化效果对比

## 优势

1. **减少文件数量**：从18个SQL文件减少到8个核心文件
2. **功能聚合**：相关功能整合在一起，便于理解和维护
3. **数据统计**：每个整合文件都包含完整的数据统计和验证
4. **灵活生成**：JS脚本支持自定义生成测试数据
5. **保持独立**：数据量大的文件保持独立，避免执行时间过长

## 注意事项

1. **执行顺序**：先执行基础文件，再执行测试数据和优化文件
2. **数据量考虑**：`order_test_data.sql`数据量较大，建议在测试环境执行
3. **参数配置**：使用JS生成器时，可根据需要修改`config`对象中的参数
4. **备份数据**：在生产环境执行前，建议先备份现有数据

## 文件大小对比

| 文件类型 | 整合前 | 整合后 | 减少比例 |
|---------|--------|--------|----------|
| 测试数据 | 3个文件，134行 | 1个文件，200+行 | 66% |
| 评论优化 | 4个文件，561行 | 1个文件，300+行 | 75% |
| 数据库优化 | 2个文件，70行 | 1个文件，200+行 | 50% |

总计减少了约65%的文件数量，同时保持了所有功能。