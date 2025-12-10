package org.linlinjava.litemall.core.llm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import okio.Buffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Qwen3Service集成测试
 * 用于验证实际请求体的构建情况
 */
@ExtendWith(MockitoExtension.class)
public class Qwen3ServiceIntegrationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(Qwen3ServiceIntegrationTest.class);
    
    @Mock
    private OkHttpClient mockHttpClient;
    
    @Mock
    private Call mockCall;
    
    private Qwen3Service qwen3Service;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        qwen3Service = new Qwen3Service();
        objectMapper = new ObjectMapper();
        
        // 使用反射设置mock的httpClient
        try {
            java.lang.reflect.Field field = Qwen3Service.class.getDeclaredField("httpClient");
            field.setAccessible(true);
            field.set(qwen3Service, mockHttpClient);
        } catch (Exception e) {
            logger.error("设置mock httpClient失败", e);
        }
    }
    
    /**
     * 测试请求体构建 - 验证实际发送的JSON格式
     */
    @Test
    void testRequestBodyConstruction() throws Exception {
        // 准备测试数据
        String testPrompt = "你好，请简单介绍一下你自己";
        
        // 创建模拟响应
        String mockResponse = "{\n" +
                "  \"choices\": [{\n" +
                "    \"message\": {\n" +
                "      \"content\": \"我是一个AI助手\"\n" +
                "    }\n" +
                "  }]\n" +
                "}";
        
        ResponseBody responseBody = ResponseBody.create(
                MediaType.parse("application/json"),
                mockResponse
        );
        
        Response response = new Response.Builder()
                .request(new Request.Builder().url("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generate").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(responseBody)
                .build();
        
        // 设置mock行为
        when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(response);
        
        // 执行测试
        String result = qwen3Service.callLLM(testPrompt);
        
        // 捕获实际发送的请求
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mockHttpClient).newCall(requestCaptor.capture());
        
        Request capturedRequest = requestCaptor.getValue();
        
        // 验证请求基本信息
        assertEquals("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generate", 
                    capturedRequest.url().toString());
        assertEquals("POST", capturedRequest.method());
        
        // 验证请求头
        assertEquals("application/json", capturedRequest.header("Content-Type"));
        assertNotNull(capturedRequest.header("Authorization"));
        assertTrue(capturedRequest.header("Authorization").startsWith("Bearer "));
        
        // 验证请求体
        RequestBody requestBody = capturedRequest.body();
        assertNotNull(requestBody);
        
        // 读取请求体内容
        Buffer buffer = new Buffer();
        requestBody.writeTo(buffer);
        String requestBodyString = buffer.readUtf8();
        
        logger.info("实际发送的请求体: {}", requestBodyString);
        
        // 验证请求体是有效的JSON
        assertTrue(isValidJson(requestBodyString), "请求体应该是有效的JSON格式");
        
        // 验证请求体包含必要的字段
        assertTrue(requestBodyString.contains("\"model\""), "请求体应包含model字段");
        assertTrue(requestBodyString.contains("\"messages\""), "请求体应包含messages字段");
        assertTrue(requestBodyString.contains("\"role\""), "请求体应包含role字段");
        assertTrue(requestBodyString.contains("\"content\""), "请求体应包含content字段");
        assertTrue(requestBodyString.contains("\"user\""), "messages中应包含user角色");
        assertTrue(requestBodyString.contains(testPrompt), "content应包含用户输入的提示词");
        
        // 验证其他字段
        assertTrue(requestBodyString.contains("\"max_tokens\""), "请求体应包含max_tokens字段");
        assertTrue(requestBodyString.contains("\"temperature\""), "请求体应包含temperature字段");
        assertTrue(requestBodyString.contains("\"enable_thinking\""), "请求体应包含enable_thinking字段");
        
        logger.info("请求体验证通过");
    }
    
    /**
     * 测试messages参数的具体格式
     */
    @Test
    void testMessagesParameterFormat() throws Exception {
        String testPrompt = "测试消息格式";
        
        // 创建模拟响应
        ResponseBody responseBody = ResponseBody.create(
                MediaType.parse("application/json"),
                "{\"choices\": [{\"message\": {\"content\": \"测试响应\"}}]}"
        );
        
        Response response = new Response.Builder()
                .request(new Request.Builder().url("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generate").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(responseBody)
                .build();
        
        when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(response);
        
        // 执行测试
        qwen3Service.callLLM(testPrompt);
        
        // 捕获请求
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mockHttpClient).newCall(requestCaptor.capture());
        
        Request capturedRequest = requestCaptor.getValue();
        RequestBody requestBody = capturedRequest.body();
        
        Buffer buffer = new Buffer();
        requestBody.writeTo(buffer);
        String requestBodyString = buffer.readUtf8();
        
        // 解析JSON验证messages结构
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(requestBodyString);
        
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
        assertEquals(testPrompt, messageNode.get("content").asText(), "content应该匹配输入的提示词");
        
        logger.info("messages参数格式验证通过: {}", messagesNode.toString());
    }
    
    /**
     * 测试重试机制下的请求体一致性
     */
    @Test
    void testRetryRequestBodyConsistency() throws Exception {
        String testPrompt = "测试重试机制";
        
        // 第一次调用失败
        ResponseBody errorResponseBody = ResponseBody.create(
                MediaType.parse("application/json"),
                "{\"error\": {\"code\": \"missing_required_parameter\", \"message\": \"缺少必要参数\"}}"
        );
        
        Response errorResponse = new Response.Builder()
                .request(new Request.Builder().url("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generate").build())
                .protocol(Protocol.HTTP_1_1)
                .code(400)
                .message("Bad Request")
                .body(errorResponseBody)
                .build();
        
        // 第二次调用成功
        ResponseBody successResponseBody = ResponseBody.create(
                MediaType.parse("application/json"),
                "{\"choices\": [{\"message\": {\"content\": \"重试成功\"}}]}"
        );
        
        Response successResponse = new Response.Builder()
                .request(new Request.Builder().url("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generate").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(successResponseBody)
                .build();
        
        when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute())
                .thenReturn(errorResponse)  // 第一次失败
                .thenReturn(successResponse); // 第二次成功
        
        // 执行测试（预期会重试）
        String result = qwen3Service.callLLM(testPrompt);
        
        // 验证调用了两次
        verify(mockHttpClient, times(2)).newCall(any(Request.class));
        
        // 捕获所有请求
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mockHttpClient, times(2)).newCall(requestCaptor.capture());
        
        List<Request> allRequests = requestCaptor.getAllValues();
        assertEquals(2, allRequests.size(), "应该发送了2个请求");
        
        // 验证两次请求的body内容一致
        for (Request request : allRequests) {
            RequestBody requestBody = request.body();
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);
            String requestBodyString = buffer.readUtf8();
            
            // 验证每次请求都包含messages参数
            assertTrue(requestBodyString.contains("\"messages\""), "每次请求都应包含messages字段");
            assertTrue(requestBodyString.contains(testPrompt), "每次请求都应包含正确的提示词");
            
            logger.info("重试请求体: {}", requestBodyString);
        }
        
        logger.info("重试机制下的请求体一致性验证通过");
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