# 大模型商品问答功能 - 执行方案

## 1. 项目概览

### 1.1 核心目标
实现基于大模型的智能商品问答系统，支持自然语言查询商品信息，提供精准的商品推荐和库存查询服务。

### 1.2 技术架构
- **后端**: Spring Boot + MyBatis + ModelScope API
- **前端**: Vue.js + Element UI  
- **数据库**: MySQL
- **缓存**: Redis (可选)

### 1.3 核心流程
```
用户提问 → 前端组件 → API接口 → LLMQAService → Qwen3Service → 数据库查询 → 结果返回
                      ↓
                格式化输出解析 → SQL生成 → 查询执行 → 结果过滤 → 最终回答
```

## 2. 模块设计引用

### 2.1 数据模型设计
商品问答系统的数据模型定义参考：<mcfile name="llm-qa-design.md" path="d:\项目\Spring\litemall\docs\coding-2\llm-qa-design.md"></mcfile>

主要包含以下核心类：
- `QueryIntent` - 查询意图模型
- `Goods` - 商品实体
- `GoodsQAResponse` - 问答响应
- `GoodsQARequest` - 问答请求

### 2.2 查询解析器设计
LLM输出解析和查询构建逻辑详见：<mcfile name="llm-qa-parser.md" path="d:\项目\Spring\litemall\docs\coding-2\llm-qa-parser.md"></mcfile>

核心组件包括：
- `LLMOutputParser` - LLM输出解析器
- `QueryIntentBuilder` - 查询意图构建器
- `SQLBuilder` - SQL语句构建器
- `ParameterBinder` - 参数绑定器

### 2.3 服务层设计
主要服务类的职责和设计参考：<mcfile name="llm-qa-design.md" path="d:\项目\Spring\litemall\docs\coding-2\llm-qa-design.md"></mcfile>

关键服务：
- `LLMQAService` - 核心问答服务
- `Qwen3Service` - LLM调用服务
- `GoodsQueryService` - 商品查询服务
- `SessionManager` - 会话管理服务

### 2.4 前端组件设计
前端界面和交互设计详见：<mcfile name="llm-qa-design.md" path="d:\项目\Spring\litemall\docs\coding-2\llm-qa-design.md"></mcfile>

主要组件：
- `LLMQA.vue` - 问答主组件
- `llm-qa.js` - API接口封装

## 3. 核心功能实现

### 3.1 查询类型支持
系统支持7种查询类型，具体实现参考：<mcfile name="llm-qa-design.md" path="d:\项目\Spring\litemall\docs\coding-2\llm-qa-design.md"></mcfile>

1. **价格范围查询** (`price_range`) - 按价格区间筛选商品
2. **库存查询** (`stock_check`) - 查询有库存的商品
3. **分类筛选** (`category_filter`) - 按商品分类筛选
4. **关键词搜索** (`keyword_search`) - 商品关键词搜索
5. **名称模式匹配** (`name_pattern`) - 支持6种匹配模式的名称查询
6. **特定商品查询** (`specific_product`) - 精确商品查询
7. **统计查询** (`statistical`) - 商品统计信息查询

### 3.2 名称匹配模式
名称模式匹配支持5种模式，详细实现参考：<mcfile name="llm-qa-parser.md" path="d:\项目\Spring\litemall\docs\coding-2\llm-qa-parser.md"></mcfile>

- **exact** - 精确匹配
- **contains** - 包含匹配
- **starts_with** - 前缀匹配
- **ends_with** - 后缀匹配
- **regex** - 正则表达式匹配

### 3.3 规则解析降级
当LLM解析失败时的规则降级方案，详见：<mcfile name="llm-qa-parser.md" path="d:\项目\Spring\litemall\docs\coding-2\llm-qa-parser.md"></mcfile>

支持识别的查询类型：
- 价格相关问题（如"1000元以下的手机"）
- 库存相关问题（如"有现货的商品"）
- 商品类别问题（如"手机、电脑、衣服"）
- 品牌相关问题（如"苹果手机"）

## 4. 增强功能

### 4.1 结果过滤机制
新增的结果过滤功能确保查询结果精确性，实现参考：<mcsymbol name="filterResultsByPattern" filename="llm-qa-implementation.md" path="d:\项目\Spring\litemall\docs\coding-2\llm-qa-implementation.md" startline="133" type="function"></mcsymbol>

过滤流程：
1. 获取数据库查询结果
2. 应用查询意图中的模式匹配条件
3. 对商品名称和简介进行精确过滤
4. 返回最终匹配的商品列表

### 4.2 多模式匹配
支持多个模式同时匹配，实现参考：<mcfile name="llm-qa-parser.md" path="d:\项目\Spring\litemall\docs\coding-2\llm-qa-parser.md"></mcfile>

特性包括：
- 支持多个关键词同时查询
- 可配置匹配逻辑（AND/OR）
- 支持大小写敏感配置
- 正则表达式批量匹配

### 4.3 缓存优化
可选的Redis缓存实现，详见：<mcfile name="llm-qa-design.md" path="d:\项目\Spring\litemall\docs\coding-2\llm-qa-design.md"></mcfile>

缓存策略：
- 问答结果缓存
- LLM响应缓存
- 热点问题预缓存

## 5. 部署配置

### 5.1 数据库配置
数据库表结构定义参考：<mcfile name="llm-qa-design.md" path="d:\项目\Spring\litemall\docs\coding-2\llm-qa-design.md"></mcfile>

主要数据表：
- `litemall_goods` - 商品信息表
- `litemall_qa_session` - 问答会话历史表

### 5.2 环境配置
配置文件和环境变量设置详见：<mcfile name="llm-qa-design.md" path="d:\项目\Spring\litemall\docs\coding-2\llm-qa-design.md"></mcfile>

关键配置项：
- ModelScope API密钥
- Redis连接配置
- 数据库连接配置
- 日志级别设置

### 5.3 部署步骤
详细的部署流程和命令参考：<mcfile name="llm-qa-design.md" path="d:\项目\Spring\litemall\docs\coding-2\llm-qa-design.md"></mcfile>

部署流程：
1. 后端服务编译和启动
2. 前端资源构建和部署
3. 数据库初始化和表创建
4. 服务健康检查和API测试

## 6. 监控与运维

### 6.1 健康检查
健康检查实现参考：<mcfile name="llm-qa-design.md" path="d:\项目\Spring\litemall\docs\coding-2\llm-qa-design.md"></mcfile>

监控指标：
- LLM服务连接状态
- 数据库连接状态
- 服务响应时间
- 错误率统计

### 6.2 性能监控
性能指标收集详见：<mcfile name="llm-qa-design.md" path="d:\项目\Spring\litemall\docs\coding-2\llm-qa-design.md"></mcfile>

监控维度：
- 问题处理数量
- 平均响应时间
- 活跃会话数
- LLM调用成功率

## 7. 测试验证

### 7.1 API接口测试
测试用例和验证方法参考：<mcfile name="llm-qa-design.md" path="d:\项目\Spring\litemall\docs\coding-2\llm-qa-design.md"></mcfile>

测试场景：
- 价格范围查询测试
- 库存状态查询测试
- 品牌筛选测试
- 多条件组合查询测试

### 7.2 性能测试
性能基准和优化建议详见各模块文档中的性能说明章节。

## 8. 后续扩展

### 8.1 功能扩展
- 商品推荐算法集成
- 多语言支持
- 语音输入输出
- 图像识别查询

### 8.2 技术优化
- 异步处理优化
- 分布式缓存
- 数据库读写分离
- 微服务架构升级

---

**说明**: 本执行方案通过引用具体模块文档的方式组织，避免了大量代码的直接包含。详细的代码实现、配置示例和具体逻辑请参考相应的模块文档。每个引用都指向了包含完整实现细节的文档位置，便于开发和维护人员查阅。