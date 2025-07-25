package com.taobao.gateway.ratelimit.adaptive.strategy;

import com.taobao.gateway.ratelimit.adaptive.AdaptiveStrategy;
import com.taobao.gateway.ratelimit.adaptive.AdaptiveStrategyType;
import com.taobao.gateway.ratelimit.adaptive.SystemMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 综合自适应策略实现
 * 基于多指标融合的自适应限流算法
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
public class ComprehensiveAdaptiveStrategy implements AdaptiveStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(ComprehensiveAdaptiveStrategy.class);
    
    /**
     * 指标权重配置
     */
    private static final double CPU_WEIGHT = 0.25;
    private static final double MEMORY_WEIGHT = 0.20;
    private static final double RESPONSE_TIME_WEIGHT = 0.25;
    private static final double ERROR_RATE_WEIGHT = 0.20;
    private static final double LOAD_SCORE_WEIGHT = 0.10;
    
    /**
     * 阈值配置
     */
    private static final double CPU_THRESHOLD = 80.0;
    private static final double MEMORY_THRESHOLD = 80.0;
    private static final double RESPONSE_TIME_THRESHOLD = 1000.0;
    private static final double ERROR_RATE_THRESHOLD = 0.1;
    private static final double LOAD_SCORE_THRESHOLD = 80.0;
    
    /**
     * 调整系数
     */
    private static final double AGGRESSIVE_ADJUSTMENT = 0.3;
    private static final double MODERATE_ADJUSTMENT = 0.15;
    private static final double CONSERVATIVE_ADJUSTMENT = 0.05;
    
    @Override
    public int calculateLimit(SystemMetrics metrics, int currentLimit) {
        if (metrics == null) {
            logger.warn("系统指标为空，保持当前限流阈值: {}", currentLimit);
            return currentLimit;
        }
        
        // 计算综合压力分数
        double pressureScore = calculatePressureScore(metrics);
        
        // 根据压力分数确定调整策略
        double adjustmentFactor = determineAdjustmentFactor(pressureScore);
        
        // 计算新的限流阈值
        int newLimit = calculateNewLimit(currentLimit, adjustmentFactor, pressureScore);
        
        logger.debug("综合自适应策略计算 - 压力分数: {}, 调整系数: {}, 当前阈值: {}, 新阈值: {}", 
                pressureScore, adjustmentFactor, currentLimit, newLimit);
        
        return newLimit;
    }
    
    @Override
    public boolean shouldAdjust(SystemMetrics metrics, int currentLimit) {
        if (metrics == null) {
            return false;
        }
        
        // 计算压力分数
        double pressureScore = calculatePressureScore(metrics);
        
        // 如果压力分数超过阈值，需要调整
        boolean shouldAdjust = pressureScore > 0.6 || metrics.isOverloaded();
        
        logger.debug("综合自适应策略判断 - 压力分数: {}, 是否过载: {}, 需要调整: {}", 
                pressureScore, metrics.isOverloaded(), shouldAdjust);
        
        return shouldAdjust;
    }
    
    @Override
    public AdaptiveStrategyType getType() {
        return AdaptiveStrategyType.COMPREHENSIVE;
    }
    
    /**
     * 计算综合压力分数 (0-1)
     */
    private double calculatePressureScore(SystemMetrics metrics) {
        double cpuScore = Math.min(metrics.getCpuUsage() / CPU_THRESHOLD, 1.0);
        double memoryScore = Math.min(metrics.getMemoryUsage() / MEMORY_THRESHOLD, 1.0);
        double responseTimeScore = Math.min(metrics.getAvgResponseTime() / RESPONSE_TIME_THRESHOLD, 1.0);
        double errorRateScore = Math.min(metrics.getErrorRate() / ERROR_RATE_THRESHOLD, 1.0);
        double loadScore = Math.min(metrics.calculateLoadScore() / LOAD_SCORE_THRESHOLD, 1.0);
        
        return cpuScore * CPU_WEIGHT +
               memoryScore * MEMORY_WEIGHT +
               responseTimeScore * RESPONSE_TIME_WEIGHT +
               errorRateScore * ERROR_RATE_WEIGHT +
               loadScore * LOAD_SCORE_WEIGHT;
    }
    
    /**
     * 确定调整系数
     */
    private double determineAdjustmentFactor(double pressureScore) {
        if (pressureScore > 0.8) {
            // 高压力：激进调整
            return AGGRESSIVE_ADJUSTMENT;
        } else if (pressureScore > 0.6) {
            // 中等压力：适度调整
            return MODERATE_ADJUSTMENT;
        } else if (pressureScore > 0.4) {
            // 低压力：保守调整
            return CONSERVATIVE_ADJUSTMENT;
        } else {
            // 正常压力：微调
            return 0.02;
        }
    }
    
    /**
     * 计算新的限流阈值
     */
    private int calculateNewLimit(int currentLimit, double adjustmentFactor, double pressureScore) {
        if (pressureScore > 0.7) {
            // 高压力：降低限流阈值
            int reduction = (int) (currentLimit * adjustmentFactor);
            return Math.max(currentLimit - reduction, 100); // 最小100
        } else if (pressureScore < 0.3) {
            // 低压力：提高限流阈值
            int increase = (int) (currentLimit * adjustmentFactor);
            return Math.min(currentLimit + increase, 10000); // 最大10000
        } else {
            // 中等压力：保持稳定
            return currentLimit;
        }
    }
} 