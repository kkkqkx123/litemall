package org.linlinjava.litemall.core.llm.parser;

import org.json.JSONObject;
import org.linlinjava.litemall.core.llm.exception.LLMOutputParseException;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON提取器
 * 从LLM输出中提取JSON格式的内容
 */
@Component
public class JSONExtractor {
    
    private static final Pattern JSON_PATTERN = Pattern.compile(
        "\\{[\\s\\S]*\\}", Pattern.MULTILINE
    );
    
    /**
     * 从文本中提取JSON
     * @param text LLM输出文本
     * @return 提取的JSON字符串
     * @throws LLMOutputParseException 当无法提取JSON时抛出此异常
     */
    public String extractJSON(String text) throws LLMOutputParseException {
        if (text == null || text.trim().isEmpty()) {
            throw new LLMOutputParseException("LLM输出为空");
        }
        
        // 移除Markdown代码块标记
        String cleanedText = removeMarkdownCodeBlocks(text);
        
        // 提取JSON部分
        Matcher matcher = JSON_PATTERN.matcher(cleanedText);
        if (matcher.find()) {
            String jsonStr = matcher.group();
            
            // 验证JSON格式
            try {
                new JSONObject(jsonStr);
                return jsonStr;
            } catch (Exception e) {
                // 尝试修复常见的JSON格式问题
                return fixCommonJSONIssues(jsonStr);
            }
        }
        
        throw new LLMOutputParseException("未找到有效的JSON输出");
    }
    
    /**
     * 移除Markdown代码块标记
     * @param text 文本内容
     * @return 清理后的文本
     */
    private String removeMarkdownCodeBlocks(String text) {
        // 移除```json和```标记
        return text.replaceAll("```json\\n?", "")
                  .replaceAll("```\\n?", "")
                  .trim();
    }
    
    /**
     * 修复常见的JSON格式问题
     * @param jsonStr JSON字符串
     * @return 修复后的JSON字符串
     * @throws LLMOutputParseException 当修复失败时抛出此异常
     */
    private String fixCommonJSONIssues(String jsonStr) throws LLMOutputParseException {
        // 修复末尾多余的逗号
        jsonStr = jsonStr.replaceAll(",\\s*}", "}");
        jsonStr = jsonStr.replaceAll(",\\s*]", "]");
        
        // 修复单引号
        jsonStr = jsonStr.replaceAll("'", "\"");
        
        // 验证修复后的JSON
        try {
            new JSONObject(jsonStr);
            return jsonStr;
        } catch (Exception e) {
            throw new LLMOutputParseException("JSON格式修复失败: " + e.getMessage());
        }
    }
}