package org.linlinjava.litemall.core.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LLM配置类
 * 用于管理LLM服务的配置参数
 */
@Component
@ConfigurationProperties(prefix = "llm")
public class LLMConfig {
    
    // 请求相关配置
    private int maxResults = 10;
    private int timeout = 30000;
    private int retryCount = 3;
    
    // 上下文相关配置
    private int maxHistoryLength = 5;
    private int maxMessageLength = 1000;
    
    // 功能开关
    private boolean enableHotQuestions = true;
    private boolean enableServiceCheck = true;
    private boolean enableQuickQuestions = true;
    
    // 默认构造函数
    public LLMConfig() {
    }
    
    // Getter和Setter方法
    public int getMaxResults() {
        return maxResults;
    }
    
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }
    
    public int getTimeout() {
        return timeout;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    public int getMaxHistoryLength() {
        return maxHistoryLength;
    }
    
    public void setMaxHistoryLength(int maxHistoryLength) {
        this.maxHistoryLength = maxHistoryLength;
    }
    
    public int getMaxMessageLength() {
        return maxMessageLength;
    }
    
    public void setMaxMessageLength(int maxMessageLength) {
        this.maxMessageLength = maxMessageLength;
    }
    
    public boolean isEnableHotQuestions() {
        return enableHotQuestions;
    }
    
    public void setEnableHotQuestions(boolean enableHotQuestions) {
        this.enableHotQuestions = enableHotQuestions;
    }
    
    public boolean isEnableServiceCheck() {
        return enableServiceCheck;
    }
    
    public void setEnableServiceCheck(boolean enableServiceCheck) {
        this.enableServiceCheck = enableServiceCheck;
    }
    
    public boolean isEnableQuickQuestions() {
        return enableQuickQuestions;
    }
    
    public void setEnableQuickQuestions(boolean enableQuickQuestions) {
        this.enableQuickQuestions = enableQuickQuestions;
    }
    
    @Override
    public String toString() {
        return "LLMConfig{" +
               "maxResults=" + maxResults +
               ", timeout=" + timeout +
               ", retryCount=" + retryCount +
               ", maxHistoryLength=" + maxHistoryLength +
               ", maxMessageLength=" + maxMessageLength +
               ", enableHotQuestions=" + enableHotQuestions +
               ", enableServiceCheck=" + enableServiceCheck +
               ", enableQuickQuestions=" + enableQuickQuestions +
               '}';
    }
}