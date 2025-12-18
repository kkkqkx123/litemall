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


    public List<Map<String, Object>> statUser() {
        return statMapper.statUser();
    }

    public List<Map<String, Object>> statOrder() {
        return statMapper.statOrder();
    }
    
    /**
     * 增强版订单统计查询，支持时间筛选和商品类别筛选
     * 根据提供的具体参数构建时间范围，优先级：day > month > quarter > year
     * @param groupBy 统计组织方式：year/quarter/month/day
     * @param categoryId 商品类别ID，可为null
     * @param year 年份，可为null
     * @param quarter 季度(1-4)，可为null
     * @param month 月份(1-12)，可为null
     * @param day 日期(yyyy-MM-dd格式)，可为null
     * @return 订单统计数据
     */
    public List<Map<String, Object>> statOrderEnhanced(String groupBy, Integer categoryId, Integer year, Integer quarter, Integer month, String day) {
        // 构建时间范围参数
        Map<String, Object> timeParams = buildTimeRange(year, quarter, month, day);
        
        // 调试日志输出
        System.out.println("Debug: statOrderEnhanced called with params:");
        System.out.println("  groupBy=" + groupBy + ", year=" + year + ", quarter=" + quarter + ", month=" + month + ", day=" + day);
        System.out.println("  startTime=" + timeParams.get("startTime"));
        System.out.println("  endTime=" + timeParams.get("endTime"));
        System.out.println("  categoryId=" + categoryId);
        
        // 调用Mapper进行查询
        List<Map<String, Object>> result = statMapper.statOrderEnhancedWithTimeRange(groupBy, categoryId, 
            (java.time.LocalDateTime) timeParams.get("startTime"), 
            (java.time.LocalDateTime) timeParams.get("endTime"));
        
        // 调试日志输出
        System.out.println("Debug: SQL query returned " + (result != null ? result.size() : "null") + " rows");
        
        return result;
    }
    
    /**
     * 根据具体参数构建时间范围参数
     * 优先级：day > month > quarter > year
     * @param year 年份
     * @param quarter 季度
     * @param month 月份
     * @param day 日期字符串
     * @return 包含开始时间和结束时间的Map
     */
    private Map<String, Object> buildTimeRange(Integer year, Integer quarter, Integer month, String day) {
        Map<String, Object> result = new HashMap<>();
        java.time.LocalDateTime startTime;
        java.time.LocalDateTime endTime;
        
        // 优先级判断：day > month > quarter > year
        if (day != null && !day.isEmpty()) {
            // 如果提供了具体日期，查询当天数据
            java.time.LocalDate date = java.time.LocalDate.parse(day);
            startTime = java.time.LocalDateTime.of(date, java.time.LocalTime.of(0, 0, 0));
            endTime = java.time.LocalDateTime.of(date, java.time.LocalTime.of(23, 59, 59));
        } else if (month != null) {
            // 如果提供了月份，使用指定月份（如果没有年份则使用当前年份）
            int targetYear = year != null ? year : java.time.LocalDate.now().getYear();
            startTime = java.time.LocalDateTime.of(targetYear, month, 1, 0, 0, 0);
            java.time.YearMonth yearMonth = java.time.YearMonth.of(targetYear, month);
            int endDay = yearMonth.lengthOfMonth();
            endTime = java.time.LocalDateTime.of(targetYear, month, endDay, 23, 59, 59);
        } else if (quarter != null && year != null) {
            // 如果提供了季度，查询整季度数据
            int startMonth = (quarter - 1) * 3 + 1;
            int endMonth = startMonth + 2;
            startTime = java.time.LocalDateTime.of(year, startMonth, 1, 0, 0, 0);
            java.time.YearMonth endYearMonth = java.time.YearMonth.of(year, endMonth);
            int endDay = endYearMonth.lengthOfMonth();
            endTime = java.time.LocalDateTime.of(year, endMonth, endDay, 23, 59, 59);
        } else if (year != null) {
            // 如果只提供了年份，查询整年数据
            startTime = java.time.LocalDateTime.of(year, 1, 1, 0, 0, 0);
            endTime = java.time.LocalDateTime.of(year, 12, 31, 23, 59, 59);
        } else {
            // 默认查询当前月份
            java.time.LocalDate now = java.time.LocalDate.now();
            startTime = java.time.LocalDateTime.of(now.getYear(), now.getMonthValue(), 1, 0, 0, 0);
            java.time.YearMonth yearMonth = java.time.YearMonth.of(now.getYear(), now.getMonthValue());
            int endDay = yearMonth.lengthOfMonth();
            endTime = java.time.LocalDateTime.of(now.getYear(), now.getMonthValue(), endDay, 23, 59, 59);
        }
        
        result.put("startTime", startTime);
        result.put("endTime", endTime);
        return result;
    }

    /**
     * 根据参数决定时间维度（保持SQL兼容性）
     * @param year 年份
     * @param quarter 季度
     * @param month 月份
     * @param day 日期
     * @return 时间维度字符串
     */
    private String determineTimeDimension(Integer year, Integer quarter, Integer month, String day) {
        if (day != null && !day.isEmpty()) {
            return "day";
        } else if (month != null && year != null) {
            return "month";
        } else if (quarter != null && year != null) {
            return "quarter";
        } else if (year != null) {
            return "year";
        } else {
            return "month"; // 默认按月
        }
    }

    public List<Map<String, Object>> statGoods() {
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
    public List<Map<String, Object>> statGoodsRating(Integer categoryId, String sort, String order, Integer page, Integer limit) {
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
    public List<Map<String, Object>> statGoodsCategories() {
        return statMapper.statGoodsCategories();
    }
    
    /**
     * 商品评论统计列表
     * @param categoryId 商品分类ID，可为null
     * @param page 页码
     * @param limit 每页条数
     * @return 商品评论统计数据
     */
    public List<Map<String, Object>> statGoodsComment(Integer categoryId, Integer page, Integer limit) {
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
    public List<Map<String, Object>> getGoodsComments(Integer goodsId) {
        return statMapper.getGoodsComments(goodsId);
    }
    
    /**
     * 获取商品评论内容（用于词云）- 支持批量商品ID
     * @param goodsIds 商品ID列表
     * @return 评论内容列表
     */
    public List<Map<String, Object>> getGoodsComments(List<Integer> goodsIds) {
        return statMapper.getGoodsComments(goodsIds);
    }
    
    /**
     * 获取分类下所有商品的评论内容（用于全局词云）
     * @param categoryId 商品分类ID，可为null
     * @return 评论内容列表
     */
    public List<Map<String, Object>> getCommentsByCategory(Integer categoryId) {
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
    public List<Map<String, Object>> getAllComments() {
        return statMapper.getAllComments();
    }
}
