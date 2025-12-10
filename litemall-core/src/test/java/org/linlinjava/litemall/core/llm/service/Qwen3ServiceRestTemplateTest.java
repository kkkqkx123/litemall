package org.linlinjava.litemall.core.llm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// 禁用PowerMock以避免Mockito插件冲突
// @PowerMockIgnore("javax.management.*", "org.mockito.*")

// 创建自定义Mockito配置以避免插件冲突
// 注意：此测试使用纯MockitoExtension，避免与PowerMock的插件冲突
// 由于PowerMock与Mockito 5.x的兼容性问题，此测试需要单独运行
// 解决方案：暂时移除@ExtendWith(MockitoExtension.class)，使用纯JUnit 5测试
// 使用手动Mockito初始化以避免插件冲突

/**
 * Qwen3Service RestTemplate 请求捕获测试
 * 用于验证实际通过RestTemplate发送的HTTP请求
 * 
 * 注意：由于PowerMock与Mockito 5.x的兼容性问题，此测试暂时不使用@ExtendWith(MockitoExtension.class)
 * 改为使用手动Mockito初始化
 */
// @ExtendWith(MockitoExtension.class)
public class Qwen3ServiceRestTemplateTest {
    
    private static final Logger logger = LoggerFactory.getLogger(Qwen3ServiceRestTemplateTest.class);
    
    @Mock
    private RestTemplate restTemplate;
    
    private Qwen3Service qwen3Service;
    
    @BeforeEach
    void setUp() throws Exception {
        // 手动初始化Mockito
        MockitoAnnotations.openMocks(this);
        
        qwen3Service = new Qwen3Service();
        
        // 使用反射设置私有字段
        setPrivateField(qwen3Service, "apiKey", "test-api-key");
        setPrivateField(qwen3Service, "apiUrl", "https://api-inference.modelscope.cn/v1/chat/completions");
        setPrivateField(qwen3Service, "model", "Qwen/Qwen3-32B");
        setPrivateField(qwen3Service, "restTemplate", restTemplate);
    }
    
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
    
    /**
     * 测试实际RestTemplate请求捕获
     */
    @Test
    void testRestTemplateRequestCapture() throws Exception {
        String testPrompt = "你好，请简单介绍一下你自己";
        
        // 创建模拟响应
        Map<String, Object> mockResponse = Map.of(
            "choices", List.of(
                Map.of("message", Map.of("content", "测试响应"))
            )
        );
        
        ResponseEntity<Map<String, Object>> responseEntity = 
            new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        // 捕获实际发送的请求
        ArgumentCaptor<HttpEntity<Map<String, Object>>> requestCaptor = 
            ArgumentCaptor.forClass(HttpEntity.class);
        
        when(restTemplate.exchange(
            eq("https://api-inference.modelscope.cn/v1/chat/completions"),
            eq(HttpMethod.POST),
            requestCaptor.capture(),
            any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);
        
        // 执行调用
        String result = qwen3Service.callLLM(testPrompt);
        
        // 验证结果
        assertNotNull(result, "响应不应为null");
        assertEquals("测试响应", result, "响应内容应匹配");
        
        // 获取捕获的请求
        HttpEntity<Map<String, Object>> capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest, "应捕获到请求");
        
        // 验证请求头
        HttpHeaders headers = capturedRequest.getHeaders();
        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType(), "Content-Type应为JSON");
        assertEquals("Bearer test-api-key", headers.getFirst("Authorization"), "Authorization头应正确");
        
        // 验证请求体
        Map<String, Object> requestBody = capturedRequest.getBody();
        assertNotNull(requestBody, "请求体不应为null");
        
        logger.info("=== 捕获的请求信息 ===");
        logger.info("请求头: {}", headers);
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
        assertTrue(messagesObj instanceof java.util.List, "messages应为List类型");
        
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> messages = (java.util.List<Map<String, Object>>) messagesObj;
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
    void testComplexPromptHandling() throws Exception {
        String complexPrompt = "你是一个商品问答助手，请根据用户的问题生成合适的商品查询意图。\\n" +
                "用户问题：请推荐一些价格在100-500元之间的商品\\n" +
                "请严格按照JSON格式输出：";
        
        Map<String, Object> mockResponse = Map.of(
            "choices", List.of(
                Map.of("message", Map.of("content", "复杂测试响应"))
            )
        );
        
        ResponseEntity<Map<String, Object>> responseEntity = 
            new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        ArgumentCaptor<HttpEntity<Map<String, Object>>> requestCaptor = 
            ArgumentCaptor.forClass(HttpEntity.class);
        
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            requestCaptor.capture(),
            any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);
        
        // 执行调用
        String result = qwen3Service.callLLM(complexPrompt);
        
        // 获取捕获的请求体
        HttpEntity<Map<String, Object>> capturedRequest = requestCaptor.getValue();
        Map<String, Object> requestBody = capturedRequest.getBody();
        
        logger.info("复杂提示词请求体: {}", requestBody);
        logger.info("复杂提示词JSON: {}", new ObjectMapper().writeValueAsString(requestBody));
        
        // 验证复杂提示词被正确处理
        Object messagesObj = requestBody.get("messages");
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> messages = (java.util.List<Map<String, Object>>) messagesObj;
        Map<String, Object> firstMessage = messages.get(0);
        
        assertEquals(complexPrompt, firstMessage.get("content"), "复杂提示词应被完整传递");
        
        // 验证JSON序列化没有破坏结构
        String jsonString = new ObjectMapper().writeValueAsString(requestBody);
        assertTrue(jsonString.contains("\\n"), "换行符应被正确转义");
        assertTrue(jsonString.contains(complexPrompt.substring(0, 50)), "JSON应包含提示词的前50个字符");
    }
}