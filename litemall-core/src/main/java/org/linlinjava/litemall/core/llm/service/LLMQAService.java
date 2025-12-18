package org.linlinjava.litemall.core.llm.service;

import org.linlinjava.litemall.core.llm.exception.LLMOutputParseException;
import org.linlinjava.litemall.core.llm.exception.LLMServiceException;
import org.linlinjava.litemall.core.llm.model.*;
import org.linlinjava.litemall.core.llm.parser.*;
import org.linlinjava.litemall.db.domain.LitemallGoods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * LLM问答服务
 * 负责协调整个问答流程
 */
@Service
public class LLMQAService {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMQAService.class);
    
    @Autowired
    private LLMOutputParser llmOutputParser;
    
    @Autowired
    private QueryIntentBuilder queryIntentBuilder;
    
    @Autowired
    private Qwen3Service qwen3Service;
    
    @Autowired
    private GoodsQueryService goodsQueryService;
    
    @Autowired
    private LLMSessionManager sessionManager;
    
    @Autowired
    private BasicRecommendationAgent recommendationAgent;
    
    /**
     * 处理商品问答请求
     * @param request 问答请求
     * @return 问答响应
     */
    public GoodsQAResponse processQuestion(GoodsQARequest request) {
        System.out.println("=== LLMQAService 开始处理商品问答请求 ===");
        System.out.println("问题：" + request.getQuestion());
        System.out.println("会话ID：" + request.getSessionId());
        
        logger.info("开始处理商品问答请求，问题：{}，会话ID：{}", request.getQuestion(), request.getSessionId());
        
        try {
            // 统计查询时间
            long startTime = System.currentTimeMillis();
            
            // 1. 验证请求
            validateRequest(request);
            System.out.println("请求验证通过");
            
            // 2. 构建提示词（增强版，支持重新查询判断）
            String prompt = buildEnhancedPrompt(request);
            System.out.println("构建的增强提示词：" + prompt);
            
            // 3. 调用大模型
            System.out.println("准备调用Qwen3Service...");
            String llmResponse = qwen3Service.callLLM(prompt);
            System.out.println("Qwen3Service返回结果：" + llmResponse);
            logger.debug("LLM响应：{}", llmResponse);
            
            // 4. 解析LLM输出（增强版，支持重新查询字段）
            QueryIntent queryIntent = llmOutputParser.parseQueryIntent(llmResponse);
            System.out.println("解析后的查询意图：" + queryIntent);
            logger.debug("解析后的查询意图：{}", queryIntent);
            
            // 5. 验证查询意图
            if (!queryIntentBuilder.validateQueryIntent(queryIntent)) {
                System.err.println("查询意图验证失败：" + queryIntent);
                logger.error("查询意图验证失败：{}", queryIntent);
                return GoodsQAResponse.serverError("查询意图验证失败");
            }
            
            System.out.println("查询意图验证通过");
            
            // 6. 使用BasicRecommendationAgent处理（集成智能数量控制和个性化）
            System.out.println("使用BasicRecommendationAgent处理...");
            Map<String, Object> agentResult = recommendationAgent.processQuestion(
                request.getSessionId(), request.getQuestion(), queryIntent);
            
            // 7. 提取结果
            String answer = (String) agentResult.get("answer");
            List<LitemallGoods> goodsList = (List<LitemallGoods>) agentResult.get("goodsList");
            Integer suggestedQuantity = (Integer) agentResult.get("quantityUsed");
            String quantityExplanation = (String) agentResult.get("quantityExplanation");
            
            System.out.println("Agent处理完成，商品数量：" + goodsList.size());
            System.out.println("建议数量：" + suggestedQuantity);
            System.out.println("数量说明：" + quantityExplanation);
            
            // 8. 构建响应
            GoodsQAResponse response = GoodsQAResponse.success(answer, goodsList);
            response.setSessionId(request.getSessionId());
            response.setQueryTime(System.currentTimeMillis());
            response.setQueryIntent(queryIntent);
            
            // 添加数量控制信息到响应中
            response.setQueryTime(System.currentTimeMillis() - startTime);
            // Note: addMetadata method not available in GoodsQAResponse
            
            System.out.println("=== LLMQAService 处理完成 ===");
            logger.info("商品问答请求处理完成，问题：{}，结果数量：{}，建议数量：{}", 
                       request.getQuestion(), goodsList.size(), suggestedQuantity);
            return response;
            
        } catch (LLMOutputParseException e) {
            System.err.println("LLM输出解析失败：" + e.getMessage());
            logger.error("LLM输出解析失败：{}", e.getMessage());
            return GoodsQAResponse.serverError("无法解析您的查询意图，请重新表述");
        } catch (LLMServiceException e) {
            System.err.println("LLM服务调用失败：" + e.getMessage());
            logger.error("LLM服务调用失败：{}", e.getMessage());
            return GoodsQAResponse.serverError("AI服务暂时不可用，请稍后再试");
        } catch (Exception e) {
            System.err.println("处理商品问答请求时发生异常：" + e.getMessage());
            e.printStackTrace();
            logger.error("处理商品问答请求时发生异常", e);
            return GoodsQAResponse.serverError("处理请求时发生错误");
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
     * 构建增强版提示词，支持重新查询判断
     * @param request 问答请求
     * @return 增强提示词
     */
    private String buildEnhancedPrompt(GoodsQARequest request) {
        StringBuilder prompt = new StringBuilder();
        
        // 获取会话上下文
        Map<String, Object> sessionContext = getSessionContext(request.getSessionId());
        
        prompt.append("你是一个智能商品推荐助手，请分析用户问题并生成查询意图。\n\n");
        
        // 会话上下文
        if (sessionContext != null && !sessionContext.isEmpty()) {
            prompt.append("会话上下文信息：\n");
            if (sessionContext.containsKey("preferences")) {
                prompt.append("用户偏好：").append(sessionContext.get("preferences")).append("\n");
            }
            if (sessionContext.containsKey("queryCount")) {
                prompt.append("查询次数：").append(sessionContext.get("queryCount")).append("\n");
            }
            prompt.append("\n");
        }
        
        // 分析步骤
        prompt.append("请按以下步骤分析：\n");
        prompt.append("1. 理解用户查询意图\n");
        prompt.append("2. 提取查询条件和参数\n");
        prompt.append("3. 确定查询类型和排序方式\n");
        prompt.append("4. 设置合适的数量限制\n");
        prompt.append("5. 判断是否需要重新查询（如结果可能太少或条件太严格）\n");
        prompt.append("6. 如果需要重新查询，说明重新查询的条件\n\n");
        
        // 输出格式
        prompt.append("输出JSON格式：\n");
        prompt.append("{\n");
        prompt.append("  \"query_type\": \"price_range|keyword_search|category_filter|recommendation\",\n");
        prompt.append("  \"conditions\": {\n");
        prompt.append("    \"min_price\": 最小价格（分）,\n");
        prompt.append("    \"max_price\": 最大价格（分）,\n");
        prompt.append("    \"keyword\": \"搜索关键词\",\n");
        prompt.append("    \"category_id\": 分类ID,\n");
        prompt.append("    \"brand\": \"品牌\"\n");
        prompt.append("  },\n");
        prompt.append("  \"sort\": \"retail_price|number|add_time\",\n");
        prompt.append("  \"limit\": 建议数量,\n");
        prompt.append("  \"needs_requery\": false|true,\n");
        prompt.append("  \"requery_conditions\": \"重新查询条件说明\"\n");
        prompt.append("}\n\n");
        
        // 查询类型说明
        prompt.append("查询类型说明：\n");
        prompt.append("- price_range: 价格范围查询\n");
        prompt.append("- keyword_search: 关键词搜索\n");
        prompt.append("- category_filter: 分类筛选\n");
        prompt.append("- recommendation: 个性化推荐\n\n");
        
        // 智能数量建议规则
        prompt.append("数量建议规则：\n");
        prompt.append("- 特定商品查询：3-8个\n");
        prompt.append("- 价格范围查询：15-25个\n");
        prompt.append("- 推荐类查询：20-30个\n");
        prompt.append("- 类目浏览：30-50个\n");
        prompt.append("- 关键词搜索：15-25个\n");
        prompt.append("- 如果用户说\"最好的\"、\"推荐\"等，减少数量到5-10个\n");
        prompt.append("- 如果用户说\"所有\"、\"列表\"等，增加数量到30-50个\n\n");
        
        // 重新查询判断规则
        prompt.append("重新查询判断：\n");
        prompt.append("- 价格范围太窄（如只相差几十元）\n");
        prompt.append("- 同时限制了太多条件（价格+类目+品牌+关键词）\n");
        prompt.append("- 查询的是特定商品但可能不存在\n");
        prompt.append("- 如果判断需要重新查询，设置needs_requery为true，并说明原因\n\n");
        
        // 用户问题
        prompt.append("用户问题：").append(request.getQuestion()).append("\n");
        
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
            return new HashMap<>();
        }
        
        Map<String, Object> context = new HashMap<>();
        
        try {
            // 从会话管理器获取会话
            LLMSessionManager.Session session = sessionManager.getSession(sessionId);
            if (session != null) {
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
            }
        } catch (Exception e) {
            logger.warn("获取会话上下文时发生异常：{}", e.getMessage());
        }
        
        return context;
    }
    
    /**
     * 执行查询
     * @param queryIntent 查询意图
     * @return 查询结果
     */
    private List<Map<String, Object>> executeQuery(QueryIntent queryIntent) {
        // 统计查询
        if ("statistical".equals(queryIntent.getQueryType())) {
            return goodsQueryService.executeStatisticalQuery(queryIntent);
        }
        
        // 普通查询
        List<Map<String, Object>> results = goodsQueryService.executeQuery(queryIntent);
        
        // 应用结果过滤（主要针对名称模式匹配）
        return filterResultsByPattern(results, queryIntent);
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
     * 生成回答
     * @param queryIntent 查询意图
     * @param results 查询结果
     * @return 回答文本
     */
    private String generateAnswer(QueryIntent queryIntent, List<Map<String, Object>> results) {
        if (results == null || results.isEmpty()) {
            return "没有找到符合条件的商品。";
        }
        
        StringBuilder answer = new StringBuilder();
        answer.append("找到 ").append(results.size()).append(" 个商品：\n\n");
        
        // 显示前5个商品
        int displayCount = Math.min(results.size(), 5);
        for (int i = 0; i < displayCount; i++) {
            Map<String, Object> goods = results.get(i);
            answer.append(i + 1).append(". ");
            answer.append(goods.getOrDefault("name", "未知商品"));
            
            if (goods.containsKey("retail_price")) {
                answer.append(" - 价格：¥").append(goods.get("retail_price"));
            }
            
            if (goods.containsKey("number")) {
                answer.append(" - 库存：").append(goods.get("number")).append("件");
            }
            
            answer.append("\n");
        }
        
        if (results.size() > displayCount) {
            answer.append("\n... 还有 ").append(results.size() - displayCount).append(" 个商品");
        }
        
        return answer.toString();
    }
    
    /**
     * 创建新的问答会话
     * @param userId 用户ID（可选）
     * @return 会话ID
     */
    public String createSession(Integer userId) {
        return sessionManager.createSession(userId);
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
        status.put("llm_service", qwen3Service.healthCheck() ? "healthy" : "unhealthy");
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
}