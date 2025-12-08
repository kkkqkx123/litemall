package org.linlinjava.litemall.core.llm.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理LLM服务相关的异常
 */
@ControllerAdvice("org.linlinjava.litemall.core.llm")
public class LLMGlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMGlobalExceptionHandler.class);
    
    /**
     * 处理LLM服务异常
     */
    @ExceptionHandler(LLMServiceException.class)
    public ResponseEntity<Map<String, Object>> handleLLMServiceException(
            LLMServiceException ex, WebRequest request) {
        
        logger.error("LLM服务异常: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorType", "LLM_SERVICE_ERROR");
        errorResponse.put("errorMessage", ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * 处理JSON解析异常
     */
    @ExceptionHandler(JSONParseException.class)
    public ResponseEntity<Map<String, Object>> handleJSONParseException(
            JSONParseException ex, WebRequest request) {
        
        logger.error("JSON解析异常: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorType", "JSON_PARSE_ERROR");
        errorResponse.put("errorMessage", "无法解析LLM返回的数据格式");
        errorResponse.put("originalMessage", ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 处理SQL生成异常
     */
    @ExceptionHandler(SQLGenerationException.class)
    public ResponseEntity<Map<String, Object>> handleSQLGenerationException(
            SQLGenerationException ex, WebRequest request) {
        
        logger.error("SQL生成异常: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorType", "SQL_GENERATION_ERROR");
        errorResponse.put("errorMessage", "无法生成有效的SQL查询");
        errorResponse.put("originalMessage", ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * 处理会话管理异常
     */
    @ExceptionHandler(ConversationException.class)
    public ResponseEntity<Map<String, Object>> handleConversationException(
            ConversationException ex, WebRequest request) {
        
        logger.error("会话管理异常: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorType", "CONVERSATION_ERROR");
        errorResponse.put("errorMessage", "会话管理出现异常");
        errorResponse.put("originalMessage", ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        logger.warn("参数验证异常: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorType", "VALIDATION_ERROR");
        errorResponse.put("errorMessage", "请求参数不合法");
        errorResponse.put("originalMessage", ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 处理服务不可用异常
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailableException(
            ServiceUnavailableException ex, WebRequest request) {
        
        logger.error("服务不可用异常: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorType", "SERVICE_UNAVAILABLE");
        errorResponse.put("errorMessage", "LLM服务暂时不可用");
        errorResponse.put("originalMessage", ex.getMessage());
        errorResponse.put("retryAfter", ex.getRetryAfterSeconds());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }
    
    /**
     * 处理速率限制异常
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitException(
            RateLimitException ex, WebRequest request) {
        
        logger.warn("速率限制异常: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorType", "RATE_LIMIT_ERROR");
        errorResponse.put("errorMessage", "请求过于频繁，请稍后再试");
        errorResponse.put("retryAfter", ex.getRetryAfterSeconds());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }
    
    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(
            Exception ex, WebRequest request) {
        
        logger.error("未捕获的异常: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorType", "INTERNAL_ERROR");
        errorResponse.put("errorMessage", "系统内部错误");
        errorResponse.put("originalMessage", ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * 通用的错误响应构建方法
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            String errorType, String errorMessage, HttpStatus status, WebRequest request) {
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorType", errorType);
        errorResponse.put("errorMessage", errorMessage);
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", request.getDescription(false));
        
        return ResponseEntity.status(status).body(errorResponse);
    }
}