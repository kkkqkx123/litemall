package org.linlinjava.litemall.core.llm.model;

import org.linlinjava.litemall.db.domain.LitemallGoods;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品问答响应模型
 * 用于返回问答结果给前端
 */
public class GoodsQAResponse {
    
    /**
     * 响应状态码
     * 200: 成功, 400: 参数错误, 500: 服务器错误
     */
    private int code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 自然语言回答
     */
    private String answer;
    
    /**
     * 查询到的商品列表
     */
    private List<LitemallGoods> goods;
    
    /**
     * 会话ID
     * 用于多轮对话
     */
    private String sessionId;
    
    /**
     * 查询耗时（毫秒）
     */
    private long queryTime;
    
    /**
     * 响应生成时间
     */
    private LocalDateTime timestamp;
    
    /**
     * 是否来自缓存
     */
    private boolean fromCache;
    
    /**
     * 查询意图（调试用）
     */
    private QueryIntent queryIntent;
    
    /**
     * 默认构造函数
     */
    public GoodsQAResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * 成功响应的工厂方法
     * @param answer 自然语言回答
     * @param goods 商品列表
     * @return 成功响应对象
     */
    public static GoodsQAResponse success(String answer, List<LitemallGoods> goods) {
        GoodsQAResponse response = new GoodsQAResponse();
        response.setCode(200);
        response.setMessage("success");
        response.setAnswer(answer);
        response.setGoods(goods);
        return response;
    }
    
    /**
     * 错误响应的工厂方法
     * @param code 错误码
     * @param message 错误消息
     * @return 错误响应对象
     */
    public static GoodsQAResponse error(int code, String message) {
        GoodsQAResponse response = new GoodsQAResponse();
        response.setCode(code);
        response.setMessage(message);
        response.setAnswer("抱歉，我无法处理您的请求。" + message);
        return response;
    }
    
    /**
     * 参数错误的工厂方法
     * @param message 错误消息
     * @return 参数错误响应对象
     */
    public static GoodsQAResponse badRequest(String message) {
        return error(400, message);
    }
    
    /**
     * 服务器错误的工厂方法
     * @param message 错误消息
     * @return 服务器错误响应对象
     */
    public static GoodsQAResponse serverError(String message) {
        return error(500, message);
    }
    
    // Getters and Setters
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getAnswer() {
        return answer;
    }
    
    public void setAnswer(String answer) {
        this.answer = answer;
    }
    
    public List<LitemallGoods> getGoods() {
        return goods;
    }
    
    public void setGoods(List<LitemallGoods> goods) {
        this.goods = goods;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public long getQueryTime() {
        return queryTime;
    }
    
    public void setQueryTime(long queryTime) {
        this.queryTime = queryTime;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isFromCache() {
        return fromCache;
    }
    
    public void setFromCache(boolean fromCache) {
        this.fromCache = fromCache;
    }
    
    public QueryIntent getQueryIntent() {
        return queryIntent;
    }
    
    public void setQueryIntent(QueryIntent queryIntent) {
        this.queryIntent = queryIntent;
    }
    
    @Override
    public String toString() {
        return "GoodsQAResponse{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", answer='" + answer + '\'' +
                ", goods=" + (goods != null ? goods.size() : 0) +
                ", sessionId='" + sessionId + '\'' +
                ", queryTime=" + queryTime +
                ", timestamp=" + timestamp +
                ", fromCache=" + fromCache +
                ", queryIntent=" + queryIntent +
                '}';
    }
}