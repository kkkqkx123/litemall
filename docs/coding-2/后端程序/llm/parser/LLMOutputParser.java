package org.linlinjava.litemall.core.llm.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.linlinjava.litemall.core.llm.exception.LLMOutputParseException;
import org.linlinjava.litemall.core.llm.model.QueryIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * LLM输出解析器
 * 解析LLM输出为QueryIntent对象
 */
@Component
public class LLMOutputParser {
    
    @Autowired
    private JSONExtractor jsonExtractor;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 解析LLM输出为查询意图
     * @param llmOutput LLM输出文本
     * @return 查询意图对象
     * @throws LLMOutputParseException 当解析失败时抛出此异常
     */
    public QueryIntent parseQueryIntent(String llmOutput) throws LLMOutputParseException {
        try {
            // 提取JSON部分
            String jsonStr = jsonExtractor.extractJSON(llmOutput);
            
            // 转换为QueryIntent对象
            QueryIntent queryIntent = objectMapper.readValue(jsonStr, QueryIntent.class);
            
            // 验证查询意图的合法性
            if (!queryIntent.isValid()) {
                throw new LLMOutputParseException("查询意图不合法: " + queryIntent);
            }
            
            return queryIntent;
            
        } catch (LLMOutputParseException e) {
            throw e; // 重新抛出已知的解析异常
        } catch (Exception e) {
            throw new LLMOutputParseException("解析LLM输出失败", e);
        }
    }
    
    /**
     * 解析LLM输出为字符串（用于回答生成）
     * @param llmOutput LLM输出文本
     * @return 解析后的字符串
     */
    public String parseString(String llmOutput) {
        if (llmOutput == null) {
            return "";
        }
        
        // 移除Markdown代码块标记
        return llmOutput.replaceAll("```[\\s\\S]*?```", "")
                       .replaceAll("`", "")
                       .trim();
    }
    
    /**
     * 验证查询意图是否有效
     * @param queryIntent 查询意图
     * @return true表示有效，false表示无效
     */
    public boolean validateQueryIntent(QueryIntent queryIntent) {
        if (queryIntent == null) {
            return false;
        }
        
        return queryIntent.isValid();
    }
}