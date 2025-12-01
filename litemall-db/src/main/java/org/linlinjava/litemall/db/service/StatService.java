package org.linlinjava.litemall.db.service;

import org.linlinjava.litemall.db.dao.StatMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

import java.util.HashMap;
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
     * 支持按年、季、月、日进行级联筛选
     * @param timeDimension 时间维度：day/week/month/quarter/year
     * @param categoryId 商品类别ID，可为null
     * @param year 年份，可为null
     * @param quarter 季度(1-4)，可为null
     * @param month 月份(1-12)，可为null
     * @param day 日期(1-31)，可为null
     * @return 订单统计数据
     */
    public List<Map> statOrderEnhanced(String timeDimension, Integer categoryId, Integer year, Integer quarter, Integer month, String day) {
        // 构建时间范围参数
        Map<String, Object> timeParams = buildTimeRange(timeDimension, year, quarter, month, day);
        
        // 调用Mapper进行查询
        return statMapper.statOrderEnhancedWithTimeRange(timeDimension, categoryId, 
            (java.time.LocalDateTime) timeParams.get("startTime"), 
            (java.time.LocalDateTime) timeParams.get("endTime"));
    }
    
    /**
     * 构建时间范围参数
     * @param timeDimension 时间维度
     * @param year 年份
     * @param quarter 季度
     * @param month 月份
     * @param day 日期字符串
     * @return 包含开始时间和结束时间的Map
     */
    private Map<String, Object> buildTimeRange(String timeDimension, Integer year, Integer quarter, Integer month, String day) {
        Map<String, Object> result = new HashMap<>();
        java.time.LocalDateTime startTime;
        java.time.LocalDateTime endTime;
        
        java.time.LocalDate now = java.time.LocalDate.now();
        
        switch (timeDimension.toLowerCase()) {
            case "year":
                if (year != null) {
                    startTime = java.time.LocalDateTime.of(year, 1, 1, 0, 0, 0);
                    endTime = java.time.LocalDateTime.of(year, 12, 31, 23, 59, 59);
                } else {
                    // 默认当年
                    startTime = java.time.LocalDateTime.of(now.getYear(), 1, 1, 0, 0, 0);
                    endTime = java.time.LocalDateTime.now();
                }
                break;
                
            case "quarter":
                if (year != null && quarter != null) {
                    int startMonth = (quarter - 1) * 3 + 1;
                    int endMonth = startMonth + 2;
                    startTime = java.time.LocalDateTime.of(year, startMonth, 1, 0, 0, 0);
                    
                    // 计算季度的最后一天
                    java.time.YearMonth endYearMonth = java.time.YearMonth.of(year, endMonth);
                    int endDay = endYearMonth.lengthOfMonth();
                    endTime = java.time.LocalDateTime.of(year, endMonth, endDay, 23, 59, 59);
                } else {
                    // 默认当前季度
                    int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;
                    int startMonth = (currentQuarter - 1) * 3 + 1;
                    int endMonth = startMonth + 2;
                    startTime = java.time.LocalDateTime.of(now.getYear(), startMonth, 1, 0, 0, 0);
                    
                    java.time.YearMonth endYearMonth = java.time.YearMonth.of(now.getYear(), endMonth);
                    int endDay = endYearMonth.lengthOfMonth();
                    endTime = java.time.LocalDateTime.of(now.getYear(), endMonth, endDay, 23, 59, 59);
                }
                break;
                
            case "month":
                if (year != null && month != null) {
                    startTime = java.time.LocalDateTime.of(year, month, 1, 0, 0, 0);
                    
                    java.time.YearMonth yearMonth = java.time.YearMonth.of(year, month);
                    int endDay = yearMonth.lengthOfMonth();
                    endTime = java.time.LocalDateTime.of(year, month, endDay, 23, 59, 59);
                } else if (year != null) {
                    // 指定年份，默认当前月份
                    startTime = java.time.LocalDateTime.of(year, now.getMonthValue(), 1, 0, 0, 0);
                    
                    java.time.YearMonth yearMonth = java.time.YearMonth.of(year, now.getMonthValue());
                    int endDay = yearMonth.lengthOfMonth();
                    endTime = java.time.LocalDateTime.of(year, now.getMonthValue(), endDay, 23, 59, 59);
                } else if (month != null) {
                    // 指定月份，默认当前年份
                    startTime = java.time.LocalDateTime.of(now.getYear(), month, 1, 0, 0, 0);
                    
                    java.time.YearMonth yearMonth = java.time.YearMonth.of(now.getYear(), month);
                    int endDay = yearMonth.lengthOfMonth();
                    endTime = java.time.LocalDateTime.of(now.getYear(), month, endDay, 23, 59, 59);
                } else {
                    // 默认当前月份
                    startTime = java.time.LocalDateTime.of(now.getYear(), now.getMonthValue(), 1, 0, 0, 0);
                    endTime = java.time.LocalDateTime.now();
                }
                break;
                
            case "day":
                if (day != null && !day.isEmpty()) {
                    // 解析日期字符串 "yyyy-MM-dd"
                    java.time.LocalDate date = java.time.LocalDate.parse(day);
                    startTime = java.time.LocalDateTime.of(date, java.time.LocalTime.of(0, 0, 0));
                    endTime = java.time.LocalDateTime.of(date, java.time.LocalTime.of(23, 59, 59));
                } else if (year != null && month != null) {
                    // 指定年月，默认当月第一天到最后一天
                    startTime = java.time.LocalDateTime.of(year, month, 1, 0, 0, 0);
                    
                    java.time.YearMonth yearMonth = java.time.YearMonth.of(year, month);
                    int endDay = yearMonth.lengthOfMonth();
                    endTime = java.time.LocalDateTime.of(year, month, endDay, 23, 59, 59);
                } else if (year != null) {
                    // 指定年份，默认当年第一天到最后一天
                    startTime = java.time.LocalDateTime.of(year, 1, 1, 0, 0, 0);
                    endTime = java.time.LocalDateTime.of(year, 12, 31, 23, 59, 59);
                } else if (month != null) {
                    // 指定月份，默认当前年份，当月第一天到最后一天
                    startTime = java.time.LocalDateTime.of(now.getYear(), month, 1, 0, 0, 0);
                    
                    java.time.YearMonth yearMonth = java.time.YearMonth.of(now.getYear(), month);
                    int endDay = yearMonth.lengthOfMonth();
                    endTime = java.time.LocalDateTime.of(now.getYear(), month, endDay, 23, 59, 59);
                } else {
                    // 默认最近30天
                    startTime = java.time.LocalDateTime.now().minusDays(30);
                    endTime = java.time.LocalDateTime.now();
                }
                break;
                
            default:
                // 默认最近30天
                startTime = java.time.LocalDateTime.now().minusDays(30);
                endTime = java.time.LocalDateTime.now();
        }
        
        result.put("startTime", startTime);
        result.put("endTime", endTime);
        return result;
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
    
    /**
     * 获取商品评论内容（用于词云）- 支持批量商品ID
     * @param goodsIds 商品ID列表
     * @return 评论内容列表
     */
    public List<Map> getGoodsComments(List<Integer> goodsIds) {
        return statMapper.getGoodsComments(goodsIds);
    }
    
    /**
     * 获取分类下所有商品的评论内容（用于全局词云）
     * @param categoryId 商品分类ID，可为null
     * @return 评论内容列表
     */
    public List<Map> getCommentsByCategory(Integer categoryId) {
        if (categoryId != null && categoryId > 0) {
            return statMapper.getCommentsByCategory(categoryId);
        } else {
            return getAllComments();
        }
    }
    
    /**
     * 获取全站所有评论内容（用于全局词云）
     * @return 评论内容列表
     */
    public List<Map> getAllComments() {
        return statMapper.getAllComments();
    }
}
