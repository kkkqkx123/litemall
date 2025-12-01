package org.linlinjava.litemall.core.domain;

import java.time.LocalDateTime;

/**
 * 订单统计参数类
 * 
 * @author litemall
 */
public class StatisticsParam {
    
    /**
     * 时间维度：day, month, quarter, year
     */
    private String timeDimension;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 商品类别ID
     */
    private Integer categoryId;
    
    /**
     * 年份
     */
    private Integer year;
    
    /**
     * 季度（1-4）
     */
    private Integer quarter;
    
    /**
     * 月份（1-12）
     */
    private Integer month;
    
    /**
     * 日期（1-31）
     */
    private Integer day;

    public StatisticsParam() {
    }

    public String getTimeDimension() {
        return timeDimension;
    }

    public void setTimeDimension(String timeDimension) {
        this.timeDimension = timeDimension;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getQuarter() {
        return quarter;
    }

    public void setQuarter(Integer quarter) {
        this.quarter = quarter;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getDay() {
        return day;
    }

    public void setDay(Integer day) {
        this.day = day;
    }

    @Override
    public String toString() {
        return "StatisticsParam{" +
                "timeDimension='" + timeDimension + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", categoryId=" + categoryId +
                ", year=" + year +
                ", quarter=" + quarter +
                ", month=" + month +
                ", day=" + day +
                '}';
    }
}