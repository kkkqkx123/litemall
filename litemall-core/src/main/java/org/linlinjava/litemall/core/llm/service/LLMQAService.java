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
    private SQLBuilder sqlBuilder;
    
    @Autowired
    private ParameterBinder parameterBinder;
    
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
        logger.info("开始处理商品问答请求，问题：{}，会话ID：{}", request.getQuestion(), request.getSessionId());
        
        try {
            // 1. 验证请求
            validateRequest(request);
            
            // 2. 构建提示词
            String prompt = buildPrompt(request);
            
            // 3. 调用大模型
            String llmResponse = qwen3Service.callLLM(prompt);
            logger.debug("LLM响应：{}", llmResponse);
            
            // 4. 解析LLM输出
            QueryIntent queryIntent = llmOutputParser.parseQueryIntent(llmResponse);
            logger.debug("解析后的查询意图：{}", queryIntent);
            
            // 5. 验证查询意图
            if (!queryIntentBuilder.validateQueryIntent(queryIntent)) {
                logger.error("查询意图验证失败：{}", queryIntent);
                return GoodsQAResponse.serverError("查询意图验证失败");
            }
            
            // 6. 执行查询
            List<Map<String, Object>> results = executeQuery(queryIntent);
            
            // 7. 生成回答
            String answer = generateAnswer(queryIntent, results);
            
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
            
            logger.info("商品问答请求处理完成，问题：{}，结果数量：{}", request.getQuestion(), results.size());
            return response;
            
        } catch (LLMOutputParseException e) {
            logger.error("LLM输出解析失败：{}", e.getMessage());
            return GoodsQAResponse.serverError("无法解析您的查询意图，请重新表述");
        } catch (LLMServiceException e) {
            logger.error("LLM服务调用失败：{}", e.getMessage());
            return GoodsQAResponse.serverError("AI服务暂时不可用，请稍后再试");
        } catch (Exception e) {
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
        
        // 基础提示词
        prompt.append("你是一个商品问答助手，请根据用户的问题生成合适的商品查询意图。\n");
        prompt.append("请严格按照以下JSON格式输出，不要包含其他内容：\n");
        prompt.append("{\n");
        prompt.append("  \"query_type\": \"查询类型\",\n");
        prompt.append("  \"conditions\": {\n");
        prompt.append("    \"条件名\": \"条件值\"\n");
        prompt.append("  },\n");
        prompt.append("  \"sort\": \"排序方式\",\n");
        prompt.append("  \"limit\": 数量\n");
        prompt.append("}\n\n");
        
        // 商品数据库表结构
        prompt.append("商品数据库表结构：\n");
        prompt.append("表名：litemall_goods\n");
        prompt.append("字段：id, name, category_id, brand_id, brief, keywords, is_on_sale, pic_url, gallery, \n");
        prompt.append("       retail_price, number, detail, sort_order, add_time, update_time, deleted\n\n");
        
        // 查询类型说明
        prompt.append("支持的查询类型：\n");
        prompt.append("- price_range: 价格范围查询\n");
        prompt.append("- stock_check: 库存查询\n");
        prompt.append("- category_filter: 分类筛选\n");
        prompt.append("- keyword_search: 关键词搜索\n");
        prompt.append("- name_pattern: 名称模式匹配\n");
        prompt.append("- specific_product: 特定商品查询\n");
        prompt.append("- statistical: 统计查询\n\n");
        
        // 条件说明
        prompt.append("条件说明：\n");
        prompt.append("- min_price: 最低价格\n");
        prompt.append("- max_price: 最高价格\n");
        prompt.append("- min_number: 最低库存\n");
        prompt.append("- max_number: 最高库存\n");
        prompt.append("- is_on_sale: 是否在售（1表示在售）\n");
        prompt.append("- keyword: 搜索关键词（会在name, keywords, brief字段中搜索）\n");
        prompt.append("- name: 名称模式匹配（包含pattern, mode, case_sensitive字段）\n");
        prompt.append("- statistic_type: 统计类型（total_count, price_stats, stock_stats, category_stats）\n\n");
        
        // 排序说明
        prompt.append("排序方式：\n");
        prompt.append("- price ASC: 价格升序\n");
        prompt.append("- price DESC: 价格降序\n");
        prompt.append("- number ASC: 库存升序\n");
        prompt.append("- number DESC: 库存降序\n");
        prompt.append("- sort_order ASC: 排序值升序\n");
        prompt.append("- sort_order DESC: 排序值降序\n");
        prompt.append("- add_time DESC: 添加时间降序\n\n");
        
        // 会话上下文
        if (request.getSessionId() != null) {
            Map<String, Object> context = getSessionContext(request.getSessionId());
            if (context != null && !context.isEmpty()) {
                prompt.append("会话上下文：\n").append(context.toString()).append("\n\n");
            }
        }
        
        // 用户问题
        prompt.append("用户问题：").append(request.getQuestion()).append("\n\n");
        
        // 输出要求
        prompt.append("请根据用户问题生成相应的查询意图，严格按照JSON格式输出。");
        
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
     * 根据模式匹配条件过滤结果
     * @param results 原始查询结果
     * @param queryIntent 查询意图
     * @return 过滤后的结果
     */
    private List<Map<String, Object>> filterResultsByPattern(List<Map<String, Object>> results, QueryIntent queryIntent) {
        if (results == null || results.isEmpty()) {
            return results;
        }
        
        // 检查是否有名称模式匹配条件
        Map<String, Object> conditions = queryIntent.getConditions();
        if (conditions == null || !conditions.containsKey("name")) {
            return results;
        }
        
        Object nameCondition = conditions.get("name");
        if (!(nameCondition instanceof Map)) {
            return results;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> namePattern = (Map<String, Object>) nameCondition;
        
        String pattern = (String) namePattern.get("pattern");
        String mode = (String) namePattern.getOrDefault("mode", "contains");
        Boolean caseSensitive = (Boolean) namePattern.getOrDefault("case_sensitive", false);
        
        if (pattern == null || pattern.trim().isEmpty()) {
            return results;
        }
        
        List<Map<String, Object>> filteredResults = new ArrayList<>();
        
        for (Map<String, Object> goods : results) {
            String goodsName = (String) goods.get("name");
            String goodsBrief = (String) goods.get("brief");
            
            if (goodsName == null) {
                continue;
            }
            
            // 对商品名称进行模式匹配
            if (matchesPattern(goodsName, pattern, mode, caseSensitive)) {
                filteredResults.add(goods);
                continue;
            }
            
            // 如果商品简介存在，也对简介进行匹配
            if (goodsBrief != null && matchesPattern(goodsBrief, pattern, mode, caseSensitive)) {
                filteredResults.add(goods);
            }
        }
        
        return filteredResults;
    }
    
    /**
     * 检查文本是否匹配指定模式
     * @param text 待匹配的文本
     * @param pattern 匹配模式
     * @param mode 匹配模式类型
     * @param caseSensitive 是否大小写敏感
     * @return 是否匹配
     */
    private boolean matchesPattern(String text, String pattern, String mode, boolean caseSensitive) {
        if (text == null || pattern == null) {
            return false;
        }
        
        // 验证模式长度，防止过长的正则表达式导致ReDoS攻击
        if (pattern.length() > 100) {
            logger.warn("正则表达式模式过长，模式：{}，长度：{}", pattern, pattern.length());
            return false;
        }
        
        String targetText = caseSensitive ? text : text.toLowerCase();
        String targetPattern = caseSensitive ? pattern : pattern.toLowerCase();
        
        switch (mode) {
            case "exact":
                return targetText.equals(targetPattern);
            case "contains":
                return targetText.contains(targetPattern);
            case "starts_with":
                return targetText.startsWith(targetPattern);
            case "ends_with":
                return targetText.endsWith(targetPattern);
            case "regex":
                try {
                    // 为正则表达式添加超时限制，防止ReDoS攻击
                    if (text.length() > 1000) {
                        logger.warn("文本过长，跳过正则匹配，文本长度：{}", text.length());
                        return false;
                    }
                    
                    int flags = 0;
                    if (!caseSensitive) {
                        flags |= Pattern.CASE_INSENSITIVE;
                    }
                    // 添加时间限制编译标志，虽然Java的正则表达式没有直接的timeout机制，
                    // 但可以通过限制文本和模式长度来防止ReDoS攻击
                    Pattern regex = Pattern.compile(pattern, flags);
                    
                    // 使用Matcher进行匹配
                    return regex.matcher(text).find();
                } catch (PatternSyntaxException e) {
                    logger.warn("正则表达式语法错误，模式：{}，错误：{}", pattern, e.getMessage());
                    return false;
                } catch (Exception e) {
                    logger.warn("正则表达式匹配失败，模式：{}，文本：{}", pattern, text, e);
                    return false;
                }
            default:
                return targetText.contains(targetPattern);
        }
    }
    
    /**
     * 生成回答
     * @param queryIntent 查询意图
     * @param results 查询结果
     * @return 回答文本
     */
    private String generateAnswer(QueryIntent queryIntent, List<Map<String, Object>> results) {
        StringBuilder answer = new StringBuilder();
        
        // 查询类型描述
        String queryTypeDesc = queryIntentBuilder.getQueryTypeDescription(queryIntent.getQueryType());
        answer.append("根据您的查询意图（").append(queryTypeDesc).append("），");
        
        if (results == null || results.isEmpty()) {
            answer.append("没有找到符合条件的商品。");
            return answer.toString();
        }
        
        // 统计查询
        if ("statistical".equals(queryIntent.getQueryType())) {
            return generateStatisticalAnswer(queryIntent, results);
        }
        
        // 普通查询结果
        answer.append("找到 ").append(results.size()).append(" 个商品：\n\n");
        
        // 显示前几个商品
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
            
            if (goods.containsKey("brief")) {
                answer.append("\n   ").append(goods.get("brief"));
            }
            
            answer.append("\n");
        }
        
        if (results.size() > displayCount) {
            answer.append("\n... 还有 ").append(results.size() - displayCount).append(" 个商品");
        }
        
        return answer.toString();
    }
    
    /**
     * 生成统计回答
     * @param queryIntent 查询意图
     * @param results 统计结果
     * @return 统计回答
     */
    private String generateStatisticalAnswer(QueryIntent queryIntent, List<Map<String, Object>> results) {
        StringBuilder answer = new StringBuilder();
        
        if (results.isEmpty()) {
            answer.append("没有获取到统计数据。");
            return answer.toString();
        }
        
        Map<String, Object> stats = results.get(0);
        String statisticType = (String) queryIntent.getConditions().get("statistic_type");
        
        switch (statisticType) {
            case "total_count":
                answer.append("商品总数：").append(stats.getOrDefault("total", 0)).append("个");
                break;
            case "price_stats":
                answer.append("价格统计信息：\n");
                answer.append("- 商品数量：").append(stats.getOrDefault("count", 0)).append("个\n");
                answer.append("- 最低价格：¥").append(stats.getOrDefault("min_price", 0)).append("\n");
                answer.append("- 最高价格：¥").append(stats.getOrDefault("max_price", 0)).append("\n");
                answer.append("- 平均价格：¥").append(String.format("%.2f", stats.getOrDefault("avg_price", 0.0)));
                break;
            case "stock_stats":
                answer.append("库存统计信息：\n");
                answer.append("- 商品数量：").append(stats.getOrDefault("count", 0)).append("个\n");
                answer.append("- 总库存：").append(stats.getOrDefault("total_stock", 0)).append("件\n");
                answer.append("- 平均库存：").append(String.format("%.1f", stats.getOrDefault("avg_stock", 0.0))).append("件");
                break;
            case "category_stats":
                answer.append("分类统计信息：\n");
                for (Map<String, Object> categoryStat : results) {
                    answer.append("- 分类ID：").append(categoryStat.getOrDefault("category_id", "未知"));
                    answer.append("，商品数量：").append(categoryStat.getOrDefault("count", 0)).append("个\n");
                }
                break;
            default:
                answer.append("获取到统计数据，但无法生成对应的回答。");
                break;
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