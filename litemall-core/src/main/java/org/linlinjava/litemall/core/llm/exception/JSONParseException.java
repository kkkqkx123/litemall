package org.linlinjava.litemall.core.llm.exception;

/**
 * JSON解析异常
 */
public class JSONParseException extends LLMServiceException {
    
    private final String jsonContent;
    private final String parsingError;
    
    public JSONParseException(String message) {
        super(message);
        this.jsonContent = null;
        this.parsingError = null;
    }
    
    public JSONParseException(String message, String jsonContent, String parsingError) {
        super(message);
        this.jsonContent = jsonContent;
        this.parsingError = parsingError;
    }
    
    public JSONParseException(String message, Throwable cause) {
        super(message, cause);
        this.jsonContent = null;
        this.parsingError = null;
    }
    
    public String getJsonContent() {
        return jsonContent;
    }
    
    public String getParsingError() {
        return parsingError;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("JSONParseException: ").append(getMessage());
        if (jsonContent != null) {
            sb.append(", JSON内容: ").append(jsonContent);
        }
        if (parsingError != null) {
            sb.append(", 解析错误: ").append(parsingError);
        }
        return sb.toString();
    }
}