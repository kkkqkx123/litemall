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
    
    /**
     * 商品评分统计查询
     * @param categoryId 商品分类ID，可为null
     * @param sort 排序字段：avg_rating
     * @param order 排序方式：asc/desc
     * @param page 页码
     * @param limit 每页条数
     * @return 商品评分统计数据
     */
    public List<Map> statGoodsRating(Integer categoryId, String sort, String order, Integer page, Integer limit) {
        // 参数校验
        if (page == null || page < 1) {
            page = 1;
        }
        if (limit == null || limit < 1) {
            limit = 10;
        }
        if (limit > 100) {
            limit = 100;
        }
        
        // 计算分页参数
        int offset = (page - 1) * limit;
        
        return statMapper.statGoodsRating(categoryId, sort, order, offset, limit);
    }
    
    /**
     * 商品评分统计总数（用于分页）
     * @param categoryId 商品分类ID，可为null
     * @return 总记录数
     */
    public int countGoodsRating(Integer categoryId) {
        return statMapper.countGoodsRating(categoryId);
    }
    
    /**
     * 获取商品分类列表（用于筛选）
     * @return 商品分类列表
     */
    public List<Map> statGoodsCategories() {
        return statMapper.statGoodsCategories();
    }
    
    /**
     * 商品评论统计列表
     * @param categoryId 商品分类ID，可为null
     * @param page 页码
     * @param limit 每页条数
     * @return 商品评论统计数据
     */
    public List<Map> statGoodsComment(Integer categoryId, Integer page, Integer limit) {
        // 参数校验
        if (page == null || page < 1) {
            page = 1;
        }
        if (limit == null || limit < 1) {
            limit = 10;
        }
        if (limit > 100) {
            limit = 100;
        }
        
        // 计算分页参数
        int offset = (page - 1) * limit;
        
        return statMapper.statGoodsComment(categoryId, offset, limit);
    }
    
    /**
     * 商品评论统计总数（用于分页）
     * @param categoryId 商品分类ID，可为null
     * @return 总记录数
     */
    public int countGoodsComment(Integer categoryId) {
        return statMapper.countGoodsComment(categoryId);
    }
    
    /**
     * 获取商品评论内容（用于词云）
     * @param goodsId 商品ID
     * @return 评论内容列表
     */
    public List<Map> getGoodsComments(Integer goodsId) {
        return statMapper.getGoodsComments(goodsId);
    }
}
