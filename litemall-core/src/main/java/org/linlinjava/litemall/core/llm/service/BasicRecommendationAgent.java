package org.linlinjava.litemall.core.llm.service;

import org.linlinjava.litemall.core.llm.model.BasicSessionInfo;
import org.linlinjava.litemall.core.llm.model.QueryIntent;
import org.linlinjava.litemall.db.domain.LitemallGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import org.linlinjava.litemall.core.llm.service.IntelligentQuantityAdvisor;
import org.linlinjava.litemall.core.llm.service.LLMSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 简化版商品建议Agent
 * 基于会话上下文和查询意图提供个性化商品建议
 */
@Service
public class BasicRecommendationAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(BasicRecommendationAgent.class);
    
    @Autowired
    private LLMSessionManager sessionManager;
    
    @Autowired
    private GoodsQueryService goodsQueryService;
    
    /**
     * 处理用户问题，生成个性化商品建议
     * @param sessionId 会话ID
     * @param question 用户问题
     * @param intent 查询意图
     * @return 个性化回答和商品列表
     */
    public Map<String, Object> processQuestion(String sessionId, String question, QueryIntent intent) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 获取会话上下文
            BasicSessionInfo sessionInfo = getSessionInfo(sessionId);
            
            // 2. 更新会话信息
            sessionInfo.addQuery(question);
            sessionInfo.extractPreferences(question);
            
            // 3. 智能数量建议
            int suggestedQuantity = IntelligentQuantityAdvisor.suggestQuantity(
                intent.getQueryType(), question, null, null).getFinalQuantity();
            
            // 4. 个性化偏好增强
            logger.debug("会话 {} 的查询意图为：{}", sessionId, intent);
            enhanceIntentWithPreferences(intent, sessionInfo);
            
            // 5. 检查是否需要重新查询
            if (intent.isNeedsRequery()) {
                // 基于重新查询条件构建新查询
                QueryIntent newIntent = buildRequeryIntent(intent, sessionInfo);
                result.put("requeryIntent", newIntent);
                result.put("requeryReason", intent.getRequeryConditions());
            }
            
            // 6. 执行商品查询（原始逻辑）
            List<Map<String, Object>> goodsList = executeQuery(intent, suggestedQuantity);
            
            // 7. 生成个性化回答
            String personalizedAnswer = generatePersonalizedAnswer(
                question, goodsList, sessionInfo, suggestedQuantity);
            
            // 8. 更新会话上下文
            updateSessionContext(sessionId, sessionInfo, question, personalizedAnswer);
            
            // 9. 构建结果
            result.put("answer", personalizedAnswer);
            result.put("goodsList", goodsList);
            result.put("quantityUsed", suggestedQuantity);
            result.put("quantityExplanation", IntelligentQuantityAdvisor.getQuantityExplanation(
                suggestedQuantity, intent.getQueryType()));
            result.put("sessionInfo", sessionInfo);
            result.put("sessionId", sessionId);
            
        } catch (Exception e) {
            result.put("error", "处理问题时发生错误: " + e.getMessage());
            result.put("answer", "抱歉，处理您的请求时出现了问题。");
            result.put("goodsList", new ArrayList<>());
        }
        
        return result;
    }
    
    /**
     * 获取会话信息
     */
    private BasicSessionInfo getSessionInfo(String sessionId) {
        if (sessionId == null) {
            return new BasicSessionInfo("temp_session");
        }
        
        // 从会话管理器获取或创建会话信息
        LLMSessionManager.Session session = sessionManager.getSession(sessionId);
        if (session == null) {
            return new BasicSessionInfo(sessionId);
        }
        
        // 从会话中提取基本信息
        BasicSessionInfo sessionInfo = new BasicSessionInfo(sessionId);
        
        // 从会话消息中提取最近的查询
        List<LLMSessionManager.Message> messages = session.getMessages();
        for (LLMSessionManager.Message message : messages) {
            if ("user".equals(message.getType()) && message.getContent() != null) {
                sessionInfo.addQuery(message.getContent());
            }
        }
        
        return sessionInfo;
    }
    
    /**
     * 使用会话偏好增强查询意图
     */
    private void enhanceIntentWithPreferences(QueryIntent intent, BasicSessionInfo sessionInfo) {
        // 如果查询没有指定价格范围，但用户有价格偏好，添加价格条件
        if (!intent.getConditions().containsKey("min_price") && 
            !intent.getConditions().containsKey("max_price") &&
            sessionInfo.getPricePreference() != null && !sessionInfo.getPricePreference().equals("中")) {
            
            // 根据价格偏好设置价格范围
            String priceLevel = (String) sessionInfo.getPricePreference();
            if (priceLevel.equals("低")) {
                intent.withCondition("max_price", 500); // 低价位
            } else if (priceLevel.equals("高")) {
                intent.withCondition("min_price", 1000); // 高价位
            }
        }
        
        // 如果查询没有指定类目，但用户有类目偏好，添加类目条件
        if (!intent.getConditions().containsKey("category") &&
            !sessionInfo.getPreferredCategories().isEmpty()) {
            // 使用第一个偏好的类目
            intent.withCondition("category", sessionInfo.getPreferredCategories().iterator().next());
        }
        
        // 如果查询没有指定品牌，但用户有品牌偏好，添加品牌条件
        if (!intent.getConditions().containsKey("brand") &&
            !sessionInfo.getPreferredBrands().isEmpty()) {
            // 使用第一个偏好的品牌
            intent.withCondition("brand", sessionInfo.getPreferredBrands().iterator().next());
        }
    }
    
    /**
     * 构建重新查询意图
     */
    private QueryIntent buildRequeryIntent(QueryIntent originalIntent, BasicSessionInfo sessionInfo) {
        QueryIntent newIntent = new QueryIntent();
        newIntent.setQueryType(originalIntent.getQueryType());
        newIntent.setConditions(new HashMap<>(originalIntent.getConditions()));
        newIntent.setSort(originalIntent.getSort());
        
        // 根据重新查询条件调整查询
        String requeryConditions = originalIntent.getRequeryConditions();
        if (requeryConditions != null) {
            if (requeryConditions.contains("价格范围")) {
                // 扩大价格范围
                if (newIntent.getConditions().containsKey("min_price")) {
                    int minPrice = (int) newIntent.getConditions().get("min_price");
                    newIntent.withCondition("min_price", Math.max(0, minPrice - 100));
                }
                if (newIntent.getConditions().containsKey("max_price")) {
                    int maxPrice = (int) newIntent.getConditions().get("max_price");
                    newIntent.withCondition("max_price", maxPrice + 200);
                }
            }
            
            if (requeryConditions.contains("类目")) {
                // 移除类目限制
                newIntent.getConditions().remove("category");
            }
            
            if (requeryConditions.contains("品牌")) {
                // 移除品牌限制
                newIntent.getConditions().remove("brand");
            }
        }
        
        return newIntent;
    }
    
    /**
     * 执行商品查询
     */
    private List<Map<String, Object>> executeQuery(QueryIntent intent, int suggestedQuantity) {
        // 设置建议的数量限制
        intent.setLimit(suggestedQuantity);
        
        // 调用商品查询服务
        List<Map<String, Object>> goodsList = goodsQueryService.executeQuery(intent);
        
        // 如果结果太少且不是已经重新查询过，尝试放宽条件
        if (goodsList.size() < suggestedQuantity * 0.3 && !intent.isNeedsRequery()) {
            QueryIntent relaxedIntent = relaxQueryConditions(intent);
            List<Map<String, Object>> relaxedResults = goodsQueryService.executeQuery(relaxedIntent);
            
            // 合并结果，避免重复
            Set<Object> existingIds = new HashSet<>();
            for (Map<String, Object> goods : goodsList) {
                if (goods.containsKey("id")) {
                    existingIds.add(goods.get("id"));
                }
            }
            
            for (Map<String, Object> goods : relaxedResults) {
                if (goods.containsKey("id") && !existingIds.contains(goods.get("id"))) {
                    goodsList.add(goods);
                }
            }
        }
        
        return goodsList;
    }
    
    /**
     * 放宽查询条件
     */
    private QueryIntent relaxQueryConditions(QueryIntent intent) {
        QueryIntent relaxed = new QueryIntent();
        relaxed.setQueryType(intent.getQueryType());
        relaxed.setConditions(new HashMap<>(intent.getConditions()));
        relaxed.setSort(intent.getSort());
        relaxed.setLimit(intent.getLimit() * 2); // 增加数量限制
        
        // 放宽价格范围
        if (relaxed.getConditions().containsKey("min_price")) {
            int minPrice = (int) relaxed.getConditions().get("min_price");
            relaxed.withCondition("min_price", Math.max(0, minPrice - 100));
        }
        if (relaxed.getConditions().containsKey("max_price")) {
            int maxPrice = (int) relaxed.getConditions().get("max_price");
            relaxed.withCondition("max_price", maxPrice + 200);
        }
        
        return relaxed;
    }
    
    /**
     * 生成个性化回答
     */
    private String generatePersonalizedAnswer(String question, List<Map<String, Object>> goodsList, 
                                             BasicSessionInfo sessionInfo, int suggestedQuantity) {
        StringBuilder answer = new StringBuilder();
        
        // 如果没有找到商品
        if (goodsList == null || goodsList.isEmpty()) {
            return "抱歉，没有找到符合条件的商品。您可以尝试调整查询条件。";
        }
        
        // 个性化问候语
        answer.append("您好！根据您的偏好，我为您推荐以下商品：\n\n");
        
        // 显示推荐商品
        for (int i = 0; i < Math.min(goodsList.size(), suggestedQuantity); i++) {
            Map<String, Object> goods = goodsList.get(i);
            String name = goods.getOrDefault("name", "未知商品").toString();
            answer.append(i + 1).append(". ").append(name);
            
            if (goods.containsKey("retail_price")) {
                Object priceObj = goods.get("retail_price");
                if (priceObj instanceof Number) {
                    answer.append(" - ¥").append(((Number) priceObj).doubleValue() / 100.0);
                } else {
                    answer.append(" - ¥").append(priceObj);
                }
            }
            
            if (goods.containsKey("brief")) {
                String brief = goods.getOrDefault("brief", "").toString();
                if (!brief.isEmpty()) {
                    answer.append("\n   简介：").append(brief);
                }
            }
            answer.append("\n");
        }
        
        // 添加个性化推荐理由
        if (sessionInfo.getPricePreference() != null && !sessionInfo.getPricePreference().equals("中")) {
            answer.append("\n💡 特别为您选择了符合您价格偏好的商品。");
        }
        
        if (!sessionInfo.getPreferredCategories().isEmpty()) {
            answer.append("\n💡 根据您喜欢的类目进行了筛选。");
        }
        
        if (!sessionInfo.getPreferredBrands().isEmpty()) {
            answer.append("\n💡 优先推荐了您偏爱的品牌。");
        }
        
        answer.append("\n如果您需要调整推荐条件或查看更多商品，请告诉我！");
        
        return answer.toString();
    }
    
    /**
     * 更新会话上下文
     */
    private void updateSessionContext(String sessionId, BasicSessionInfo sessionInfo, 
                                    String question, String answer) {
        if (sessionId != null && !"temp_session".equals(sessionId)) {
            // 添加用户消息
            sessionManager.addSessionMessage(sessionId, question, "user");
            
            // 添加助手消息
            sessionManager.addSessionMessage(sessionId, answer, "assistant");
            
            // 更新会话上下文
            Map<String, Object> context = new HashMap<>();
            context.put("preferences", Map.of(
                "price", sessionInfo.getPricePreference(),
                "category", sessionInfo.getPreferredCategories(),
                "brand", sessionInfo.getPreferredBrands()
            ));
            context.put("queryCount", sessionInfo.getQueryCount());
            
            sessionManager.updateSessionContext(sessionId, context);
        }
    }
}