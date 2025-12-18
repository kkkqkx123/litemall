package org.linlinjava.litemall.core.llm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 会话管理服务
 * 负责管理用户会话，包括会话创建、状态维护、超时处理等
 */
@Service
public class LLMSessionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMSessionManager.class);
    
    // 会话超时时间（30分钟）
    private static final long SESSION_TIMEOUT = 30 * 60 * 1000L;
    
    // 会话清理间隔（5分钟）
    private static final long CLEANUP_INTERVAL = 5 * 60 * 1000L;
    
    // 会话存储
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    
    // 会话ID生成器
    private final AtomicLong sessionIdGenerator = new AtomicLong(System.currentTimeMillis());
    
    // 定时清理任务
    private final ScheduledExecutorService cleanupExecutor;
    
    public LLMSessionManager() {
        // 启动定时清理任务
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "SessionCleanupThread");
            thread.setDaemon(true);
            return thread;
        });
        
        cleanupExecutor.scheduleWithFixedDelay(
            this::cleanupExpiredSessions,
            CLEANUP_INTERVAL,
            CLEANUP_INTERVAL,
            TimeUnit.MILLISECONDS
        );
        
        logger.info("会话管理器已启动，清理间隔：{}分钟", CLEANUP_INTERVAL / (60 * 1000));
    }
    
    /**
     * 创建新会话
     * @param userId 用户ID
     * @return 会话ID
     */
    public String createSession(Integer userId) {
        String sessionId = generateSessionId();
        Session session = new Session(sessionId, userId);
        sessions.put(sessionId, session);
        
        logger.debug("创建新会话：sessionId={}, userId={}", sessionId, userId);
        return sessionId;
    }
    
    /**
     * 获取会话
     * @param sessionId 会话ID
     * @return 会话信息，不存在或已过期返回null
     */
    public Session getSession(String sessionId) {
        Session session = sessions.get(sessionId);
        
        if (session == null) {
            logger.debug("会话不存在：sessionId={}", sessionId);
            return null;
        }
        
        if (session.isExpired()) {
            sessions.remove(sessionId);
            logger.debug("会话已过期并移除：sessionId={}", sessionId);
            return null;
        }
        
        // 更新最后访问时间
        session.updateLastAccessTime();
        return session;
    }
    
    /**
     * 更新会话状态
     * @param sessionId 会话ID
     * @param context 上下文信息
     */
    public void updateSessionContext(String sessionId, Map<String, Object> context) {
        Session session = getSession(sessionId);
        if (session != null) {
            session.updateContext(context);
            logger.debug("更新会话上下文：sessionId={}", sessionId);
        }
    }
    
    /**
     * 添加会话消息
     * @param sessionId 会话ID
     * @param message 消息内容
     * @param messageType 消息类型（user/assistant）
     */
    public void addSessionMessage(String sessionId, String message, String messageType) {
        Session session = getSession(sessionId);
        if (session != null) {
            session.addMessage(message, messageType);
            logger.debug("添加会话消息：sessionId={}, type={}", sessionId, messageType);
        }
    }
    
    /**
     * 销毁会话
     * @param sessionId 会话ID
     */
    public void destroySession(String sessionId) {
        Session removed = sessions.remove(sessionId);
        if (removed != null) {
            logger.info("销毁会话：sessionId={}, userId={}", sessionId, removed.getUserId());
        }
    }
    
    /**
     * 获取活跃会话数
     * @return 活跃会话数量
     */
    public int getActiveSessionCount() {
        cleanupExpiredSessions();
        return sessions.size();
    }
    
    /**
     * 生成会话统计信息
     * @return 统计信息
     */
    public Map<String, Object> getSessionStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalSessions", sessions.size());
        stats.put("activeSessions", getActiveSessionCount());
        stats.put("sessionTimeout", SESSION_TIMEOUT);
        
        return stats;
    }
    
    /**
     * 获取会话数量
     * @return 会话数量
     */
    public int getSessionCount() {
        return sessions.size();
    }
    
    /**
     * 获取统计信息（兼容方法）
     * @return 统计信息
     */
    public Map<String, Object> getStatistics() {
        return getSessionStatistics();
    }
    
    /**
     * 生成会话ID
     * @return 会话ID
     */
    private String generateSessionId() {
        return "session_" + sessionIdGenerator.incrementAndGet() + "_" + System.nanoTime();
    }
    
    /**
     * 清理过期会话
     */
    private void cleanupExpiredSessions() {
        int expiredCount = 0;
        
        for (Map.Entry<String, Session> entry : sessions.entrySet()) {
            if (entry.getValue().isExpired()) {
                sessions.remove(entry.getKey());
                expiredCount++;
            }
        }
        
        if (expiredCount > 0) {
            logger.info("清理过期会话：{}个", expiredCount);
        }
    }
    
    /**
     * 销毁会话管理器
     */
    public void destroy() {
        if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        sessions.clear();
        logger.info("会话管理器已销毁");
    }
    
    /**
     * 会话实体类
     */
    public static class Session {
        private final String sessionId;
        private final Integer userId;
        private long lastAccessTime;
        private final long createTime;
        private Map<String, Object> context;
        private final List<Message> messages;
        private int messageCount;
        
        private static final int MAX_MESSAGES = 50; // 最大消息数
        
        public Session(String sessionId, Integer userId) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.createTime = System.currentTimeMillis();
            this.lastAccessTime = createTime;
            this.context = new java.util.HashMap<>();
            this.messages = new java.util.ArrayList<>();
            this.messageCount = 0;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public Integer getUserId() {
            return userId;
        }
        
        public long getLastAccessTime() {
            return lastAccessTime;
        }
        
        public long getCreateTime() {
            return createTime;
        }
        
        public Map<String, Object> getContext() {
            return new java.util.HashMap<>(context);
        }
        
        public List<Message> getMessages() {
            return new java.util.ArrayList<>(messages);
        }
        
        public int getMessageCount() {
            return messageCount;
        }
        
        public void updateLastAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
        
        public void updateContext(Map<String, Object> newContext) {
            if (newContext != null) {
                this.context.putAll(newContext);
            }
        }
        
        public void addMessage(String content, String type) {
            if (messageCount >= MAX_MESSAGES) {
                // 移除最早的消息
                messages.remove(0);
            } else {
                messageCount++;
            }
            
            messages.add(new Message(content, type, System.currentTimeMillis()));
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - lastAccessTime > SESSION_TIMEOUT;
        }
        
        public long getSessionDuration() {
            return System.currentTimeMillis() - createTime;
        }
        
        public boolean isActive() {
            return !isExpired();
        }
        
        @Override
        public String toString() {
            return "Session{" +
                   "sessionId='" + sessionId + '\'' +
                   ", userId=" + userId +
                   ", createTime=" + createTime +
                   ", lastAccessTime=" + lastAccessTime +
                   ", messageCount=" + messageCount +
                   ", expired=" + isExpired() +
                   '}';
        }
    }
    
    /**
     * 消息实体类
     */
    public static class Message {
        private final String content;
        private final String type; // user/assistant
        private final long timestamp;
        
        public Message(String content, String type, long timestamp) {
            this.content = content;
            this.type = type;
            this.timestamp = timestamp;
        }
        
        public String getContent() {
            return content;
        }
        
        public String getType() {
            return type;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return "Message{" +
                   "type='" + type + '\'' +
                   ", content='" + content + '\'' +
                   ", timestamp=" + timestamp +
                   '}';
        }
    }
    
    /**
     * 获取最后一条消息
     * @param sessionId 会话ID
     * @return 最后一条消息，如果没有返回null
     */
    public Message getLastMessage(String sessionId) {
        Session session = getSession(sessionId);
        if (session != null && !session.getMessages().isEmpty()) {
            List<Message> messages = session.getMessages();
            return messages.get(messages.size() - 1);
        }
        return null;
    }
    
    /**
     * 获取用户偏好设置
     * @param sessionId 会话ID
     * @return 用户偏好映射
     */
    public Map<String, Object> getPreferences(String sessionId) {
        Session session = getSession(sessionId);
        if (session != null) {
            return new HashMap<>(session.getContext());
        }
        return new HashMap<>();
    }
    
    /**
     * 创建指定ID的会话（用于测试和修复）
     * @param userId 用户ID
     * @param sessionId 指定的会话ID
     * @return 会话ID
     */
    public String createSession(Integer userId, String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return createSession(userId);
        }
        
        Session session = new Session(sessionId, userId);
        sessions.put(sessionId, session);
        
        logger.info("创建指定ID的会话：sessionId={}, userId={}", sessionId, userId);
        return sessionId;
    }
}