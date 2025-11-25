package org.linlinjava.litemall.core.llm.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 商品问答请求模型
 * 用于接收前端发送的问答请求
 */
public class GoodsQARequest {
    
    /**
     * 用户问题
     * 必填项，长度限制1-500字符
     */
    @NotBlank(message = "问题不能为空")
    @Size(min = 1, max = 500, message = "问题长度必须在1-500字符之间")
    private String question;
    
    /**
     * 会话ID
     * 可选，用于多轮对话
     */
    private String sessionId;
    
    /**
     * 用户ID
     * 可选，用于用户个性化
     */
    private Integer userId;
    
    /**
     * 是否启用缓存
     * 默认启用
     */
    private boolean enableCache = true;
    
    /**
     * 超时时间（毫秒）
     * 默认30秒
     */
    private long timeout = 30000;
    
    /**
     * 是否启用多轮对话
     * 默认启用
     */
    private boolean enableConversation = true;
    
    /**
     * 默认构造函数
     */
    public GoodsQARequest() {
    }
    
    /**
     * 带参数的构造函数
     * @param question 用户问题
     */
    public GoodsQARequest(String question) {
        this.question = question;
    }
    
    /**
     * 带参数的构造函数
     * @param question 用户问题
     * @param sessionId 会话ID
     */
    public GoodsQARequest(String question, String sessionId) {
        this.question = question;
        this.sessionId = sessionId;
    }
    
    // Getters and Setters
    public String getQuestion() {
        return question;
    }
    
    public void setQuestion(String question) {
        this.question = question;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public Integer getUserId() {
        return userId;
    }
    
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    
    public boolean isEnableCache() {
        return enableCache;
    }
    
    public void setEnableCache(boolean enableCache) {
        this.enableCache = enableCache;
    }
    
    public long getTimeout() {
        return timeout;
    }
    
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    
    public boolean isEnableConversation() {
        return enableConversation;
    }
    
    public void setEnableConversation(boolean enableConversation) {
        this.enableConversation = enableConversation;
    }
    
    @Override
    public String toString() {
        return "GoodsQARequest{" +
                "question='" + question + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", userId=" + userId +
                ", enableCache=" + enableCache +
                ", timeout=" + timeout +
                ", enableConversation=" + enableConversation +
                '}';
    }
}