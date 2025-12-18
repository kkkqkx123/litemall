package org.linlinjava.litemall.core.llm.util;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 商品详情过滤工具类
 * 用于移除HTML标签、图片URL等非文本信息，保留纯文本内容
 */
@Component
public class GoodsDetailFilter {
    
    // HTML标签正则表达式
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    
    // 图片URL正则表达式
    private static final Pattern IMAGE_URL_PATTERN = Pattern.compile("(http[s]?://[^\\s]+(jpg|jpeg|png|gif|webp))");
    
    // HTML属性正则表达式
    private static final Pattern HTML_ATTRIBUTE_PATTERN = Pattern.compile("\\s+[a-zA-Z-]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)");
    
    // 多个空白字符正则表达式
    private static final Pattern MULTIPLE_WHITESPACE_PATTERN = Pattern.compile("\\s+");
    
    /**
     * 过滤商品详情，移除HTML标签和图片URL
     * @param detail 原始详情
     * @return 过滤后的纯文本详情
     */
    public static String filterDetail(String detail) {
        if (detail == null || detail.trim().isEmpty()) {
            return "";
        }
        
        // 1. 移除所有图片URL
        String result = IMAGE_URL_PATTERN.matcher(detail).replaceAll("");
        
        // 2. 移除HTML属性
        result = HTML_ATTRIBUTE_PATTERN.matcher(result).replaceAll("");
        
        // 3. 移除HTML标签
        result = HTML_TAG_PATTERN.matcher(result).replaceAll("");
        
        // 4. 规范化空白字符
        result = MULTIPLE_WHITESPACE_PATTERN.matcher(result).replaceAll(" ");
        
        // 5. 去除首尾空白
        result = result.trim();
        
        // 6. 限制长度，避免过长的文本
        if (result.length() > 500) {
            result = result.substring(0, 500) + "...";
        }
        
        return result;
    }
    
    /**
     * 提取商品详情的纯文本摘要
     * @param detail 原始详情
     * @param maxLength 最大长度
     * @return 纯文本摘要
     */
    public static String extractSummary(String detail, int maxLength) {
        String filtered = filterDetail(detail);
        if (filtered.length() <= maxLength) {
            return filtered;
        }
        
        // 尝试在句子边界截断
        String[] sentences = filtered.split("[。！？]");
        StringBuilder summary = new StringBuilder();
        
        for (String sentence : sentences) {
            if (summary.length() + sentence.length() > maxLength) {
                break;
            }
            if (summary.length() > 0) {
                summary.append("。");
            }
            summary.append(sentence);
        }
        
        if (summary.length() == 0) {
            return filtered.substring(0, maxLength) + "...";
        }
        
        return summary.toString();
    }
}