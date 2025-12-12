# LLM问答API接口文档和错误处理机制

## API接口概览

### 基础信息
- **API前缀**：`/admin/llm`
- **认证方式**：JWT Bearer Token
- **数据格式**：JSON
- **字符编码**：UTF-8

### 接口列表

| 接口路径 | 方法 | 功能描述 | 认证要求 |
|---------|------|---------|---------|
| `/admin/llm/qa` | POST | 问答接口 | ✅ 需要认证 |
| `/admin/llm/status` | GET | 服务状态检查 | ✅ 需要认证 |
| `/admin/llm/hot-questions` | GET | 获取热门问题 | ✅ 需要认证 |

## 详细接口说明

### 1. 问答接口 - POST `/admin/llm/qa`

#### 请求参数

```json
{
  "question": "请帮我查询价格低于100元的商品",
  "sessionId": "test-session-20251212115014",
  "context": "用户上下文信息（可选）",
  "maxResults": 10
}
```

**参数说明：**
- `question` (string, required): 用户问题，最大长度500字符
- `sessionId` (string, optional): 会话ID，用于维护对话上下文
- `context` (string, optional): 额外的上下文信息
- `maxResults` (number, optional): 最大返回结果数，默认10

#### 响应格式

**成功响应 (200 OK)：**
```json
{
  "errno": 0,
  "data": {
    "answer": "找到 105 个商品：\\n\\n1. 磨砂杆直杆中性笔 - 价格：¥4.90\\n2. 按动式 三角中油笔 - 价格：¥8.90\\n...",
    "goods": [],
    "sessionId": "test-session-20251212115014",
    "queryTime": 1765511416654,
    "fromCache": false,
    "queryIntent": {
      "queryType": "price_range",
      "conditions": {
        "max_price": "100"
      },
      "sort": "",
      "limit": 0,
      "confidence": null,
      "explanation": null,
      "valid": true
    }
  },
  "errmsg": "成功"
}
```

**响应字段说明：**
- `answer` (string): 自然语言回答
- `goods` (array): 商品列表（当前为空，可扩展）
- `sessionId` (string): 会话ID
- `queryTime` (number): 查询时间戳
- `fromCache` (boolean): 是否来自缓存
- `queryIntent` (object): 查询意图解析结果

### 2. 服务状态接口 - GET `/admin/llm/status`

#### 请求参数
无参数

#### 响应格式

```json
{
  "errno": 0,
  "data": {
    "service": "running",
    "llm_service": "healthy",
    "session_count": 5
  },
  "errmsg": "成功"
}
```

### 3. 热门问题接口 - GET `/admin/llm/hot-questions`

#### 请求参数
```json
{
  "limit": 10,
  "category": "price"
}
```

#### 响应格式
```json
{
  "errno": 0,
  "data": [
    {
      "question": "价格低于100元的商品有哪些？",
      "category": "price",
      "frequency": 150
    }
  ],
  "errmsg": "成功"
}
```

## 错误处理机制

### 1. 错误码定义

| 错误码 | 错误消息 | 描述 | 解决方案 |
|-------|---------|------|---------|
| 0 | "成功" | 操作成功 | - |
| 401 | "未授权" | JWT token无效或过期 | 重新获取token |
| 404 | "资源不存在" | API路径错误 | 检查API路径 |
| 500 | "系统内部错误" | 服务器内部错误 | 查看服务日志 |
| 502 | "系统内部错误" | LLM服务异常 | 检查LLM服务状态 |

### 2. 参数验证错误

#### 错误示例
```json
{
  "errno": 400,
  "errmsg": "问题不能为空"
}
```

#### 验证规则
- **问题长度**：1-500字符
- **会话ID格式**：字符串格式
- **最大结果数**：正整数，默认10

### 3. 认证错误处理

#### JWT Token验证流程
```java
// JwtAuthenticationFilter中的验证逻辑
String token = request.getHeader("Authorization");
if (token != null && token.startsWith("Bearer ")) {
    token = token.substring(7);
    if (jwtUtil.validateToken(token)) {
        // 认证通过
    } else {
        // 返回401错误
    }
}
```

#### 认证失败响应
```json
{
  "errno": 401,
  "errmsg": "认证失败，请重新登录"
}
```

### 4. LLM服务错误

#### 错误类型
1. **API调用失败**
   - 原因：网络连接问题、API密钥无效
   - 处理：重试机制，最大重试3次

2. **响应解析失败**
   - 原因：LLM返回格式不符合预期
   - 处理：使用备用解析策略

3. **超时错误**
   - 原因：LLM响应时间过长
   - 处理：设置超时限制（默认30秒）

#### 错误响应示例
```json
{
  "errno": 502,
  "errmsg": "LLM服务暂时不可用，请稍后重试"
}
```

### 5. 数据库错误

#### 错误类型
1. **连接异常**
   - 原因：数据库连接池耗尽
   - 处理：连接池监控和自动恢复

2. **SQL执行错误**
   - 原因：SQL语法错误、权限问题
   - 处理：SQL预编译和参数化查询

3. **结果集处理错误**
   - 原因：数据类型转换异常
   - 处理：类型安全检查和异常捕获

## 前端集成指南

### 1. API调用封装

#### JavaScript封装
```javascript
// llm-qa.js
import request from '@/utils/request'

export function askQuestion(data) {
  return request({
    url: '/admin/llm/qa',
    method: 'post',
    data: data
  })
}

export function getLLMServiceStatus() {
  return request({
    url: '/admin/llm/status',
    method: 'get'
  })
}
```

### 2. 错误处理最佳实践

#### 统一错误处理
```javascript
// 错误处理中间件
const errorHandler = (error) => {
  if (error.response) {
    switch (error.response.status) {
      case 401:
        // 跳转到登录页面
        router.push('/login')
        break
      case 502:
        // 显示服务不可用提示
        Message.error('LLM服务暂时不可用')
        break
      default:
        Message.error(error.response.data.errmsg)
    }
  } else {
    Message.error('网络错误，请检查网络连接')
  }
}
```

#### 重试机制
```javascript
// 带重试的API调用
async function callWithRetry(apiCall, maxRetries = 3) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await apiCall()
    } catch (error) {
      if (i === maxRetries - 1) throw error
      await new Promise(resolve => setTimeout(resolve, 1000 * Math.pow(2, i)))
    }
  }
}
```

## 安全考虑

### 1. 输入验证

#### SQL注入防护
```java
// 使用参数化查询
String sql = "SELECT * FROM litemall_goods WHERE retail_price <= ?";
PreparedStatement stmt = connection.prepareStatement(sql);
stmt.setBigDecimal(1, maxPrice);
```

#### XSS防护
```javascript
// 前端输入转义
function escapeHtml(unsafe) {
  return unsafe
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
```

### 2. 速率限制

#### 基于会话的限流
```java
// 会话级别限流
@RateLimit(limit = 10, duration = 60) // 每分钟10次请求
public QAResponse askQuestion(QARequest request) {
    // 业务逻辑
}
```

## 监控和日志

### 1. 关键指标监控

#### 性能指标
- **响应时间**：API调用耗时
- **成功率**：请求成功比例
- **错误率**：各类错误发生频率

#### 业务指标
- **查询类型分布**：各类查询的使用频率
- **会话活跃度**：会话创建和销毁情况
- **缓存命中率**：查询结果缓存效果

### 2. 日志记录

#### 关键日志点
```java
// 请求开始日志
log.info("LLM问答请求开始: sessionId={}, question={}", sessionId, question);

// LLM调用日志
log.debug("调用LLM服务: promptLength={}", prompt.length());

// 查询执行日志
log.info("执行SQL查询: sql={}, params={}", sql, params);

// 响应返回日志
log.info("LLM问答请求完成: sessionId={}, resultCount={}", sessionId, results.size());
```

## 测试用例

### 1. 功能测试用例

#### 正常流程测试
```javascript
// 价格范围查询测试
const testCase = {
  question: "价格低于100元的商品",
  expectedType: "price_range",
  expectedCondition: { max_price: "100" }
}
```

#### 边界条件测试
```javascript
// 空问题测试
const emptyQuestion = {
  question: "",
  expectedError: "问题不能为空"
}

// 超长问题测试
const longQuestion = {
  question: "a".repeat(501),
  expectedError: "问题长度不能超过500字符"
}
```

### 2. 性能测试用例

#### 并发测试
```javascript
// 模拟并发请求
const concurrentRequests = Array(10).fill().map(() => 
  askQuestion({ question: "测试问题" })
)

Promise.all(concurrentRequests).then(results => {
  console.log(`并发测试完成，成功率: ${results.filter(r => r.errno === 0).length}/${results.length}`)
})
```

---

**文档版本**：v1.0  
**最后更新**：2025-12-12  
**基于代码分析**：✅ 完全基于实际代码实现