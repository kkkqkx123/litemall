package org.linlinjava.litemall.core.llm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Qwen3Service集成测试
 * 用于验证实际请求体的构建情况
 */
@ExtendWith(MockitoExtension.class)
public class Qwen3ServiceIntegrationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(Qwen3ServiceIntegrationTest.class);
    
    @Mock
    private RestTemplate mockRestTemplate;
    
    private Qwen3Service qwen3Service;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        qwen3Service = new Qwen3Service();
        objectMapper = new ObjectMapper();
        
        // 使用反射设置mock的restTemplate
        try {
            java.lang.reflect.Field field = Qwen3Service.class.getDeclaredField("restTemplate");
            field.setAccessible(true);
            field.set(qwen3Service, mockRestTemplate);
        } catch (Exception e) {
            logger.error("设置mock RestTemplate失败", e);
        }
        
        // 设置测试配置
        ReflectionTestUtils.setField(qwen3Service, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(qwen3Service, "apiUrl", "https://api-inference.modelscope.cn/v1/chat/completions");
        ReflectionTestUtils.setField(qwen3Service, "model", "Qwen/Qwen3-32B");
        ReflectionTestUtils.setField(qwen3Service, "timeout", 30000);
        ReflectionTestUtils.setField(qwen3Service, "maxRetries", 3);
    }
    
    /**
     * 测试请求体构建 - 验证实际发送的JSON格式
     */
    @Test
    void testRequestBodyConstruction() throws Exception {
        // 准备测试数据
        String testPrompt = "你好，请简单介绍一下你自己";
        
        // 创建模拟响应
        Map<String, Object> mockResponse = new HashMap<>();
        List<Map<String, Object>> choices = new ArrayList<>();
        Map<String, Object> choice = new HashMap<>();
        Map<String, Object> message = new HashMap<>();
        message.put("content", "我是一个AI助手");
        choice.put("message", message);
        choices.add(choice);
        mockResponse.put("choices", choices);
        
        ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        // 设置mock行为 - 捕获实际的HttpEntity参数
        when(mockRestTemplate.exchange(
                eq("https://api-inference.modelscope.cn/v1/chat/completions"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(new ParameterizedTypeReference<Map<String, Object>>() {})
        )).thenReturn(responseEntity);
        
        // 执行测试
        String result = qwen3Service.callLLM(testPrompt);
        
        // 捕获实际发送的HttpEntity
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(mockRestTemplate).exchange(
                eq("https://api-inference.modelscope.cn/v1/chat/completions"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(new ParameterizedTypeReference<Map<String, Object>>() {})
        );
        
        HttpEntity<Map<String, Object>> capturedEntity = entityCaptor.getValue();
        
        // 验证请求头
        HttpHeaders headers = capturedEntity.getHeaders();
        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
        assertEquals("Bearer test-api-key", headers.getFirst("Authorization"));
        
        // 验证请求体
        Map<String, Object> requestBody = capturedEntity.getBody();
        assertNotNull(requestBody, "请求体不能为空");
        
        logger.info("实际发送的请求体: {}", requestBody);
        
        // 验证请求体包含必要的字段
        assertTrue(requestBody.containsKey("model"), "请求体应包含model字段");
        assertTrue(requestBody.containsKey("messages"), "请求体应包含messages字段");
        assertTrue(requestBody.containsKey("max_tokens"), "请求体应包含max_tokens字段");
        assertTrue(requestBody.containsKey("temperature"), "请求体应包含temperature字段");
        assertTrue(requestBody.containsKey("enable_thinking"), "请求体应包含enable_thinking字段");
        
        // 验证messages参数
        Object messagesObj = requestBody.get("messages");
        assertNotNull(messagesObj, "messages参数不能为空");
        assertTrue(messagesObj instanceof List, "messages参数必须是List类型");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) messagesObj;
        assertFalse(messages.isEmpty(), "messages数组不能为空");
        assertEquals(1, messages.size(), "messages数组应该包含1个元素");
        
        // 验证第一个消息
        Map<String, Object> firstMessage = messages.get(0);
        assertEquals("user", firstMessage.get("role"), "消息角色必须是user");
        
        // 由于Qwen3Service会使用buildFullPrompt方法添加系统提示词，实际内容会包含系统提示词+用户输入
        String actualContent = (String) firstMessage.get("content");
        assertTrue(actualContent.contains(testPrompt), "消息内容应包含用户输入的提示词");
        assertTrue(actualContent.contains("你是一个智能商品问答助手"), "消息内容应包含系统提示词");
        
        logger.info("请求体验证通过");
    }
    
    /**
     * 测试messages参数的具体格式
     */
    @Test
    void testMessagesParameterFormat() throws Exception {
        String testPrompt = "测试消息格式";
        
        // 创建模拟响应
        Map<String, Object> mockResponse = new HashMap<>();
        List<Map<String, Object>> choices = new ArrayList<>();
        Map<String, Object> choice = new HashMap<>();
        Map<String, Object> message = new HashMap<>();
        message.put("content", "测试响应");
        choice.put("message", message);
        choices.add(choice);
        mockResponse.put("choices", choices);
        
        ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        when(mockRestTemplate.exchange(
                eq("https://api-inference.modelscope.cn/v1/chat/completions"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(new ParameterizedTypeReference<Map<String, Object>>() {})
        )).thenReturn(responseEntity);
        
        // 执行测试
        qwen3Service.callLLM(testPrompt);
        
        // 捕获请求
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(mockRestTemplate).exchange(
                eq("https://api-inference.modelscope.cn/v1/chat/completions"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(new ParameterizedTypeReference<Map<String, Object>>() {})
        );
        
        HttpEntity<Map<String, Object>> capturedEntity = entityCaptor.getValue();
        Map<String, Object> requestBody = capturedEntity.getBody();
        
        // 解析JSON验证messages结构
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.valueToTree(requestBody);
        
        // 验证messages是数组
        JsonNode messagesNode = rootNode.get("messages");
        assertNotNull(messagesNode, "messages字段不应为null");
        assertTrue(messagesNode.isArray(), "messages应该是数组类型");
        assertEquals(1, messagesNode.size(), "messages数组应该包含1个元素");
        
        // 验证message对象结构
        JsonNode messageNode = messagesNode.get(0);
        assertNotNull(messageNode.get("role"), "message应包含role字段");
        assertEquals("user", messageNode.get("role").asText(), "role应该是'user'");
        
        assertNotNull(messageNode.get("content"), "message应包含content字段");
        String actualContent = messageNode.get("content").asText();
        assertTrue(actualContent.contains(testPrompt), "content应包含输入的提示词");
        assertTrue(actualContent.contains("你是一个智能商品问答助手"), "content应包含系统提示词");
        
        logger.info("messages参数格式验证通过: {}", messagesNode.toString());
    }
    
    /**
     * 调试测试 - 打印实际构建的请求体用于问题排查
     */
    @Test
    void debugRequestBody() throws Exception {
        String testPrompt = "调试测试消息";
        
        // 创建模拟响应
        Map<String, Object> mockResponse = new HashMap<>();
        List<Map<String, Object>> choices = new ArrayList<>();
        Map<String, Object> choice = new HashMap<>();
        Map<String, Object> message = new HashMap<>();
        message.put("content", "调试响应");
        choice.put("message", message);
        choices.add(choice);
        mockResponse.put("choices", choices);
        
        ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        // 设置mock行为
        when(mockRestTemplate.exchange(
                eq("https://api-inference.modelscope.cn/v1/chat/completions"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(new ParameterizedTypeReference<Map<String, Object>>() {})
        )).thenReturn(responseEntity);
        
        // 执行测试
        qwen3Service.callLLM(testPrompt);
        
        // 捕获请求
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(mockRestTemplate).exchange(
                eq("https://api-inference.modelscope.cn/v1/chat/completions"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(new ParameterizedTypeReference<Map<String, Object>>() {})
        );
        
        HttpEntity<Map<String, Object>> capturedEntity = entityCaptor.getValue();
        Map<String, Object> requestBody = capturedEntity.getBody();
        
        System.out.println("=== 实际构建的请求体 ===");
        System.out.println("请求体内容: " + requestBody);
        System.out.println("Model: " + requestBody.get("model"));
        System.out.println("Messages: " + requestBody.get("messages"));
        System.out.println("Max tokens: " + requestBody.get("max_tokens"));
        System.out.println("Temperature: " + requestBody.get("temperature"));
        System.out.println("Enable thinking: " + requestBody.get("enable_thinking"));
        
        // 序列化为JSON格式进行对比
        try {
            String json = objectMapper.writeValueAsString(requestBody);
            System.out.println("\n=== JSON格式请求体 ===");
            System.out.println(json);
            
            // 与curl命令对比
            System.out.println("\n=== 对比成功的curl请求 ===");
            System.out.println("curl -X POST \"https://api-inference.modelscope.cn/v1/chat/completions\" \\");
            System.out.println("  -H \"Content-Type: application/json\" \\");
            System.out.println("  -H \"Authorization: Bearer ms-84ab62cd-bb59-487a-a214-e90349dd7e28\" \\");
            System.out.println("  -d '" + json + "'");
            
        } catch (Exception e) {
            System.out.println("JSON序列化失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证字符串是否为有效的JSON
     */
    private boolean isValidJson(String jsonString) {
        try {
            objectMapper.readTree(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}