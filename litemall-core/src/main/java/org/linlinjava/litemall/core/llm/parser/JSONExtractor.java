package org.linlinjava.litemall.core.llm.parser;

import org.json.JSONObject;
import org.linlinjava.litemall.core.llm.exception.LLMOutputParseException;
import org.springframework.stereotype.Component;

/**
 * JSON提取器
 * 从LLM输出中提取JSON格式的内容
 */
@Component
public class JSONExtractor {
    
    /**
     * 从文本中提取JSON
     * @param text LLM输出文本
     * @return 提取的JSON字符串
     * @throws LLMOutputParseException 当无法提取JSON时抛出此异常
     */
    public String extractJSON(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        // 移除Markdown代码块标记
        String cleanedText = text.replaceAll("```json\\n?", "")
                                .replaceAll("```\\n?", "")
                                .trim();
        
        // 查找第一个{和最后一个}
        int firstBrace = cleanedText.indexOf('{');
        int lastBrace = cleanedText.lastIndexOf('}');
        
        if (firstBrace != -1 && lastBrace != -1 && firstBrace < lastBrace) {
            String candidate = cleanedText.substring(firstBrace, lastBrace + 1);
            
            // 简单验证JSON格式
            try {
                new JSONObject(candidate);
                return candidate;
            } catch (Exception e) {
                // 尝试修复单引号和末尾逗号
                candidate = candidate.replaceAll("'", "\"")
                                   .replaceAll(",\\s*}", "}")
                                   .replaceAll(",\\s*]", "]");
                try {
                    new JSONObject(candidate);
                    return candidate;
                } catch (Exception e2) {
                    // 如果JSON仍然无效，尝试提取第一个有效的JSON对象
                    return extractFirstValidJSONObject(cleanedText);
                }
            }
        }
        
        return null;
    }
    
    /**
     * 提取第一个有效的JSON对象，处理被截断的情况
     * @param text 输入文本
     * @return 有效的JSON字符串，如果找不到返回null
     */
    private String extractFirstValidJSONObject(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        int startIndex = text.indexOf('{');
        if (startIndex == -1) {
            return null;
        }
        
        // 尝试从第一个{开始，逐步构建有效的JSON
        int braceCount = 0;
        boolean inString = false;
        char stringDelimiter = '\0';
        
        for (int i = startIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            
            // 处理字符串内的字符
            if (inString) {
                if (c == stringDelimiter && text.charAt(i - 1) != '\\') {
                    inString = false;
                }
                continue;
            }
            
            // 处理字符串开始
            if (c == '"' || c == '\'') {
                inString = true;
                stringDelimiter = c;
                continue;
            }
            
            // 处理括号
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    // 找到完整的JSON对象
                    String candidate = text.substring(startIndex, i + 1);
                    try {
                        new JSONObject(candidate);
                        return candidate;
                    } catch (Exception e) {
                        // 继续寻找下一个完整的对象
                        startIndex = text.indexOf('{', i + 1);
                        if (startIndex == -1) break;
                        i = startIndex - 1;
                        braceCount = 0;
                    }
                }
            }
        }
        
        return null;
    }
}