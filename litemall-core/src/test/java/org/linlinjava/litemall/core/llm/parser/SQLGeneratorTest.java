package org.linlinjava.litemall.core.llm.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linlinjava.litemall.core.llm.exception.SQLGenerationException;
import org.linlinjava.litemall.core.llm.model.QueryIntent;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SQL生成器单元测试
 */
@ExtendWith(MockitoExtension.class)
class SQLGeneratorTest {
    
    private SQLGenerator sqlGenerator;
    
    @Mock
    private SQLBuilder sqlBuilder;
    
    @BeforeEach
    void setUp() {
        sqlGenerator = new SQLGenerator();
        // 使用反射注入mock对象
        try {
            java.lang.reflect.Field field = SQLGenerator.class.getDeclaredField("sqlBuilder");
            field.setAccessible(true);
            field.set(sqlGenerator, sqlBuilder);
        } catch (Exception e) {
            throw new RuntimeException("注入mock对象失败", e);
        }
    }
    
    @Test
    @DisplayName("测试有效的产品搜索SQL生成")
    void testGenerateProductSearchSQL() throws SQLGenerationException {
        QueryIntent queryIntent = new QueryIntent();
        queryIntent.setQueryType("product_search");
        queryIntent.setConfidence(0.85);
        
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("category", "电子产品");
        conditions.put("price_range", Map.of("min", 1000, "max", 5000));
        queryIntent.setConditions(conditions);
        
        String expectedSQL = "SELECT * FROM litemall_product WHERE category = '电子产品' AND price BETWEEN 1000 AND 5000";
        when(sqlBuilder.buildQuerySQL(queryIntent)).thenReturn(expectedSQL);
        
        String result = sqlGenerator.generateSQL(queryIntent);
        
        assertNotNull(result);
        assertEquals(expectedSQL, result);
        verify(sqlBuilder).buildQuerySQL(queryIntent);
    }
    
    @Test
    @DisplayName("测试价格范围SQL生成")
    void testGeneratePriceRangeSQL() throws SQLGenerationException {
        QueryIntent queryIntent = new QueryIntent();
        queryIntent.setQueryType("price_range");
        queryIntent.setConfidence(0.92);
        queryIntent.setLimit(10);
        
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("product_name", "手机");
        conditions.put("price_range", Map.of("min", 2000, "max", 8000));
        queryIntent.setConditions(conditions);
        
        String expectedSQL = "SELECT * FROM litemall_product WHERE name LIKE '%手机%' AND price BETWEEN 2000 AND 8000 LIMIT 10";
        when(sqlBuilder.buildQuerySQL(queryIntent)).thenReturn(expectedSQL);
        
        String result = sqlGenerator.generateSQL(queryIntent);
        
        assertNotNull(result);
        assertEquals(expectedSQL, result);
        verify(sqlBuilder).buildQuerySQL(queryIntent);
    }
    
    @Test
    @DisplayName("测试库存检查SQL生成")
    void testGenerateStockCheckSQL() throws SQLGenerationException {
        QueryIntent queryIntent = new QueryIntent();
        queryIntent.setQueryType("stock_check");
        queryIntent.setConfidence(0.88);
        
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("product_id", 123);
        conditions.put("stock_threshold", 10);
        queryIntent.setConditions(conditions);
        
        String expectedSQL = "SELECT * FROM litemall_product WHERE id = 123 AND number <= 10";
        when(sqlBuilder.buildQuerySQL(queryIntent)).thenReturn(expectedSQL);
        
        String result = sqlGenerator.generateSQL(queryIntent);
        
        assertNotNull(result);
        assertEquals(expectedSQL, result);
        verify(sqlBuilder).buildQuerySQL(queryIntent);
    }
    
    @Test
    @DisplayName("测试统计查询SQL生成")
    void testGenerateStatisticalSQL() throws SQLGenerationException {
        QueryIntent queryIntent = new QueryIntent();
        queryIntent.setQueryType("statistical_query");
        queryIntent.setConfidence(0.91);
        queryIntent.setExplanation("统计2024年服装类别的销售数据");
        
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("category", "服装");
        conditions.put("date_range", Map.of("start", "2024-01-01", "end", "2024-12-31"));
        queryIntent.setConditions(conditions);
        
        String expectedSQL = "SELECT COUNT(*) as total, AVG(price) as avg_price FROM litemall_product WHERE category = '服装' AND date BETWEEN '2024-01-01' AND '2024-12-31'";
        when(sqlBuilder.buildStatisticalSQL(queryIntent)).thenReturn(expectedSQL);
        
        String result = sqlGenerator.generateSQL(queryIntent);
        
        assertNotNull(result);
        assertEquals(expectedSQL, result);
        verify(sqlBuilder).buildStatisticalSQL(queryIntent);
    }
    
    @Test
    @DisplayName("测试空查询意图抛出异常")
    void testGenerateSQLWithNullQueryIntentThrowsException() {
        SQLGenerationException exception = assertThrows(SQLGenerationException.class, () -> {
            sqlGenerator.generateSQL(null);
        });
        
        assertTrue(exception.getMessage().contains("查询意图对象为空"));
    }
    
    @Test
    @DisplayName("测试空查询类型抛出异常")
    void testGenerateSQLWithNullQueryTypeThrowsException() {
        QueryIntent queryIntent = new QueryIntent();
        queryIntent.setQueryType(null);
        queryIntent.setConfidence(0.85);
        
        SQLGenerationException exception = assertThrows(SQLGenerationException.class, () -> {
            sqlGenerator.generateSQL(queryIntent);
        });
        
        assertTrue(exception.getMessage().contains("查询类型为空"));
        assertNull(exception.getQueryType());
        assertEquals("query_type", exception.getInvalidCondition());
    }
    
    @Test
    @DisplayName("测试不支持的查询类型抛出异常")
    void testGenerateSQLWithUnsupportedQueryTypeThrowsException() {
        QueryIntent queryIntent = new QueryIntent();
        queryIntent.setQueryType("unsupported_type");
        queryIntent.setConfidence(0.85);
        
        SQLGenerationException exception = assertThrows(SQLGenerationException.class, () -> {
            sqlGenerator.generateSQL(queryIntent);
        });
        
        assertTrue(exception.getMessage().contains("不支持的查询类型"));
        assertEquals("unsupported_type", exception.getQueryType());
    }
    
    @Test
    @DisplayName("测试生成的SQL为空抛出异常")
    void testGenerateSQLEmptyResultThrowsException() throws SQLGenerationException {
        QueryIntent queryIntent = new QueryIntent();
        queryIntent.setQueryType("product_search");
        queryIntent.setConfidence(0.85);
        
        when(sqlBuilder.buildQuerySQL(queryIntent)).thenReturn("");
        
        SQLGenerationException exception = assertThrows(SQLGenerationException.class, () -> {
            sqlGenerator.generateSQL(queryIntent);
        });
        
        assertTrue(exception.getMessage().contains("生成的SQL语句为空"));
    }
    
    @Test
    @DisplayName("测试验证有效查询意图")
    void testValidateValidQueryIntent() {
        QueryIntent queryIntent = new QueryIntent();
        queryIntent.setQueryType("product_search");
        queryIntent.setConfidence(0.85);
        queryIntent.setLimit(10);
        
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("category", "电子产品");
        conditions.put("price_range", Map.of("min", 1000, "max", 5000));
        queryIntent.setConditions(conditions);
        
        boolean result = sqlGenerator.validateQueryIntent(queryIntent);
        
        assertTrue(result);
    }
    
    @Test
    @DisplayName("测试验证空查询意图返回false")
    void testValidateNullQueryIntentReturnsFalse() {
        boolean result = sqlGenerator.validateQueryIntent(null);
        
        assertFalse(result);
    }
    
    @Test
    @DisplayName("测试验证空查询类型返回false")
    void testValidateEmptyQueryTypeReturnsFalse() {
        QueryIntent queryIntent = new QueryIntent();
        queryIntent.setQueryType("");
        
        boolean result = sqlGenerator.validateQueryIntent(queryIntent);
        
        assertFalse(result);
    }
    
    @Test
    @DisplayName("测试验证不支持的查询类型返回false")
    void testValidateUnsupportedQueryTypeReturnsFalse() {
        QueryIntent queryIntent = new QueryIntent();
        queryIntent.setQueryType("invalid_type");
        
        boolean result = sqlGenerator.validateQueryIntent(queryIntent);
        
        assertFalse(result);
    }
    
    @Test
    @DisplayName("测试验证无效的限制数量返回false")
    void testValidateInvalidLimitReturnsFalse() {
        QueryIntent queryIntent = new QueryIntent();
        queryIntent.setQueryType("product_search");
        queryIntent.setLimit(0);
        
        boolean result = sqlGenerator.validateQueryIntent(queryIntent);
        
        assertFalse(result);
    }
    
    @Test
    @DisplayName("测试验证无效的置信度返回false")
    void testValidateInvalidConfidenceReturnsFalse() {
        QueryIntent queryIntent = new QueryIntent();
        queryIntent.setQueryType("product_search");
        queryIntent.setConfidence(1.5);
        
        boolean result = sqlGenerator.validateQueryIntent(queryIntent);
        
        assertFalse(result);
    }
    
    @Test
    @DisplayName("测试验证空条件值返回false")
    void testValidateNullConditionValueReturnsFalse() {
        QueryIntent queryIntent = new QueryIntent();
        queryIntent.setQueryType("product_search");
        
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("category", null);
        queryIntent.setConditions(conditions);
        
        boolean result = sqlGenerator.validateQueryIntent(queryIntent);
        
        assertFalse(result);
    }
}