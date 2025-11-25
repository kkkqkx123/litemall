package org.linlinjava.litemall.core.llm.service;

import org.linlinjava.litemall.core.llm.exception.LLMServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Qwen3大模型调用服务
 * 负责调用通义千问大模型API
 */
@Service
public class Qwen3Service {
    
    private static final Logger logger = LoggerFactory.getLogger(Qwen3Service.class);
    
    @Value("${litemall.llm.qwen3.api-key:}")
    private String apiKey;
    
    @Value("${litemall.llm.qwen3.api-url:https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation}")
    private String apiUrl;
    
    @Value("${litemall.llm.qwen3.model:qwen-turbo}")
    private String model;
    
    @Value("${litemall.llm.qwen3.timeout:30000}")
    private int timeout;
    
    @Value("${litemall.llm.qwen3.max-retries:3}")
    private int maxRetries;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * 调用Qwen3大模型
     * @param prompt 提示词
     * @return 模型响应
     * @throws LLMServiceException 当调用失败时抛出
     */
    public String callLLM(String prompt) throws LLMServiceException {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("提示词不能为空");
        }
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("Qwen3 API密钥未配置，使用模拟响应");
            return generateMockResponse(prompt);
        }
        
        int retryCount = 0;
        Exception lastException = null;
        
        while (retryCount < maxRetries) {
            try {
                return callLLMApi(prompt);
            } catch (Exception e) {
                lastException = e;
                retryCount++;
                logger.warn("第{}次调用Qwen3 API失败：{}", retryCount, e.getMessage());
                
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(1000 * retryCount); // 指数退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new LLMServiceException("调用被中断", ie);
                    }
                }
            }
        }
        
        throw new LLMServiceException("调用Qwen3 API失败，重试" + maxRetries + "次后仍失败：" + lastException.getMessage(), lastException);
    }
    
    /**
     * 调用Qwen3 API
     * @param prompt 提示词
     * @return 模型响应
     * @throws Exception 当调用失败时抛出
     */
    private String callLLMApi(String prompt) throws Exception {
        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        
        Map<String, Object> input = new HashMap<>();
        input.put("prompt", prompt);
        requestBody.put("input", input);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("result_format", "text");
        parameters.put("temperature", 0.1); // 降低随机性，提高稳定性
        parameters.put("top_p", 0.8);
        parameters.put("max_tokens", 2000);
        requestBody.put("parameters", parameters);
        
        // 发送请求
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        logger.debug("调用Qwen3 API，模型：{}，提示词长度：{}", model, prompt.length());
        
        ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
        
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new LLMServiceException("API调用失败，状态码：" + response.getStatusCode());
        }
        
        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new LLMServiceException("API响应为空");
        }
        
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
            Map<String, Object> error = (Map<String, Object>) responseBody.get("error");
            String errorCode = (String) error.get("code");
            String errorMessage = (String) error.get("message");
            throw new LLMServiceException("API错误：" + errorCode + " - " + errorMessage);
        }
        
        // 获取输出结果
        Map<String, Object> output = (Map<String, Object>) responseBody.get("output");
        if (output == null) {
            throw new LLMServiceException("响应中缺少output字段");
        }
        
        String result = (String) output.get("text");
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
        
        // 简单的模拟逻辑，根据提示词内容返回不同的响应
        if (prompt.contains("价格") && (prompt.contains("便宜") || prompt.contains("低"))) {
            return "{\n" +
               "  \"query_type\": \"price_range\",\n" +
               "  \"conditions\": {\n" +
               "    \"min_price\": 100,\n" +
               "    \"max_price\": 500\n" +
               "  },\n" +
               "  \"sort\": \"price ASC\",\n" +
               "  \"limit\": 10\n" +
               "}";
        } else if (prompt.contains("库存") || prompt.contains("存货")) {
            return "{\n" +
                   "  \"query_type\": \"stock_check\",\n" +
                   "  \"conditions\": {\n" +
                   "    \"min_number\": 10,\n" +
                   "    \"is_on_sale\": 1\n" +
                   "  },\n" +
                   "  \"sort\": \"number DESC\",\n" +
                   "  \"limit\": 5\n" +
                   "}";
        } else if (prompt.contains("统计") || prompt.contains("总数")) {
            return "{\n" +
                   "  \"query_type\": \"statistical\",\n" +
                   "  \"conditions\": {\n" +
                   "    \"statistic_type\": \"total_count\",\n" +
                   "    \"is_on_sale\": 1\n" +
                   "  }\n" +
                   "}";
        } else {
            return "{\n" +
                   "  \"query_type\": \"keyword_search\",\n" +
                   "  \"conditions\": {\n" +
                   "    \"keyword\": \"商品\",\n" +
                   "    \"is_on_sale\": 1\n" +
                   "  },\n" +
                   "  \"sort\": \"sort_order ASC\",\n" +
                   "  \"limit\": 10\n" +
                   "}";
        }
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