package org.linlinjava.litemall.db.service;

import org.linlinjava.litemall.db.dao.StatMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class StatService {
    @Resource
    private StatMapper statMapper;


    public List<Map> statUser() {
        return statMapper.statUser();
    }

    public List<Map> statOrder() {
        return statMapper.statOrder();
    }
    
    /**
     * 增强版订单统计查询，支持时间维度和商品类别筛选
     * @param timeDimension 时间维度：day/week/month
     * @param categoryId 商品类别ID，可为null
     * @return 订单统计数据
     */
    public List<Map> statOrderEnhanced(String timeDimension, Integer categoryId) {
        return statMapper.statOrderEnhanced(timeDimension, categoryId);
    }

    public List<Map> statGoods() {
        return statMapper.statGoods();
    }
}
