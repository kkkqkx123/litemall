# LLMQA模块前后端交互详细文档

## 1. 后端API端点详细格式

### 1.1 问答接口 `/admin/llm/qa/ask`

#### 请求格式
```http
POST /admin/llm/qa/ask
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>

{
  "question": "用户提出的问题",
  "sessionId": "会话ID（可选）"
}
```

#### 响应格式

**成功响应 (HTTP 200)**:
```json
{
  "errno": 0,
  "errmsg": "成功",
  "data": {
    "code": 200,
    "message": "success",
    "answer": "AI生成的回答内容",
    "goods": [
      {
        "id": 1,
        "name": "商品名称",
        "price": 19900,
        "brief": "商品简介",
        "picUrl": "商品图片URL",
        // ... 其他商品字段
      }
      // ... 更多商品
    ],
    "sessionId": "会话ID",
    "queryTime": 1500,
    "timestamp": "2023-12-01T10:30:00",
    "fromCache": false,
    "queryIntent": {
      "queryType": "price_range",
      "conditions": {
        "minPrice": 10000,
        "maxPrice": 20000
      }
    }
  }
}
```

**错误响应 (HTTP 200)**:
```json
{
  "errno": 500,
  "errmsg": "处理请求时发生错误：具体错误信息",
  "data": null
}
```

### 1.2 创建会话接口 `/admin/llm/qa/session/create`

#### 请求格式
```http
POST /admin/llm/qa/session/create
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>

{
  "title": "会话标题"
}
```

#### 响应格式
```json
{
  "errno": 0,
  "errmsg": "成功",
  "data": {
    "sessionId": "会话ID"
  }
}
```

### 1.3 获取会话历史接口 `/admin/llm/qa/session/{sessionId}/history`

#### 请求格式
```http
GET /admin/llm/qa/session/{sessionId}/history?page=1&limit=10
Authorization: Bearer <JWT_TOKEN>
```

#### 响应格式
```json
{
  "errno": 0,
  "errmsg": "成功",
  "data": {
    "sessionId": "会话ID",
    "messages": [
      {
        "role": "user",
        "content": "用户问题",
        "timestamp": "2023-12-01T10:30:00"
      },
      {
        "role": "assistant",
        "content": "AI回答",
        "timestamp": "2023-12-01T10:30:05"
      }
      // ... 更多消息
    ]
  }
}
```

### 1.4 获取服务状态接口 `/admin/llm/qa/status`

#### 请求格式
```http
GET /admin/llm/qa/status
Authorization: Bearer <JWT_TOKEN>
```

#### 响应格式
```json
{
  "errno": 0,
  "errmsg": "成功",
  "data": {
    "serviceStatus": "running",
    "activeSessions": 5,
    "totalQueries": 150,
    "cacheHits": 30,
    "averageResponseTime": 1200
  }
}
```

### 1.5 获取热门问题接口 `/admin/llm/qa/hot-questions`

#### 请求格式
```http
GET /admin/llm/qa/hot-questions?limit=5&category=general
Authorization: Bearer <JWT_TOKEN>
```

#### 响应格式
```json
{
  "errno": 0,
  "errmsg": "成功",
  "data": {
    "questions": [
      "有什么价格在100到200元之间的商品推荐吗？",
      "最近有什么新品上市？",
      // ... 更多问题
    ],
    "total": 5,
    "category": "general"
  }
}
```

## 2. 前端API调用格式

### 2.1 问答API调用

```javascript
// 使用askQuestion函数调用问答接口
import { askQuestion } from '@/api/llm-qa'

const response = await askQuestion({
  question: "用户提出的问题",
  sessionId: "会话ID（可选）"
})

// 响应处理
if (response.errno === 0) {
  // 成功响应
  const answer = response.data.answer
  const goods = response.data.goods || []
  const sessionId = response.data.sessionId
} else {
  // 错误处理
  console.error('请求失败:', response.errmsg)
}
```

### 2.2 响应验证

```javascript
// 使用validateLLMResponse函数验证响应
import { validateLLMResponse } from '@/utils/llm-utils'

const validation = validateLLMResponse(response)
if (validation.valid) {
  // 验证通过，提取数据
  const { answer, goods, sessionId } = validation.data
} else {
  // 验证失败，显示错误
  console.error('响应验证失败:', validation.error)
}
```

## 3. 数据流程详解

### 3.1 请求流程

1. 用户在前端界面输入问题
2. 前端调用`askQuestion` API函数
3. API函数发送POST请求到`/admin/llm/qa/ask`
4. 后端`AdminLLMQAController.ask`方法接收请求
5. 后端调用`LLMQAService.processQuestion`处理问题
6. 服务层调用LLM API并处理响应
7. 后端返回`GoodsQAResponse`对象
8. 控制器使用`ResponseUtil.ok(response)`包装响应
9. 前端接收响应并验证数据格式
10. 前端显示回答和商品列表

### 3.2 数据结构转换

1. **后端原始响应** (`GoodsQAResponse`):
   ```java
   {
     "code": 200,
     "message": "success",
     "answer": "AI生成的回答",
     "goods": [商品列表],
     "sessionId": "会话ID"
   }
   ```

2. **ResponseUtil包装后**:
   ```json
   {
     "errno": 0,
     "errmsg": "成功",
     "data": {
       "code": 200,
       "message": "success",
       "answer": "AI生成的回答",
       "goods": [商品列表],
       "sessionId": "会话ID"
     }
   }
   ```

3. **前端验证后提取**:
   ```javascript
   {
     "answer": "AI生成的回答",
     "goods": [商品列表],
     "sessionId": "会话ID"
   }
   ```

## 4. 错误处理机制

### 4.1 后端错误处理

1. **参数错误** (HTTP 200, errno 401):
   ```json
   {
     "errno": 401,
     "errmsg": "参数不对",
     "data": null
   }
   ```

2. **系统错误** (HTTP 200, errno 500):
   ```json
   {
     "errno": 500,
     "errmsg": "处理请求时发生错误：具体错误信息",
     "data": null
   }
   ```

3. **业务错误** (HTTP 200, errno 其他):
   ```json
   {
     "errno": 503,
     "errmsg": "业务不支持",
     "data": null
   }
   ```

### 4.2 前端错误处理

1. **网络错误**:
   ```javascript
   try {
     const response = await askQuestion(params)
   } catch (error) {
     // 处理网络错误
     console.error('网络请求失败:', error)
   }
   ```

2. **响应验证错误**:
   ```javascript
   const validation = validateLLMResponse(response)
   if (!validation.valid) {
     // 处理验证错误
     console.error('响应验证失败:', validation.error)
   }
   ```

3. **业务逻辑错误**:
   ```javascript
   if (response.errno !== 0) {
     // 处理业务错误
     console.error('请求失败:', response.errmsg)
   }
   ```

## 5. 关键问题分析

### 5.1 当前问题：错误码undefined

**问题描述**：
后端返回正确的数据结构，但前端仍报错"请求失败 (错误码: undefined) - 未知错误"。

**原因分析**：
1. 后端返回的数据结构中，answer和goods字段位于`response.data.data`下
2. 前端`validateLLMResponse`函数尝试从`response.data`中直接提取answer和goods字段
3. 由于字段路径不匹配，导致answer为undefined，验证失败

**解决方案**：
修改前端`validateLLMResponse`函数，正确处理嵌套的数据结构：

```javascript
// 修复前
const answer = response.data.answer
const goods = response.data.goods || []

// 修复后
const innerData = response.data.data || response.data; // 兼容两种格式
const answer = innerData.answer
const goods = innerData.goods || []
```

### 5.2 数据结构不一致问题

**问题描述**：
后端使用了双重嵌套的数据结构（ResponseUtil包装GoodsQAResponse），导致前端处理复杂。

**长期解决方案**：
1. 统一数据结构，减少嵌套层级
2. 在前端和后端之间定义明确的API契约
3. 使用TypeScript接口定义数据模型，确保类型安全

## 6. 调试建议

### 6.1 后端调试

1. 查看`litemall-all/logs/log.log`日志文件
2. 使用调试接口`/admin/llm/qa/debug/config`获取服务状态
3. 使用测试接口`/admin/llm/qa/debug/test-call`测试API调用

### 6.2 前端调试

1. 在浏览器开发者工具中查看Network标签页的请求和响应
2. 在Console中查看详细的错误日志
3. 使用`console.log`输出响应数据结构进行分析

## 7. 最佳实践

### 7.1 后端最佳实践

1. 保持API响应格式一致性
2. 提供详细的错误信息和错误码
3. 实现适当的日志记录
4. 添加API文档和示例

### 7.2 前端最佳实践

1. 实现健壮的错误处理机制
2. 添加加载状态和用户反馈
3. 使用TypeScript定义数据类型
4. 实现响应式设计和良好的用户体验

## 8. 测试用例

### 8.1 成功场景测试

```javascript
// 测试用例1：普通问答
const request1 = {
  question: "有什么价格在100到200元之间的商品推荐吗？"
}

// 测试用例2：带会话ID的问答
const request2 = {
  question: "这些商品中哪些有现货？",
  sessionId: "session_123456"
}
```

### 8.2 错误场景测试

```javascript
// 测试用例3：空问题
const request3 = {
  question: ""
}

// 测试用例4：超长问题
const request4 = {
  question: "a".repeat(501)
}
```

## 9. 总结

LLMQA模块的前后端交互主要涉及问答请求的处理和响应的验证。当前的主要问题是前端对后端返回的嵌套数据结构处理不当，导致验证失败。通过修改前端的`validateLLMResponse`函数，正确处理`response.data.data`的数据结构，可以解决"错误码undefined"的问题。

长期来看，建议统一前后端的数据结构，减少不必要的嵌套，提高代码的可维护性和可读性。同时，加强错误处理和日志记录，提高系统的稳定性和可调试性。