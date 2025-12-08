package org.linlinjava.litemall.core.llm.service;

import org.linlinjava.litemall.core.llm.exception.LLMServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Qwen3大模型调用服务
 * 负责调用通义千问大模型API
 */
@Service
public class Qwen3Service {
    
    private static final Logger logger = LoggerFactory.getLogger(Qwen3Service.class);
    
    @Value("${litemall.modelscope.api-key:}")
    private String apiKey;
    
    @Value("${litemall.modelscope.api-url}")
    private String apiUrl;
    
    @Value("${litemall.modelscope.model-name:Qwen/Qwen3-32B}")
    private String model;
    
    @Value("${litemall.modelscope.session-timeout:30000}")
    private int timeout;
    
    @Value("${litemall.modelscope.max-context-length:3}")
    private int maxRetries;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * 调用Qwen3大模型
     * @param prompt 提示词
     * @return 模型响应
     * @throws LLMServiceException 当调用失败时抛出
     */
    public String callLLM(String prompt) throws LLMServiceException {
        return callLLM(prompt, null);
    }
    
    /**
     * 调用Qwen3大模型（带会话上下文）
     * @param prompt 提示词
     * @param sessionContext 会话上下文（可选）
     * @return 模型响应
     * @throws LLMServiceException 当调用失败时抛出
     */
    public String callLLM(String prompt, String sessionContext) throws LLMServiceException {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("提示词不能为空");
        }
        
        // 构建完整的提示词
        String fullPrompt = buildFullPrompt(prompt, sessionContext);
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("Qwen3 API密钥未配置，使用模拟响应");
            return generateMockResponse(prompt);
        }
        
        // 临时注释掉重试逻辑，避免浪费API调用次数
        // int retryCount = 0;
        // Exception lastException = null;
        // 
        // while (retryCount < maxRetries) {
        //     try {
        //         return callLLMApi(fullPrompt);
        //     } catch (Exception e) {
        //         lastException = e;
        //         retryCount++;
        //         logger.warn("第{}次调用Qwen3 API失败：{}", retryCount, e.getMessage());
        //         
        //         if (retryCount < maxRetries) {
        //             try {
        //                 Thread.sleep(1000 * retryCount); // 指数退避
        //             } catch (InterruptedException ie) {
        //                 Thread.currentThread().interrupt();
        //                 throw new LLMServiceException("调用被中断", ie);
        //             }
        //         }
        //     }
        // }
        // 
        // throw new LLMServiceException("调用Qwen3 API失败，重试" + maxRetries + "次后仍失败：" + lastException.getMessage(), lastException);
        
        // 只调用一次，不 retry
        try {
            return callLLMApi(fullPrompt);
        } catch (Exception e) {
            logger.warn("调用Qwen3 API失败：{}", e.getMessage());
            throw new LLMServiceException("调用Qwen3 API失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 调用Qwen3 API
     * @param prompt 提示词
     * @return 模型响应
     * @throws Exception 当调用失败时抛出
     */
    private String callLLMApi(String prompt) throws Exception {
        logger.info("=== Qwen3Service 开始调用API ===");
        logger.info("提示词：{}", prompt);
        logger.info("提示词长度：{}", prompt.length());
        
        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        logger.info("API Key：{}", apiKey != null ? "已配置" : "未配置");
        logger.info("API端点：{}", apiUrl);
        
        // 构建请求体 - 使用ModelScope Chat Completions API格式
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        
        // 构建消息列表 - ModelScope Chat Completions API使用messages数组
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);
        requestBody.put("messages", messages);  // 使用messages数组参数
        
        // 直接设置参数，而不是嵌套在parameters对象中
        requestBody.put("temperature", 0.1); // 降低随机性，提高稳定性
        requestBody.put("top_p", 0.8);
        requestBody.put("max_tokens", 2000);
        requestBody.put("enable_thinking", false); // 非流式调用必须设置为false
        
        // 发送请求
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        logger.info("调用Qwen3 API，模型：{}，提示词长度：{}，API端点：{}", model, prompt.length(), apiUrl);
        
        // 将请求体序列化为JSON字符串，便于调试
        try {
            String requestBodyJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(requestBody);
            logger.info("实际发送的请求体JSON：{}", requestBodyJson);
            
            // 详细检查messages参数
            Object messagesObj = requestBody.get("messages");
            if (messagesObj != null) {
                logger.info("messages参数类型：{}", messagesObj.getClass().getName());
                logger.info("messages参数内容：{}", messagesObj);
                if (messagesObj instanceof List) {
                    List<?> messagesList = (List<?>) messagesObj;
                    logger.info("messages数组长度：{}", messagesList.size());
                    if (!messagesList.isEmpty()) {
                        Object firstMessage = messagesList.get(0);
                        logger.info("第一个消息类型：{}", firstMessage.getClass().getName());
                        logger.info("第一个消息内容：{}", firstMessage);
                    }
                }
            } else {
                logger.error("messages参数为空！");
            }
            
        } catch (Exception e) {
            logger.warn("无法序列化请求体为JSON", e);
        }
        
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request,
                new ParameterizedTypeReference<Map<String, Object>>() {});
        
        System.out.println("Qwen3 API响应状态码：" + response.getStatusCode());
        logger.info("Qwen3 API响应状态码：{}", response.getStatusCode());
        
        if (response.getStatusCode() != HttpStatus.OK) {
            System.out.println("API调用失败，状态码：" + response.getStatusCode() + "，响应体：" + response.getBody());
            logger.error("API调用失败，状态码：{}，响应体：{}", response.getStatusCode(), response.getBody());
            throw new LLMServiceException("API调用失败，状态码：" + response.getStatusCode());
        }
        
        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            System.out.println("API响应为空");
            logger.error("API响应为空");
            throw new LLMServiceException("API响应为空");
        }
        
        System.out.println("API响应体：" + responseBody);
        logger.debug("API响应体：{}", responseBody);
        
        // 解析响应
        return parseResponse(responseBody);
    }
    
    /**
     * 解析API响应
     * @param responseBody 响应体
     * @return 模型生成的文本
     * @throws LLMServiceException 当解析失败时抛出
     */
    private String parseResponse(Map<String, Object> responseBody) throws LLMServiceException {
        // 检查错误信息
        if (responseBody.containsKey("error")) {
            Object errorObj = responseBody.get("error");
             if (errorObj instanceof Map<?, ?>) {
                 @SuppressWarnings("unchecked")
                 Map<String, Object> error = (Map<String, Object>) errorObj;
                 String errorCode = (String) error.get("code");
                 String errorMessage = (String) error.get("message");
                 throw new LLMServiceException("API错误：" + errorCode + " - " + errorMessage);
             } else {
                 throw new LLMServiceException("API返回错误格式");
             }
         }
         
         // 解析OpenAI兼容的Chat Completions格式响应
         Object choicesObj = responseBody.get("choices");
         if (!(choicesObj instanceof List<?>)) {
             throw new LLMServiceException("响应格式错误，缺少choices字段");
         }
         
         @SuppressWarnings("unchecked")
         List<Map<String, Object>> choices = (List<Map<String, Object>>) choicesObj;
         
         if (choices.isEmpty()) {
             throw new LLMServiceException("模型未生成任何响应");
         }
         
         // 获取第一个选择
         Map<String, Object> firstChoice = choices.get(0);
         Object messageObj = firstChoice.get("message");
         
         if (!(messageObj instanceof Map<?, ?>)) {
             throw new LLMServiceException("消息格式错误");
         }
         
         @SuppressWarnings("unchecked")
         Map<String, Object> message = (Map<String, Object>) messageObj;
         
         String result = (String) message.get("content");
         if (result == null || result.trim().isEmpty()) {
             throw new LLMServiceException("模型未生成任何文本");
         }
         
         logger.debug("Qwen3 API响应成功，生成文本长度：{}", result.length());
         return result.trim();
    }
    
    /**
     * 生成模拟响应（用于测试或API密钥未配置时）
     * @param prompt 提示词
     * @return 模拟响应
     */
    private String generateMockResponse(String prompt) {
        logger.info("使用模拟Qwen3响应");
        
        // 根据提示词内容返回不同的响应，包含更丰富的查询类型和条件
        if (prompt.contains("价格") && (prompt.contains("便宜") || prompt.contains("低") || prompt.contains("优惠"))) {
            // 价格范围查询
            return "{\n" +
                   "  \"query_type\": \"price_range\",\n" +
                   "  \"conditions\": {\n" +
                   "    \"min_price\": 100,\n" +
                   "    \"max_price\": 500,\n" +
                   "    \"is_on_sale\": 1\n" +
                   "  },\n" +
                   "  \"sort\": \"retail_price ASC\",\n" +
                   "  \"limit\": 10,\n" +
                   "  \"confidence\": 0.95,\n" +
                   "  \"explanation\": \"根据用户询问低价商品，推荐100-500元价格区间的商品\"\n" +
                   "}";
        } else if (prompt.contains("库存") || prompt.contains("存货") || prompt.contains("现货")) {
            // 库存查询
            int minStock = 10;
            if (prompt.contains("充足")) {
                minStock = 50;
            } else if (prompt.contains("少量")) {
                minStock = 1;
            }
            
            return "{\n" +
                   "  \"query_type\": \"stock_check\",\n" +
                   "  \"conditions\": {\n" +
                   "    \"min_number\": " + minStock + ",\n" +
                   "    \"is_on_sale\": 1\n" +
                   "  },\n" +
                   "  \"sort\": \"number DESC\",\n" +
                   "  \"limit\": 8,\n" +
                   "  \"confidence\": 0.90,\n" +
                   "  \"explanation\": \"根据用户询问库存情况，推荐库存充足的商品\"\n" +
                   "}";
        } else if (prompt.contains("统计") || prompt.contains("总数") || prompt.contains("多少种")) {
            // 统计查询
            String statType = "total_count";
            if (prompt.contains("价格") || prompt.contains("平均")) {
                statType = "price_stats";
            } else if (prompt.contains("库存") || prompt.contains("总库存")) {
                statType = "stock_stats";
            } else if (prompt.contains("分类") || prompt.contains("类别")) {
                statType = "category_stats";
            }
            
            return "{\n" +
                   "  \"query_type\": \"statistical\",\n" +
                   "  \"conditions\": {\n" +
                   "    \"statistic_type\": \"" + statType + "\",\n" +
                   "    \"is_on_sale\": 1\n" +
                   "  },\n" +
                   "  \"confidence\": 0.85,\n" +
                   "  \"explanation\": \"根据用户询问统计信息，提供相应的统计数据\"\n" +
                   "}";
        } else if (prompt.contains("名称") || prompt.contains("名字") || prompt.contains("商品名")) {
            // 名称模式匹配查询
            String pattern = "手机";
            if (prompt.contains("电脑") || prompt.contains("笔记本")) {
                pattern = "电脑";
            } else if (prompt.contains("衣服") || prompt.contains("服装")) {
                pattern = "衣服";
            } else if (prompt.contains("鞋子") || prompt.contains("鞋")) {
                pattern = "鞋子";
            }
            
            return "{\n" +
                   "  \"query_type\": \"name_pattern\",\n" +
                   "  \"conditions\": {\n" +
                   "    \"name\": {\n" +
                   "      \"pattern\": \"" + pattern + "\",\n" +
                   "      \"mode\": \"contains\",\n" +
                   "      \"case_sensitive\": false\n" +
                   "    },\n" +
                   "    \"is_on_sale\": 1\n" +
                   "  },\n" +
                   "  \"sort\": \"name ASC\",\n" +
                   "  \"limit\": 15,\n" +
                   "  \"confidence\": 0.88,\n" +
                   "  \"explanation\": \"根据用户询问特定名称的商品，进行模式匹配查询\"\n" +
                   "}";
        } else if (prompt.contains("推荐") || prompt.contains("热销") || prompt.contains("热门")) {
            // 推荐查询
            return "{\n" +
                   "  \"query_type\": \"recommendation\",\n" +
                   "  \"conditions\": {\n" +
                   "    \"is_on_sale\": 1,\n" +
                   "    \"is_hot\": 1\n" +
                   "  },\n" +
                   "  \"sort\": \"sort_order ASC, retail_price ASC\",\n" +
                   "  \"limit\": 12,\n" +
                   "  \"confidence\": 0.82,\n" +
                   "  \"explanation\": \"根据用户请求推荐，提供热销商品\"\n" +
                   "}";
        } else {
            // 默认关键词搜索
            String keyword = "商品";
            if (prompt.length() > 2) {
                // 提取用户问题中的关键词
                keyword = extractKeyword(prompt);
            }
            
            return "{\n" +
                   "  \"query_type\": \"keyword_search\",\n" +
                   "  \"conditions\": {\n" +
                   "    \"keyword\": \"" + keyword + "\",\n" +
                   "    \"is_on_sale\": 1\n" +
                   "  },\n" +
                   "  \"sort\": \"sort_order ASC, add_time DESC\",\n" +
                   "  \"limit\": 10,\n" +
                   "  \"confidence\": 0.75,\n" +
                   "  \"explanation\": \"根据用户问题进行关键词搜索\"\n" +
                   "}";
        }
    }
    
    /**
     * 从用户问题中提取关键词
     * @param prompt 用户问题
     * @return 提取的关键词
     */
    private String extractKeyword(String prompt) {
        // 简单的关键词提取逻辑
        String[] keywords = {"手机", "电脑", "衣服", "鞋子", "包包", "化妆品", "食品", "图书", "家电", "数码"};
        
        for (String keyword : keywords) {
            if (prompt.contains(keyword)) {
                return keyword;
            }
        }
        
        // 如果没有匹配到特定关键词，返回问题的前几个字符
        return prompt.length() > 10 ? prompt.substring(0, 10) : prompt;
    }
    
    /**
     * 健康检查
     * @return true表示服务正常，false表示服务异常
     */
    public boolean healthCheck() {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                logger.info("Qwen3服务未配置API密钥，使用模拟模式");
                return true;
            }
            
            // 简单的健康检查调用
            String testPrompt = "你好";
            String response = callLLM(testPrompt);
            return response != null && !response.trim().isEmpty();
            
        } catch (Exception e) {
            logger.error("Qwen3服务健康检查失败", e);
            return false;
        }
    }
    
    /**
     * 构建完整的提示词
     * @param userPrompt 用户提示词
     * @param sessionContext 会话上下文
     * @return 完整的提示词
     */
    private String buildFullPrompt(String userPrompt, String sessionContext) {
        StringBuilder fullPrompt = new StringBuilder();
        
        // 添加系统提示词
        fullPrompt.append("你是一个智能商品问答助手。请根据用户的商品查询需求，生成结构化的查询意图。\n");
        fullPrompt.append("请严格按照以下JSON格式返回结果，不要包含任何其他文本：\n");
        fullPrompt.append("{\n");
        fullPrompt.append("  \"query_type\": \"查询类型\",\n");
        fullPrompt.append("  \"conditions\": {\n");
        fullPrompt.append("    \"条件1\": \"值1\",\n");
        fullPrompt.append("    \"条件2\": \"值2\"\n");
        fullPrompt.append("  },\n");
        fullPrompt.append("  \"sort\": \"排序字段 排序方式\",\n");
        fullPrompt.append("  \"limit\": 数量,\n");
        fullPrompt.append("  \"confidence\": 置信度,\n");
        fullPrompt.append("  \"explanation\": \"查询解释\"\n");
        fullPrompt.append("}\n\n");
        
        // 添加会话上下文（如果有）
        if (sessionContext != null && !sessionContext.trim().isEmpty()) {
            fullPrompt.append("之前的对话上下文：\n");
            fullPrompt.append(sessionContext).append("\n\n");
        }
        
        // 添加用户问题
        fullPrompt.append("用户问题：").append(userPrompt).append("\n");
        fullPrompt.append("请生成查询意图JSON：");
        
        return fullPrompt.toString();
    }
    
    /**
     * 获取服务状态信息
     * @return 状态信息
     */
    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "Qwen3");
        status.put("model", model);
        status.put("apiUrl", apiUrl);
        status.put("apiKeyConfigured", apiKey != null && !apiKey.trim().isEmpty());
        status.put("healthy", healthCheck());
        status.put("timestamp", System.currentTimeMillis());
        return status;
    }
}