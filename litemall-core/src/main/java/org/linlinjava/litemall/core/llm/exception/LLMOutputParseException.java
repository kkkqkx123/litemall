package org.linlinjava.litemall.core.llm.exception;

/**
 * LLM输出解析异常
 * 当LLM输出无法解析时抛出此异常
 */
public class LLMOutputParseException extends RuntimeException {
    
    public LLMOutputParseException(String message) {
        super(message);
    }
    
    public LLMOutputParseException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public LLMOutputParseException(Throwable cause) {
        super(cause);
    }
}