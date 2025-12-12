# LLM问答功能完整指南

## 文档索引

本指南包含LLM问答功能的完整文档，分为以下几个部分：

### 1. 验证结果和使用指南
- **文件**：`llm-qa-validation-guide.md`
- **内容**：功能验证状态、测试记录、使用指南、错误处理
- **重点**：实际验证结果和操作指南

### 2. 处理逻辑和功能支持
- **文件**：`llm-qa-processing-logic.md`
- **内容**：系统架构、核心处理流程、功能支持说明
- **重点**：技术实现细节和功能扩展性

### 3. API接口和错误处理
- **文件**：`llm-qa-api-error-handling.md`
- **内容**：API接口文档、错误处理机制、安全考虑
- **重点**：接口规范和错误处理最佳实践

## 功能概述

LLM问答功能是一个基于大语言模型的商品查询系统，具有以下特点：

### 核心能力
- ✅ **自然语言理解**：将用户问题转换为结构化查询
- ✅ **多类型查询**：支持价格范围、关键词搜索、分类筛选等
- ✅ **智能映射**：自动将自然语言字段映射为数据库字段
- ✅ **会话管理**：支持多轮对话和上下文维护

### 技术栈
- **后端框架**：Spring Boot 3.5.6
- **LLM服务**：Qwen/Qwen3-32B（通过ModelScope API）
- **数据库**：MySQL + MyBatis
- **前端框架**：Vue.js + Element UI

## 验证结果总结

### 功能验证状态
- ✅ **JWT认证**：正常工作
- ✅ **API路径**：正确配置为`/admin/llm/qa`
- ✅ **字段映射**：price→retail_price映射已修复
- ✅ **查询执行**：成功执行SQL查询并返回结果
- ✅ **错误处理**：完善的错误处理机制

### 测试验证示例
```json
{
  "question": "价格低于100元的商品",
  "result": "找到105个商品",
  "queryType": "price_range",
  "conditions": {"max_price": "100"}
}
```

## 快速开始

### 1. 获取JWT Token
```bash
curl -X POST "http://localhost:8080/admin/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin123","password":"admin123"}'
```

### 2. 调用问答接口
```bash
curl -X POST "http://localhost:8080/admin/llm/qa" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"question": "价格低于100元的商品"}'
```

### 3. 检查服务状态
```bash
curl -X GET "http://localhost:8080/admin/llm/status" \
  -H "Authorization: Bearer $TOKEN"
```

## 支持的查询类型

| 查询类型 | 描述 | 示例问题 |
|---------|------|---------|
| price_range | 价格范围查询 | "价格低于100元的商品" |
| keyword_search | 关键词搜索 | "包含'手机'的商品" |
| category_filter | 分类筛选 | "电子产品分类的商品" |
| statistical | 统计查询 | "商品总数是多少" |

## 错误代码参考

| 错误码 | 含义 | 解决方案 |
|-------|------|---------|
| 0 | 成功 | - |
| 401 | 未授权 | 重新获取token |
| 404 | 资源不存在 | 检查API路径 |
| 500 | 系统内部错误 | 查看服务日志 |
| 502 | LLM服务异常 | 检查LLM服务状态 |

## 性能指标

- **响应时间**：2-3秒（包含LLM调用）
- **并发支持**：支持多会话并发
- **结果限制**：默认显示前5个商品
- **会话管理**：支持会话创建和销毁

## 开发指南

### 前端集成
```javascript
import { askQuestion } from '@/api/llm-qa'

const response = await askQuestion({
  question: '你的问题',
  sessionId: 'your-session-id'
})
```

### 后端扩展
- 添加新的查询类型：扩展`QueryIntent.queryType`
- 自定义字段映射：修改`SQLBuilder.mapSortField`
- 添加新的LLM服务：实现`LLMService`接口

## 监控和维护

### 关键日志位置
```
litemall-all/logs/log.log
```

### 服务状态检查
- Spring Boot应用状态：`/admin/llm/status`
- 数据库连接状态：检查连接池
- LLM服务状态：API健康检查

## 故障排除

### 常见问题
1. **401错误**：检查JWT token是否过期
2. **404错误**：确认API路径为`/admin/llm/qa`
3. **502错误**：检查LLM服务是否可用
4. **查询无结果**：确认数据库中有匹配的商品

### 日志分析
查看应用日志中的关键信息：
- JWT认证状态
- LLM调用记录
- SQL查询执行
- 错误堆栈信息

## 版本信息

- **文档版本**：v1.0
- **最后更新**：2025-12-12
- **基于代码版本**：litemall最新版本
- **验证状态**：✅ 完全通过

---

**相关文档**：
- [验证结果和使用指南](./llm-qa-validation-guide.md)
- [处理逻辑和功能支持](./llm-qa-processing-logic.md)  
- [API接口和错误处理](./llm-qa-api-error-handling.md)

**技术支持**：如有问题，请查看相关代码文件或联系开发团队。