package org.linlinjava.litemall.core.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 订单统计结果类
 * 
 * @author litemall
 */
public class StatisticsResult {
    /**
     * 时间标签（日、月、年等）
     */
    private String timeLabel;
    
    /**
     * 订单数量
     */
    private Long orderCount;
    
    /**
     * 订单金额
     */
    private BigDecimal orderAmount;
    
    /**
     * 平均订单金额
     */
    private BigDecimal avgAmount;
    
    /**
     * 客户数量（去重）
     */
    private Long customerCount;

    public StatisticsResult() {
    }

    public StatisticsResult(String timeLabel, Long orderCount, BigDecimal orderAmount) {
        this.timeLabel = timeLabel;
        this.orderCount = orderCount;
        this.orderAmount = orderAmount;
        if (orderCount != null && orderCount > 0 && orderAmount != null) {
            this.avgAmount = orderAmount.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);
        } else {
            this.avgAmount = BigDecimal.ZERO;
        }
    }

    public String getTimeLabel() {
        return timeLabel;
    }

    public void setTimeLabel(String timeLabel) {
        this.timeLabel = timeLabel;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }

    public BigDecimal getAvgAmount() {
        return avgAmount;
    }

    public void setAvgAmount(BigDecimal avgAmount) {
        this.avgAmount = avgAmount;
    }

    public Long getCustomerCount() {
        return customerCount;
    }

    public void setCustomerCount(Long customerCount) {
        this.customerCount = customerCount;
    }

    @Override
    public String toString() {
        return "StatisticsResult{" +
                "timeLabel='" + timeLabel + '\'' +
                ", orderCount=" + orderCount +
                ", orderAmount=" + orderAmount +
                ", avgAmount=" + avgAmount +
                ", customerCount=" + customerCount +
                '}';
    }
}