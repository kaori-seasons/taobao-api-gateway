package com.taobao.gateway.loadbalancer.adaptive;

/**
 * 自适应负载均衡配置类
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
public class AdaptiveLoadBalanceConfig {
    
    /**
     * 自适应策略类型
     */
    private AdaptiveLoadBalanceStrategyType strategyType = AdaptiveLoadBalanceStrategyType.SCORE_BASED;
    
    /**
     * 是否启用自适应负载均衡
     */
    private boolean enabled = true;
    
    /**
     * 健康检查间隔（毫秒）
     */
    private long healthCheckInterval = 30000;
    
    /**
     * 指标更新间隔（毫秒）
     */
    private long metricsUpdateInterval = 5000;
    
    /**
     * 响应时间阈值（毫秒）
     */
    private double responseTimeThreshold = 1000.0;
    
    /**
     * 错误率阈值
     */
    private double errorRateThreshold = 0.1;
    
    /**
     * 负载阈值
     */
    private double loadThreshold = 80.0;
    
    /**
     * 权重调整因子
     */
    private double weightAdjustmentFactor = 0.1;
    
    /**
     * 最小权重
     */
    private double minWeight = 0.1;
    
    /**
     * 最大权重
     */
    private double maxWeight = 10.0;
    
    /**
     * 是否启用平滑调整
     */
    private boolean smoothAdjustment = true;
    
    /**
     * 平滑因子 (0-1)
     */
    private double smoothFactor = 0.3;
    
    /**
     * 是否启用故障转移
     */
    private boolean failoverEnabled = true;
    
    /**
     * 故障转移阈值
     */
    private int failoverThreshold = 3;
    
    /**
     * 是否启用预热
     */
    private boolean warmupEnabled = true;
    
    /**
     * 预热时间（毫秒）
     */
    private long warmupTime = 60000;
    
    // Getter和Setter方法
    public AdaptiveLoadBalanceStrategyType getStrategyType() { return strategyType; }
    public void setStrategyType(AdaptiveLoadBalanceStrategyType strategyType) { this.strategyType = strategyType; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public long getHealthCheckInterval() { return healthCheckInterval; }
    public void setHealthCheckInterval(long healthCheckInterval) { this.healthCheckInterval = healthCheckInterval; }
    
    public long getMetricsUpdateInterval() { return metricsUpdateInterval; }
    public void setMetricsUpdateInterval(long metricsUpdateInterval) { this.metricsUpdateInterval = metricsUpdateInterval; }
    
    public double getResponseTimeThreshold() { return responseTimeThreshold; }
    public void setResponseTimeThreshold(double responseTimeThreshold) { this.responseTimeThreshold = responseTimeThreshold; }
    
    public double getErrorRateThreshold() { return errorRateThreshold; }
    public void setErrorRateThreshold(double errorRateThreshold) { this.errorRateThreshold = errorRateThreshold; }
    
    public double getLoadThreshold() { return loadThreshold; }
    public void setLoadThreshold(double loadThreshold) { this.loadThreshold = loadThreshold; }
    
    public double getWeightAdjustmentFactor() { return weightAdjustmentFactor; }
    public void setWeightAdjustmentFactor(double weightAdjustmentFactor) { this.weightAdjustmentFactor = weightAdjustmentFactor; }
    
    public double getMinWeight() { return minWeight; }
    public void setMinWeight(double minWeight) { this.minWeight = minWeight; }
    
    public double getMaxWeight() { return maxWeight; }
    public void setMaxWeight(double maxWeight) { this.maxWeight = maxWeight; }
    
    public boolean isSmoothAdjustment() { return smoothAdjustment; }
    public void setSmoothAdjustment(boolean smoothAdjustment) { this.smoothAdjustment = smoothAdjustment; }
    
    public double getSmoothFactor() { return smoothFactor; }
    public void setSmoothFactor(double smoothFactor) { this.smoothFactor = smoothFactor; }
    
    public boolean isFailoverEnabled() { return failoverEnabled; }
    public void setFailoverEnabled(boolean failoverEnabled) { this.failoverEnabled = failoverEnabled; }
    
    public int getFailoverThreshold() { return failoverThreshold; }
    public void setFailoverThreshold(int failoverThreshold) { this.failoverThreshold = failoverThreshold; }
    
    public boolean isWarmupEnabled() { return warmupEnabled; }
    public void setWarmupEnabled(boolean warmupEnabled) { this.warmupEnabled = warmupEnabled; }
    
    public long getWarmupTime() { return warmupTime; }
    public void setWarmupTime(long warmupTime) { this.warmupTime = warmupTime; }
    
    @Override
    public String toString() {
        return "AdaptiveLoadBalanceConfig{" +
                "strategyType=" + strategyType +
                ", enabled=" + enabled +
                ", healthCheckInterval=" + healthCheckInterval +
                ", metricsUpdateInterval=" + metricsUpdateInterval +
                ", responseTimeThreshold=" + responseTimeThreshold +
                ", errorRateThreshold=" + errorRateThreshold +
                ", loadThreshold=" + loadThreshold +
                ", weightAdjustmentFactor=" + weightAdjustmentFactor +
                ", minWeight=" + minWeight +
                ", maxWeight=" + maxWeight +
                ", smoothAdjustment=" + smoothAdjustment +
                ", smoothFactor=" + smoothFactor +
                ", failoverEnabled=" + failoverEnabled +
                ", failoverThreshold=" + failoverThreshold +
                ", warmupEnabled=" + warmupEnabled +
                ", warmupTime=" + warmupTime +
                '}';
    }
}