package org.linlinjava.litemall.core.llm.exception;

/**
 * 服务不可用异常
 */
public class ServiceUnavailableException extends LLMServiceException {
    
    private final int retryAfterSeconds;
    private final String serviceName;
    
    public ServiceUnavailableException(String message) {
        super(message);
        this.retryAfterSeconds = 60; // 默认60秒后重试
        this.serviceName = "unknown";
    }
    
    public ServiceUnavailableException(String message, String serviceName, int retryAfterSeconds) {
        super(message);
        this.serviceName = serviceName;
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
        this.retryAfterSeconds = 60;
        this.serviceName = "unknown";
    }
    
    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ServiceUnavailableException: ").append(getMessage());
        sb.append(", 服务名称: ").append(serviceName);
        sb.append(", 重试等待时间: ").append(retryAfterSeconds).append("秒");
        return sb.toString();
    }
}