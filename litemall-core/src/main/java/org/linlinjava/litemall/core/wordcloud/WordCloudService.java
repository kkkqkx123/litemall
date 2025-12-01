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
        "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这", "那", "他", "她", "它", "们", "与", "或", "但", "而", "因为", "所以", "如果", "虽然", "然而", "不过", "只是", "还是", "就是", "真是", "觉得", "感觉", "使用", "购买", "商品", "产品", "质量", "服务", "物流", "快递", "包装", "收到", "发货", "速度", "态度", "客服", "售后", "体验", "整体", "总体", "方面", "比较", "相对", "一般", "普通", "正常", "还行", "可以", "不错", "挺好", "满意", "喜欢", "推荐", "值得", "下次", "还会", "继续", "支持", "好评", "差评", "中评", "非常", "特别", "有点", "比较", "很", "太", "真", "实在", "确实", "真的", "特别", "尤其", "主要", "基本", "根本", "完全", "绝对", "相当", "挺", "蛮", "满", "蛮", "蛮", "蛮"
    ));
    
    // 中文分词简单实现（基于字符长度和常见词）
    private static final Set<String> COMMON_WORDS = new HashSet<>(Arrays.asList(
        "喜欢", "满意", "质量", "不错", "很好", "非常好", "太差", "不好", "一般", "还行", "可以", "推荐", "值得购买", "物美价廉", "性价比高", "包装", "物流", "快递", "发货", "速度", "客服", "服务", "态度", "售后", "体验", "效果", "外观", "颜色", "尺寸", "大小", "重量", "材质", "做工", "精致", "粗糙", "漂亮", "美观", "时尚", "实用", "方便", "简单", "复杂", "容易", "困难", "便宜", "贵", "值得", "划算", "超值", "失望", "惊喜", "意外", "满意", "开心", "高兴", "生气", "愤怒", "抱怨", "投诉", "退货", "换货", "退款", "赔偿", "五星", "好评", "差评", "中评", "正品", "假货", "山寨", "高仿", "原装", "正品", "行货", "水货", "二手", "全新", "未拆封", "已拆封", "使用过", "试用", "试穿", "试吃", "闻", "摸", "看", "观察", "对比", "比较", "选择", "挑选", "筛选", "决定", "考虑", "犹豫", "纠结", "后悔", "庆幸", "幸运", "倒霉", "郁闷", "烦躁", "生气", "愤怒", "开心", "高兴", "激动", "感动", "感谢", "谢谢", "多谢", "辛苦", "麻烦", "打扰", "抱歉", "对不起", "不好意思", "请", "您好", "你好", "嗨", "嘿", "哇", "啊", "呀", "呢", "吧", "吗", "嘛", "咯", "喽", "咧", "哒", "么么哒", "棒棒哒", "美美哒", "萌萌哒", "酷酷哒"
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
            .filter(entry -> entry.getValue() > 1) // 只保留出现次数大于1的词
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
     * 判断字符是否为中文
     */
    private boolean isChinese(char c) {
        return c >= 0x4E00 && c <= 0x9FA5;
    }
    
    /**
     * 简单的中文分词
     */
    private List<String> segmentWords(String text) {
        List<String> words = new ArrayList<>();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                // 处理英文和数字
                StringBuilder word = new StringBuilder();
                while (i < length && Character.isLetterOrDigit(text.charAt(i))) {
                    word.append(text.charAt(i));
                    i++;
                }
                if (word.length() > 1) { // 英文单词至少2个字符
                    words.add(word.toString().toLowerCase());
                }
                i--; // 回退一位
            } else if (isChinese(c)) {
                // 处理中文 - 尝试匹配常见词
                boolean matched = false;

                // 优先尝试匹配2-4个字符的常见词
                for (int wordLen = 4; wordLen >= 2; wordLen--) {
                    if (i + wordLen <= length) {
                        String candidate = text.substring(i, i + wordLen);
                        if (COMMON_WORDS.contains(candidate)) {
                            words.add(candidate);
                            i += wordLen - 1; // 跳过已匹配的字符
                            matched = true;
                            break;
                        }
                    }
                }

                // 如果没有匹配到常见词，尝试匹配2个字符的组合
                if (!matched && i + 1 < length) {
                    String twoChar = text.substring(i, i + 2);
                    if (COMMON_WORDS.contains(twoChar)) {
                        words.add(twoChar);
                        i += 1; // 跳过已匹配的字符
                        matched = true;
                    }
                }

                // 如果没有匹配到常见词，则按单字符处理（但跳过停用词）
                if (!matched) {
                    String singleChar = String.valueOf(c);
                    if (!STOP_WORDS.contains(singleChar)) {
                        words.add(singleChar);
                    }
                }
            }
            // 其他字符（标点符号等）直接跳过
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