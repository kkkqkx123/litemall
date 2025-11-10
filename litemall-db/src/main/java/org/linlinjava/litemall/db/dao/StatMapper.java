package org.linlinjava.litemall.db.dao;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

public interface StatMapper {
    List<Map> statUser();

    List<Map> statOrder();
    
    /**
     * 增强版订单统计查询，支持时间维度和商品类别筛选
     * @param timeDimension 时间维度：day/week/month
     * @param categoryId 商品类别ID，可为null
     * @return 订单统计数据
     */
    List<Map> statOrderEnhanced(@Param("timeDimension") String timeDimension, @Param("categoryId") Integer categoryId);

    List<Map> statGoods();
    
    /**
     * 商品评分统计查询
     * @param categoryId 商品分类ID，可为null
     * @param sort 排序字段：avg_rating
     * @param order 排序方式：asc/desc
     * @param offset 偏移量
     * @param limit 每页条数
     * @return 商品评分统计数据
     */
    List<Map> statGoodsRating(@Param("categoryId") Integer categoryId, 
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
    List<Map> statGoodsCategories();
}