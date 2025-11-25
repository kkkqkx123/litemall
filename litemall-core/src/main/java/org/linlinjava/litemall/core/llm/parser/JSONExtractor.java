package org.linlinjava.litemall.core.llm.parser;

import org.json.JSONObject;
import org.linlinjava.litemall.core.llm.exception.LLMOutputParseException;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON提取器
 * 从LLM输出中提取JSON格式的内容
 */
@Component
public class JSONExtractor {
    
    // 改进的JSON匹配模式，更精确地匹配JSON结构
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile(
        "\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}", Pattern.MULTILINE
    );
    
    // 匹配数组模式
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile(
        "\\[[^\\[\\]]*(?:\\[[^\\[\\]]*\\][^\\[\\]]*)*\\]", Pattern.MULTILINE
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
        
        // 尝试多种提取策略
        List<String> jsonCandidates = extractJSONCandidates(cleanedText);
        
        for (String candidate : jsonCandidates) {
            try {
                // 验证JSON格式
                new JSONObject(candidate);
                return candidate;
            } catch (Exception e) {
                // 尝试修复
                try {
                    String fixed = fixCommonJSONIssues(candidate);
                    new JSONObject(fixed);
                    return fixed;
                } catch (Exception fixException) {
                    // 继续尝试下一个候选者
                    continue;
                }
            }
        }
        
        throw new LLMOutputParseException("未找到有效的JSON输出，已尝试所有候选者");
    }
    
    /**
     * 提取JSON候选者
     * @param text 清理后的文本
     * @return JSON候选者列表
     */
    private List<String> extractJSONCandidates(String text) {
        List<String> candidates = new ArrayList<>();
        
        // 策略1: 查找JSON对象
        Matcher objectMatcher = JSON_OBJECT_PATTERN.matcher(text);
        while (objectMatcher.find()) {
            String candidate = objectMatcher.group().trim();
            if (isValidJSONStructure(candidate)) {
                candidates.add(candidate);
            }
        }
        
        // 策略2: 查找JSON数组
        Matcher arrayMatcher = JSON_ARRAY_PATTERN.matcher(text);
        while (arrayMatcher.find()) {
            String candidate = arrayMatcher.group().trim();
            if (isValidJSONStructure(candidate)) {
                candidates.add(candidate);
            }
        }
        
        // 策略3: 查找最外层大括号包围的内容
        findOuterBraceContent(text, candidates);
        
        return candidates;
    }
    
    /**
     * 验证JSON结构的有效性
     * @param jsonStr JSON字符串
     * @return 是否有效
     */
    private boolean isValidJSONStructure(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return false;
        }
        
        char firstChar = jsonStr.trim().charAt(0);
        char lastChar = jsonStr.trim().charAt(jsonStr.length() - 1);
        
        // 必须以{或[开始，以}或]结束
        if (!((firstChar == '{' && lastChar == '}') || 
              (firstChar == '[' && lastChar == ']'))) {
            return false;
        }
        
        // 检查括号匹配
        return areBracketsBalanced(jsonStr);
    }
    
    /**
     * 检查括号是否平衡
     * @param text 文本
     * @return 是否平衡
     */
    private boolean areBracketsBalanced(String text) {
        java.util.Stack<Character> stack = new java.util.Stack<>();
        
        for (char c : text.toCharArray()) {
            if (c == '{' || c == '[') {
                stack.push(c);
            } else if (c == '}' || c == ']') {
                if (stack.isEmpty()) {
                    return false;
                }
                char top = stack.pop();
                if ((c == '}' && top != '{') || (c == ']' && top != '[')) {
                    return false;
                }
            }
        }
        
        return stack.isEmpty();
    }
    
    /**
     * 查找最外层大括号包围的内容
     * @param text 文本
     * @param candidates 候选者列表
     */
    private void findOuterBraceContent(String text, List<String> candidates) {
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        
        if (firstBrace != -1 && lastBrace != -1 && firstBrace < lastBrace) {
            String candidate = text.substring(firstBrace, lastBrace + 1);
            if (isValidJSONStructure(candidate)) {
                candidates.add(0, candidate); // 优先尝试
            }
        }
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
        String original = jsonStr;
        
        // 修复1: 末尾多余的逗号
        jsonStr = jsonStr.replaceAll(",\\s*}", "}");
        jsonStr = jsonStr.replaceAll(",\\s*]", "]");
        
        // 修复2: 单引号替换为双引号
        jsonStr = jsonStr.replaceAll("'", "\"");
        
        // 修复3: 移除控制字符
        jsonStr = jsonStr.replaceAll("[\\x00-\\x1F\\x7F]", "");
        
        // 修复4: 修复未加引号的键名（简单情况）
        jsonStr = jsonStr.replaceAll("(\\w+):", "\"$1\":");
        
        // 验证修复后的JSON
        try {
            new JSONObject(jsonStr);
            return jsonStr;
        } catch (Exception e) {
            // 如果第一次修复失败，尝试渐进式修复
            return progressiveFix(original);
        }
    }
    
    /**
     * 渐进式修复JSON
     * @param jsonStr 原始JSON字符串
     * @return 修复后的字符串
     * @throws LLMOutputParseException 修复失败
     */
    private String progressiveFix(String jsonStr) throws LLMOutputParseException {
        List<String> fixes = new ArrayList<>();
        fixes.add(jsonStr);
        
        // 尝试不同的修复策略
        fixes.add(jsonStr.replaceAll(",\\s*}", "}")); // 移除对象末尾逗号
        fixes.add(jsonStr.replaceAll(",\\s*]", "]")); // 移除数组末尾逗号
        fixes.add(jsonStr.replaceAll("'", "\""));     // 替换单引号
        
        for (String fix : fixes) {
            try {
                new JSONObject(fix);
                return fix;
            } catch (Exception e) {
                continue;
            }
        }
        
        throw new LLMOutputParseException("JSON格式修复失败，已尝试所有策略");
    }
}