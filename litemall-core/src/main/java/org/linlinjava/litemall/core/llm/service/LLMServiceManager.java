package org.linlinjava.litemall.core.llm.service;

import org.linlinjava.litemall.core.llm.exception.LLMServiceException;
import org.linlinjava.litemall.core.llm.exception.LLMOutputParseException;
import org.linlinjava.litemall.core.llm.model.ConversationSession;
import org.linlinjava.litemall.core.llm.model.QueryIntent;
import org.linlinjava.litemall.core.llm.parser.LLMOutputParser;
import org.linlinjava.litemall.core.llm.parser.SQLGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * LLM服务管理器
 * 统一处理LLM服务调用、错误处理和异常管理
 */
@Service
public class LLMServiceManager {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMServiceManager.class);
    
    @Autowired
    private Qwen3Service qwen3Service;
    
    @Autowired
    private LLMOutputParser llmOutputParser;
    
    @Autowired
    private SQLGenerator sqlGenerator;
    
    @Autowired
    private ConversationManager conversationManager;
    
    // 服务状态缓存
    private final Map<String, ServiceStatus> serviceStatusCache = new ConcurrentHashMap<>();
    
    // 请求统计
    private final Map<String, RequestStats> requestStats = new ConcurrentHashMap<>();
    
    /**
     * 处理用户查询请求
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param userQuery 用户查询
     * @return 处理结果
     */
    public QueryResult processUserQuery(String userId, String sessionId, String userQuery) {
        long startTime = System.currentTimeMillis();
        String requestId = generateRequestId();
        
        try {
            logger.info("[{}] 开始处理用户查询 - userId: {}, sessionId: {}, query: {}", 
                       requestId, userId, sessionId, userQuery);
            
            // 1. 获取或创建会话（通过sessionId直接操作）
            
            // 2. 构建会话上下文
            String sessionContext = conversationManager.buildSessionContext(sessionId);
            
            // 3. 调用LLM服务（移除重试逻辑，直接调用一次）
            String llmResponse = qwen3Service.callLLM(userQuery, sessionContext);
            
            // 4. 解析LLM输出
            QueryIntent queryIntent = parseLLMOutput(llmResponse, requestId);
            
            // 5. 生成SQL查询
            String sqlQuery = generateSQLQuery(queryIntent, requestId);
            
            // 6. 更新会话状态
            conversationManager.addTurn(sessionId, userQuery, llmResponse, queryIntent);
            
            // 7. 构建结果
            QueryResult result = buildQueryResult(queryIntent, sqlQuery, llmResponse);
            
            // 记录成功统计
            recordSuccess(userId, requestId, System.currentTimeMillis() - startTime);
            
            logger.info("[{}] 用户查询处理完成 - userId: {}, query: {}, resultType: {}", 
                       requestId, userId, userQuery, queryIntent.getQueryType());
            
            return result;
            
        } catch (LLMServiceException e) {
            logger.error("[{}] LLM服务异常 - userId: {}, error: {}", requestId, userId, e.getMessage());
            recordFailure(userId, requestId, "LLM_SERVICE_ERROR", e.getMessage());
            return buildErrorResult("LLM服务异常，请稍后重试", e);
            
        } catch (Exception e) {
            logger.error("[{}] 未知异常 - userId: {}, error: {}", requestId, userId, e.getMessage(), e);
            recordFailure(userId, requestId, "UNKNOWN_ERROR", e.getMessage());
            return buildErrorResult("系统异常，请联系技术支持", e);
        }
    }
    

    
    /**
     * 解析LLM输出
     */
    private QueryIntent parseLLMOutput(String llmResponse, String requestId) throws LLMServiceException {
        try {
            logger.debug("[{}] 开始解析LLM输出 - response: {}", requestId, llmResponse);
            
            QueryIntent queryIntent = llmOutputParser.parseQueryIntent(llmResponse);
            
            if (queryIntent == null) {
                throw new LLMServiceException("无法解析LLM输出为有效的查询意图");
            }
            
            logger.debug("[{}] LLM输出解析成功 - queryType: {}", requestId, queryIntent.getQueryType());
            return queryIntent;
            
        } catch (LLMOutputParseException e) {
            throw new LLMServiceException("解析LLM输出失败: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new LLMServiceException("解析LLM输出失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成SQL查询
     */
    private String generateSQLQuery(QueryIntent queryIntent, String requestId) throws LLMServiceException {
        try {
            logger.debug("[{}] 开始生成SQL查询 - queryType: {}", requestId, queryIntent.getQueryType());
            
            String sqlQuery = sqlGenerator.generateSQL(queryIntent);
            
            if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
                throw new LLMServiceException("生成的SQL查询为空");
            }
            
            logger.debug("[{}] SQL查询生成成功 - sql: {}", requestId, sqlQuery);
            return sqlQuery;
            
        } catch (Exception e) {
            throw new LLMServiceException("生成SQL查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建查询结果
     */
    private QueryResult buildQueryResult(QueryIntent queryIntent, String sqlQuery, String llmResponse) {
        QueryResult result = new QueryResult();
        result.setSuccess(true);
        result.setQueryIntent(queryIntent);
        result.setSqlQuery(sqlQuery);
        result.setLlmResponse(llmResponse);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }
    
    /**
     * 构建错误结果
     */
    private QueryResult buildErrorResult(String errorMessage, Exception exception) {
        QueryResult result = new QueryResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setErrorType(exception != null ? exception.getClass().getSimpleName() : "UNKNOWN");
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }
    
    /**
     * 记录成功统计
     */
    private void recordSuccess(String userId, String requestId, long duration) {
        RequestStats stats = requestStats.computeIfAbsent(userId, k -> new RequestStats());
        stats.totalRequests++;
        stats.successfulRequests++;
        stats.totalDuration += duration;
        stats.lastRequestTime = System.currentTimeMillis();
    }
    
    /**
     * 记录失败统计
     */
    private void recordFailure(String userId, String requestId, String errorType, String errorMessage) {
        RequestStats stats = requestStats.computeIfAbsent(userId, k -> new RequestStats());
        stats.totalRequests++;
        stats.failedRequests++;
        stats.lastErrorType = errorType;
        stats.lastErrorMessage = errorMessage;
        stats.lastRequestTime = System.currentTimeMillis();
    }
    
    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return "REQ_" + System.currentTimeMillis() + "_" + Thread.currentThread().threadId();
    }
    
    /**
     * 获取服务状态
     */
    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("llmServiceStatus", qwen3Service.getServiceStatus());
        status.put("conversationManagerStatus", conversationManager.getServiceStatus());
        status.put("totalRequests", requestStats.values().stream().mapToLong(s -> s.totalRequests).sum());
        status.put("successfulRequests", requestStats.values().stream().mapToLong(s -> s.successfulRequests).sum());
        status.put("failedRequests", requestStats.values().stream().mapToLong(s -> s.failedRequests).sum());
        status.put("avgResponseTime", calculateAverageResponseTime());
        status.put("lastErrorInfo", getLastErrorInfo());
        status.put("timestamp", System.currentTimeMillis());
        return status;
    }
    
    /**
     * 计算平均响应时间
     * @return 平均响应时间（毫秒）
     */
    private long calculateAverageResponseTime() {
        return requestStats.values().stream()
                .filter(s -> s.totalRequests > 0)
                .mapToLong(s -> s.totalRequests > 0 ? s.totalDuration / s.totalRequests : 0)
                .sum();
    }
    
    /**
     * 获取最近的错误信息
     * @return 最近的错误信息
     */
    private Map<String, Object> getLastErrorInfo() {
        return requestStats.values().stream()
                .filter(s -> s.lastErrorType != null)
                .max((s1, s2) -> Long.compare(s1.lastRequestTime, s2.lastRequestTime))
                .map(s -> {
                    Map<String, Object> errorInfo = new HashMap<>();
                    errorInfo.put("errorType", s.lastErrorType);
                    errorInfo.put("errorMessage", s.lastErrorMessage);
                    errorInfo.put("errorTime", s.lastRequestTime);
                    return errorInfo;
                })
                .orElse(new HashMap<>());
    }
    
    /**
     * 清理过期数据
     */
    public void cleanup() {
        logger.info("开始清理LLM服务管理器数据");
        conversationManager.cleanupExpiredSessions();
        serviceStatusCache.clear();
        logger.info("LLM服务管理器数据清理完成");
    }
    
    /**
     * 查询结果类
     */
    public static class QueryResult {
        private boolean success;
        private QueryIntent queryIntent;
        private String sqlQuery;
        private String llmResponse;
        private String errorMessage;
        private String errorType;
        private long timestamp;
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public QueryIntent getQueryIntent() { return queryIntent; }
        public void setQueryIntent(QueryIntent queryIntent) { this.queryIntent = queryIntent; }
        
        public String getSqlQuery() { return sqlQuery; }
        public void setSqlQuery(String sqlQuery) { this.sqlQuery = sqlQuery; }
        
        public String getLlmResponse() { return llmResponse; }
        public void setLlmResponse(String llmResponse) { this.llmResponse = llmResponse; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getErrorType() { return errorType; }
        public void setErrorType(String errorType) { this.errorType = errorType; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
    
    /**
     * 服务状态类
     */
    public static class ServiceStatus {
        public Map<String, Object> llmServiceStatus;
        public Map<String, Object> conversationManagerStatus;
        public long totalRequests;
        public long successfulRequests;
        public long failedRequests;
        public long timestamp;
    }
    
    /**
     * 请求统计类
     */
    private static class RequestStats {
        long totalRequests = 0;
        long successfulRequests = 0;
        long failedRequests = 0;
        long totalDuration = 0;
        long lastRequestTime = 0;
        String lastErrorType;
        String lastErrorMessage;
    }
}