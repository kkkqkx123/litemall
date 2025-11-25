# 大模型商品问答功能 - 数据库层优化设计

## 1. 数据库结构优化

### 1.1 问答会话历史表创建

为支持问答功能，需要创建会话历史记录表：

```sql
-- 问答会话历史表
CREATE TABLE `litemall_qa_session` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL COMMENT '会话ID',
  `user_id` int(11) DEFAULT NULL COMMENT '用户ID（可选）',
  `question` text NOT NULL COMMENT '用户问题',
  `answer` text COMMENT '系统回答',
  `query_intent` json COMMENT '查询意图JSON',
  `sql_query` text COMMENT '执行的SQL查询',
  `query_result` json COMMENT '查询结果',
  `response_time` int(11) DEFAULT NULL COMMENT '响应时间（毫秒）',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态：1-成功，0-失败',
  `error_msg` varchar(255) DEFAULT NULL COMMENT '错误信息',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答会话历史表';

-- 问答会话统计表（可选，用于性能监控）
CREATE TABLE `litemall_qa_statistics` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL COMMENT '会话ID',
  `query_type` varchar(32) DEFAULT NULL COMMENT '查询类型',
  `total_queries` int(11) DEFAULT '0' COMMENT '总查询次数',
  `avg_response_time` int(11) DEFAULT NULL COMMENT '平均响应时间',
  `success_count` int(11) DEFAULT '0' COMMENT '成功次数',
  `fail_count` int(11) DEFAULT '0' COMMENT '失败次数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`),
  KEY `idx_query_type` (`query_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答会话统计表';
```

### 1.2 商品表索引优化

为提升问答查询性能，在商品表上添加复合索引：

```sql
-- 商品表索引优化
ALTER TABLE `litemall_goods` 
ADD INDEX `idx_name_category` (`name`, `category_id`),
ADD INDEX `idx_price_sale` (`retail_price`, `is_on_sale`),
ADD INDEX `idx_category_brand` (`category_id`, `brand_id`),
ADD INDEX `idx_keywords` (`keywords`),
ADD FULLTEXT INDEX `ft_name_brief` (`name`, `brief`);

-- 库存查询优化索引
ALTER TABLE `litemall_goods_product` 
ADD INDEX `idx_goods_number` (`goods_id`, `number`);
```

### 1.3 分类和品牌表索引

```sql
-- 分类表索引
ALTER TABLE `litemall_category` 
ADD INDEX `idx_name_level` (`name`, `level`),
ADD INDEX `idx_pid` (`pid`);

-- 品牌表索引
ALTER TABLE `litemall_brand` 
ADD INDEX `idx_name` (`name`);
```

## 2. MyBatis映射文件优化

### 2.1 商品查询优化映射

在`LitemallGoodsMapper.xml`中添加问答专用查询：

```xml
<!-- 商品问答专用查询 -->
<!-- 根据名称模式查询商品 -->
<select id="selectByNamePattern" parameterType="map" resultMap="BaseResultMap">
  SELECT 
    <include refid="Base_Column_List" />
  FROM litemall_goods
  WHERE deleted = 0
    AND is_on_sale = 1
    <choose>
      <when test="matchMode == 'exact'">
        AND name = #{pattern}
      </when>
      <when test="matchMode == 'contains'">
        AND name LIKE CONCAT('%', #{pattern}, '%')
      </when>
      <when test="matchMode == 'starts_with'">
        AND name LIKE CONCAT(#{pattern}, '%')
      </when>
      <when test="matchMode == 'ends_with'">
        AND name LIKE CONCAT('%', #{pattern})
      </when>
      <when test="matchMode == 'regex'">
        AND name REGEXP #{pattern}
      </when>
      <otherwise>
        AND name LIKE CONCAT('%', #{pattern}, '%')
      </otherwise>
    </choose>
    <if test="caseSensitive != true">
      AND LOWER(name) LIKE LOWER(#{pattern})
    </if>
  <if test="sort != null">
    ORDER BY ${sort}
  </if>
  <if test="limit != null">
    LIMIT #{limit}
  </if>
</select>

<!-- 根据价格范围查询商品 -->
<select id="selectByPriceRange" parameterType="map" resultMap="BaseResultMap">
  SELECT 
    <include refid="Base_Column_List" />
  FROM litemall_goods
  WHERE deleted = 0
    AND is_on_sale = 1
    <if test="minPrice != null">
      AND retail_price >= #{minPrice}
    </if>
    <if test="maxPrice != null">
      AND retail_price &lt;= #{maxPrice}
    </if>
  <if test="sort != null">
    ORDER BY ${sort}
  </if>
  <if test="limit != null">
    LIMIT #{limit}
  </if>
</select>

<!-- 根据分类查询商品 -->
<select id="selectByCategory" parameterType="map" resultMap="BaseResultMap">
  SELECT 
    <include refid="Base_Column_List" />
  FROM litemall_goods
  WHERE deleted = 0
    AND is_on_sale = 1
    AND category_id = #{categoryId}
  <if test="sort != null">
    ORDER BY ${sort}
  </if>
  <if test="limit != null">
    LIMIT #{limit}
  </if>
</select>

<!-- 根据库存状态查询商品 -->
<select id="selectByStockStatus" parameterType="map" resultMap="BaseResultMap">
  SELECT 
    g.<include refid="Base_Column_List" />
  FROM litemall_goods g
  INNER JOIN litemall_goods_product p ON g.id = p.goods_id
  WHERE g.deleted = 0
    AND g.is_on_sale = 1
    AND p.deleted = 0
    <choose>
      <when test="inStock == true">
        AND p.number > 0
      </when>
      <otherwise>
        AND p.number = 0
      </otherwise>
    </choose>
  GROUP BY g.id
  <if test="sort != null">
    ORDER BY ${sort}
  </if>
  <if test="limit != null">
    LIMIT #{limit}
  </if>
</select>

<!-- 多条件组合查询 -->
<select id="selectByMultipleConditions" parameterType="map" resultMap="BaseResultMap">
  SELECT DISTINCT
    g.<include refid="Base_Column_List" />
  FROM litemall_goods g
  <if test="checkStock == true">
    INNER JOIN litemall_goods_product p ON g.id = p.goods_id AND p.deleted = 0
  </if>
  WHERE g.deleted = 0
    AND g.is_on_sale = 1
    
    <if test="namePattern != null">
      AND g.name LIKE CONCAT('%', #{namePattern}, '%')
    </if>
    <if test="categoryId != null">
      AND g.category_id = #{categoryId}
    </if>
    <if test="brandId != null">
      AND g.brand_id = #{brandId}
    </if>
    <if test="minPrice != null">
      AND g.retail_price >= #{minPrice}
    </if>
    <if test="maxPrice != null">
      AND g.retail_price &lt;= #{maxPrice}
    </if>
    <if test="keywords != null and keywords.size() > 0">
      AND (
        <foreach collection="keywords" item="keyword" separator="OR">
          g.keywords LIKE CONCAT('%', #{keyword}, '%')
          OR g.name LIKE CONCAT('%', #{keyword}, '%')
        </foreach>
      )
    </if>
    <if test="inStock == true">
      <if test="checkStock == true">
        AND p.number > 0
      </if>
    </if>
    
  <if test="sort != null">
    ORDER BY ${sort}
  </if>
  <if test="limit != null">
    LIMIT #{limit}
  </if>
</select>
```

### 2.2 问答会话历史映射

创建`LitemallQaSessionMapper.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.linlinjava.litemall.db.dao.LitemallQaSessionMapper">
  
  <resultMap id="BaseResultMap" type="org.linlinjava.litemall.db.domain.LitemallQaSession">
    <id column="id" jdbcType="INTEGER" property="id" />
    <result column="session_id" jdbcType="VARCHAR" property="sessionId" />
    <result column="user_id" jdbcType="INTEGER" property="userId" />
    <result column="question" jdbcType="LONGVARCHAR" property="question" />
    <result column="answer" jdbcType="LONGVARCHAR" property="answer" />
    <result column="query_intent" jdbcType="VARCHAR" property="queryIntent" typeHandler="org.linlinjava.litemall.db.mybatis.JsonTypeHandler"/>
    <result column="sql_query" jdbcType="LONGVARCHAR" property="sqlQuery" />
    <result column="query_result" jdbcType="VARCHAR" property="queryResult" typeHandler="org.linlinjava.litemall.db.mybatis.JsonTypeHandler"/>
    <result column="response_time" jdbcType="INTEGER" property="responseTime" />
    <result column="status" jdbcType="BIT" property="status" />
    <result column="error_msg" jdbcType="VARCHAR" property="errorMsg" />
    <result column="create_time" jdbcType="TIMESTAMP" property="createTime" />
    <result column="update_time" jdbcType="TIMESTAMP" property="updateTime" />
  </resultMap>
  
  <sql id="Base_Column_List">
    id, session_id, user_id, question, answer, query_intent, sql_query, 
    query_result, response_time, status, error_msg, create_time, update_time
  </sql>
  
  <!-- 插入问答记录 -->
  <insert id="insert" parameterType="org.linlinjava.litemall.db.domain.LitemallQaSession">
    INSERT INTO litemall_qa_session 
    (session_id, user_id, question, answer, query_intent, sql_query, 
     query_result, response_time, status, error_msg, create_time, update_time)
    VALUES 
    (#{sessionId}, #{userId}, #{question}, #{answer}, 
     #{queryIntent,typeHandler=org.linlinjava.litemall.db.mybatis.JsonTypeHandler}, 
     #{sqlQuery}, 
     #{queryResult,typeHandler=org.linlinjava.litemall.db.mybatis.JsonTypeHandler}, 
     #{responseTime}, #{status}, #{errorMsg}, #{createTime}, #{updateTime})
  </insert>
  
  <!-- 根据会话ID查询 -->
  <select id="selectBySessionId" parameterType="string" resultMap="BaseResultMap">
    SELECT <include refid="Base_Column_List" />
    FROM litemall_qa_session
    WHERE session_id = #{sessionId}
    ORDER BY create_time DESC
  </select>
  
  <!-- 查询最近的问答记录 -->
  <select id="selectRecentBySessionId" parameterType="map" resultMap="BaseResultMap">
    SELECT <include refid="Base_Column_List" />
    FROM litemall_qa_session
    WHERE session_id = #{sessionId}
    <if test="status != null">
      AND status = #{status}
    </if>
    ORDER BY create_time DESC
    <if test="limit != null">
      LIMIT #{limit}
    </if>
  </select>
  
  <!-- 查询用户问答历史 -->
  <select id="selectByUserId" parameterType="map" resultMap="BaseResultMap">
    SELECT <include refid="Base_Column_List" />
    FROM litemall_qa_session
    WHERE user_id = #{userId}
    <if test="status != null">
      AND status = #{status}
    </if>
    ORDER BY create_time DESC
    <if test="limit != null">
      LIMIT #{limit}
    </if>
  </select>
  
  <!-- 统计问答数据 -->
  <select id="getStatistics" parameterType="map" resultType="map">
    SELECT 
      COUNT(*) as total_count,
      SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as success_count,
      SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) as fail_count,
      AVG(response_time) as avg_response_time,
      MAX(response_time) as max_response_time,
      MIN(response_time) as min_response_time
    FROM litemall_qa_session
    WHERE 1=1
    <if test="startTime != null">
      AND create_time >= #{startTime}
    </if>
    <if test="endTime != null">
      AND create_time &lt;= #{endTime}
    </if>
    <if test="userId != null">
      AND user_id = #{userId}
    </if>
  </select>
  
</mapper>
```

### 2.3 DAO接口扩展

在`LitemallGoodsMapper`接口中添加问答专用方法：

```java
package org.linlinjava.litemall.db.dao;

import org.apache.ibatis.annotations.Param;
import org.linlinjava.litemall.db.domain.LitemallGoods;
import java.util.List;
import java.util.Map;

public interface LitemallGoodsMapper {
    // 原有方法保持不变...
    
    // ===== 问答功能专用查询方法 =====
    
    /**
     * 根据名称模式查询商品
     */
    List<LitemallGoods> selectByNamePattern(@Param("pattern") String pattern, 
                                          @Param("matchMode") String matchMode,
                                          @Param("caseSensitive") Boolean caseSensitive,
                                          @Param("sort") String sort,
                                          @Param("limit") Integer limit);
    
    /**
     * 根据价格范围查询商品
     */
    List<LitemallGoods> selectByPriceRange(@Param("minPrice") Double minPrice,
                                         @Param("maxPrice") Double maxPrice,
                                         @Param("sort") String sort,
                                         @Param("limit") Integer limit);
    
    /**
     * 根据分类查询商品
     */
    List<LitemallGoods> selectByCategory(@Param("categoryId") Integer categoryId,
                                        @Param("sort") String sort,
                                        @Param("limit") Integer limit);
    
    /**
     * 根据库存状态查询商品
     */
    List<LitemallGoods> selectByStockStatus(@Param("inStock") Boolean inStock,
                                          @Param("sort") String sort,
                                          @Param("limit") Integer limit);
    
    /**
     * 多条件组合查询
     */
    List<LitemallGoods> selectByMultipleConditions(Map<String, Object> params);
}
```

创建问答会话历史DAO接口：

```java
package org.linlinjava.litemall.db.dao;

import org.apache.ibatis.annotations.Param;
import org.linlinjava.litemall.db.domain.LitemallQaSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface LitemallQaSessionMapper {
    
    /**
     * 插入问答记录
     */
    int insert(LitemallQaSession record);
    
    /**
     * 根据会话ID查询
     */
    List<LitemallQaSession> selectBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 查询最近的问答记录
     */
    List<LitemallQaSession> selectRecentBySessionId(@Param("sessionId") String sessionId,
                                                     @Param("status") Boolean status,
                                                     @Param("limit") Integer limit);
    
    /**
     * 查询用户问答历史
     */
    List<LitemallQaSession> selectByUserId(@Param("userId") Integer userId,
                                            @Param("status") Boolean status,
                                            @Param("limit") Integer limit);
    
    /**
     * 统计问答数据
     */
    Map<String, Object> getStatistics(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime,
                                     @Param("userId") Integer userId);
}
```

## 3. 性能优化建议

### 3.1 索引使用建议

1. **复合索引优化**：
   - `(name, category_id)`：支持名称+分类的联合查询
   - `(retail_price, is_on_sale)`：支持价格范围+上架状态的查询
   - `(category_id, brand_id)`：支持分类+品牌的联合筛选

2. **全文索引**：
   - `FULLTEXT(name, brief)`：支持商品名称和简介的全文搜索
   - 注意：全文索引仅支持MyISAM和InnoDB（5.6+）引擎

3. **前缀索引**：
   - `name(50)`：对于长文本字段，可以创建前缀索引节省空间

### 3.2 查询优化

1. **避免全表扫描**：
   - 所有查询都必须包含`deleted = 0`和`is_on_sale = 1`条件
   - 使用合适的索引避免文件排序

2. **JOIN优化**：
   - 库存查询使用INNER JOIN时确保关联字段有索引
   - 考虑使用EXISTS替代IN子查询

3. **分页优化**：
   - 大数据量分页使用游标或延迟关联
   - 避免使用OFFSET大的LIMIT查询

### 3.3 配置优化

1. **MySQL配置**：
   ```ini
   # 增加InnoDB缓冲池大小
   innodb_buffer_pool_size = 1G
   
   # 启用查询缓存（MySQL 5.7及以下）
   query_cache_size = 64M
   query_cache_type = 1
   
   # 优化连接数
   max_connections = 200
   
   # 启用慢查询日志
   slow_query_log = 1
   long_query_time = 1
   ```

2. **连接池配置**：
   ```properties
   # HikariCP连接池配置
   spring.datasource.hikari.maximum-pool-size=20
   spring.datasource.hikari.minimum-idle=5
   spring.datasource.hikari.connection-timeout=30000
   spring.datasource.hikari.idle-timeout=600000
   spring.datasource.hikari.max-lifetime=1800000
   ```

## 4. 监控与维护

### 4.1 查询性能监控

```sql
-- 查看慢查询
SHOW VARIABLES LIKE 'slow_query%';
SHOW PROCESSLIST;

-- 分析查询执行计划
EXPLAIN SELECT * FROM litemall_goods WHERE name LIKE '%手机%';

-- 查看索引使用情况
SHOW INDEX FROM litemall_goods;
```

### 4.2 定期维护任务

```sql
-- 更新统计信息
ANALYZE TABLE litemall_goods;

-- 优化表
OPTIMIZE TABLE litemall_goods;

-- 清理历史数据（可配置保留时间）
DELETE FROM litemall_qa_session 
WHERE create_time < DATE_SUB(NOW(), INTERVAL 90 DAY);
```

## 5. 安全考虑

### 5.1 SQL注入防护

1. **使用MyBatis参数绑定**：
   - 所有用户输入都通过`#{}`进行参数绑定
   - 避免使用`${}`直接拼接SQL

2. **输入验证**：
   - 限制搜索关键词长度（如最多50个字符）
   - 过滤特殊字符（如SQL关键字）

### 5.2 数据权限控制

1. **会话隔离**：
   - 确保用户只能查看自己的问答历史
   - 会话ID使用随机字符串避免猜测

2. **数据脱敏**：
   - 敏感信息（如用户ID）在日志中脱敏处理
   - 错误信息不要暴露系统内部细节

通过以上数据库层优化，可以显著提升商品问答功能的查询性能和用户体验。