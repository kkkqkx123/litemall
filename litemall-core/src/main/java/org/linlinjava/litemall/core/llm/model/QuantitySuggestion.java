package org.linlinjava.litemall.core.llm.model;

/**
 * 数量建议结果
 * 包含建议的商品数量、原因说明和是否覆盖启发式建议
 */
public class QuantitySuggestion {
    
    private int finalQuantity;
    private String reason;
    private boolean overrideHeuristic;
    
    public QuantitySuggestion(int finalQuantity, String reason, boolean overrideHeuristic) {
        this.finalQuantity = finalQuantity;
        this.reason = reason;
        this.overrideHeuristic = overrideHeuristic;
    }
    
    public int getFinalQuantity() {
        return finalQuantity;
    }
    
    public void setFinalQuantity(int finalQuantity) {
        this.finalQuantity = finalQuantity;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public boolean isOverrideHeuristic() {
        return overrideHeuristic;
    }
    
    public void setOverrideHeuristic(boolean overrideHeuristic) {
        this.overrideHeuristic = overrideHeuristic;
    }
}