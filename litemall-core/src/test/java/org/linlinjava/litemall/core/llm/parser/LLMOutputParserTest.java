package org.linlinjava.litemall.core.llm.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.linlinjava.litemall.core.llm.exception.JSONParseException;
import org.linlinjava.litemall.core.llm.model.QueryIntent;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LLM输出解析器单元测试
 */
class LLMOutputParserTest {
    
    private LLMOutputParser parser;
    
    @BeforeEach
    void setUp() {
        parser = new LLMOutputParser();
    }
    
    @Test
    @DisplayName("测试有效的JSON输出解析")
    void testParseValidJSONOutput() throws JSONParseException {
        String llmOutput = "{\n" +
            "    \"query_type\": \"product_search\",\n" +
            "    \"conditions\": {\n" +
            "        \"category\": \"电子产品\",\n" +
            "        \"price_range\": {\n" +
            "            \"min\": 1000,\n" +
            "            \"max\": 5000\n" +
            "        }\n" +
            "    },\n" +
            "    \"confidence\": 0.85,\n" +
            "    \"explanation\": \"用户搜索价格在1000-5000元之间的电子产品\"\n" +
            "}";
        
        QueryIntent result = parser.parseLLMOutput(llmOutput);
        
        assertNotNull(result);
        assertEquals("product_search", result.getQueryType());
        assertEquals(0.85, result.getConfidence());
        assertEquals("用户搜索价格在1000-5000元之间的电子产品", result.getExplanation());
        
        assertNotNull(result.getConditions());
        assertEquals("电子产品", result.getConditions().get("category"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> priceRange = (Map<String, Object>) result.getConditions().get("price_range");
        assertNotNull(priceRange);
        assertEquals(1000, priceRange.get("min"));
        assertEquals(5000, priceRange.get("max"));
    }
    
    @Test
    @DisplayName("测试价格范围查询解析")
    void testParsePriceRangeQuery() throws JSONParseException {
        String llmOutput = "{\n" +
            "    \"query_type\": \"price_range\",\n" +
            "    \"conditions\": {\n" +
            "        \"product_name\": \"手机\",\n" +
            "        \"price_range\": {\n" +
            "            \"min\": 2000,\n" +
            "            \"max\": 8000\n" +
            "        }\n" +
            "    },\n" +
            "    \"confidence\": 0.92,\n" +
            "    \"limit\": 10\n" +
            "}";
        
        QueryIntent result = parser.parseLLMOutput(llmOutput);
        
        assertNotNull(result);
        assertEquals("price_range", result.getQueryType());
        assertEquals(0.92, result.getConfidence());
        assertEquals(10, result.getLimit());
        
        assertNotNull(result.getConditions());
        assertEquals("手机", result.getConditions().get("product_name"));
    }
    
    @Test
    @DisplayName("测试库存检查查询解析")
    void testParseStockCheckQuery() throws JSONParseException {
        String llmOutput = "{\n" +
            "    \"query_type\": \"stock_check\",\n" +
            "    \"conditions\": {\n" +
            "        \"product_id\": 123,\n" +
            "        \"stock_threshold\": 10\n" +
            "    },\n" +
            "    \"confidence\": 0.88\n" +
            "}";
        
        QueryIntent result = parser.parseLLMOutput(llmOutput);

        assertNotNull(result);
        assertEquals("stock_check", result.getQueryType());
        assertEquals(0.88, result.getConfidence());

        assertNotNull(result.getConditions());
        assertEquals(123, result.getConditions().get("product_id"));
        assertEquals(10, result.getConditions().get("stock_threshold"));
    }

    @Test
    @DisplayName("测试统计查询解析")
    void testParseStatisticalQuery() throws JSONParseException {
        String llmOutput = "{\n" +
            "    \"query_type\": \"statistical_query\",\n" +
            "    \"conditions\": {\n" +
            "        \"category\": \"服装\",\n" +
            "        \"date_range\": {\n" +
            "            \"start\": \"2024-01-01\",\n" +
            "            \"end\": \"2024-12-31\"\n" +
            "        }\n" +
            "    },\n" +
            "    \"confidence\": 0.91,\n" +
            "    \"explanation\": \"统计2024年服装类别的销售数据\"\n" +
            "}";

        QueryIntent result = parser.parseLLMOutput(llmOutput);

        assertNotNull(result);
        assertEquals("statistical_query", result.getQueryType());
        assertEquals(0.91, result.getConfidence());
        assertEquals("统计2024年服装类别的销售数据", result.getExplanation());
    }

    @Test
    @DisplayName("测试空输出抛出异常")
    void testParseEmptyOutputThrowsException() {
        String llmOutput = "";

        JSONParseException exception = assertThrows(JSONParseException.class, () -> {
            parser.parseLLMOutput(llmOutput);
        });

        assertTrue(exception.getMessage().contains("LLM输出为空"));
    }

    @Test
    @DisplayName("测试无效JSON抛出异常")
    void testParseInvalidJSONThrowsException() {
        String llmOutput = "{\n" +
            "    \"query_type\": \"product_search\",\n" +
            "    \"conditions\": {\n" +
            "        \"category\": \"电子产品\"\n" +
            "    // 缺少闭合括号";

        JSONParseException exception = assertThrows(JSONParseException.class, () -> {
            parser.parseLLMOutput(llmOutput);
        });

        assertTrue(exception.getMessage().contains("JSON格式解析失败"));
    }

    @Test
    @DisplayName("测试缺少query_type抛出异常")
    void testParseMissingQueryTypeThrowsException() {
        String llmOutput = "{\n" +
            "    \"conditions\": {\n" +
            "        \"category\": \"电子产品\"\n" +
            "    },\n" +
            "    \"confidence\": 0.85\n" +
            "}";

        JSONParseException exception = assertThrows(JSONParseException.class, () -> {
            parser.parseLLMOutput(llmOutput);
        });
        
        assertTrue(exception.getMessage().contains("缺少必需的query_type字段"));
    }
    
    @Test
    @DisplayName("测试提取JSON内容功能")
    void testExtractJSONContent() {
        String llmOutput = "我来帮您分析这个查询。\n" +
            "\n" +
            "```json\n" +
            "{\n" +
            "    \"query_type\": \"product_search\",\n" +
            "    \"conditions\": {\n" +
            "        \"name\": \"手机\"\n" +
            "    },\n" +
            "    \"confidence\": 0.95\n" +
            "}\n" +
            "```\n" +
            "\n" +
            "这个查询将搜索手机产品。";
        
        String extractedJSON = parser.extractJSONContent(llmOutput);
        
        assertNotNull(extractedJSON);
        assertTrue(extractedJSON.contains("\"query_type\": \"product_search\""));
        assertFalse(extractedJSON.contains("我来帮您分析这个查询"));
    }
    
    @Test
    @DisplayName("测试提取JSON内容 - 无代码块")
    void testExtractJSONContentNoCodeBlock() {
        String llmOutput = "直接输出JSON：\n" +
            "{\n" +
            "    \"query_type\": \"price_range\",\n" +
            "    \"conditions\": {\n" +
            "        \"price_range\": {\"min\": 100, \"max\": 500}\n" +
            "    }\n" +
            "}";
        
        String extractedJSON = parser.extractJSONContent(llmOutput);
        
        assertNotNull(extractedJSON);
        assertTrue(extractedJSON.contains("\"query_type\": \"price_range\""));
    }
    
    @Test
    @DisplayName("测试提取JSON内容 - 无效输入")
    void testExtractJSONContentInvalidInput() {
        String llmOutput = "这不是有效的JSON内容";
        
        String extractedJSON = parser.extractJSONContent(llmOutput);
        
        assertEquals(llmOutput, extractedJSON);
    }
}