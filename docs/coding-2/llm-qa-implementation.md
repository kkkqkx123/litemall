# 大模型商品问答功能 - 完整实现方案

## 1. 架构概览

### 1.1 核心流程
```
用户提问 → 前端组件 → API接口 → LLMQAService → Qwen3Service → 数据库查询 → 结果返回给用户
                      ↓
                格式化输出解析 → SQL生成 → 查询执行 → 结果格式化
```

### 1.2 技术栈
- **后端**: Spring Boot + MyBatis + ModelScope API
- **前端**: Vue.js + Element UI
- **数据库**: MySQL
- **缓存**: Redis (可选)

## 2. 后端实现

### 2.1 主服务类 - LLMQAService
```java
@Service
@Slf4j
public class LLMQAService {
    
    @Autowired
    private Qwen3Service qwen3Service;
    
    @Autowired
    private GoodsQueryService goodsQueryService;
    
    @Autowired
    private LLMOutputParser llmOutputParser;
    
    @Autowired
    private SessionManager sessionManager;
    
    /**
     * 处理商品问答请求
     */
    public GoodsQAResponse processQuestion(GoodsQARequest request) {
        String sessionId = request.getSessionId();
        String question = request.getQuestion();
        
        try {
            // 1. 构建完整的上下文（包含历史对话）
            String fullContext = buildFullContext(sessionId, question);
            
            // 2. 调用LLM生成查询指令
            String llmResponse = qwen3Service.generateQueryIntent(fullContext);
            
            // 3. 解析LLM输出
            ParsedQuery parsedQuery = llmOutputParser.parseLLMOutput(llmResponse);
            
            // 4. 执行数据库查询
            List<Goods> goodsList = executeQuery(parsedQuery);
            
            // 5. 生成最终回答
            String finalAnswer = qwen3Service.generateAnswer(question, goodsList, sessionId);
            
            // 6. 保存会话历史
            sessionManager.saveToHistory(sessionId, question, finalAnswer);
            
            return GoodsQAResponse.builder()
                .answer(finalAnswer)
                .relatedGoods(goodsList)
                .sessionId(sessionId)
                .questionType(parsedQuery.getQueryIntent().getQueryType())
                .build();
                
        } catch (Exception e) {
            log.error("问答处理失败", e);
            return handleError(sessionId, question, e);
        }
    }
    
    /**
     * 构建完整的上下文
     */
    private String buildFullContext(String sessionId, String currentQuestion) {
        List<ChatMessage> history = sessionManager.getHistory(sessionId);
        
        StringBuilder context = new StringBuilder();
        
        // 添加系统提示
        context.append("你是一个商品数据库查询专家。请根据用户问题生成结构化的JSON查询指令。\n");
        context.append("数据库表结构：litemall_goods(id, name, brief, price, number, category_id, is_on_sale)\n");
        context.append("可用查询类型：price_range, stock_check, category_filter, keyword_search, statistical\n");
        context.append("输出要求：必须返回有效的JSON格式，包含query_type、conditions、sort、limit字段\n\n");
        
        // 添加历史对话
        if (!history.isEmpty()) {
            context.append("历史对话：\n");
            for (ChatMessage message : history) {
                context.append("用户：").append(message.getQuestion()).append("\n");
                context.append("助手：").append(message.getAnswer()).append("\n");
            }
            context.append("\n");
        }
        
        // 添加当前问题
        context.append("当前问题：").append(currentQuestion);
        
        return context.toString();
    }
    
    /**
     * 执行数据库查询
     */
    private List<Goods> executeQuery(ParsedQuery parsedQuery) {
        String sql = parsedQuery.getSql();
        List<Object> parameters = parsedQuery.getParameters();
        
        return goodsQueryService.executeQuery(sql, parameters);
    }
    
    /**
     * 错误处理
     */
    private GoodsQAResponse handleError(String sessionId, String question, Exception e) {
        String errorAnswer = "抱歉，我遇到了问题。请尝试重新提问或联系客服。";
        
        return GoodsQAResponse.builder()
            .answer(errorAnswer)
            .relatedGoods(Collections.emptyList())
            .sessionId(sessionId)
            .questionType("error")
            .errorMessage(e.getMessage())
            .build();
    }
}
```

### 2.2 LLM服务类 - Qwen3Service
```java
@Service
@Slf4j
public class Qwen3Service {
    
    @Value("${litemall.modelscope.api-url}")
    private String apiUrl;
    
    @Value("${litemall.modelscope.api-key}")
    private String apiKey;
    
    @Autowired
    private RestTemplate restTemplate;
    
    /**
     * 生成查询意图
     */
    public String generateQueryIntent(String context) {
        String prompt = buildQueryGenerationPrompt(context);
        return callLLM(prompt);
    }
    
    /**
     * 生成最终回答
     */
    public String generateAnswer(String question, List<Goods> goodsList, String sessionId) {
        String prompt = buildAnswerGenerationPrompt(question, goodsList);
        return callLLM(prompt);
    }
    
    /**
     * 构建查询生成提示词
     */
    private String buildQueryGenerationPrompt(String context) {
        return String.format("""
            你是一个商品数据库查询专家。请根据用户问题生成结构化的JSON查询指令。
            
            数据库表结构：
            表名：litemall_goods
            字段：id(商品ID), name(商品名称), brief(商品简介), price(价格), number(库存), category_id(分类ID), is_on_sale(是否在售)
            
            可用查询类型：
            1. price_range - 价格范围查询
            2. stock_check - 库存检查  
            3. category_filter - 分类筛选
            4. keyword_search - 关键词搜索
            5. statistical - 统计查询
            
            输出要求：
            - 必须返回有效的JSON格式
            - 包含query_type、conditions、sort、limit字段
            - conditions中的字段必须是数据库中存在的字段
            - price字段使用数值范围，格式：{"min": 100, "max": 200}
            - number字段使用库存条件，格式：{"min": 10, "max": null}
            
            示例输出：
            {
              "query_type": "price_range",
              "conditions": {
                "price": {"min": 100, "max": 200},
                "is_on_sale": 1
              },
              "sort": "price ASC",
              "limit": 10
            }
            
            %s
            
            请只返回JSON，不要其他解释。
            """, context);
    }
    
    /**
     * 构建回答生成提示词
     */
    private String buildAnswerGenerationPrompt(String question, List<Goods> goodsList) {
        StringBuilder goodsInfo = new StringBuilder();
        for (Goods goods : goodsList) {
            goodsInfo.append(String.format("- %s: 价格%.2f元, 库存%d件, %s%n", 
                goods.getName(), goods.getPrice(), goods.getNumber(), goods.getBrief()));
        }
        
        return String.format("""
            你是一个商品销售助手。请根据查询结果生成友好的回答。
            
            用户问题：%s
            查询结果：
            %s
            
            要求：
            1. 用自然语言描述结果
            2. 包含具体的商品信息（名称、价格、库存）
            3. 如果结果为空，给出合适的建议
            4. 保持回答简洁明了，不超过200字
            5. 使用中文回答
            
            请直接给出回答，不要其他解释。
            """, question, goodsInfo.toString());
    }
    
    /**
     * 调用LLM API
     */
    private String callLLM(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "qwen-turbo");
            requestBody.put("prompt", prompt);
            requestBody.put("max_tokens", 1000);
            requestBody.put("temperature", 0.7);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl + "/chat/completions",
                HttpMethod.POST,
                request,
                Map.class
            );
            
            if (response.getBody() != null && response.getBody().containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (!choices.isEmpty() && choices.get(0).containsKey("text")) {
                    return (String) choices.get(0).get("text");
                }
            }
            
            throw new RuntimeException("LLM响应格式错误");
            
        } catch (Exception e) {
            log.error("LLM调用失败", e);
            throw new RuntimeException("LLM服务调用失败: " + e.getMessage());
        }
    }
}
```

### 2.3 会话管理器 - SessionManager
```java
@Component
public class SessionManager {
    
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_SIZE = 10;
    private static final long SESSION_TIMEOUT = 600000; // 10分钟
    
    /**
     * 获取会话信息
     */
    public SessionInfo getSession(String sessionId) {
        SessionInfo session = sessions.get(sessionId);
        
        if (session == null) {
            session = new SessionInfo(sessionId);
            sessions.put(sessionId, session);
        } else {
            // 检查会话是否过期
            if (System.currentTimeMillis() - session.getLastActiveTime() > SESSION_TIMEOUT) {
                session.clearHistory();
            }
            session.updateLastActiveTime();
        }
        
        return session;
    }
    
    /**
     * 获取会话历史
     */
    public List<ChatMessage> getHistory(String sessionId) {
        SessionInfo session = getSession(sessionId);
        return session.getHistory();
    }
    
    /**
     * 保存到会话历史
     */
    public void saveToHistory(String sessionId, String question, String answer) {
        SessionInfo session = getSession(sessionId);
        session.addMessage(new ChatMessage(question, answer, System.currentTimeMillis()));
        
        // 清理过期会话
        cleanupExpiredSessions();
    }
    
    /**
     * 清理过期会话
     */
    private void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> {
            SessionInfo session = entry.getValue();
            return currentTime - session.getLastActiveTime() > SESSION_TIMEOUT;
        });
    }
    
    /**
     * 会话信息类
     */
    @Data
    public static class SessionInfo {
        private String sessionId;
        private List<ChatMessage> history = new ArrayList<>();
        private long lastActiveTime;
        
        public SessionInfo(String sessionId) {
            this.sessionId = sessionId;
            this.lastActiveTime = System.currentTimeMillis();
        }
        
        public void addMessage(ChatMessage message) {
            history.add(message);
            
            // 限制历史记录大小
            if (history.size() > MAX_HISTORY_SIZE) {
                history.remove(0);
            }
            
            updateLastActiveTime();
        }
        
        public void clearHistory() {
            history.clear();
            updateLastActiveTime();
        }
        
        public void updateLastActiveTime() {
            this.lastActiveTime = System.currentTimeMillis();
        }
    }
    
    /**
     * 聊天消息类
     */
    @Data
    @AllArgsConstructor
    public static class ChatMessage {
        private String question;
        private String answer;
        private long timestamp;
    }
}
```

### 2.4 商品查询服务 - GoodsQueryService
```java
@Service
public class GoodsQueryService {
    
    @Autowired
    private GoodsMapper goodsMapper;
    
    /**
     * 执行SQL查询
     */
    public List<Goods> executeQuery(String sql, List<Object> parameters) {
        return goodsMapper.executeQuery(sql, parameters);
    }
    
    /**
     * 根据条件查询商品
     */
    public List<Goods> queryGoodsByCondition(Map<String, Object> conditions) {
        QueryWrapper<Goods> queryWrapper = new QueryWrapper<>();
        
        // 价格范围
        if (conditions.containsKey("priceMin") && conditions.containsKey("priceMax")) {
            queryWrapper.between("price", conditions.get("priceMin"), conditions.get("priceMax"));
        } else if (conditions.containsKey("priceMin")) {
            queryWrapper.ge("price", conditions.get("priceMin"));
        } else if (conditions.containsKey("priceMax")) {
            queryWrapper.le("price", conditions.get("priceMax"));
        }
        
        // 库存范围
        if (conditions.containsKey("stockMin") && conditions.containsKey("stockMax")) {
            queryWrapper.between("number", conditions.get("stockMin"), conditions.get("stockMax"));
        } else if (conditions.containsKey("stockMin")) {
            queryWrapper.ge("number", conditions.get("stockMin"));
        } else if (conditions.containsKey("stockMax")) {
            queryWrapper.le("number", conditions.get("stockMax"));
        }
        
        // 分类
        if (conditions.containsKey("categoryId")) {
            queryWrapper.eq("category_id", conditions.get("categoryId"));
        }
        
        // 在售状态
        if (conditions.containsKey("isOnSale")) {
            queryWrapper.eq("is_on_sale", conditions.get("isOnSale"));
        }
        
        // 关键词搜索
        if (conditions.containsKey("keyword")) {
            String keyword = (String) conditions.get("keyword");
            queryWrapper.and(wrapper -> 
                wrapper.like("name", keyword)
                      .or()
                      .like("brief", keyword)
            );
        }
        
        // 排序
        if (conditions.containsKey("sort")) {
            String sort = (String) conditions.get("sort");
            String[] sortParts = sort.split("\\s+");
            if (sortParts.length == 2) {
                String column = sortParts[0];
                String order = sortParts[1].toUpperCase();
                if ("ASC".equals(order)) {
                    queryWrapper.orderByAsc(column);
                } else if ("DESC".equals(order)) {
                    queryWrapper.orderByDesc(column);
                }
            }
        }
        
        // 限制数量
        if (conditions.containsKey("limit")) {
            Integer limit = (Integer) conditions.get("limit");
            queryWrapper.last("LIMIT " + limit);
        }
        
        return goodsMapper.selectList(queryWrapper);
    }
}
```

### 2.5 控制器 - AdminGoodsQAController
```java
@RestController
@RequestMapping("/admin/goods/qa")
@Validated
public class AdminGoodsQAController {
    
    private static final Logger log = LoggerFactory.getLogger(AdminGoodsQAController.class);
    
    @Autowired
    private LLMQAService llmqaService;
    
    /**
     * 商品问答接口
     */
    @PostMapping("/ask")
    public Object askQuestion(@RequestBody @Valid GoodsQARequest request) {
        log.info("商品问答请求: question={}, sessionId={}", request.getQuestion(), request.getSessionId());
        
        try {
            // 处理问答请求
            GoodsQAResponse response = llmqaService.processQuestion(request);
            
            log.info("商品问答响应: sessionId={}, questionType={}", 
                response.getSessionId(), response.getQuestionType());
            
            return ResponseUtil.ok(response);
            
        } catch (Exception e) {
            log.error("商品问答失败", e);
            return ResponseUtil.fail(BizCodeEnum.QUESTION_PROCESS_ERROR, "问答处理失败");
        }
    }
    
    /**
     * 清空问答历史
     */
    @PostMapping("/clear")
    public Object clearHistory(@RequestParam String sessionId) {
        log.info("清空问答历史: sessionId={}", sessionId);
        
        try {
            llmqaService.clearHistory(sessionId);
            return ResponseUtil.ok();
            
        } catch (Exception e) {
            log.error("清空问答历史失败", e);
            return ResponseUtil.fail(BizCodeEnum.CLEAR_HISTORY_ERROR, "清空历史失败");
        }
    }
}
```

## 3. 前端实现

### 3.1 主组件 - LLMQA.vue
```vue
<template>
  <div class="llm-qa-container">
    <!-- 头部标题 -->
    <div class="qa-header">
      <h2>智能商品问答</h2>
      <div class="session-info" v-if="sessionId">
        <span>会话ID: {{ sessionId }}</span>
        <span>对话轮数: {{ messageCount }}</span>
      </div>
    </div>

    <!-- 对话历史 -->
    <div class="chat-history" ref="chatHistory">
      <div v-for="(message, index) in messages" :key="index" 
           :class="['message', message.type]">
        <div class="message-content">
          <div class="message-text">{{ message.text }}</div>
          <div class="message-time">{{ formatTime(message.timestamp) }}</div>
        </div>
      </div>
      
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-message">
        <i class="el-icon-loading"></i>
        <span>正在思考中...</span>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-area">
      <el-input
        v-model="currentQuestion"
        type="textarea"
        :rows="3"
        placeholder="请输入您的商品相关问题，如：价格在100-200元的商品有哪些？"
        @keyup.enter.native="sendQuestion"
        :disabled="loading"
      />
      <div class="input-actions">
        <el-button 
          type="primary" 
          @click="sendQuestion" 
          :loading="loading"
          :disabled="!currentQuestion.trim()"
        >
          发送问题
        </el-button>
        <el-button @click="clearHistory" :disabled="messages.length === 0">
          清空对话
        </el-button>
      </div>
    </div>

    <!-- 快速提问按钮 -->
    <div class="quick-questions">
      <span>快速提问：</span>
      <el-button 
        v-for="question in quickQuestions" 
        :key="question"
        size="small"
        @click="quickAsk(question)"
        :disabled="loading"
      >
        {{ question }}
      </el-button>
    </div>
  </div>
</template>

<script>
import { askQuestion, clearHistory } from '@/api/llm-qa'

export default {
  name: 'LLMQA',
  data() {
    return {
      sessionId: '',
      messages: [],
      currentQuestion: '',
      loading: false,
      quickQuestions: [
        '价格在100-200元的商品有哪些？',
        '库存不足的商品有哪些？',
        '电子产品分类下的商品有哪些？',
        '统计在售商品总数'
      ]
    }
  },
  computed: {
    messageCount() {
      return Math.floor(this.messages.length / 2)
    }
  },
  created() {
    this.generateSessionId()
  },
  methods: {
    /**
     * 生成会话ID
     */
    generateSessionId() {
      const timestamp = Date.now()
      const random = Math.random().toString(36).substr(2, 9)
      this.sessionId = `session_${timestamp}_${random}`
    },

    /**
     * 发送问题
     */
    async sendQuestion() {
      if (!this.currentQuestion.trim() || this.loading) {
        return
      }

      const question = this.currentQuestion.trim()
      this.currentQuestion = ''
      this.loading = true

      // 添加用户消息到对话历史
      this.addMessage(question, 'user')

      try {
        // 构建上下文历史
        const contextHistory = this.buildContextHistory()

        // 调用API
        const response = await askQuestion({
          question: question,
          sessionId: this.sessionId,
          context: contextHistory
        })

        if (response.errno === 0) {
          const data = response.data
          
          // 添加助手回复到对话历史
          this.addMessage(data.answer, 'assistant')
          
          // 如果有相关商品，显示商品信息
          if (data.relatedGoods && data.relatedGoods.length > 0) {
            const goodsInfo = this.formatGoodsInfo(data.relatedGoods)
            this.addMessage(goodsInfo, 'goods')
          }
          
          // 更新会话ID（如果需要）
          if (data.sessionId && data.sessionId !== this.sessionId) {
            this.sessionId = data.sessionId
          }
        } else {
          this.addMessage('抱歉，处理您的问题时出现了错误。', 'assistant')
        }
      } catch (error) {
        console.error('问答请求失败:', error)
        this.addMessage('抱歉，网络请求失败，请稍后重试。', 'assistant')
      } finally {
        this.loading = false
        this.$nextTick(() => {
          this.scrollToBottom()
        })
      }
    },

    /**
     * 构建上下文历史
     */
    buildContextHistory() {
      const history = []
      for (let i = 0; i < this.messages.length - 1; i += 2) {
        if (this.messages[i] && this.messages[i + 1]) {
          history.push({
            question: this.messages[i].text,
            answer: this.messages[i + 1].text
          })
        }
      }
      return history.slice(-5) // 只保留最近5轮对话
    },

    /**
     * 快速提问
     */
    quickAsk(question) {
      this.currentQuestion = question
      this.sendQuestion()
    },

    /**
     * 清空对话历史
     */
    async clearHistory() {
      try {
        await clearHistory(this.sessionId)
        this.messages = []
        this.generateSessionId()
        this.$message.success('对话历史已清空')
      } catch (error) {
        console.error('清空历史失败:', error)
        this.$message.error('清空历史失败')
      }
    },

    /**
     * 添加消息到对话历史
     */
    addMessage(text, type) {
      this.messages.push({
        text: text,
        type: type,
        timestamp: new Date()
      })
    },

    /**
     * 格式化商品信息
     */
    formatGoodsInfo(goodsList) {
      if (!goodsList || goodsList.length === 0) {
        return '未找到相关商品'
      }

      let info = '为您找到以下商品：\\n'
      goodsList.forEach((goods, index) => {
        info += `${index + 1}. ${goods.name} - 价格：¥${goods.price}，库存：${goods.number}件`
        if (goods.brief) {
          info += `，${goods.brief}`
        }
        info += '\\n'
      })
      
      return info.trim()
    },

    /**
     * 格式化时间
     */
    formatTime(date) {
      const now = new Date()
      const diff = now - date
      
      if (diff < 60000) {
        return '刚刚'
      } else if (diff < 3600000) {
        return Math.floor(diff / 60000) + '分钟前'
      } else {
        return date.toLocaleString()
      }
    },

    /**
     * 滚动到底部
     */
    scrollToBottom() {
      const chatHistory = this.$refs.chatHistory
      if (chatHistory) {
        chatHistory.scrollTop = chatHistory.scrollHeight
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.llm-qa-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.qa-header {
  text-align: center;
  margin-bottom: 20px;
  
  h2 {
    color: #303133;
    margin-bottom: 10px;
  }
  
  .session-info {
    font-size: 12px;
    color: #909399;
    
    span {
      margin: 0 10px;
    }
  }
}

.chat-history {
  height: 400px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 15px;
  margin-bottom: 20px;
  background: #fafafa;
}

.message {
  margin-bottom: 15px;
  display: flex;
  
  &.user {
    justify-content: flex-end;
    
    .message-content {
      background: #409eff;
      color: white;
      border-radius: 18px 18px 0 18px;
    }
  }
  
  &.assistant, &.goods {
    justify-content: flex-start;
    
    .message-content {
      background: #f3f5f7;
      color: #303133;
      border-radius: 18px 18px 18px 0;
    }
  }
  
  &.goods {
    .message-content {
      background: #e6f7ff;
      border: 1px solid #91d5ff;
    }
  }
}

.message-content {
  max-width: 70%;
  padding: 10px 15px;
  word-wrap: break-word;
  
  .message-text {
    margin-bottom: 5px;
    line-height: 1.5;
  }
  
  .message-time {
    font-size: 11px;
    opacity: 0.7;
    text-align: right;
  }
}

.loading-message {
  text-align: center;
  color: #909399;
  font-size: 14px;
  
  i {
    margin-right: 8px;
  }
}

.input-area {
  margin-bottom: 15px;
  
  .input-actions {
    margin-top: 10px;
    text-align: center;
    
    .el-button {
      margin: 0 10px;
    }
  }
}

.quick-questions {
  text-align: center;
  
  span {
    color: #606266;
    margin-right: 10px;
  }
  
  .el-button {
    margin: 0 5px 5px 0;
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .llm-qa-container {
    padding: 15px;
    margin: 10px;
  }
  
  .chat-history {
    height: 300px;
  }
  
  .message-content {
    max-width: 85%;
  }
  
  .quick-questions {
    .el-button {
      display: block;
      width: 100%;
      margin: 5px 0;
    }
  }
}
</style>
```

### 3.2 API封装 - llm-qa.js
```javascript
import request from '@/utils/request'

/**
 * 商品问答接口
 * @param {Object} data - 请求数据
 * @param {string} data.question - 用户问题
 * @param {string} data.sessionId - 会话ID
 * @param {Array} data.context - 上下文历史
 * @returns {Promise}
 */
export function askQuestion(data) {
  return request({
    url: '/admin/goods/qa/ask',
    method: 'post',
    data: data
  })
}

/**
 * 清空问答历史
 * @param {string} sessionId - 会话ID
 * @returns {Promise}
 */
export function clearHistory(sessionId) {
  return request({
    url: '/admin/goods/qa/clear',
    method: 'post',
    params: { sessionId }
  })
}

/**
 * 获取问答历史
 * @param {string} sessionId - 会话ID
 * @returns {Promise}
 */
export function getHistory(sessionId) {
  return request({
    url: '/admin/goods/qa/history',
    method: 'get',
    params: { sessionId }
  })
}

/**
 * 获取热门问题
 * @returns {Promise}
 */
export function getHotQuestions() {
  return request({
    url: '/admin/goods/qa/hot-questions',
    method: 'get'
  })
}
```

## 4. 数据库设计

### 4.1 商品表结构（使用现有litemall_goods表）
```sql
-- 商品表（已存在）
CREATE TABLE `litemall_goods` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(120) NOT NULL DEFAULT '' COMMENT '商品名称',
  `brief` varchar(255) DEFAULT '' COMMENT '商品简介',
  `description` text COMMENT '商品详细介绍',
  `pic_url` varchar(255) DEFAULT '' COMMENT '商品页面商品图片',
  `gallery` varchar(1023) DEFAULT NULL COMMENT '商品宣传图片列表，采用JSON数组格式',
  `category_id` int(11) DEFAULT '0' COMMENT '商品所属类目ID',
  `brand_id` int(11) DEFAULT '0' COMMENT '品牌ID',
  `goods_sn` varchar(60) DEFAULT '' COMMENT '商品编号',
  `price` decimal(10,2) DEFAULT '0.00' COMMENT '商品价格',
  `original_price` decimal(10,2) DEFAULT '0.00' COMMENT '商品原价',
  `cost` decimal(10,2) DEFAULT '0.00' COMMENT '商品成本价',
  `number` int(11) DEFAULT '0' COMMENT '商品库存数量',
  `unit` varchar(31) DEFAULT '' COMMENT '商品单位，例如件、盒',
  `is_on_sale` tinyint(1) DEFAULT '1' COMMENT '是否上架销售：0下架，1上架',
  `sort_order` smallint(4) DEFAULT '100' COMMENT '商品排序',
  `is_new` tinyint(1) DEFAULT '0' COMMENT '是否新品首发',
  `is_hot` tinyint(1) DEFAULT '0' COMMENT '是否人气推荐',
  `add_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `category_id` (`category_id`),
  KEY `brand_id` (`brand_id`),
  KEY `goods_sn` (`goods_sn`),
  KEY `is_on_sale` (`is_on_sale`),
  KEY `sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品基本信息表';
```

### 4.2 会话历史表（可选）
```sql
-- 会话历史表（用于持久化会话数据）
CREATE TABLE `litemall_qa_session` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL COMMENT '会话ID',
  `user_id` int(11) DEFAULT NULL COMMENT '用户ID（可选）',
  `question` text NOT NULL COMMENT '用户问题',
  `answer` text COMMENT '系统回答',
  `question_type` varchar(32) DEFAULT NULL COMMENT '问题类型',
  `related_goods` text COMMENT '相关商品ID列表（JSON格式）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品问答会话历史表';
```

## 5. 配置说明

### 5.1 application.yml配置
```yaml
# ModelScope配置
litemall:
  modelscope:
    api-key: demo-key  # 当前为演示模式，无需真实API密钥
    api-url: https://api.modelscope.cn/v1  # ModelScope API地址
    enabled: true  # 启用问答功能
    max-context-length: 5000  # 最大上下文长度
    session-timeout: 600  # 会话超时时间（秒）
```

### 5.2 环境变量配置
```bash
# 开发环境
export MODELSCOPE_API_KEY="your-actual-api-key"
export MODELSCOPE_API_URL="https://api.modelscope.cn/v1"

# 生产环境
export MODELSCOPE_API_KEY="${MODELSCOPE_API_KEY}"
export MODELSCOPE_API_URL="https://api.modelscope.cn/v1"
```

## 6. 部署步骤

### 6.1 后端部署
```bash
# 1. 确保litemall项目已正确配置
# 2. 添加相关依赖（已在pom.xml中）
# 3. 创建数据库表（如需要会话历史表）
# 4. 配置application.yml
# 5. 启动Spring Boot应用
mvn spring-boot:run
```

### 6.2 前端部署
```bash
# 1. 添加llm-qa.js API文件到src/api/
# 2. 创建LLMQA.vue组件到src/views/
# 3. 在路由配置中添加问答页面
# 4. 构建前端项目
npm run build
# 5. 部署到nginx或集成到Spring Boot
```

### 6.3 验证部署
```bash
# 测试API接口
curl -X POST http://localhost:8080/admin/goods/qa/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question":"价格在100-200元的商品有哪些？",
    "sessionId":"test123",
    "context":[]
  }'
```

## 7. 性能优化

### 7.1 缓存策略
```java
@Service
public class QACacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String CACHE_PREFIX = "qa:";
    private static final long CACHE_TTL = 300; // 5分钟
    
    /**
     * 缓存问答结果
     */
    public void cacheQAResult(String key, GoodsQAResponse response) {
        String cacheKey = CACHE_PREFIX + "response:" + key;
        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL, TimeUnit.SECONDS);
    }
    
    /**
     * 获取缓存的问答结果
     */
    public GoodsQAResponse getCachedQAResult(String key) {
        String cacheKey = CACHE_PREFIX + "response:" + key;
        return (GoodsQAResponse) redisTemplate.opsForValue().get(cacheKey);
    }
    
    /**
     * 缓存LLM响应
     */
    public void cacheLLMResponse(String prompt, String response) {
        String cacheKey = CACHE_PREFIX + "llm:" + DigestUtils.md5DigestAsHex(prompt.getBytes());
        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL, TimeUnit.SECONDS);
    }
    
    /**
     * 获取缓存的LLM响应
     */
    public String getCachedLLMResponse(String prompt) {
        String cacheKey = CACHE_PREFIX + "llm:" + DigestUtils.md5DigestAsHex(prompt.getBytes());
        return (String) redisTemplate.opsForValue().get(cacheKey);
    }
}
```

### 7.2 异步处理
```java
@Service
public class AsyncQAService {
    
    @Async("qaExecutor")
    public CompletableFuture<GoodsQAResponse> processQuestionAsync(GoodsQARequest request) {
        try {
            // 异步处理问答请求
            GoodsQAResponse response = llmqaService.processQuestion(request);
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            log.error("异步问答处理失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
```

## 8. 监控与告警

### 8.1 健康检查
```java
@Component
public class GoodsQAHealthIndicator implements HealthIndicator {
    
    @Autowired
    private Qwen3Service qwen3Service;
    
    @Override
    public Health health() {
        try {
            // 检查LLM API连接
            boolean llmHealthy = checkLLMConnection();
            
            // 检查数据库连接
            boolean dbHealthy = checkDatabaseConnection();
            
            if (llmHealthy && dbHealthy) {
                return Health.up()
                    .withDetail("llm", "connected")
                    .withDetail("database", "connected")
                    .build();
            } else {
                return Health.down()
                    .withDetail("llm", llmHealthy ? "connected" : "disconnected")
                    .withDetail("database", dbHealthy ? "connected" : "disconnected")
                    .build();
            }
        } catch (Exception e) {
            return Health.down().withException(e).build();
        }
    }
    
    private boolean checkLLMConnection() {
        try {
            // 简单的LLM连接测试
            String testPrompt = "测试连接";
            String response = qwen3Service.generateQueryIntent(testPrompt);
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            log.error("LLM连接检查失败", e);
            return false;
        }
    }
    
    private boolean checkDatabaseConnection() {
        try {
            // 简单的数据库连接测试
            return goodsQueryService.executeQuery("SELECT 1", Collections.emptyList()) != null;
        } catch (Exception e) {
            log.error("数据库连接检查失败", e);
            return false;
        }
    }
}
```

### 8.2 指标监控
```java
@Component
public class GoodsQAMetrics {
    
    private final Counter questionCounter;
    private final Timer responseTimer;
    private final Counter errorCounter;
    private final Gauge sessionCountGauge;
    
    public GoodsQAMetrics(MeterRegistry registry) {
        this.questionCounter = Counter.builder("goods_qa_questions_total")
            .description("Total number of questions asked")
            .register(registry);
            
        this.responseTimer = Timer.builder("goods_qa_response_time")
            .description("Response time for questions")
            .register(registry);
            
        this.errorCounter = Counter.builder("goods_qa_errors_total")
            .description("Total number of errors")
            .register(registry);
            
        this.sessionCountGauge = Gauge.builder("goods_qa_sessions_active")
            .description("Number of active sessions")
            .register(registry, this, GoodsQAMetrics::getActiveSessionCount);
    }
    
    public void recordQuestion() {
        questionCounter.increment();
    }
    
    public void recordResponseTime(Duration duration) {
        responseTimer.record(duration);
    }
    
    public void recordError() {
        errorCounter.increment();
    }
    
    private double getActiveSessionCount() {
        return sessionManager.getActiveSessionCount();
    }
}
```

这个完整的实现方案提供了从后端服务到前端组件的全套代码，包含了多轮对话支持、格式化输出解析、错误处理、性能优化和监控告警等完整功能。