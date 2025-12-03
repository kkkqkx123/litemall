package org.linlinjava.litemall.core.llm.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 会话模型
 * 用于管理多轮对话的会话状态
 */
public class ConversationSession {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 会话开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;
    
    /**
     * 对话历史
     */
    private List<ConversationTurn> conversationHistory;
    
    /**
     * 当前上下文
     */
    private ConversationContext context;
    
    /**
     * 会话是否过期
     */
    private boolean expired;
    
    /**
     * 会话过期时间（分钟）
     */
    private static final int SESSION_TIMEOUT_MINUTES = 30;
    
    /**
     * 默认构造函数
     */
    public ConversationSession() {
        this.sessionId = UUID.randomUUID().toString();
        this.startTime = LocalDateTime.now();
        this.lastActiveTime = LocalDateTime.now();
        this.conversationHistory = new ArrayList<>();
        this.context = new ConversationContext();
        this.expired = false;
    }
    
    /**
     * 带用户ID的构造函数
     * @param userId 用户ID
     */
    public ConversationSession(String userId) {
        this();
        this.userId = userId;
    }
    
    /**
     * 添加对话轮次
     * @param question 用户问题
     * @param answer 系统回答
     * @param queryIntent 查询意图
     */
    public void addTurn(String question, String answer, QueryIntent queryIntent) {
        ConversationTurn turn = new ConversationTurn();
        turn.setQuestion(question);
        turn.setAnswer(answer);
        turn.setQueryIntent(queryIntent);
        turn.setTimestamp(LocalDateTime.now());
        
        conversationHistory.add(turn);
        lastActiveTime = LocalDateTime.now();
        
        // 更新上下文
        updateContext(queryIntent);
    }
    
    /**
     * 更新上下文
     * @param queryIntent 查询意图
     */
    private void updateContext(QueryIntent queryIntent) {
        if (queryIntent != null) {
            context.setLastQueryType(queryIntent.getQueryType());
            context.setLastConditions(queryIntent.getConditions());
            context.setLastSort(queryIntent.getSort());
            context.setLastLimit(queryIntent.getLimit());
        }
    }
    
    /**
     * 检查会话是否过期
     * @return true表示过期，false表示未过期
     */
    public boolean isSessionExpired() {
        if (expired) {
            return true;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = lastActiveTime.plusMinutes(SESSION_TIMEOUT_MINUTES);
        return now.isAfter(expireTime);
    }
    
    /**
     * 获取最近N条对话历史
     * @param count 数量
     * @return 对话历史列表
     */
    public List<ConversationTurn> getRecentHistory(int count) {
        if (conversationHistory.isEmpty()) {
            return new ArrayList<>();
        }
        
        int startIndex = Math.max(0, conversationHistory.size() - count);
        return conversationHistory.subList(startIndex, conversationHistory.size());
    }
    
    /**
     * 清除过期状态并更新最后活跃时间
     */
    public void refresh() {
        this.lastActiveTime = LocalDateTime.now();
        this.expired = false;
    }
    
    /**
     * 标记会话为过期
     */
    public void markExpired() {
        this.expired = true;
    }
    
    /**
     * 获取对话轮次数量
     * @return 轮次数量
     */
    public int getTurnCount() {
        return conversationHistory.size();
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public LocalDateTime getCreatedAt() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getLastActiveTime() {
        return lastActiveTime;
    }
    
    public LocalDateTime getLastAccessed() {
        return lastActiveTime;
    }
    
    public void setLastActiveTime(LocalDateTime lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }
    
    public List<ConversationTurn> getConversationHistory() {
        return conversationHistory;
    }
    
    public void setConversationHistory(List<ConversationTurn> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }
    
    public ConversationContext getContext() {
        return context;
    }
    
    public void setContext(ConversationContext context) {
        this.context = context;
    }
    
    public boolean isExpired() {
        return expired;
    }
    
    public void setExpired(boolean expired) {
        this.expired = expired;
    }
    
    @Override
    public String toString() {
        return "ConversationSession{" +
                "sessionId='" + sessionId + '\'' +
                ", userId=" + userId +
                ", startTime=" + startTime +
                ", lastActiveTime=" + lastActiveTime +
                ", turnCount=" + getTurnCount() +
                ", expired=" + expired +
                '}';
    }
    
    /**
     * 对话轮次内部类
     */
    public static class ConversationTurn {
        private String question;
        private String answer;
        private QueryIntent queryIntent;
        private LocalDateTime timestamp;
        
        // Getters and Setters
        public String getQuestion() {
            return question;
        }
        
        public void setQuestion(String question) {
            this.question = question;
        }
        
        public String getUserQuery() {
            return question;
        }
        
        public String getAnswer() {
            return answer;
        }
        
        public String getAssistantResponse() {
            return answer;
        }
        
        public void setAnswer(String answer) {
            this.answer = answer;
        }
        
        public QueryIntent getQueryIntent() {
            return queryIntent;
        }
        
        public void setQueryIntent(QueryIntent queryIntent) {
            this.queryIntent = queryIntent;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }
    
    /**
     * 对话上下文内部类
     */
    public static class ConversationContext {
        private String lastQueryType;
        private java.util.Map<String, Object> lastConditions;
        private String lastSort;
        private Integer lastLimit;
        
        // Getters and Setters
        public String getLastQueryType() {
            return lastQueryType;
        }
        
        public void setLastQueryType(String lastQueryType) {
            this.lastQueryType = lastQueryType;
        }
        
        public java.util.Map<String, Object> getLastConditions() {
            return lastConditions;
        }
        
        public void setLastConditions(java.util.Map<String, Object> lastConditions) {
            this.lastConditions = lastConditions;
        }
        
        public String getLastSort() {
            return lastSort;
        }
        
        public void setLastSort(String lastSort) {
            this.lastSort = lastSort;
        }
        
        public Integer getLastLimit() {
            return lastLimit;
        }
        
        public void setLastLimit(Integer lastLimit) {
            this.lastLimit = lastLimit;
        }
    }
}