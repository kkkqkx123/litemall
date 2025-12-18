package org.linlinjava.litemall.db.dao;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

public interface StatMapper {
    List<Map<String, Object>> statUser();

    List<Map<String, Object>> statOrder();
    
    /**
     * 增强版订单统计查询，支持时间维度和商品类别筛选
     * @param groupBy 统计组织方式：year/quarter/month/day
     * @param categoryId 商品类别ID，可为null
     * @return 订单统计数据
     */
    List<Map<String, Object>> statOrderEnhanced(@Param("groupBy") String groupBy, @Param("categoryId") Integer categoryId);
    
    /**
     * 增强版订单统计查询，支持时间范围和商品类别筛选
     * @param groupBy 统计组织方式：year/quarter/month/day
     * @param categoryId 商品类别ID，可为null
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 订单统计数据
     */
    List<Map<String, Object>> statOrderEnhancedWithTimeRange(@Param("groupBy") String groupBy, 
                                           @Param("categoryId") Integer categoryId,
                                           @Param("startTime") java.time.LocalDateTime startTime,
                                           @Param("endTime") java.time.LocalDateTime endTime);

    List<Map<String, Object>> statGoods();
    
    /**
     * 商品评分统计查询
     * @param categoryId 商品分类ID，可为null
     * @param sort 排序字段：avg_rating
     * @param order 排序方式：asc/desc
     * @param offset 偏移量
     * @param limit 每页条数
     * @return 商品评分统计数据
     */
    List<Map<String, Object>> statGoodsRating(@Param("categoryId") Integer categoryId, 
                             @Param("sort") String sort, 
                             @Param("order") String order,
                             @Param("offset") Integer offset,
                             @Param("limit") Integer limit);
    
    /**
     * 商品评分统计总数（用于分页）
     * @param categoryId 商品分类ID，可为null
     * @return 总记录数
     */
    int countGoodsRating(@Param("categoryId") Integer categoryId);
    
    /**
     * 获取商品分类列表（用于筛选）
     * @return 商品分类列表
     */
    List<Map<String, Object>> statGoodsCategories();
    
    /**
     * 商品评论统计列表
     * @param categoryId 商品分类ID，可为null
     * @param offset 偏移量
     * @param limit 每页条数
     * @return 商品评论统计数据
     */
    List<Map<String, Object>> statGoodsComment(@Param("categoryId") Integer categoryId,
                              @Param("offset") Integer offset,
                              @Param("limit") Integer limit);
    
    /**
     * 商品评论统计总数（用于分页）
     * @param categoryId 商品分类ID，可为null
     * @return 总记录数
     */
    int countGoodsComment(@Param("categoryId") Integer categoryId);
    
    /**
     * 获取商品评论内容（用于词云）
     * @param goodsId 商品ID
     * @return 评论内容列表
     */
    List<Map<String, Object>> getGoodsComments(@Param("goodsId") Integer goodsId);
    
    /**
     * 获取商品评论内容（用于词云）- 支持批量商品ID
     * @param goodsIds 商品ID列表
     * @return 评论内容列表
     */
    List<Map<String, Object>> getGoodsComments(@Param("goodsIds") List<Integer> goodsIds);
    
    /**
     * 获取分类下所有商品的评论内容（用于全局词云）
     * @param categoryId 商品分类ID，可为null
     * @return 评论内容列表
     */
    List<Map<String, Object>> getCommentsByCategory(@Param("categoryId") Integer categoryId);
    
    /**
     * 获取全站所有评论内容（用于全局词云）
     * @return 评论内容列表
     */
    List<Map<String, Object>> getAllComments();
}