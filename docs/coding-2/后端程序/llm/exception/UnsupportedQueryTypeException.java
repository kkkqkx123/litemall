package org.linlinjava.litemall.core.llm.exception;

/**
 * 不支持的查询类型异常
 * 当遇到不支持的查询类型时抛出此异常
 */
public class UnsupportedQueryTypeException extends RuntimeException {
    
    public UnsupportedQueryTypeException(String message) {
        super(message);
    }
    
    public UnsupportedQueryTypeException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public UnsupportedQueryTypeException(Throwable cause) {
        super(cause);
    }
}