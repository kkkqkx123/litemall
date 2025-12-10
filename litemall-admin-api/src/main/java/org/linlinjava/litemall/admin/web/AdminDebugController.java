package org.linlinjava.litemall.admin.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 调试控制器
 * 用于调试请求头、参数等HTTP信息
 */
@RestController
@RequestMapping("/admin/debug")
@Tag(name = "调试接口", description = "用于调试和测试的接口")
public class AdminDebugController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminDebugController.class);
    
    /**
     * 获取请求头信息
     */
    @GetMapping("/headers")
    @Operation(summary = "获取请求头", description = "返回所有请求头信息")
    public Object getHeaders(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取所有请求头
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        
        result.put("headers", headers);
        result.put("method", request.getMethod());
        result.put("requestUri", request.getRequestURI());
        result.put("queryString", request.getQueryString());
        result.put("remoteAddr", request.getRemoteAddr());
        result.put("remoteHost", request.getRemoteHost());
        result.put("remotePort", request.getRemotePort());
        
        logger.info("获取请求头信息: {}", result);
        return result;
    }
    
    /**
     * 回显请求体内容（用于调试POST请求）
     */
    @PostMapping("/echo")
    @Operation(summary = "回显请求体", description = "返回接收到的请求体内容")
    public Object echoRequest(@RequestBody(required = false) Object body, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        // 基本信息
        result.put("method", request.getMethod());
        result.put("requestUri", request.getRequestURI());
        result.put("contentType", request.getContentType());
        result.put("characterEncoding", request.getCharacterEncoding());
        
        // 请求头
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        result.put("headers", headers);
        
        // 请求体
        result.put("body", body);
        
        // 查询参数
        Map<String, String[]> parameters = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            parameters.put(paramName, request.getParameterValues(paramName));
        }
        result.put("parameters", parameters);
        
        logger.info("回显请求信息: {}", result);
        return result;
    }
    
    /**
     * 测试LLM服务调用（用于调试实际请求）
     */
    @PostMapping("/test-llm")
    @Operation(summary = "测试LLM服务", description = "测试LLM服务调用并返回详细的请求和响应信息")
    public Object testLLMService(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        
        String prompt = request.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            result.put("error", "prompt参数不能为空");
            return result;
        }
        
        try {
            // 这里可以集成实际的LLM服务调用
            // 为了调试目的，我们返回模拟数据
            result.put("prompt", prompt);
            result.put("requestBody", createMockLLMRequest(prompt));
            result.put("response", createMockLLMResponse(prompt));
            result.put("status", "模拟调用成功");
            
            logger.info("LLM测试调用 - 提示词: {}", prompt);
            
        } catch (Exception e) {
            logger.error("LLM测试调用失败", e);
            result.put("error", e.getMessage());
            result.put("status", "调用失败");
        }
        
        return result;
    }
    
    /**
     * 创建模拟的LLM请求体
     */
    private Map<String, Object> createMockLLMRequest(String prompt) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", "qwen3-0.5b");
        
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);
        
        request.put("messages", messages);
        request.put("max_tokens", 100);
        request.put("temperature", 0.7);
        request.put("enable_thinking", false);
        
        return request;
    }
    
    /**
     * 创建模拟的LLM响应
     */
    private Map<String, Object> createMockLLMResponse(String prompt) {
        Map<String, Object> response = new HashMap<>();
        
        Map<String, Object> choice = new HashMap<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "assistant");
        message.put("content", "这是对您的问题的模拟响应: " + prompt);
        choice.put("message", message);
        choice.put("finish_reason", "stop");
        
        List<Map<String, Object>> choices = new ArrayList<>();
        choices.add(choice);
        
        response.put("choices", choices);
        response.put("model", "qwen3-0.5b");
        response.put("usage", Map.of(
            "prompt_tokens", prompt.length(),
            "completion_tokens", 20,
            "total_tokens", prompt.length() + 20
        ));
        
        return response;
    }
    
    /**
     * 获取系统信息
     */
    @GetMapping("/system-info")
    @Operation(summary = "获取系统信息", description = "返回系统相关的调试信息")
    public Object getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // JVM信息
        Runtime runtime = Runtime.getRuntime();
        info.put("jvm", Map.of(
            "availableProcessors", runtime.availableProcessors(),
            "freeMemory", runtime.freeMemory(),
            "totalMemory", runtime.totalMemory(),
            "maxMemory", runtime.maxMemory()
        ));
        
        // 系统属性
        Properties props = System.getProperties();
        info.put("javaVersion", props.getProperty("java.version"));
        info.put("javaVendor", props.getProperty("java.vendor"));
        info.put("osName", props.getProperty("os.name"));
        info.put("osVersion", props.getProperty("os.version"));
        info.put("userDir", props.getProperty("user.dir"));
        
        // 时间信息
        info.put("currentTime", System.currentTimeMillis());
        info.put("timestamp", new Date().toString());
        
        logger.info("获取系统信息: {}", info);
        return info;
    }
}