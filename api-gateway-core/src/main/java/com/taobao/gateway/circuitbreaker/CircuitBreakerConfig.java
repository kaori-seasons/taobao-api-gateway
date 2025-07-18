package com.taobao.gateway.circuitbreaker;

/**
 * 熔断器配置类
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class CircuitBreakerConfig {
    
    /**
     * 熔断器名称
     */
    private String name;
    
    /**
     * 失败阈值（失败次数达到此值时开启熔断）
     */
    private int failureThreshold = 5;
    
    /**
     * 失败率阈值（失败率达到此值时开启熔断，0.0-1.0）
     */
    private double failureRateThreshold = 0.5;
    
    /**
     * 熔断时间窗口（毫秒）
     */
    private long timeoutWindow = 60000;
    
    /**
     * 恢复时间（毫秒）
     */
    private long recoveryTime = 30000;
    
    /**
     * 半开状态下的成功阈值
     */
    private int successThreshold = 3;
    
    /**
     * 是否启用
     */
    private boolean enabled = true;
    
    public CircuitBreakerConfig() {
    }
    
    public CircuitBreakerConfig(String name) {
        this.name = name;
    }
    
    public CircuitBreakerConfig(String name, int failureThreshold, double failureRateThreshold) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.failureRateThreshold = failureRateThreshold;
    }
    
    // Getter和Setter方法
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getFailureThreshold() {
        return failureThreshold;
    }
    
    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }
    
    public double getFailureRateThreshold() {
        return failureRateThreshold;
    }
    
    public void setFailureRateThreshold(double failureRateThreshold) {
        this.failureRateThreshold = failureRateThreshold;
    }
    
    public long getTimeoutWindow() {
        return timeoutWindow;
    }
    
    public void setTimeoutWindow(long timeoutWindow) {
        this.timeoutWindow = timeoutWindow;
    }
    
    public long getRecoveryTime() {
        return recoveryTime;
    }
    
    public void setRecoveryTime(long recoveryTime) {
        this.recoveryTime = recoveryTime;
    }
    
    public int getSuccessThreshold() {
        return successThreshold;
    }
    
    public void setSuccessThreshold(int successThreshold) {
        this.successThreshold = successThreshold;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public String toString() {
        return "CircuitBreakerConfig{" +
                "name='" + name + '\'' +
                ", failureThreshold=" + failureThreshold +
                ", failureRateThreshold=" + failureRateThreshold +
                ", timeoutWindow=" + timeoutWindow +
                ", recoveryTime=" + recoveryTime +
                ", successThreshold=" + successThreshold +
                ", enabled=" + enabled +
                '}';
    }
} 