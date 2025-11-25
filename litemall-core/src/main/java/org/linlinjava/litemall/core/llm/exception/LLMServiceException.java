package org.linlinjava.litemall.core.llm.exception;

/**
 * LLM服务异常
 * 当LLM服务调用失败时抛出此异常
 */
public class LLMServiceException extends RuntimeException {
    
    public LLMServiceException(String message) {
        super(message);
    }
    
    public LLMServiceException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public LLMServiceException(Throwable cause) {
        super(cause);
    }
}