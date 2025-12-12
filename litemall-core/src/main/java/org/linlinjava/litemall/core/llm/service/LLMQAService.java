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
            // 1. 验证请求
            validateRequest(request);
            System.out.println("请求验证通过");
            
            // 2. 构建提示词
            String prompt = buildPrompt(request);
            System.out.println("构建的提示词：" + prompt);
            
            // 3. 调用大模型
            System.out.println("准备调用Qwen3Service...");
            String llmResponse = qwen3Service.callLLM(prompt);
            System.out.println("Qwen3Service返回结果：" + llmResponse);
            logger.debug("LLM响应：{}", llmResponse);
            
            // 4. 解析LLM输出
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
            
            // 6. 执行查询
            System.out.println("开始执行查询...");
            List<Map<String, Object>> results = executeQuery(queryIntent);
            System.out.println("查询完成，结果数量：" + results.size());
            
            // 7. 生成回答
            System.out.println("生成回答...");
            String answer = generateAnswer(queryIntent, results);
            System.out.println("生成的回答：" + answer);
            
            // 8. 构建响应
            List<LitemallGoods> goodsList = convertToGoodsList(results);
            GoodsQAResponse response = GoodsQAResponse.success(answer, goodsList);
            response.setSessionId(request.getSessionId());
            response.setQueryTime(System.currentTimeMillis());
            response.setQueryIntent(queryIntent);
            
            // 9. 更新会话
            if (request.getSessionId() != null) {
                Map<String, Object> context = new HashMap<>();
                context.put("question", request.getQuestion());
                context.put("answer", answer);
                context.put("timestamp", LocalDateTime.now());
                sessionManager.updateSessionContext(request.getSessionId(), context);
            }
            
            System.out.println("=== LLMQAService 处理完成 ===");
            logger.info("商品问答请求处理完成，问题：{}，结果数量：{}", request.getQuestion(), results.size());
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
     * 构建提示词
     * @param request 问答请求
     * @return 提示词
     */
    private String buildPrompt(GoodsQARequest request) {
        StringBuilder prompt = new StringBuilder();
        
        // 简化后的提示词
        prompt.append("根据用户问题生成商品查询意图，输出JSON格式：\n");
        prompt.append("{\n");
        prompt.append("  \"query_type\": \"price_range|keyword_search|category_filter\",\n");
        prompt.append("  \"conditions\": {条件对象},\n");
        prompt.append("  \"sort\": \"排序字段\",\n");
        prompt.append("  \"limit\": 10\n");
        prompt.append("}\n\n");
        
        // 简化查询类型说明
        prompt.append("查询类型：price_range(价格范围), keyword_search(关键词), category_filter(分类)\n");
        
        // 简化条件说明
        prompt.append("条件字段：min_price, max_price, keyword, category_id\n");
        
        // 简化排序说明
        prompt.append("排序字段：retail_price, number, add_time\n");
        
        // 用户问题
        prompt.append("用户问题：").append(request.getQuestion()).append("\n");
        
        return prompt.toString();
    }
    
    /**
     * 获取会话上下文
     * @param sessionId 会话ID
     * @return 会话上下文
     */
    private Map<String, Object> getSessionContext(String sessionId) {
        // 暂时返回空上下文，后续可以实现从数据库或缓存获取
        return new HashMap<>();
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