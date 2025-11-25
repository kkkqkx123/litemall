package org.linlinjava.litemall.core.llm.service;

import org.linlinjava.litemall.core.llm.exception.LLMServiceException;
import org.linlinjava.litemall.core.llm.model.QueryIntent;
import org.linlinjava.litemall.core.llm.parser.ParameterBinder;
import org.linlinjava.litemall.core.llm.parser.SQLBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;

/**
 * 商品查询服务
 * 负责执行数据库查询操作
 */
@Service
public class GoodsQueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(GoodsQueryService.class);
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private SQLBuilder sqlBuilder;
    
    @Autowired
    private ParameterBinder parameterBinder;
    
    /**
     * 执行查询
     * @param queryIntent 查询意图
     * @return 查询结果
     */
    public List<Map<String, Object>> executeQuery(QueryIntent queryIntent) {
        if (queryIntent == null || !queryIntent.isValid()) {
            throw new IllegalArgumentException("无效的查询意图");
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            // 构建SQL
            String sql = sqlBuilder.buildQuerySQL(queryIntent);
            logger.debug("执行查询SQL：{}", sql);
            
            // 获取连接
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            
            // 绑定参数
            List<Object> parameters = parameterBinder.extractParameters(queryIntent);
            logger.debug("查询参数：{}", parameters);
            parameterBinder.bindParameters(stmt, parameters);
            
            // 执行查询
            rs = stmt.executeQuery();
            
            // 转换结果
            return convertResultSetToList(rs);
            
        } catch (Exception e) {
            logger.error("执行查询失败", e);
            throw new LLMServiceException("执行查询失败：" + e.getMessage(), e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
    
    /**
     * 执行统计查询
     * @param queryIntent 查询意图
     * @return 统计结果
     */
    public List<Map<String, Object>> executeStatisticalQuery(QueryIntent queryIntent) {
        if (queryIntent == null || !queryIntent.isValid()) {
            throw new IllegalArgumentException("无效的查询意图");
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            // 构建统计SQL
            String sql = sqlBuilder.buildStatisticalSQL(queryIntent);
            logger.debug("执行统计查询SQL：{}", sql);
            
            // 获取连接
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            
            // 统计查询通常不需要参数绑定
            
            // 执行查询
            rs = stmt.executeQuery();
            
            // 转换结果
            return convertResultSetToList(rs);
            
        } catch (Exception e) {
            logger.error("执行统计查询失败", e);
            throw new LLMServiceException("执行统计查询失败：" + e.getMessage(), e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
    
    /**
     * 将ResultSet转换为List<Map>
     * @param rs 结果集
     * @return 转换后的列表
     * @throws Exception 当转换失败时抛出
     */
    private List<Map<String, Object>> convertResultSetToList(ResultSet rs) throws Exception {
        List<Map<String, Object>> resultList = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                Object value = rs.getObject(i);
                row.put(columnName, value);
            }
            resultList.add(row);
        }
        
        return resultList;
    }
    
    /**
     * 关闭数据库资源
     * @param conn 连接
     * @param stmt 语句
     * @param rs 结果集
     */
    private void closeResources(Connection conn, PreparedStatement stmt, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (Exception e) {
            logger.warn("关闭ResultSet失败", e);
        }
        
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (Exception e) {
            logger.warn("关闭PreparedStatement失败", e);
        }
        
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (Exception e) {
            logger.warn("关闭Connection失败", e);
        }
    }
    
    /**
     * 获取商品总数
     * @return 商品总数
     */
    public long getTotalGoodsCount() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement("SELECT COUNT(*) FROM litemall_goods WHERE is_on_sale = 1 AND deleted = 0");
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            
            return 0;
            
        } catch (Exception e) {
            logger.error("获取商品总数失败", e);
            throw new LLMServiceException("获取商品总数失败：" + e.getMessage(), e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
    
    /**
     * 获取价格范围
     * @return 价格范围（包含minPrice和maxPrice）
     */
    public Map<String, Object> getPriceRange() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(
                "SELECT MIN(retail_price) as min_price, MAX(retail_price) as max_price " +
                "FROM litemall_goods WHERE is_on_sale = 1 AND deleted = 0"
            );
            rs = stmt.executeQuery();
            
            Map<String, Object> result = new HashMap<>();
            if (rs.next()) {
                result.put("minPrice", rs.getDouble("min_price"));
                result.put("maxPrice", rs.getDouble("max_price"));
            } else {
                result.put("minPrice", 0.0);
                result.put("maxPrice", 0.0);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("获取价格范围失败", e);
            throw new LLMServiceException("获取价格范围失败：" + e.getMessage(), e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
    
    /**
     * 获取库存统计
     * @return 库存统计信息
     */
    public Map<String, Object> getStockStatistics() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(
                "SELECT COUNT(*) as total_count, SUM(number) as total_stock, AVG(number) as avg_stock " +
                "FROM litemall_goods WHERE is_on_sale = 1 AND deleted = 0"
            );
            rs = stmt.executeQuery();
            
            Map<String, Object> result = new HashMap<>();
            if (rs.next()) {
                result.put("totalCount", rs.getLong("total_count"));
                result.put("totalStock", rs.getLong("total_stock"));
                result.put("avgStock", rs.getDouble("avg_stock"));
            } else {
                result.put("totalCount", 0L);
                result.put("totalStock", 0L);
                result.put("avgStock", 0.0);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("获取库存统计失败", e);
            throw new LLMServiceException("获取库存统计失败：" + e.getMessage(), e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
    
    /**
     * 获取分类统计
     * @return 分类统计信息
     */
    public List<Map<String, Object>> getCategoryStatistics() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(
                "SELECT category_id, COUNT(*) as goods_count " +
                "FROM litemall_goods WHERE is_on_sale = 1 AND deleted = 0 " +
                "GROUP BY category_id ORDER BY goods_count DESC"
            );
            rs = stmt.executeQuery();
            
            return convertResultSetToList(rs);
            
        } catch (Exception e) {
            logger.error("获取分类统计失败", e);
            throw new LLMServiceException("获取分类统计失败：" + e.getMessage(), e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
    
    /**
     * 根据ID获取商品
     * @param id 商品ID
     * @return 商品信息
     */
    public Map<String, Object> getGoodsById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(
                "SELECT * FROM litemall_goods WHERE id = ? AND deleted = 0"
            );
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            List<Map<String, Object>> results = convertResultSetToList(rs);
            return results.isEmpty() ? null : results.get(0);
            
        } catch (Exception e) {
            logger.error("获取商品失败，ID：{}" , id, e);
            throw new LLMServiceException("获取商品失败：" + e.getMessage(), e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
    
    /**
     * 搜索商品
     * @param keyword 关键词
     * @param limit 限制数量
     * @return 商品列表
     */
    public List<Map<String, Object>> searchGoods(String keyword, Integer limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("搜索关键词不能为空");
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            String searchKeyword = "%" + keyword + "%";
            
            conn = dataSource.getConnection();
            StringBuilder sqlBuilder = new StringBuilder(
                "SELECT * FROM litemall_goods " +
                "WHERE (name LIKE ? OR keywords LIKE ? OR brief LIKE ?) " +
                "AND is_on_sale = 1 AND deleted = 0 " +
                "ORDER BY sort_order ASC, add_time DESC"
            );
            
            // 验证limit参数，防止SQL注入
            if (limit != null) {
                if (limit < 0) {
                    throw new IllegalArgumentException("限制数量不能为负数");
                }
                if (limit > 1000) {
                    limit = 1000; // 设置合理的上限
                    logger.warn("限制数量超过上限，已调整为1000");
                }
                sqlBuilder.append(" LIMIT ?");
            }
            
            stmt = conn.prepareStatement(sqlBuilder.toString());
            stmt.setString(1, searchKeyword);
            stmt.setString(2, searchKeyword);
            stmt.setString(3, searchKeyword);
            
            // 设置limit参数
            if (limit != null) {
                stmt.setInt(4, limit);
            }
            
            rs = stmt.executeQuery();
            
            return convertResultSetToList(rs);
            
        } catch (Exception e) {
            logger.error("搜索商品失败，关键词：{}", keyword, e);
            throw new LLMServiceException("搜索商品失败：" + e.getMessage(), e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
}