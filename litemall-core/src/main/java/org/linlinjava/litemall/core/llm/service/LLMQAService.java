package org.linlinjava.litemall.core.llm.service;

import org.linlinjava.litemall.core.llm.config.LLMConfig;
import org.linlinjava.litemall.core.llm.exception.LLMOutputParseException;
import org.linlinjava.litemall.core.llm.exception.LLMServiceException;
import org.linlinjava.litemall.core.llm.model.*;
import org.linlinjava.litemall.core.llm.parser.*;
import org.linlinjava.litemall.core.llm.util.GoodsDetailFilter;
import org.linlinjava.litemall.db.domain.LitemallGoods;
import org.linlinjava.litemall.db.dao.LitemallGoodsMapper;
import org.linlinjava.litemall.db.domain.LitemallGoodsExample;
import org.linlinjava.litemall.db.service.LitemallGoodsService;
import com.github.pagehelper.PageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.regex.Matcher;

/**
 * LLM问答服务
 * 负责协调整个问答流程
 */
@Service
public class LLMQAService {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMQAService.class);
    
    @Autowired
    private LLMServiceManager llmService;
    
    @Autowired
    private Qwen3Service qwen3Service;
    
    @Autowired
    private LLMConfig llmConfig;
    
    @Autowired
    private LitemallGoodsService goodsService;
    
    @Autowired
    private LitemallGoodsMapper goodsMapper;
    
    @Autowired
    private LLMSessionManager sessionManager;
    
    @Autowired
    private QueryIntentBuilder queryIntentBuilder;
    
    @Autowired
    private LLMOutputParser llmOutputParser;
    
    @Autowired
    private GoodsDetailFilter goodsDetailFilter;
    
    // 本地缓存
    private final Map<String, GoodsQAResponse> responseCache = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> cacheTimestamps = new ConcurrentHashMap<>();
    
    /**
     * 处理商品问答请求
     * @param request 问答请求
     * @return 问答响应
     */
    /**
     * 处理用户问题
     * @param request 问答请求
     * @return 问答响应
     */
    public GoodsQAResponse processQuestion(GoodsQARequest request) {
        // 处理sessionId为null的情况，生成默认会话ID
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = "default-session-" + System.currentTimeMillis();
            request.setSessionId(sessionId);
        }
        
        logger.info("开始处理问答请求，会话ID: {}", sessionId);
        
        try {
            // 参数验证
            if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
                return createErrorResponse("问题不能为空");
            }
            
            // 创建或获取会话
            LLMSessionManager.Session session = sessionManager.getSession(sessionId);
            if (session == null) {
                // 先创建会话，然后获取会话对象
                sessionManager.createSession(1, sessionId); // 使用默认用户ID 1
                session = sessionManager.getSession(sessionId);
            }
            
            // 更新会话中的用户问题
            session.addMessage("user", request.getQuestion());
            
            // 构建提示词并调用LLM
            String prompt = buildEnhancedPrompt(request);
            logger.info("=== 构建的提示词开始 ===");
            logger.info("提示词内容：{}", prompt);
            logger.info("提示词长度：{}", prompt.length());
            logger.info("=== 构建的提示词结束 ===");
            
            String sessionContext = getSessionContextAsString(request.getSessionId());
            String llmResponse = qwen3Service.callLLM(prompt, sessionContext);
            
            // 详细记录LLM响应
            logger.info("=== LLM响应处理开始 ===");
            logger.info("原始LLM响应：{}", llmResponse);
            logger.info("LLM响应长度：{}", llmResponse.length());
            logger.info("是否包含JSON查询：{}", containsJSONQuery(llmResponse));
            
            // 判断是否需要数据库查询
            List<LitemallGoods> goodsList = new ArrayList<>();
            String answer;
            
            if (containsJSONQuery(llmResponse)) {
                // LLM输出包含JSON查询，需要执行数据库查询
                QueryIntent queryIntent = parseQueryIntentFromLLM(llmResponse);
                logger.info("查询意图解析完成：{}", queryIntent);
                logger.info("查询类型：{}", queryIntent.getQueryType());
                logger.info("查询条件：{}", queryIntent.getConditions());
                
                // 检查查询类型是否为unknown，如果是则跳过数据库查询
                if ("unknown".equals(queryIntent.getQueryType())) {
                    // 查询类型为unknown，直接使用自然语言回答
                    answer = extractNaturalLanguageAnswer(llmResponse);
                    logger.info("查询类型为unknown，直接使用LLM的自然语言回答，跳过数据库查询");
                } else {
                    // 执行查询
                    goodsList = executeQuery(queryIntent);
                    logger.info("查询完成，找到 {} 个商品", goodsList.size());
                    
                    // 生成基于查询结果的答案
                    answer = generateAnswerFromQueryResults(request, goodsList, queryIntent);
                    logger.info("生成的答案：{}", answer);
                }
            } else {
                // LLM直接给出了自然语言回答，无需数据库查询
                answer = extractNaturalLanguageAnswer(llmResponse);
                logger.info("直接使用LLM的自然语言回答，跳过数据库查询");
                logger.info("自然语言回答：{}", answer);
            }
            
            logger.info("=== LLM响应处理结束 ===");
            
            // 构建响应
            GoodsQAResponse response = new GoodsQAResponse();
            response.setErrno(0);
            response.setErrmsg("success");
            response.setAnswer(answer);
            response.setGoods(goodsList);
            
            // 更新会话历史
            session.addMessage("assistant", answer);
            
            logger.info("问答处理完成，会话ID: {}", request.getSessionId());
            return response;
            
        } catch (Exception e) {
            logger.error("处理问答请求失败，会话ID: " + request.getSessionId(), e);
            return createErrorResponse("处理请求时出现错误: " + e.getMessage());
        }
    }
    
    /**
     * 验证请求
     * @param request 问答请求
     */
    private void validateRequest(GoodsQARequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        
        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        
        if (request.getQuestion().length() > 500) {
            throw new IllegalArgumentException("问题长度不能超过500字符");
        }
    }
    
    /**
     * 构建增强提示词
     * @param request 问答请求
     * @return 提示词
     */
    private String buildEnhancedPrompt(GoodsQARequest request) {
        StringBuilder prompt = new StringBuilder();
        
        // 系统提示词 - 智能判断版
        prompt.append("你是一个智能商品问答助手。请分析用户问题并自主决定是否需要查询商品数据库。\n");
        prompt.append("重要规则：\n");
        prompt.append("1. 禁止执行任何模糊查询，只允许明确的查询条件（如价格范围、是否在售等）\n");
        prompt.append("2. 对于基于上下文的后续问题，优先使用已有信息回答，避免不必要的数据库查询\n");
        prompt.append("3. 如果需要查询数据库，请同时提供自然语言解释和JSON查询条件\n\n");
        
        // 判断标准
        prompt.append("=== 判断标准 ===\n");
        prompt.append("【需要查询数据库的情况】\n");
        prompt.append("- 用户询问具体商品价格范围（必须明确指定价格区间）\n");
        prompt.append("- 用户查询商品是否在售\n");
        prompt.append("- 用户需要特定分类的商品列表\n");
        prompt.append("- 用户需要基于明确条件的商品筛选\n");
        prompt.append("【不需要查询数据库的情况】\n");
        prompt.append("- 用户询问基于之前查询结果的后续问题（如\"其中哪些适合送给长辈？\"）\n");
        prompt.append("- 用户询问系统功能或操作指南\n");
        prompt.append("- 用户进行一般性对话或问候\n");
        prompt.append("- 用户询问商品使用建议或解释\n");
        prompt.append("- 用户的问题不涉及具体商品信息\n");
        prompt.append("- 用户的问题可以通过已有知识或上下文回答\n\n");
        
        // 获取会话上下文
        Map<String, Object> sessionContext = getSessionContext(request.getSessionId());
        
        // 添加上下文信息到提示词
        if (sessionContext != null && !sessionContext.isEmpty()) {
            prompt.append("=== 会话上下文 ===\n");
            
            // 会话基本信息
            String sessionId = (String) sessionContext.get("sessionId");
            Integer queryCount = (Integer) sessionContext.get("queryCount");
            if (sessionId != null) {
                prompt.append("会话ID: ").append(sessionId).append("\n");
            }
            if (queryCount != null && queryCount > 0) {
                prompt.append("历史查询次数: ").append(queryCount).append("\n");
            }
            
            // 用户偏好
            Map<String, Object> preferences = (Map<String, Object>) sessionContext.get("preferences");
            if (preferences != null && !preferences.isEmpty()) {
                prompt.append("用户偏好:\n");
                for (Map.Entry<String, Object> entry : preferences.entrySet()) {
                    prompt.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }
            
            // 会话历史消息
            LLMSessionManager.Session session = sessionManager.getSession(request.getSessionId());
            if (session != null && !session.getMessages().isEmpty()) {
                prompt.append("历史对话:\n");
                List<LLMSessionManager.Message> messages = session.getMessages();
                // 只显示最近5条消息，避免提示词过长
                int startIndex = Math.max(0, messages.size() - 5);
                for (int i = startIndex; i < messages.size(); i++) {
                    LLMSessionManager.Message message = messages.get(i);
                    prompt.append(message.getType()).append(": ").append(message.getContent()).append("\n");
                }
            }
            
            prompt.append("\n");
        }
        
        // 分析步骤
        prompt.append("=== 分析步骤 ===\n");
        prompt.append("1. 理解用户查询意图\n");
        prompt.append("2. 检查会话上下文，判断是否可以基于已有信息回答\n");
        prompt.append("3. 如果需要查询：提取明确的查询条件和参数\n");
        prompt.append("4. 如果不需要查询：直接给出自然语言回答\n\n");
        
        // 输出格式
        prompt.append("=== 输出格式 ===\n");
        prompt.append("【如果需要查询数据库】请按以下格式输出：\n");
        prompt.append("自然语言解释（说明您将要执行什么查询）\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"query_type\": \"price_range|category_filter|is_on_sale\",\n");
        prompt.append("  \"conditions\": {\n");
        prompt.append("    \"minPrice\": \"最小价格（分，必须为整数）\",\n");
        prompt.append("    \"maxPrice\": \"最大价格（分，必须为整数）\",\n");
        prompt.append("    \"categoryId\": \"分类ID（必须为整数）\",\n");
        prompt.append("    \"isOnSale\": \"是否在售（true/false）\"\n");
        prompt.append("  }\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        
        prompt.append("【如果不需要查询数据库】请直接输出自然语言回答。\n\n");
        
        // 查询类型说明
        prompt.append("=== 查询类型说明 ===\n");
        prompt.append("- price_range: 价格范围查询（必须同时提供minPrice和maxPrice）\n");
        prompt.append("- category_filter: 分类筛选（必须提供categoryId）\n");
        prompt.append("- is_on_sale: 是否在售查询（必须提供isOnSale参数）\n\n");
        
        // 用户问题
        prompt.append("=== 当前用户问题 ===\n");
        prompt.append("用户问题：").append(request.getQuestion()).append("\n\n");
        
        // 智能结束语
        prompt.append("请基于以上判断标准和分析步骤，自主决定是否需要查询数据库，并选择相应的输出格式。");
        
        return prompt.toString();
    }
    
    /**
     * 构建提示词（兼容旧版本）
     * @param request 问答请求
     * @return 提示词
     */
    private String buildPrompt(GoodsQARequest request) {
        return buildEnhancedPrompt(request); // 使用增强版提示词
    }
    
    /**
     * 获取会话上下文
     * @param sessionId 会话ID
     * @return 会话上下文
     */
    private Map<String, Object> getSessionContext(String sessionId) {
        if (sessionId == null) {
            logger.warn("会话ID为空，返回空上下文");
            return new HashMap<>();
        }
        
        Map<String, Object> context = new HashMap<>();
        
        try {
            // 从会话管理器获取会话
            LLMSessionManager.Session session = sessionManager.getSession(sessionId);
            if (session == null) {
                logger.warn("会话不存在，创建新会话: {}", sessionId);
                // 创建新会话，userId设为默认值1（管理员）
                sessionManager.createSession(1, sessionId);
                // 重新获取会话
                session = sessionManager.getSession(sessionId);
                if (session == null) {
                    logger.error("创建会话失败，返回默认上下文: {}", sessionId);
                    context.put("sessionId", sessionId);
                    return context;
                }
            }
            
            // 获取已有的上下文数据
            Map<String, Object> existingContext = session.getContext();
            if (existingContext != null && !existingContext.isEmpty()) {
                context.putAll(existingContext);
            }
            
            // 构建基础会话信息
            BasicSessionInfo sessionInfo = new BasicSessionInfo(sessionId);
            
            // 从会话消息中提取查询历史
            List<LLMSessionManager.Message> messages = session.getMessages();
            for (LLMSessionManager.Message message : messages) {
                if ("user".equals(message.getType()) && message.getContent() != null) {
                    sessionInfo.addQuery(message.getContent());
                }
            }
            
            // 提取用户偏好
            Map<String, Object> preferences = new HashMap<>();
            if (sessionInfo.getPricePreference() != null) {
                preferences.put("price", sessionInfo.getPricePreference());
            }
            if (!sessionInfo.getPreferredCategories().isEmpty()) {
                preferences.put("category", sessionInfo.getPreferredCategories());
            }
            if (!sessionInfo.getPreferredBrands().isEmpty()) {
                preferences.put("brand", sessionInfo.getPreferredBrands());
            }
            
            if (!preferences.isEmpty()) {
                context.put("preferences", preferences);
            }
            
            context.put("queryCount", sessionInfo.getQueryCount());
            context.put("sessionId", sessionId);
            
            logger.debug("成功获取会话上下文: sessionId={}, queryCount={}, hasPreferences={}", 
                        sessionId, sessionInfo.getQueryCount(), !preferences.isEmpty());
            
        } catch (Exception e) {
            logger.error("获取会话上下文时发生异常：sessionId={}, error={}", sessionId, e.getMessage(), e);
            // 返回默认上下文而不是空map
            context.put("sessionId", sessionId);
            context.put("queryCount", 0);
        }
        
        return context;
    }
    
    /**
     * 判断LLM响应是否包含JSON查询
     * @param llmResponse LLM响应
     * @return 是否包含JSON查询
     */
    private boolean containsJSONQuery(String llmResponse) {
        if (llmResponse == null || llmResponse.trim().isEmpty()) {
            return false;
        }
        
        // 检查是否包含JSON格式的查询意图（包含query_type和conditions字段）
        // 同时检查是否包含"query_type"字段，这表示需要查询数据库
        return llmResponse.contains("\"query_type\"") && llmResponse.contains("\"conditions\"");
    }
    
    /**
     * 从LLM响应中解析查询意图
     * @param llmResponse LLM响应
     * @return 查询意图
     */
    private QueryIntent parseQueryIntentFromLLM(String llmResponse) {
        // 首先检查是否需要查询数据库
        if (!containsJSONQuery(llmResponse)) {
            // 如果LLM决定不需要查询数据库，返回空的查询意图
            QueryIntent intent = new QueryIntent();
            intent.setQueryType("no_query");
            intent.setConditions(new HashMap<>());
            return intent;
        }
        
        try {
            // 提取JSON部分
            String jsonPart = extractJSONFromLLMResponse(llmResponse);
            if (jsonPart != null && !jsonPart.isEmpty()) {
                return LLMOutputParser.parseLLMOutput(jsonPart);
            }
        } catch (Exception e) {
            logger.warn("从LLM响应解析查询意图失败，使用默认解析: {}", e.getMessage());
        }
        
        // 如果解析失败，使用原始问题解析
        return parseQueryIntent("");
    }
    
    /**
     * 从LLM响应中提取JSON部分
     * @param llmResponse LLM响应
     * @return JSON字符串
     */
    private String extractJSONFromLLMResponse(String llmResponse) {
        if (llmResponse == null) {
            return null;
        }
        
        // 查找JSON开始和结束位置
        int jsonStart = llmResponse.indexOf('{');
        int jsonEnd = llmResponse.lastIndexOf('}');
        
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return llmResponse.substring(jsonStart, jsonEnd + 1);
        }
        
        return null;
    }
    
    /**
     * 将会话上下文转换为字符串格式
     * @param sessionId 会话ID
     * @return 会话上下文字符串
     */
    private String getSessionContextAsString(String sessionId) {
        Map<String, Object> sessionContext = getSessionContext(sessionId);
        if (sessionContext == null || sessionContext.isEmpty()) {
            return "";
        }
        
        StringBuilder contextBuilder = new StringBuilder();
        
        // 会话基本信息
        String sessionIdStr = (String) sessionContext.get("sessionId");
        Integer queryCount = (Integer) sessionContext.get("queryCount");
        if (sessionIdStr != null) {
            contextBuilder.append("会话ID: ").append(sessionIdStr).append("\n");
        }
        if (queryCount != null && queryCount > 0) {
            contextBuilder.append("历史查询次数: ").append(queryCount).append("\n");
        }
        
        // 用户偏好
        Map<String, Object> preferences = (Map<String, Object>) sessionContext.get("preferences");
        if (preferences != null && !preferences.isEmpty()) {
            contextBuilder.append("用户偏好:\n");
            for (Map.Entry<String, Object> entry : preferences.entrySet()) {
                contextBuilder.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        
        // 会话历史消息
        LLMSessionManager.Session session = sessionManager.getSession(sessionId);
        if (session != null && !session.getMessages().isEmpty()) {
            contextBuilder.append("历史对话:\n");
            List<LLMSessionManager.Message> messages = session.getMessages();
            // 只显示最近5条消息，避免上下文过长
            int startIndex = Math.max(0, messages.size() - 5);
            for (int i = startIndex; i < messages.size(); i++) {
                LLMSessionManager.Message message = messages.get(i);
                contextBuilder.append(message.getType()).append(": ").append(message.getContent()).append("\n");
            }
        }
        
        return contextBuilder.toString();
    }
    
    /**
     * 从LLM响应中提取自然语言回答
     * @param llmResponse LLM响应
     * @return 自然语言回答
     */
    private String extractNaturalLanguageAnswer(String llmResponse) {
        if (llmResponse == null || llmResponse.trim().isEmpty()) {
            return "抱歉，我无法理解您的问题。";
        }
        
        // 检查是否包含JSON查询意图
        if (containsJSONQuery(llmResponse)) {
            // LLM决定需要查询数据库，提取JSON之前的自然语言解释
            int jsonStart = llmResponse.indexOf('{');
            
            // 检查是否有markdown代码块标记
            int codeBlockStart = llmResponse.indexOf("```json");
            if (codeBlockStart > 0 && codeBlockStart < jsonStart) {
                // 如果有代码块标记，提取代码块之前的部分
                String naturalAnswer = llmResponse.substring(0, codeBlockStart).trim();
                if (!naturalAnswer.isEmpty()) {
                    return naturalAnswer;
                }
            } else if (jsonStart > 0) {
                // 如果没有代码块标记但有JSON，提取JSON之前的部分
                String naturalAnswer = llmResponse.substring(0, jsonStart).trim();
                if (!naturalAnswer.isEmpty()) {
                    return naturalAnswer;
                }
            }
            
            // 如果没有自然语言解释，返回默认消息
            return "我理解您的问题，正在为您查询相关商品信息...";
        } else {
            // LLM决定不需要查询数据库，直接返回整个响应作为自然语言回答
            return llmResponse.trim();
        }
    }
    
    /**
     * 根据查询结果生成答案
     * @param request 问答请求
     * @param goodsList 商品列表
     * @param queryIntent 查询意图
     * @return 生成的答案
     */
    private String generateAnswerFromQueryResults(GoodsQARequest request, List<LitemallGoods> goodsList, QueryIntent queryIntent) {
        // 如果查询结果为空，提供更有帮助的响应
        if (goodsList == null || goodsList.isEmpty()) {
            StringBuilder helpfulResponse = new StringBuilder();
            helpfulResponse.append("根据您的查询条件，暂时没有找到完全匹配的商品。\n");
            helpfulResponse.append("建议您可以：\n");
            helpfulResponse.append("1. 放宽价格范围或搜索条件\n");
            helpfulResponse.append("2. 尝试不同的关键词搜索\n");
            helpfulResponse.append("3. 浏览其他商品分类\n");
            helpfulResponse.append("4. 或者告诉我您的具体需求，我可以为您推荐相关商品");
            return helpfulResponse.toString();
        }
        
        // 有查询结果时，复用原来的generateAnswer逻辑
        return generateAnswer(request, goodsList, queryIntent);
    }
    
    /**
     * 根据查询意图执行商品查询
     * @param queryIntent 查询意图
     * @return 商品列表
     */
    private List<LitemallGoods> executeQuery(QueryIntent queryIntent) {
        // 简化查询实现，直接使用goodsService进行基本查询
        List<LitemallGoods> goodsList = new ArrayList<>();
        
        // 根据查询类型进行不同的查询
        String queryType = queryIntent.getQueryType();
        Map<String, Object> conditions = queryIntent.getConditions();
        
        try {
            if ("price_range".equals(queryType) && conditions.containsKey("minPrice") && conditions.containsKey("maxPrice")) {
                // 价格范围查询 - 使用自定义的价格范围查询方法
                int minPrice = (int) conditions.get("minPrice");
                int maxPrice = (int) conditions.get("maxPrice");
                // LLM返回的价格是以分为单位，需要转换为元（除以100）
                BigDecimal minPriceYuan = new BigDecimal(minPrice).divide(new BigDecimal(100));
                BigDecimal maxPriceYuan = new BigDecimal(maxPrice).divide(new BigDecimal(100));
                // 使用自定义的价格范围查询方法
                goodsList = queryGoodsByPriceRange(minPriceYuan, maxPriceYuan, 0, 100);
            } else if ("category_filter".equals(queryType) && conditions.containsKey("categoryId")) {
                // 分类筛选
                Integer categoryId = (Integer) conditions.get("categoryId");
                goodsList = goodsService.queryByCategory(categoryId, 0, 100);
            } else if ("is_on_sale".equals(queryType) && conditions.containsKey("isOnSale")) {
                // 是否在售查询
                Boolean isOnSale = (Boolean) conditions.get("isOnSale");
                goodsList = goodsService.querySelective(null, null, null, null, isOnSale, 0, 100, null, null);
            } else if ("statistical_query".equals(queryType)) {
                // 统计查询 - 根据统计类型进行不同的统计查询
                String statisticType = (String) conditions.get("statistic_type");
                if ("category_stats".equals(statisticType)) {
                    // 分类统计：查询所有商品用于分类分析
                    goodsList = goodsService.querySelective(null, null, null, null, null, 0, 1000, null, null);
                } else {
                    // 其他统计类型，默认查询所有商品
                    goodsList = goodsService.querySelective(null, null, null, null, null, 0, 100, null, null);
                }
            } else {
                // 默认查询所有商品
                goodsList = goodsService.querySelective(null, null, null, null, null, 0, 100, null, null);
            }
        } catch (Exception e) {
            logger.warn("查询商品时发生异常，使用默认查询: {}", e.getMessage());
            goodsList = goodsService.querySelective(null, null, null, null, null, 0, 100, null, null);
        }
        
        // 对于非价格范围查询的结果，也需要过滤detail字段
        if (!"price_range".equals(queryType)) {
            for (LitemallGoods goods : goodsList) {
                if (goods.getDetail() != null && !goods.getDetail().trim().isEmpty()) {
                    String filteredDetail = goodsDetailFilter.filterDetail(goods.getDetail());
                    goods.setDetail(filteredDetail);
                }
            }
        }
        
        // 应用结果过滤
        return filterGoodsByPattern(goodsList, queryIntent);
    }
    
    /**
     * 根据价格范围查询商品
     * @param minPrice 最低价格（元）
     * @param maxPrice 最高价格（元）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 商品列表
     */
    private List<LitemallGoods> queryGoodsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Integer offset, Integer limit) {
        try {
            // 使用MyBatis Example进行价格范围查询
            LitemallGoodsExample example = new LitemallGoodsExample();
            LitemallGoodsExample.Criteria criteria = example.createCriteria();
            
            // 设置价格范围条件
            criteria.andRetailPriceGreaterThanOrEqualTo(minPrice);
            criteria.andRetailPriceLessThanOrEqualTo(maxPrice);
            criteria.andIsOnSaleEqualTo(true);
            criteria.andDeletedEqualTo(false);
            
            // 设置排序（按价格升序）
            example.setOrderByClause("retail_price ASC");
            
            // 执行查询（先不使用分页，直接查询所有符合条件的商品）
            List<LitemallGoods> allGoods = goodsMapper.selectByExampleWithBLOBs(example);
            
            // 手动分页
            int startIndex = Math.min(offset, allGoods.size());
            int endIndex = Math.min(startIndex + limit, allGoods.size());
            
            List<LitemallGoods> resultGoods = allGoods.subList(startIndex, endIndex);
            
            // 过滤每个商品的detail字段，移除HTML标签和图片URL
            for (LitemallGoods goods : resultGoods) {
                if (goods.getDetail() != null && !goods.getDetail().trim().isEmpty()) {
                    String filteredDetail = goodsDetailFilter.filterDetail(goods.getDetail());
                    goods.setDetail(filteredDetail);
                }
            }
            
            return resultGoods;
        } catch (Exception e) {
            logger.error("价格范围查询失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 将Map结果转换为LitemallGoods列表
     * @param results Map结果列表
     * @return LitemallGoods列表
     */
    private List<LitemallGoods> convertToGoodsList(List<Map<String, Object>> results) {
        // 这里需要根据实际的转换逻辑来实现
        // 暂时返回空列表，实际应该将Map转换为LitemallGoods对象
        return java.util.Collections.emptyList();
    }

    /**
     * 根据查询意图过滤结果
     * @param results 查询结果
     * @param queryIntent 查询意图
     * @return 过滤后的结果
     */
    private List<Map<String, Object>> filterResultsByPattern(List<Map<String, Object>> results, QueryIntent queryIntent) {
        if (results == null || results.isEmpty()) {
            return results;
        }
        
        Map<String, Object> conditions = queryIntent.getConditions();
        if (conditions == null || !conditions.containsKey("keyword")) {
            return results;
        }
        
        String keyword = (String) conditions.get("keyword");
        if (keyword == null || keyword.trim().isEmpty()) {
            return results;
        }
        
        List<Map<String, Object>> filteredResults = new ArrayList<>();
        
        for (Map<String, Object> result : results) {
            Object nameValue = result.get("name");
            if (nameValue == null) {
                continue;
            }
            
            String name = nameValue.toString().toLowerCase();
            if (name.contains(keyword.toLowerCase())) {
                filteredResults.add(result);
            }
        }
        
        return filteredResults;
    }
    
    /**
     * 生成答案
     * @param request 问答请求
     * @param goodsList 商品列表
     * @param queryIntent 查询意图
     * @return 答案
     */
    private String generateAnswer(GoodsQARequest request, List<LitemallGoods> goodsList, QueryIntent queryIntent) {
        if (goodsList == null || goodsList.isEmpty()) {
            return "抱歉，没有找到符合条件的商品。";
        }
        
        StringBuilder answer = new StringBuilder();
        
        // 获取会话上下文以个性化答案
        Map<String, Object> sessionContext = getSessionContext(request.getSessionId());
        Map<String, Object> preferences = sessionContext != null ? 
            (Map<String, Object>) sessionContext.get("preferences") : new HashMap<>();
        
        // 根据查询类型生成不同的答案开头
        String queryType = queryIntent.getQueryType();
        switch (queryType) {
            case "price_range":
                answer.append("根据您的价格要求，");
                if (queryIntent.getConditions().containsKey("minPrice") && queryIntent.getConditions().containsKey("maxPrice")) {
                    // 价格在数据库中存储为分，显示时需要转换为元
                    int minPrice = (int)queryIntent.getConditions().get("minPrice") / 100;
                    int maxPrice = (int)queryIntent.getConditions().get("maxPrice") / 100;
                    answer.append(String.format("在%d-%d元价格区间内，", minPrice, maxPrice));
                }
                break;
            case "product_search":
                answer.append("根据您的搜索要求，");
                break;
            case "stock_check":
                answer.append("关于库存情况，");
                break;
            case "statistical_query":
                answer.append("根据统计数据，");
                break;
            default:
                answer.append("根据您的查询，");
        }
        
        answer.append(String.format("为您找到 %d 个商品：\n\n", goodsList.size()));
        
        // 按用户偏好排序商品（如果有偏好）
        List<LitemallGoods> sortedGoods = new ArrayList<>(goodsList);
        if (!preferences.isEmpty()) {
            sortedGoods.sort((g1, g2) -> {
                // 简单的偏好排序逻辑
                int score1 = calculatePreferenceScore(g1, preferences);
                int score2 = calculatePreferenceScore(g2, preferences);
                return Integer.compare(score2, score1); // 降序
            });
        }
        
        // 显示商品详情
        for (int i = 0; i < Math.min(5, sortedGoods.size()); i++) {
            LitemallGoods goods = sortedGoods.get(i);
            answer.append(String.format("%d. %s\n", i + 1, goods.getName()));
            answer.append(String.format("   价格：¥%.2f\n", goods.getRetailPrice().doubleValue()));
            if (goods.getBrief() != null && !goods.getBrief().isEmpty()) {
                answer.append(String.format("   简介：%s\n", goods.getBrief()));
            }
            // 完全移除详情信息，不再显示detail字段
            answer.append("\n");
        }
        
        if (sortedGoods.size() > 5) {
            answer.append(String.format("... 还有 %d 个商品符合您的要求。\n", sortedGoods.size() - 5));
        }
        
        // 添加统计信息
        if (queryIntent.getQueryType().equals("statistical_query")) {
            double avgPrice = sortedGoods.stream()
                .mapToDouble(g -> g.getRetailPrice().doubleValue())
                .average()
                .orElse(0.0);
            answer.append(String.format("平均价格：¥%.2f\n", avgPrice));
        }
        
        // 个性化建议
        if (!preferences.isEmpty()) {
            answer.append("\n根据您的偏好，我为您优先推荐以上商品。");
        }
        
        // 后续提示
        if (queryIntent.getQueryType().equals("price_range")) {
            answer.append("\n\n您可以继续询问：");
            answer.append("\n- \"这些商品中哪些有现货？\"");
            answer.append("\n- \"哪个商品的评分最高？\"");
            answer.append("\n- \"给我推荐其中最热门的一个\"");
        }
        
        return answer.toString();
    }
    
    /**
     * 创建新的问答会话
     * @param userId 用户ID（可选）
     * @return 会话ID
     */
    public String createSession(Integer userId) {
        return sessionManager.createSession(userId != null ? userId : 1);
    }
    
    /**
     * 创建指定ID的问答会话（用于测试和修复）
     * @param userId 用户ID
     * @param sessionId 指定的会话ID
     * @return 会话ID
     */
    public String createSession(Integer userId, String sessionId) {
        return sessionManager.createSession(userId != null ? userId : 1, sessionId);
    }
    
    /**
     * 获取会话历史记录
     * @param sessionId 会话ID
     * @return 会话历史
     */
    public Object getSessionHistory(String sessionId) {
        LLMSessionManager.Session session = sessionManager.getSession(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在或已过期");
        }
        return session.getMessages();
    }
    
    /**
     * 销毁会话
     * @param sessionId 会话ID
     */
    public void destroySession(String sessionId) {
        sessionManager.destroySession(sessionId);
    }
    
    /**
     * 获取服务状态信息
     * @return 服务状态
     */
    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "running");
        status.put("llm_service", "healthy"); // 简化状态检查
        status.put("session_count", sessionManager.getSessionCount());
        return status;
    }
    
    /**
     * 获取会话统计信息
     * @return 会话统计
     */
    public Map<String, Object> getSessionStatistics() {
        return sessionManager.getStatistics();
    }
    
    /**
     * 创建错误响应
     * @param message 错误信息
     * @return 错误响应对象
     */
    private GoodsQAResponse createErrorResponse(String message) {
        GoodsQAResponse response = new GoodsQAResponse();
        response.setErrno(502);
        response.setErrmsg(message);
        response.setAnswer("抱歉，处理您的请求时出现错误。");
        response.setGoods(new ArrayList<>());
        return response;
    }
    
    /**
     * 计算商品偏好分数
     * @param goods 商品
     * @param preferences 用户偏好
     * @return 偏好分数
     */
    private int calculatePreferenceScore(LitemallGoods goods, Map<String, Object> preferences) {
        int score = 0;
        
        // 品牌偏好
        if (preferences.containsKey("preferred_brand")) {
            String preferredBrand = (String) preferences.get("preferred_brand");
            if (goods.getBrandId() != null && goods.getBrandId().equals(preferredBrand)) {
                score += 10;
            }
        }
        
        // 价格偏好
        if (preferences.containsKey("price_range")) {
            Map<String, Integer> priceRange = (Map<String, Integer>) preferences.get("price_range");
            int minPrice = priceRange.getOrDefault("min", 0);
            int maxPrice = priceRange.getOrDefault("max", Integer.MAX_VALUE);
            if (goods.getRetailPrice().compareTo(new BigDecimal(minPrice)) >= 0 && 
                goods.getRetailPrice().compareTo(new BigDecimal(maxPrice)) <= 0) {
                score += 5;
            }
        }
        
        // 关键词偏好
        if (preferences.containsKey("keywords")) {
            List<String> keywords = (List<String>) preferences.get("keywords");
            for (String keyword : keywords) {
                if (goods.getName().contains(keyword) || 
                    (goods.getKeywords() != null && goods.getKeywords().contains(keyword))) {
                    score += 3;
                }
            }
        }
        
        return score;
    }
    
    /**
     * 解析查询意图
     * @param question 用户问题
     * @return 查询意图对象
     */
    private QueryIntent parseQueryIntent(String question) {
        QueryIntent queryIntent = new QueryIntent();
        Map<String, Object> conditions = new HashMap<>();
        
        if (question == null || question.trim().isEmpty()) {
            queryIntent.setQueryType("default");
            queryIntent.setConditions(conditions);
            return queryIntent;
        }
        
        String lowerQuestion = question.toLowerCase();
        
        // 检测价格范围查询
        if (lowerQuestion.contains("价格") || lowerQuestion.contains("多少钱") || lowerQuestion.contains("元")) {
            queryIntent.setQueryType("price_range");
            
            // 改进价格范围提取逻辑
            // 匹配"50到100元"、"50-100元"、"50至100元"等格式
            Pattern pricePattern = Pattern.compile("(\\d+)[\\s]*[到至-][\\s]*(\\d+)\\s*元");
            Matcher matcher = pricePattern.matcher(question);
            if (matcher.find()) {
                int minPrice = Integer.parseInt(matcher.group(1)); // 直接使用元为单位
                int maxPrice = Integer.parseInt(matcher.group(2));
                
                // 确保minPrice小于maxPrice
                if (minPrice > maxPrice) {
                    int temp = minPrice;
                    minPrice = maxPrice;
                    maxPrice = temp;
                }
                
                conditions.put("minPrice", minPrice);
                conditions.put("maxPrice", maxPrice);
            } else {
                // 尝试匹配单个价格范围，如"100元以下的商品"、"200元以上的商品"
                Pattern singlePricePattern = Pattern.compile("(\\d+)\\s*元\\s*(以下|以上|以内|以外)");
                Matcher singleMatcher = singlePricePattern.matcher(question);
                if (singleMatcher.find()) {
                    int price = Integer.parseInt(singleMatcher.group(1));
                    String direction = singleMatcher.group(2);
                    
                    if ("以下".equals(direction) || "以内".equals(direction)) {
                        conditions.put("minPrice", 0);
                        conditions.put("maxPrice", price);
                    } else if ("以上".equals(direction) || "以外".equals(direction)) {
                        conditions.put("minPrice", price);
                        conditions.put("maxPrice", Integer.MAX_VALUE);
                    }
                }
            }
        }
        
        // 检测关键词搜索
        else if (lowerQuestion.contains("搜索") || lowerQuestion.contains("查找") || lowerQuestion.contains("找")) {
            queryIntent.setQueryType("product_search");
            
            // 提取关键词
            String keyword = question.replaceAll("(?i)(搜索|查找|找)", "").trim();
            if (!keyword.isEmpty()) {
                conditions.put("keyword", keyword);
            }
        }
        
        // 检测分类查询
        else if (lowerQuestion.contains("分类") || lowerQuestion.contains("类别")) {
            queryIntent.setQueryType("category_query");
            
            // 提取分类信息
            Pattern categoryPattern = Pattern.compile("(?i)(分类|类别)[：:]?\\s*(\\S+)");
            Matcher categoryMatcher = categoryPattern.matcher(question);
            if (categoryMatcher.find()) {
                conditions.put("category", categoryMatcher.group(2));
            }
        }
        
        // 检测库存查询
        else if (lowerQuestion.contains("库存") || lowerQuestion.contains("现货") || lowerQuestion.contains("有货")) {
            queryIntent.setQueryType("stock_check");
            conditions.put("inStock", true);
        }
        
        // 检测统计查询
        else if (lowerQuestion.contains("统计") || lowerQuestion.contains("总数") || lowerQuestion.contains("平均")) {
            queryIntent.setQueryType("statistical_query");
        }
        
        // 默认查询
        else {
            queryIntent.setQueryType("default");
            // 将整个问题作为关键词
            conditions.put("keyword", question);
        }
        
        queryIntent.setConditions(conditions);
        return queryIntent;
    }
    
    /**
     * 过滤商品列表
     * @param goodsList 商品列表
     * @param queryIntent 查询意图
     * @return 过滤后的商品列表
     */
    private List<LitemallGoods> filterGoodsByPattern(List<LitemallGoods> goodsList, QueryIntent queryIntent) {
        if (goodsList == null || goodsList.isEmpty()) {
            return goodsList;
        }
        
        // 如果有名称模式匹配，应用过滤
        if (queryIntent.getConditions().containsKey("namePattern")) {
            String namePattern = (String) queryIntent.getConditions().get("namePattern");
            if (namePattern != null && !namePattern.trim().isEmpty()) {
                try {
                    Pattern pattern = Pattern.compile(namePattern, Pattern.CASE_INSENSITIVE);
                    goodsList = goodsList.stream()
                        .filter(goods -> pattern.matcher(goods.getName()).find())
                        .collect(Collectors.toList());
                } catch (PatternSyntaxException e) {
                    logger.warn("名称模式语法错误: {}", e.getMessage());
                }
            }
        }
        
        return goodsList;
    }
}