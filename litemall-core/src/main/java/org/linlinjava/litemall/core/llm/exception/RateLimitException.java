package org.linlinjava.litemall.core.llm.exception;

/**
 * 速率限制异常
 */
public class RateLimitException extends LLMServiceException {
    
    private final int retryAfterSeconds;
    private final String limitType;
    private final long currentUsage;
    private final long limit;
    
    public RateLimitException(String message) {
        super(message);
        this.retryAfterSeconds = 60; // 默认60秒后重试
        this.limitType = "unknown";
        this.currentUsage = 0;
        this.limit = 0;
    }
    
    public RateLimitException(String message, String limitType, long currentUsage, long limit, int retryAfterSeconds) {
        super(message);
        this.limitType = limitType;
        this.currentUsage = currentUsage;
        this.limit = limit;
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
        this.retryAfterSeconds = 60;
        this.limitType = "unknown";
        this.currentUsage = 0;
        this.limit = 0;
    }
    
    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
    
    public String getLimitType() {
        return limitType;
    }
    
    public long getCurrentUsage() {
        return currentUsage;
    }
    
    public long getLimit() {
        return limit;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RateLimitException: ").append(getMessage());
        sb.append(", 限制类型: ").append(limitType);
        sb.append(", 当前使用量: ").append(currentUsage);
        sb.append(", 限制: ").append(limit);
        sb.append(", 重试等待时间: ").append(retryAfterSeconds).append("秒");
        return sb.toString();
    }
}