package org.linlinjava.litemall.core.llm.service;

import okhttp3.*;
import okio.Buffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Qwen3Service HTTP请求捕获测试
 * 用于捕获和验证实际发送的HTTP请求
 */
@ExtendWith(MockitoExtension.class)
public class Qwen3ServiceHttpCaptureTest {
    
    private static final Logger logger = LoggerFactory.getLogger(Qwen3ServiceHttpCaptureTest.class);
    
    private Qwen3Service qwen3Service;
    private OkHttpClient httpClient;
    private AtomicReference<String> capturedRequestBody;
    private AtomicReference<String> capturedAuthorization;
    
    @BeforeEach
    void setUp() {
        capturedRequestBody = new AtomicReference<>();
        capturedAuthorization = new AtomicReference<>();
        
        // 创建自定义拦截器来捕获请求
        Interceptor requestCaptureInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                
                // 捕获授权头
                String auth = request.header("Authorization");
                if (auth != null) {
                    capturedAuthorization.set(auth);
                    logger.info("捕获到Authorization头: {}", auth);
                }
                
                // 捕获请求体
                RequestBody body = request.body();
                if (body != null) {
                    Buffer buffer = new Buffer();
                    body.writeTo(buffer);
                    String bodyString = buffer.readUtf8();
                    capturedRequestBody.set(bodyString);
                    logger.info("捕获到请求体: {}", bodyString);
                }
                
                // 创建模拟响应
                String mockResponse = "{\n" +
                        "  \"choices\": [{\n" +
                        "    \"message\": {\n" +
                        "      \"content\": \"测试响应\"\n" +
                        "    }\n" +
                        "  }]\n" +
                        "}";
                
                return new Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                .body(ResponseBody.create(MediaType.parse("application/json"), mockResponse))
                .build();
            }
        };
        
        httpClient = new OkHttpClient.Builder()
                .addInterceptor(requestCaptureInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        qwen3Service = new Qwen3Service();
        
        // 使用反射设置HTTP客户端
        try {
            java.lang.reflect.Field field = Qwen3Service.class.getDeclaredField("httpClient");
            field.setAccessible(true);
            field.set(qwen3Service, httpClient);
        } catch (Exception e) {
            logger.error("设置HTTP客户端失败", e);
        }
    }
    
    /**
     * 测试实际HTTP请求捕获
     */
    @Test
    void testActualHttpRequestCapture() {
        String testPrompt = "你好，请简单介绍一下你自己";
        
        // 执行调用
        String result = qwen3Service.callLLM(testPrompt);
        
        // 验证结果
        assertNotNull(result, "响应不应为null");
        assertEquals("测试响应", result, "响应内容应匹配");
        
        // 验证捕获的请求信息
        assertNotNull(capturedRequestBody.get(), "应捕获到请求体");
        assertNotNull(capturedAuthorization.get(), "应捕获到Authorization头");
        
        String requestBody = capturedRequestBody.get();
        String authHeader = capturedAuthorization.get();
        
        logger.info("=== 捕获的请求信息 ===");
        logger.info("Authorization头: {}", authHeader);
        logger.info("请求体: {}", requestBody);
        logger.info("====================");
        
        // 验证请求体格式
        assertTrue(requestBody.contains("\"model\""), "请求体应包含model字段");
        assertTrue(requestBody.contains("\"messages\""), "请求体应包含messages字段");
        assertTrue(requestBody.contains("\"role\""), "请求体应包含role字段");
        assertTrue(requestBody.contains("\"content\""), "请求体应包含content字段");
        assertTrue(requestBody.contains("\"user\""), "messages中应包含user角色");
        assertTrue(requestBody.contains(testPrompt), "content应包含用户输入的提示词");
        
        // 验证Authorization格式
        assertTrue(authHeader.startsWith("Bearer "), "Authorization应以Bearer开头");
        assertTrue(authHeader.length() > 10, "Authorization应有合理的长度");
    }
    
    /**
     * 测试复杂提示词的请求构建
     */
    @Test
    void testComplexPromptRequest() {
        String complexPrompt = "你是一个商品问答助手，请根据用户的问题生成合适的商品查询意图。\n" +
                "用户问题：请推荐一些价格在100-500元之间的商品\n" +
                "请严格按照JSON格式输出：";
        
        String result = qwen3Service.callLLM(complexPrompt);
        
        assertNotNull(result, "响应不应为null");
        
        String requestBody = capturedRequestBody.get();
        assertNotNull(requestBody, "应捕获到请求体");
        
        logger.info("复杂提示词请求体: {}", requestBody);
        
        // 验证复杂内容被正确处理
        assertTrue(requestBody.contains(complexPrompt), "请求体应包含完整的复杂提示词");
        assertTrue(requestBody.contains("\"max_tokens\""), "请求体应包含max_tokens");
        assertTrue(requestBody.contains("\"temperature\""), "请求体应包含temperature");
        assertTrue(requestBody.contains("\"enable_thinking\""), "请求体应包含enable_thinking");
    }
    
    /**
     * 测试特殊字符处理
     */
    @Test
    void testSpecialCharactersInPrompt() {
        String specialPrompt = "测试特殊字符：\"quotes\", 'apostrophes', \n换行, \t制表符, \\反斜杠";
        
        String result = qwen3Service.callLLM(specialPrompt);
        
        assertNotNull(result, "响应不应为null");
        
        String requestBody = capturedRequestBody.get();
        assertNotNull(requestBody, "应捕获到请求体");
        
        logger.info("特殊字符请求体: {}", requestBody);
        
        // 验证特殊字符被正确转义
        assertTrue(requestBody.contains("\"quotes\""), "双引号应被正确转义");
        assertTrue(requestBody.contains("\\n"), "换行符应被正确转义");
        assertTrue(requestBody.contains("\\t"), "制表符应被正确转义");
        assertTrue(requestBody.contains("\\\\"), "反斜杠应被正确转义");
    }
}