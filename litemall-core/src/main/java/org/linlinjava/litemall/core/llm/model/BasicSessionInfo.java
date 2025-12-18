package org.linlinjava.litemall.core.llm.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 基础会话信息
 * 用于维护用户会话的基本信息和偏好设置
 */
public class BasicSessionInfo {
    private String sessionId;
    private List<String> recentQueries;     // 最近5个查询
    private String pricePreference;         // 价格偏好：高/中/低
    private Set<String> preferredCategories; // 偏好的类目
    private Set<String> preferredBrands;    // 偏好的品牌
    private LocalDateTime lastActiveTime;
    
    // 预定义的类目关键词
    private static final String[] CATEGORIES = {
        "手机", "电脑", "服装", "食品", "家电", "图书", "美妆", "运动", "家居", "数码"
    };
    
    // 预定义的品牌关键词  
    private static final String[] BRANDS = {
        "苹果", "华为", "小米", "三星", "耐克", "阿迪达斯", "优衣库", "海尔", "美的", "格力"
    };
    
    public BasicSessionInfo(String sessionId) {
        this.sessionId = sessionId;
        this.recentQueries = new ArrayList<>();
        this.pricePreference = "中"; // 默认中等价位
        this.preferredCategories = new HashSet<>();
        this.preferredBrands = new HashSet<>();
        this.lastActiveTime = LocalDateTime.now();
    }
    
    /**
     * 从查询中提取用户偏好
     * @param question 用户问题
     */
    public void extractPreferences(String question) {
        if (question == null || question.trim().isEmpty()) {
            return;
        }
        
        String lowerQuestion = question.toLowerCase();
        
        // 提取价格偏好
        extractPricePreference(lowerQuestion);
        
        // 提取类目偏好
        extractCategoryPreference(lowerQuestion);
        
        // 提取品牌偏好
        extractBrandPreference(lowerQuestion);
        
        // 更新最近查询
        updateRecentQueries(question);
        
        // 更新最后活跃时间
        this.lastActiveTime = LocalDateTime.now();
    }
    
    /**
     * 提取价格偏好
     */
    private void extractPricePreference(String question) {
        // 低价相关词汇
        if (question.contains("便宜") || question.contains("实惠") || 
            question.contains("低价") || question.contains("经济") ||
            question.contains("划算") || question.contains("性价比")) {
            this.pricePreference = "低";
        }
        // 高价相关词汇
        else if (question.contains("贵") || question.contains("高端") || 
                 question.contains("豪华") || question.contains("奢侈") ||
                 question.contains("品质") || question.contains("高档")) {
            this.pricePreference = "高";
        }
        // 中价相关词汇
        else if (question.contains("中等") || question.contains("适中") || 
                 question.contains("一般") || question.contains("普通")) {
            this.pricePreference = "中";
        }
    }
    
    /**
     * 提取类目偏好
     */
    private void extractCategoryPreference(String question) {
        for (String category : CATEGORIES) {
            if (question.contains(category)) {
                this.preferredCategories.add(category);
            }
        }
    }
    
    /**
     * 提取品牌偏好
     */
    private void extractBrandPreference(String question) {
        for (String brand : BRANDS) {
            if (question.contains(brand)) {
                this.preferredBrands.add(brand);
            }
        }
    }
    
    /**
     * 更新最近查询列表
     */
    private void updateRecentQueries(String question) {
        // 避免重复添加相同的查询
        if (!recentQueries.isEmpty() && recentQueries.get(recentQueries.size() - 1).equals(question)) {
            return;
        }
        
        recentQueries.add(question);
        
        // 保持最近5个查询
        if (recentQueries.size() > 5) {
            recentQueries.remove(0);
        }
    }
    
    /**
     * 获取用户偏好摘要
     */
    public String getPreferenceSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("价格偏好：").append(pricePreference).append("价位");
        
        if (!preferredCategories.isEmpty()) {
            summary.append("，偏好类目：").append(String.join(",", preferredCategories));
        }
        
        if (!preferredBrands.isEmpty()) {
            summary.append("，偏好品牌：").append(String.join(",", preferredBrands));
        }
        
        return summary.toString();
    }
    
    /**
     * 检查是否包含指定偏好
     */
    public boolean hasPreference(String preference) {
        return pricePreference.equals(preference) ||
               preferredCategories.contains(preference) ||
               preferredBrands.contains(preference);
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public List<String> getRecentQueries() {
        return new ArrayList<>(recentQueries);
    }
    
    public void setRecentQueries(List<String> recentQueries) {
        this.recentQueries = recentQueries;
    }
    
    public String getPricePreference() {
        return pricePreference;
    }
    
    public void setPricePreference(String pricePreference) {
        this.pricePreference = pricePreference;
    }
    
    public Set<String> getPreferredCategories() {
        return new HashSet<>(preferredCategories);
    }
    
    public void setPreferredCategories(Set<String> preferredCategories) {
        this.preferredCategories = preferredCategories;
    }
    
    public Set<String> getPreferredBrands() {
        return new HashSet<>(preferredBrands);
    }
    
    public void setPreferredBrands(Set<String> preferredBrands) {
        this.preferredBrands = preferredBrands;
    }
    
    public LocalDateTime getLastActiveTime() {
        return lastActiveTime;
    }
    
    public void setLastActiveTime(LocalDateTime lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }
    
    /**
     * 获取查询次数
     * @return 查询次数
     */
    public int getQueryCount() {
        return recentQueries.size();
    }
    
    /**
     * 添加查询到历史
     * @param question 查询问题
     */
    public void addQuery(String question) {
        if (question != null && !question.trim().isEmpty()) {
            updateRecentQueries(question);
            extractPreferences(question);
        }
    }
    
    @Override
    public String toString() {
        return "BasicSessionInfo{" +
                "sessionId='" + sessionId + '\'' +
                ", recentQueries=" + recentQueries +
                ", pricePreference='" + pricePreference + '\'' +
                ", preferredCategories=" + preferredCategories +
                ", preferredBrands=" + preferredBrands +
                ", lastActiveTime=" + lastActiveTime +
                '}';
    }
}