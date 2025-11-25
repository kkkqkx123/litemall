# 大模型商品问答功能 - 后端API设计文档

## 1. 接口概览

### 1.1 基础信息
- **Base URL**: `http://localhost:8080/admin`
- **认证方式**: JWT Token (Header: `X-Litemall-Token`)
- **数据格式**: JSON
- **编码**: UTF-8

### 1.2 接口列表

| 接口 | 方法 | 路径 | 描述 |
|------|------|------|------|
| 商品问答 | POST | `/goods/qa/ask` | 处理用户提问 |
| 获取历史 | GET | `/goods/qa/history/{sessionId}` | 获取会话历史 |
| 清空历史 | DELETE | `/goods/qa/history/{sessionId}` | 清空会话历史 |
| 健康检查 | GET | `/goods/qa/health` | 服务健康检查 |

## 2. 请求响应模型

### 2.1 请求模型

#### GoodsQARequest
```java
@Data
public class GoodsQARequest {
    @NotBlank(message = "问题不能为空")
    private String question;      // 用户问题
    
    private String sessionId;     // 会话ID（用于多轮对话）
    private String context;       // 上下文信息（可选）
    
    @Min(value = 1, message = "返回数量必须大于0")
    @Max(value = 50, message = "返回数量不能超过50")
    private Integer maxResults = 10;  // 最大返回结果数
}
```

#### GoodsQAResponse
```java
@Data
public class GoodsQAResponse {
    private boolean success;      // 请求是否成功
    private String answer;         // AI回答内容
    private String question;     // 用户问题
    private String sessionId;    // 会话ID
    
    private List<GoodsInfo> relatedGoods;  // 相关商品列表
    private String queryType;     // 查询类型
    private Long timestamp;      // 时间戳
    private String error;        // 错误信息（失败时）
}

@Data
public class GoodsInfo {
    private Integer id;          // 商品ID
    private String name;         // 商品名称
    private String brief;        // 商品简介
    private BigDecimal price;    // 价格
    private Integer number;      // 库存数量
    private String picUrl;       // 图片URL
}
```

## 3. API接口详细设计

### 3.1 商品问答接口

#### 基本信息
- **路径**: `/admin/goods/qa/ask`
- **方法**: POST
- **描述**: 处理用户的商品问答请求

#### 请求参数
```json
{
  "question": "价格在100-200元的商品有哪些？",
  "sessionId": "session_123456",
  "maxResults": 10
}
```

#### 成功响应
```json
{
  "success": true,
  "answer": "为您找到3个价格在100-200元的商品：\n1. 商品A - 价格：150元\n2. 商品B - 价格：180元\n3. 商品C - 价格：199元",
  "question": "价格在100-200元的商品有哪些？",
  "sessionId": "session_123456",
  "queryType": "price_range",
  "timestamp": 1640995200000,
  "relatedGoods": [
    {
      "id": 1,
      "name": "商品A",
      "brief": "优质商品A",
      "price": 150.00,
      "number": 100,
      "picUrl": "http://example.com/pic1.jpg"
    }
  ]
}
```

#### 失败响应
```json
{
  "success": false,
  "error": "AI服务暂时不可用，请稍后再试",
  "timestamp": 1640995200000
}
```

#### 实现代码
```java
@RestController
@RequestMapping("/admin/goods/qa")
@Validated
public class AdminGoodsQAController {
    
    @Autowired
    private LLMQAService llmQAService;
    
    @PostMapping("/ask")
    public Object askQuestion(@RequestBody @Valid GoodsQARequest request) {
        try {
            // 生成会话ID（如果不存在）
            String sessionId = request.getSessionId();
            if (StringUtils.isEmpty(sessionId)) {
                sessionId = UUID.randomUUID().toString();
            }
            
            // 调用LLM问答服务
            GoodsQAResponse response = llmQAService.processQuestion(
                request.getQuestion(), 
                sessionId,
                request.getMaxResults()
            );
            
            return ResponseUtil.ok(response);
            
        } catch (Exception e) {
            log.error("问答处理失败", e);
            return ResponseUtil.fail(BizErrorCode.GOODS_QA_ERROR, "问答处理失败");
        }
    }
}
```

### 3.2 获取历史记录接口

#### 基本信息
- **路径**: `/admin/goods/qa/history/{sessionId}`
- **方法**: GET
- **描述**: 获取指定会话的历史问答记录

#### 请求参数
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| sessionId | String | 是 | 会话ID |

#### 成功响应
```json
{
  "success": true,
  "data": {
    "sessionId": "session_123456",
    "history": [
      {
        "question": "价格在100-200元的商品有哪些？",
        "answer": "为您找到3个商品...",
        "timestamp": 1640995200000
      },
      {
        "question": "这些商品中哪些库存充足？",
        "answer": "商品A库存充足...",
        "timestamp": 1640995260000
      }
    ]
  }
}
```

#### 实现代码
```java
@GetMapping("/history/{sessionId}")
public Object getHistory(@PathVariable String sessionId) {
    try {
        ConversationSession session = conversationManager.getSession(sessionId);
        List<ConversationTurn> history = session.getHistory();
        
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("history", history);
        
        return ResponseUtil.ok(result);
        
    } catch (Exception e) {
        log.error("获取历史记录失败", e);
        return ResponseUtil.fail(BizErrorCode.GOODS_QA_ERROR, "获取历史记录失败");
    }
}
```

### 3.3 清空历史记录接口

#### 基本信息
- **路径**: `/admin/goods/qa/history/{sessionId}`
- **方法**: DELETE
- **描述**: 清空指定会话的历史问答记录

#### 成功响应
```json
{
  "success": true,
  "message": "历史记录已清空"
}
```

#### 实现代码
```java
@DeleteMapping("/history/{sessionId}")
public Object clearHistory(@PathVariable String sessionId) {
    try {
        conversationManager.clearSession(sessionId);
        return ResponseUtil.ok("历史记录已清空");
        
    } catch (Exception e) {
        log.error("清空历史记录失败", e);
        return ResponseUtil.fail(BizErrorCode.GOODS_QA_ERROR, "清空历史记录失败");
    }
}
```

### 3.4 健康检查接口

#### 基本信息
- **路径**: `/admin/goods/qa/health`
- **方法**: GET
- **描述**: 检查问答服务健康状态

#### 成功响应
```json
{
  "success": true,
  "data": {
    "status": "healthy",
    "llmService": "connected",
    "database": "connected",
    "timestamp": 1640995200000
  }
}
```

## 4. 错误码定义

### 4.1 业务错误码
```java
public enum BizErrorCode {
    GOODS_QA_ERROR(7000, "商品问答处理失败"),
    LLM_SERVICE_ERROR(7001, "AI服务异常"),
    QUERY_PARSE_ERROR(7002, "查询解析失败"),
    DATABASE_ERROR(7003, "数据库操作失败"),
    SESSION_NOT_FOUND(7004, "会话不存在"),
    INVALID_QUERY_TYPE(7005, "不支持的查询类型");
    
    private final int code;
    private final String message;
}
```

## 5. 接口安全

### 5.1 认证要求
所有接口都需要有效的JWT Token，在请求头中传递：
```
X-Litemall-Token: your-jwt-token
```

### 5.2 限流策略
- 每个用户每分钟最多100次请求
- 超过限制返回429状态码

### 5.3 输入验证
- 问题长度限制：1-500字符
- sessionId格式验证：UUID格式
- SQL注入防护：参数化查询

## 6. 性能指标

### 6.1 响应时间
- 平均响应时间：< 2秒
- P95响应时间：< 5秒
- P99响应时间：< 10秒

### 6.2 并发能力
- 支持并发请求：100 QPS
- 数据库连接池：最大20连接
- LLM API超时：30秒

## 7. 监控指标

### 7.1 业务指标
- 问答请求总数
- 问答成功率
- 平均响应时间
- 会话数量

### 7.2 技术指标
- LLM API调用次数
- 数据库查询次数
- 缓存命中率
- 错误率