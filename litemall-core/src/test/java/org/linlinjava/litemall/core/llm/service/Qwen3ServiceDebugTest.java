package org.linlinjava.litemall.core.llm.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Qwen3Service调试测试类
 * 用于验证实际的API调用问题
 */
public class Qwen3ServiceDebugTest {
    
    private static final Logger logger = LoggerFactory.getLogger(Qwen3ServiceDebugTest.class);
    
    private RestTemplate restTemplate;
    private String apiKey;
    private String apiUrl;
    private String model;
    
    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        // 使用配置中的实际值
        apiKey = "ms-84ab62cd-bb59-487a-a214-e90349dd7e28";
        apiUrl = "https://api-inference.modelscope.cn/v1/chat/completions";
        model = "Qwen/Qwen3-32B";
    }
    
    /**
     * 测试直接调用Qwen3 API，验证请求格式
     */
    @Test
    void testDirectApiCall() {
        logger.info("=== 开始直接API调用测试 ===");
        
        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            // 构建请求体 - 完全按照Qwen3Service的逻辑
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            
            // 构建消息列表
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "你好，请简单介绍一下你自己");
            messages.add(userMessage);
            requestBody.put("messages", messages);
            
            // 设置其他参数
            requestBody.put("max_tokens", 100);
            requestBody.put("temperature", 0.7);
            requestBody.put("enable_thinking", false);
            
            // 序列化为JSON进行日志记录
            String requestBodyJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(requestBody);
            logger.info("请求体JSON: {}", requestBodyJson);
            
            // 验证messages参数
            Object messagesObj = requestBody.get("messages");
            logger.info("messages参数类型: {}", messagesObj.getClass().getName());
            logger.info("messages参数内容: {}", messagesObj);
            
            if (messagesObj instanceof List) {
                List<?> messagesList = (List<?>) messagesObj;
                logger.info("messages数组长度: {}", messagesList.size());
                if (!messagesList.isEmpty()) {
                    Object firstMessage = messagesList.get(0);
                    logger.info("第一个消息类型: {}", firstMessage.getClass().getName());
                    logger.info("第一个消息内容: {}", firstMessage);
                }
            }
            
            // 发送请求
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            logger.info("发送POST请求到: {}", apiUrl);
            logger.info("请求头: {}", headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiUrl, 
                HttpMethod.POST, 
                request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            logger.info("响应状态码: {}", response.getStatusCode());
            logger.info("响应体: {}", response.getBody());
            
            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("API调用成功！");
            } else {
                logger.error("API调用失败，状态码: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("API调用异常: {}", e.getMessage(), e);
            
            // 如果是HttpClientErrorException，尝试获取响应体
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                org.springframework.web.client.HttpClientErrorException httpEx = 
                    (org.springframework.web.client.HttpClientErrorException) e;
                logger.error("错误响应体: {}", httpEx.getResponseBodyAsString());
            }
        }
    }
    
    /**
     * 测试使用String而不是Map作为请求体
     */
    @Test
    void testStringRequestBody() {
        logger.info("=== 开始String请求体测试 ===");
        
        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            // 构建JSON字符串请求体
            String requestBodyJson = "{"
                + "\"model\": \"" + model + "\","
                + "\"messages\": [{\"role\": \"user\", \"content\": \"你好，请简单介绍一下你自己\"}],"
                + "\"max_tokens\": 100,"
                + "\"temperature\": 0.7,"
                + "\"enable_thinking\": false"
                + "}";
            
            logger.info("String请求体: {}", requestBodyJson);
            
            // 发送请求
            HttpEntity<String> request = new HttpEntity<>(requestBodyJson, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiUrl, 
                HttpMethod.POST, 
                request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            logger.info("响应状态码: {}", response.getStatusCode());
            logger.info("响应体: {}", response.getBody());
            
        } catch (Exception e) {
            logger.error("String请求体测试失败: {}", e.getMessage(), e);
            
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                org.springframework.web.client.HttpClientErrorException httpEx = 
                    (org.springframework.web.client.HttpClientErrorException) e;
                logger.error("错误响应体: {}", httpEx.getResponseBodyAsString());
            }
        }
    }
    
    /**
     * 测试curl命令等效的Java实现
     */
    @Test
    void testCurlEquivalent() {
        logger.info("=== 开始curl等效测试 ===");
        
        try {
            // 完全模拟成功的curl命令
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            // 与成功curl完全相同的JSON
            String jsonBody = "{"
                + "\"model\": \"Qwen/Qwen3-32B\","
                + "\"messages\": [{\"role\": \"user\", \"content\": \"你好，请简单介绍一下你自己\"}],"
                + "\"max_tokens\": 100,"
                + "\"temperature\": 0.7,"
                + "\"enable_thinking\": false"
                + "}";
            
            logger.info("curl等效JSON: {}", jsonBody);
            
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiUrl, 
                HttpMethod.POST, 
                request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            logger.info("curl等效测试响应状态码: {}", response.getStatusCode());
            logger.info("curl等效测试响应体: {}", response.getBody());
            
        } catch (Exception e) {
            logger.error("curl等效测试失败: {}", e.getMessage(), e);
            
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                org.springframework.web.client.HttpClientErrorException httpEx = 
                    (org.springframework.web.client.HttpClientErrorException) e;
                logger.error("错误响应体: {}", httpEx.getResponseBodyAsString());
            }
        }
    }
}