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
}