package org.linlinjava.litemall.core.llm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.linlinjava.litemall.core.llm.exception.ConversationException;
import org.linlinjava.litemall.core.llm.model.ConversationSession;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 对话会话管理器单元测试
 */
class ConversationManagerTest {
    
    private ConversationManager conversationManager;
    private static final String TEST_SESSION_ID = "test-session-123";
    private static final String TEST_USER_ID = "user-456";
    
    @BeforeEach
    void setUp() {
        // 创建会话管理器，设置较短的超时时间以便测试
        conversationManager = new ConversationManager(30000, 5); // 30秒超时，最大5轮对话
    }
    
    @AfterEach
    void tearDown() {
        // 清理所有会话
        conversationManager.clearAllSessions();
    }
    
    @Test
    @DisplayName("测试获取或创建新会话")
    void testGetOrCreateSession() {
        ConversationSession session = conversationManager.getOrCreateSession(TEST_SESSION_ID, TEST_USER_ID);
        
        assertNotNull(session);
        assertEquals(TEST_SESSION_ID, session.getSessionId());
        assertEquals(TEST_USER_ID, session.getUserId());
        assertNotNull(session.getCreatedAt());
        assertNotNull(session.getLastAccessed());
        assertTrue(session.getConversationHistory().isEmpty());
    }
    
    @Test
    @DisplayName("测试获取已存在的会话")
    void testGetExistingSession() {
        // 先创建会话
        ConversationSession firstSession = conversationManager.getOrCreateSession(TEST_SESSION_ID, TEST_USER_ID);
        LocalDateTime firstAccessTime = firstSession.getLastAccessed();
        
        // 等待一小段时间
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 再次获取会话
        ConversationSession secondSession = conversationManager.getOrCreateSession(TEST_SESSION_ID, TEST_USER_ID);
        
        assertNotNull(secondSession);
        assertEquals(firstSession.getSessionId(), secondSession.getSessionId());
        assertTrue(secondSession.getLastAccessed().isAfter(firstAccessTime));
    }
    
    @Test
    @DisplayName("测试添加对话轮次")
    void testAddConversationTurn() {
        ConversationSession session = conversationManager.getOrCreateSession(TEST_SESSION_ID, TEST_USER_ID);
        
        String userQuery = "查询价格在1000-5000之间的手机";
        String assistantResponse = "SELECT * FROM litemall_product WHERE name LIKE '%手机%' AND price BETWEEN 1000 AND 5000";
        
        boolean result = conversationManager.addConversationTurn(TEST_SESSION_ID, userQuery, assistantResponse);
        
        assertTrue(result);
        assertEquals(1, session.getConversationHistory().size());
        
        ConversationSession.ConversationTurn turn = session.getConversationHistory().get(0);
        assertEquals(userQuery, turn.getUserQuery());
        assertEquals(assistantResponse, turn.getAssistantResponse());
        assertNotNull(turn.getTimestamp());
    }
    
    @Test
    @DisplayName("测试添加多轮对话")
    void testAddMultipleConversationTurns() {
        ConversationSession session = conversationManager.getOrCreateSession(TEST_SESSION_ID, TEST_USER_ID);
        
        // 添加第一轮对话
        conversationManager.addConversationTurn(TEST_SESSION_ID, "查询手机", "SELECT * FROM litemall_product WHERE name LIKE '%手机%'");
        
        // 添加第二轮对话
        conversationManager.addConversationTurn(TEST_SESSION_ID, "价格在2000-5000之间", "SELECT * FROM litemall_product WHERE name LIKE '%手机%' AND price BETWEEN 2000 AND 5000");
        
        // 添加第三轮对话
        conversationManager.addConversationTurn(TEST_SESSION_ID, "按价格排序", "SELECT * FROM litemall_product WHERE name LIKE '%手机%' AND price BETWEEN 2000 AND 5000 ORDER BY price ASC");
        
        assertEquals(3, session.getConversationHistory().size());
        
        // 验证对话历史顺序
        assertEquals("查询手机", session.getConversationHistory().get(0).getUserQuery());
        assertEquals("价格在2000-5000之间", session.getConversationHistory().get(1).getUserQuery());
        assertEquals("按价格排序", session.getConversationHistory().get(2).getUserQuery());
    }
    
    @Test
    @DisplayName("测试达到最大轮数限制")
    void testMaxConversationTurnsLimit() {
        ConversationSession session = conversationManager.getOrCreateSession(TEST_SESSION_ID, TEST_USER_ID);
        
        // 添加5轮对话（达到限制）
        for (int i = 0; i < 5; i++) {
            boolean result = conversationManager.addConversationTurn(TEST_SESSION_ID, 
                "查询" + i, "响应" + i);
            assertTrue(result);
        }
        
        assertEquals(5, session.getConversationHistory().size());
        
        // 尝试添加第6轮对话（应该失败）
        boolean result = conversationManager.addConversationTurn(TEST_SESSION_ID, 
            "查询5", "响应5");
        assertFalse(result);
        assertEquals(5, session.getConversationHistory().size()); // 数量不变
    }
    
    @Test
    @DisplayName("测试获取不存在的会话抛出异常")
    void testGetNonExistentSessionThrowsException() {
        ConversationException exception = assertThrows(ConversationException.class, () -> {
            conversationManager.addConversationTurn("non-existent-session", "查询", "响应");
        });
        
        assertTrue(exception.getMessage().contains("会话不存在"));
        assertEquals("non-existent-session", exception.getSessionId());
        assertEquals("addConversationTurn", exception.getOperation());
    }
    
    @Test
    @DisplayName("测试会话过期清理")
    void testSessionExpirationCleanup() throws InterruptedException {
        // 创建会话管理器，设置很短的超时时间
        ConversationManager shortTimeoutManager = new ConversationManager(100, 10); // 100毫秒超时
        
        // 创建会话
        ConversationSession session = shortTimeoutManager.getOrCreateSession(TEST_SESSION_ID, TEST_USER_ID);
        conversationManager.addConversationTurn(TEST_SESSION_ID, "查询", "响应");
        
        // 等待会话过期
        Thread.sleep(200);
        
        // 手动触发清理（在实际应用中，这通常由定时任务完成）
        shortTimeoutManager.cleanupExpiredSessions();
        
        // 验证会话已被清理
        ConversationException exception = assertThrows(ConversationException.class, () -> {
            shortTimeoutManager.addConversationTurn(TEST_SESSION_ID, "新查询", "新响应");
        });
        
        assertTrue(exception.getMessage().contains("会话不存在"));
    }
    
    @Test
    @DisplayName("测试获取会话上下文")
    void testGetSessionContext() {
        ConversationSession session = conversationManager.getOrCreateSession(TEST_SESSION_ID, TEST_USER_ID);
        
        // 添加几轮对话
        conversationManager.addConversationTurn(TEST_SESSION_ID, "查询手机", "SELECT * FROM litemall_product WHERE name LIKE '%手机%'");
        conversationManager.addConversationTurn(TEST_SESSION_ID, "价格在2000-5000之间", "SELECT * FROM litemall_product WHERE name LIKE '%手机%' AND price BETWEEN 2000 AND 5000");
        
        String context = conversationManager.getSessionContext(TEST_SESSION_ID);
        
        assertNotNull(context);
        assertTrue(context.contains("查询手机"));
        assertTrue(context.contains("价格在2000-5000之间"));
        assertTrue(context.contains("SELECT * FROM litemall_product"));
    }
    
    @Test
    @DisplayName("测试获取空会话的上下文")
    void testGetEmptySessionContext() {
        ConversationSession session = conversationManager.getOrCreateSession(TEST_SESSION_ID, TEST_USER_ID);
        
        String context = conversationManager.getSessionContext(TEST_SESSION_ID);
        
        assertEquals("", context);
    }
    
    @Test
    @DisplayName("测试获取不存在的会话上下文抛出异常")
    void testGetNonExistentSessionContextThrowsException() {
        ConversationException exception = assertThrows(ConversationException.class, () -> {
            conversationManager.getSessionContext("non-existent-session");
        });
        
        assertTrue(exception.getMessage().contains("会话不存在"));
        assertEquals("non-existent-session", exception.getSessionId());
        assertEquals("getSessionContext", exception.getOperation());
    }
    
    @Test
    @DisplayName("测试更新会话最后访问时间")
    void testUpdateLastAccessedTime() {
        ConversationSession session = conversationManager.getOrCreateSession(TEST_SESSION_ID, TEST_USER_ID);
        LocalDateTime originalTime = session.getLastAccessed();
        
        // 等待一小段时间
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 更新最后访问时间
        conversationManager.updateLastAccessed(TEST_SESSION_ID);
        
        assertTrue(session.getLastAccessed().isAfter(originalTime));
    }
    
    @Test
    @DisplayName("测试会话是否存在检查")
    void testSessionExists() {
        // 会话不存在
        assertFalse(conversationManager.sessionExists("non-existent-session"));
        
        // 创建会话
        conversationManager.getOrCreateSession(TEST_SESSION_ID, TEST_USER_ID);
        
        // 会话存在
        assertTrue(conversationManager.sessionExists(TEST_SESSION_ID));
    }
    
    @Test
    @DisplayName("测试获取服务状态")
    void testGetServiceStatus() {
        // 创建一些会话
        conversationManager.getOrCreateSession("session1", "user1");
        conversationManager.getOrCreateSession("session2", "user2");
        
        conversationManager.addConversationTurn("session1", "查询1", "响应1");
        conversationManager.addConversationTurn("session1", "查询2", "响应2");
        conversationManager.addConversationTurn("session2", "查询3", "响应3");
        
        ConversationManager.ServiceStatus status = conversationManager.getServiceStatus();
        
        assertNotNull(status);
        assertEquals(2, status.getActiveSessions());
        assertEquals(3, status.getTotalConversations());
        assertTrue(status.getUptime() > 0);
        assertTrue(status.isHealthy());
    }
    
    @Test
    @DisplayName("测试清除所有会话")
    void testClearAllSessions() {
        // 创建多个会话
        conversationManager.getOrCreateSession("session1", "user1");
        conversationManager.getOrCreateSession("session2", "user2");
        conversationManager.getOrCreateSession("session3", "user3");
        
        // 验证会话存在
        assertTrue(conversationManager.sessionExists("session1"));
        assertTrue(conversationManager.sessionExists("session2"));
        assertTrue(conversationManager.sessionExists("session3"));
        
        // 清除所有会话
        conversationManager.clearAllSessions();
        
        // 验证会话已被清除
        assertFalse(conversationManager.sessionExists("session1"));
        assertFalse(conversationManager.sessionExists("session2"));
        assertFalse(conversationManager.sessionExists("session3"));
    }
}