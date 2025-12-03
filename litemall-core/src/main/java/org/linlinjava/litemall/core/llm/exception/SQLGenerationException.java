package org.linlinjava.litemall.core.llm.exception;

/**
 * SQL生成异常
 */
public class SQLGenerationException extends LLMServiceException {
    
    private final String queryType;
    private final String invalidCondition;
    
    public SQLGenerationException(String message) {
        super(message);
        this.queryType = null;
        this.invalidCondition = null;
    }
    
    public SQLGenerationException(String message, String queryType, String invalidCondition) {
        super(message);
        this.queryType = queryType;
        this.invalidCondition = invalidCondition;
    }
    
    public SQLGenerationException(String message, Throwable cause) {
        super(message, cause);
        this.queryType = null;
        this.invalidCondition = null;
    }
    
    public String getQueryType() {
        return queryType;
    }
    
    public String getInvalidCondition() {
        return invalidCondition;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SQLGenerationException: ").append(getMessage());
        if (queryType != null) {
            sb.append(", 查询类型: ").append(queryType);
        }
        if (invalidCondition != null) {
            sb.append(", 无效条件: ").append(invalidCondition);
        }
        return sb.toString();
    }
}