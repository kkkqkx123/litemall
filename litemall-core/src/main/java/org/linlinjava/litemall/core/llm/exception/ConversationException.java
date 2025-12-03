package org.linlinjava.litemall.core.llm.exception;

/**
 * 会话管理异常
 */
public class ConversationException extends LLMServiceException {
    
    private final String sessionId;
    private final String operation;
    
    public ConversationException(String message) {
        super(message);
        this.sessionId = null;
        this.operation = null;
    }
    
    public ConversationException(String message, String sessionId, String operation) {
        super(message);
        this.sessionId = sessionId;
        this.operation = operation;
    }
    
    public ConversationException(String message, Throwable cause) {
        super(message, cause);
        this.sessionId = null;
        this.operation = null;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public String getOperation() {
        return operation;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ConversationException: ").append(getMessage());
        if (sessionId != null) {
            sb.append(", 会话ID: ").append(sessionId);
        }
        if (operation != null) {
            sb.append(", 操作: ").append(operation);
        }
        return sb.toString();
    }
}