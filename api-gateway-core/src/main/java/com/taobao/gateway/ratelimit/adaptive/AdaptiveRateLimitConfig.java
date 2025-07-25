package com.taobao.gateway.ratelimit.adaptive;

import com.taobao.gateway.ratelimit.RateLimitConfig;

/**
 * 自适应限流配置类
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
public class AdaptiveRateLimitConfig extends RateLimitConfig {
    
    /**
     * 自适应策略类型
     */
    private AdaptiveStrategyType strategyType = AdaptiveStrategyType.COMPREHENSIVE;
    
    /**
     * 基础限流阈值
     */
    private int baseLimit = 1000;
    
    /**
     * 最小限流阈值
     */
    private int minLimit = 100;
    
    /**
     * 最大限流阈值
     */
    private int maxLimit = 10000;
    
    /**
     * 调整步长
     */
    private double adjustmentStep = 0.1;
    
    /**
     * 调整间隔（毫秒）
     */
    private long adjustmentInterval = 5000;
    
    /**
     * CPU使用率阈值
     */
    private double cpuThreshold = 80.0;
    
    /**
     * 内存使用率阈值
     */
    private double memoryThreshold = 80.0;
    
    /**
     * 响应时间阈值（毫秒）
     */
    private double responseTimeThreshold = 1000.0;
    
    /**
     * 错误率阈值
     */
    private double errorRateThreshold = 0.1;
    
    /**
     * 负载分数阈值
     */
    private double loadScoreThreshold = 80.0;
    
    /**
     * 是否启用自适应限流
     */
    private boolean enabled = true;
    
    /**
     * 是否启用平滑调整
     */
    private boolean smoothAdjustment = true;
    
    /**
     * 平滑因子 (0-1)
     */
    private double smoothFactor = 0.3;
    
    /**
     * 历史数据窗口大小
     */
    private int historyWindowSize = 10;
    
    // Getter和Setter方法
    public AdaptiveStrategyType getStrategyType() { return strategyType; }
    public void setStrategyType(AdaptiveStrategyType strategyType) { this.strategyType = strategyType; }
    
    public int getBaseLimit() { return baseLimit; }
    public void setBaseLimit(int baseLimit) { this.baseLimit = baseLimit; }
    
    public int getMinLimit() { return minLimit; }
    public void setMinLimit(int minLimit) { this.minLimit = minLimit; }
    
    public int getMaxLimit() { return maxLimit; }
    public void setMaxLimit(int maxLimit) { this.maxLimit = maxLimit; }
    
    public double getAdjustmentStep() { return adjustmentStep; }
    public void setAdjustmentStep(double adjustmentStep) { this.adjustmentStep = adjustmentStep; }
    
    public long getAdjustmentInterval() { return adjustmentInterval; }
    public void setAdjustmentInterval(long adjustmentInterval) { this.adjustmentInterval = adjustmentInterval; }
    
    public double getCpuThreshold() { return cpuThreshold; }
    public void setCpuThreshold(double cpuThreshold) { this.cpuThreshold = cpuThreshold; }
    
    public double getMemoryThreshold() { return memoryThreshold; }
    public void setMemoryThreshold(double memoryThreshold) { this.memoryThreshold = memoryThreshold; }
    
    public double getResponseTimeThreshold() { return responseTimeThreshold; }
    public void setResponseTimeThreshold(double responseTimeThreshold) { this.responseTimeThreshold = responseTimeThreshold; }
    
    public double getErrorRateThreshold() { return errorRateThreshold; }
    public void setErrorRateThreshold(double errorRateThreshold) { this.errorRateThreshold = errorRateThreshold; }
    
    public double getLoadScoreThreshold() { return loadScoreThreshold; }
    public void setLoadScoreThreshold(double loadScoreThreshold) { this.loadScoreThreshold = loadScoreThreshold; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public boolean isSmoothAdjustment() { return smoothAdjustment; }
    public void setSmoothAdjustment(boolean smoothAdjustment) { this.smoothAdjustment = smoothAdjustment; }
    
    public double getSmoothFactor() { return smoothFactor; }
    public void setSmoothFactor(double smoothFactor) { this.smoothFactor = smoothFactor; }
    
    public int getHistoryWindowSize() { return historyWindowSize; }
    public void setHistoryWindowSize(int historyWindowSize) { this.historyWindowSize = historyWindowSize; }
    
    @Override
    public String toString() {
        return "AdaptiveRateLimitConfig{" +
                "strategyType=" + strategyType +
                ", baseLimit=" + baseLimit +
                ", minLimit=" + minLimit +
                ", maxLimit=" + maxLimit +
                ", adjustmentStep=" + adjustmentStep +
                ", adjustmentInterval=" + adjustmentInterval +
                ", cpuThreshold=" + cpuThreshold +
                ", memoryThreshold=" + memoryThreshold +
                ", responseTimeThreshold=" + responseTimeThreshold +
                ", errorRateThreshold=" + errorRateThreshold +
                ", loadScoreThreshold=" + loadScoreThreshold +
                ", enabled=" + enabled +
                ", smoothAdjustment=" + smoothAdjustment +
                ", smoothFactor=" + smoothFactor +
                ", historyWindowSize=" + historyWindowSize +
                '}';
    }
} 