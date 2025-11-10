package org.linlinjava.litemall.core.wordcloud;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 词云生成服务
 * 基于简单的词频统计算法生成词云数据
 */
@Service
public class WordCloudService {
    
    // 常见停用词列表
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这", "那", "他", "她", "它", "们", "与", "或", "但", "而", "因为", "所以", "如果", "虽然", "然而", "不过", "只是", "还是", "就是", "真是", "觉得", "感觉", "使用", "购买", "商品", "产品", "质量", "服务", "物流", "快递", "包装", "收到", "发货", "速度", "态度", "客服", "售后", "体验", "整体", "总体", "方面", "比较", "相对", "一般", "普通", "正常", "还行", "可以", "不错", "挺好", "满意", "喜欢", "推荐", "值得", "下次", "还会", "继续", "支持", "好评", "差评", "中评"
    ));
    
    // 中文分词简单实现（基于字符长度和常见词）
    private static final Set<String> COMMON_WORDS = new HashSet<>(Arrays.asList(
        "喜欢", "满意", "质量", "不错", "很好", "非常好", "太差", "不好", "一般", "还行", "可以", "推荐", "值得购买", "物美价廉", "性价比高", "包装", "物流", "快递", "发货", "速度", "客服", "服务", "态度", "售后", "体验", "效果", "外观", "颜色", "尺寸", "大小", "重量", "材质", "做工", "精致", "粗糙", "漂亮", "美观", "时尚", "实用", "方便", "简单", "复杂", "容易", "困难", "便宜", "贵", "值得", "划算", "超值", "失望", "惊喜", "意外", "满意", "开心", "高兴", "生气", "愤怒", "抱怨", "投诉", "退货", "换货", "退款", "赔偿", "五星", "好评", "差评", "中评"
    ));
    
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\u4e00-\u9fa5]+");
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]+");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
    
    /**
     * 生成词云数据
     * @param texts 文本列表
     * @param maxWords 最大词数
     * @return 词云数据列表，包含词、频率、大小等信息
     */
    public List<WordCloudData> generateWordCloud(List<String> texts, int maxWords) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 1. 文本预处理
        List<String> processedTexts = texts.stream()
            .filter(text -> text != null && !text.trim().isEmpty())
            .map(this::preprocessText)
            .collect(Collectors.toList());
        
        if (processedTexts.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 2. 分词和词频统计
        Map<String, Integer> wordFrequency = new HashMap<>();
        for (String text : processedTexts) {
            List<String> words = segmentWords(text);
            for (String word : words) {
                if (isValidWord(word)) {
                    wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
                }
            }
        }
        
        // 3. 过滤和排序
        List<Map.Entry<String, Integer>> sortedWords = wordFrequency.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(maxWords)
            .collect(Collectors.toList());
        
        // 4. 生成词云数据
        return generateWordCloudData(sortedWords);
    }
    
    /**
     * 文本预处理
     */
    private String preprocessText(String text) {
        if (text == null) {
            return "";
        }
        
        // 移除HTML标签
        text = text.replaceAll("<[^>]*>", "");
        
        // 移除特殊字符，保留中文、英文、数字
        text = text.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s]", " ");
        
        // 移除多余空白
        text = text.trim().replaceAll("\\s+", " ");
        
        return text;
    }
    
    /**
     * 简单的中文分词
     */
    private List<String> segmentWords(String text) {
        List<String> words = new ArrayList<>();
        
        // 首先尝试匹配常见词
        String remainingText = text;
        for (String commonWord : COMMON_WORDS) {
            if (remainingText.contains(commonWord)) {
                words.add(commonWord);
                remainingText = remainingText.replace(commonWord, " ");
            }
        }
        
        // 然后按字符分割，提取连续的中文、英文、数字
        String[] segments = remainingText.split("\\s+");
        for (String segment : segments) {
            if (segment.length() >= 2) {
                // 提取连续的中文
                java.util.regex.Matcher chineseMatcher = CHINESE_PATTERN.matcher(segment);
                while (chineseMatcher.find()) {
                    String chineseWord = chineseMatcher.group();
                    if (chineseWord.length() >= 2) {
                        words.add(chineseWord);
                    }
                }
                
                // 提取连续的英文
                java.util.regex.Matcher englishMatcher = ENGLISH_PATTERN.matcher(segment);
                while (englishMatcher.find()) {
                    String englishWord = englishMatcher.group();
                    if (englishWord.length() >= 2) {
                        words.add(englishWord.toLowerCase());
                    }
                }
            }
        }
        
        return words;
    }
    
    /**
     * 检查是否为有效词
     */
    private boolean isValidWord(String word) {
        if (word == null || word.length() < 2) {
            return false;
        }
        
        // 过滤停用词
        if (STOP_WORDS.contains(word)) {
            return false;
        }
        
        // 过滤纯数字
        if (NUMBER_PATTERN.matcher(word).matches()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 生成词云数据
     */
    private List<WordCloudData> generateWordCloudData(List<Map.Entry<String, Integer>> sortedWords) {
        if (sortedWords.isEmpty()) {
            return new ArrayList<>();
        }
        
        int maxFreq = sortedWords.get(0).getValue();
        int minFreq = sortedWords.get(sortedWords.size() - 1).getValue();
        
        List<WordCloudData> wordCloudData = new ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : sortedWords) {
            String word = entry.getKey();
            int frequency = entry.getValue();
            
            // 计算字体大小 (12-48px)
            int fontSize = calculateFontSize(frequency, minFreq, maxFreq, 12, 48);
            
            // 随机颜色
            String color = generateRandomColor();
            
            wordCloudData.add(new WordCloudData(word, frequency, fontSize, color));
        }
        
        return wordCloudData;
    }
    
    /**
     * 计算字体大小
     */
    private int calculateFontSize(int frequency, int minFreq, int maxFreq, int minSize, int maxSize) {
        if (maxFreq == minFreq) {
            return (minSize + maxSize) / 2;
        }
        
        double ratio = (double) (frequency - minFreq) / (maxFreq - minFreq);
        return (int) (minSize + ratio * (maxSize - minSize));
    }
    
    /**
     * 生成随机颜色
     */
    private String generateRandomColor() {
        String[] colors = {
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7",
            "#DDA0DD", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9",
            "#F8C471", "#82E0AA", "#F1948A", "#85C1E9", "#D7BDE2"
        };
        
        Random random = new Random();
        return colors[random.nextInt(colors.length)];
    }
    
    /**
     * 词云数据类
     */
    public static class WordCloudData {
        private String text;
        private int frequency;
        private int fontSize;
        private String color;
        
        public WordCloudData(String text, int frequency, int fontSize, String color) {
            this.text = text;
            this.frequency = frequency;
            this.fontSize = fontSize;
            this.color = color;
        }
        
        // Getters
        public String getText() { return text; }
        public int getFrequency() { return frequency; }
        public int getFontSize() { return fontSize; }
        public String getColor() { return color; }
        
        // Setters
        public void setText(String text) { this.text = text; }
        public void setFrequency(int frequency) { this.frequency = frequency; }
        public void setFontSize(int fontSize) { this.fontSize = fontSize; }
        public void setColor(String color) { this.color = color; }
    }
}