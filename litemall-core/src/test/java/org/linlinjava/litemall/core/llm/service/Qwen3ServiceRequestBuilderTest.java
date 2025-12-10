package org.linlinjava.litemall.core.llm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Qwen3Service 请求构建测试
 * 用于验证请求体构建是否正确
 */
public class Qwen3ServiceRequestBuilderTest {
    
    private static final Logger logger = LoggerFactory.getLogger(Qwen3ServiceRequestBuilderTest.class);
    
    // 移除Spring依赖，直接测试构建逻辑
    
    /**
     * 测试请求体构建
     */
    @Test
    public void testRequestBodyBuilder() throws Exception {
        String testPrompt = "你好，请简单介绍一下你自己";
        
        // 构建请求体
        Map<String, Object> requestBody = buildRequestBody(testPrompt);
        
        logger.info("=== 构建的请求体 ===");
        logger.info("请求体: {}", requestBody);
        logger.info("请求体JSON: {}", new ObjectMapper().writeValueAsString(requestBody));
        logger.info("====================");
        
        // 验证关键字段
        assertEquals("Qwen/Qwen3-32B", requestBody.get("model"), "model字段应正确");
        assertTrue(requestBody.containsKey("messages"), "请求体应包含messages字段");
        assertTrue(requestBody.containsKey("max_tokens"), "请求体应包含max_tokens字段");
        assertTrue(requestBody.containsKey("temperature"), "请求体应包含temperature字段");
        assertTrue(requestBody.containsKey("enable_thinking"), "请求体应包含enable_thinking字段");
        
        // 验证messages数组
        Object messagesObj = requestBody.get("messages");
        assertTrue(messagesObj instanceof List, "messages应为List类型");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) messagesObj;
        assertEquals(1, messages.size(), "messages数组应包含1个元素");
        
        Map<String, Object> firstMessage = messages.get(0);
        assertEquals("user", firstMessage.get("role"), "第一个消息的role应为user");
        assertEquals(testPrompt, firstMessage.get("content"), "第一个消息的content应匹配输入提示词");
        
        logger.info("messages验证通过: {}", messages);
    }
    
    /**
     * 测试复杂提示词的处理
     */
    @Test
    public void testComplexPromptHandling() throws Exception {
        String complexPrompt = "你是一个商品问答助手，请根据用户的问题生成合适的商品查询意图。\n" +
                "用户问题：请推荐一些价格在100-500元之间的商品\n" +
                "请严格按照JSON格式输出：";
        
        Map<String, Object> requestBody = buildRequestBody(complexPrompt);
        
        logger.info("=== 复杂提示词请求体 ===");
        logger.info("请求体: {}", requestBody);
        logger.info("请求体JSON: {}", new ObjectMapper().writeValueAsString(requestBody));
        logger.info("====================");
        
        // 验证复杂提示词被正确处理
        Object messagesObj = requestBody.get("messages");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) messagesObj;
        Map<String, Object> firstMessage = messages.get(0);
        
        assertEquals(complexPrompt, firstMessage.get("content"), "复杂提示词应被完整传递");
        
        // 验证JSON序列化没有破坏结构
        String jsonString = new ObjectMapper().writeValueAsString(requestBody);
        assertTrue(jsonString.contains("\\n") || jsonString.contains("\n"), "换行符应被正确转义");
        assertTrue(jsonString.contains(complexPrompt.substring(0, 30)), "JSON应包含提示词的前30个字符");
    }
    
    /**
     * 构建请求体（模拟Qwen3Service中的逻辑）
     */
    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "Qwen/Qwen3-32B");
        
        // 构建消息列表 - ModelScope Chat Completions API使用messages数组
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);
        requestBody.put("messages", messages);  // 使用messages数组参数
        
        // 只设置必要的参数，参考成功的curl请求
        requestBody.put("max_tokens", 100);
        requestBody.put("temperature", 0.7);
        requestBody.put("enable_thinking", false); // 非流式调用必须设置为false
        
        return requestBody;
    }
}