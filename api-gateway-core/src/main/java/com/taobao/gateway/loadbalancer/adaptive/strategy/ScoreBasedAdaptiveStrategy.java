package com.taobao.gateway.loadbalancer.adaptive.strategy;

import com.taobao.gateway.loadbalancer.ServiceInstance;
import com.taobao.gateway.loadbalancer.adaptive.AdaptiveLoadBalanceStrategy;
import com.taobao.gateway.loadbalancer.adaptive.AdaptiveLoadBalanceStrategyType;
import com.taobao.gateway.loadbalancer.adaptive.InstanceMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 综合评分自适应策略
 * 基于多维度指标计算综合评分，选择评分最高的实例
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
public class ScoreBasedAdaptiveStrategy implements AdaptiveLoadBalanceStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(ScoreBasedAdaptiveStrategy.class);
    
    /**
     * 指标权重配置
     */
    private static final double RESPONSE_TIME_WEIGHT = 0.35;
    private static final double LOAD_WEIGHT = 0.25;
    private static final double ERROR_RATE_WEIGHT = 0.20;
    private static final double CONNECTION_WEIGHT = 0.10;
    private static final double QPS_WEIGHT = 0.10;
    
    /**
     * 阈值配置
     */
    private static final double MAX_RESPONSE_TIME = 2000.0;
    private static final double MAX_LOAD_SCORE = 100.0;
    private static final double MAX_ERROR_RATE = 1.0;
    private static final double MAX_CONNECTION_RATIO = 1.0;
    private static final double MAX_QPS = 1000.0;
    
    @Override
    public ServiceInstance select(String serviceName, List<ServiceInstance> instances, 
                                Map<String, InstanceMetrics> instanceMetrics) {
        return select(serviceName, instances, instanceMetrics, null);
    }
    
    @Override
    public ServiceInstance select(String serviceName, List<ServiceInstance> instances, 
                                Map<String, InstanceMetrics> instanceMetrics, String requestKey) {
        if (instances == null || instances.isEmpty()) {
            logger.warn("服务实例列表为空: {}", serviceName);
            return null;
        }
        
        // 计算每个实例的综合评分
        ServiceInstance bestInstance = null;
        double bestScore = -1;
        
        for (ServiceInstance instance : instances) {
            InstanceMetrics metrics = instanceMetrics.get(instance.getInstanceId());
            double score = calculateWeight(instance, metrics);
            
            if (score > bestScore) {
                bestScore = score;
                bestInstance = instance;
            }
            
            logger.debug("实例评分: service={}, instance={}, score={}", 
                    serviceName, instance.getInstanceId(), score);
        }
        
        // 如果没有找到合适的实例，随机选择一个
        if (bestInstance == null) {
            bestInstance = instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
            logger.warn("未找到合适的实例，随机选择: service={}, instance={}", 
                    serviceName, bestInstance.getInstanceId());
        }
        
        logger.debug("综合评分策略选择实例: service={}, instance={}, score={}", 
                serviceName, bestInstance.getInstanceId(), bestScore);
        
        return bestInstance;
    }
    
    @Override
    public AdaptiveLoadBalanceStrategyType getType() {
        return AdaptiveLoadBalanceStrategyType.SCORE_BASED;
    }
    
    @Override
    public double calculateWeight(ServiceInstance instance, InstanceMetrics metrics) {
        if (metrics == null) {
            // 如果没有指标数据，返回默认权重
            return instance.getWeight();
        }
        
        // 计算各维度评分
        double responseTimeScore = calculateResponseTimeScore(metrics);
        double loadScore = calculateLoadScore(metrics);
        double errorRateScore = calculateErrorRateScore(metrics);
        double connectionScore = calculateConnectionScore(metrics);
        double qpsScore = calculateQpsScore(metrics);
        
        // 计算综合评分
        double totalScore = responseTimeScore * RESPONSE_TIME_WEIGHT +
                           loadScore * LOAD_WEIGHT +
                           errorRateScore * ERROR_RATE_WEIGHT +
                           connectionScore * CONNECTION_WEIGHT +
                           qpsScore * QPS_WEIGHT;
        
        // 应用实例基础权重
        totalScore *= instance.getWeight();
        
        logger.debug("计算实例权重: instance={}, responseTimeScore={}, loadScore={}, " +
                "errorRateScore={}, connectionScore={}, qpsScore={}, totalScore={}", 
                instance.getInstanceId(), responseTimeScore, loadScore, 
                errorRateScore, connectionScore, qpsScore, totalScore);
        
        return totalScore;
    }
    
    /**
     * 计算响应时间评分
     */
    private double calculateResponseTimeScore(InstanceMetrics metrics) {
        double responseTime = metrics.getResponseTime();
        if (responseTime <= 0) {
            return 1.0; // 没有响应时间数据，给满分
        }
        
        // 响应时间越短，评分越高
        double score = Math.max(0.0, 1.0 - (responseTime / MAX_RESPONSE_TIME));
        return Math.pow(score, 0.5); // 使用平方根函数平滑评分
    }
    
    /**
     * 计算负载评分
     */
    private double calculateLoadScore(InstanceMetrics metrics) {
        double loadScore = metrics.calculateLoadScore();
        
        // 负载越低，评分越高
        double score = Math.max(0.0, 1.0 - (loadScore / MAX_LOAD_SCORE));
        return Math.pow(score, 0.8); // 使用幂函数调整评分曲线
    }
    
    /**
     * 计算错误率评分
     */
    private double calculateErrorRateScore(InstanceMetrics metrics) {
        double errorRate = metrics.getErrorRate();
        
        // 错误率越低，评分越高
        double score = Math.max(0.0, 1.0 - (errorRate / MAX_ERROR_RATE));
        return Math.pow(score, 2.0); // 使用平方函数，对错误率更敏感
    }
    
    /**
     * 计算连接数评分
     */
    private double calculateConnectionScore(InstanceMetrics metrics) {
        long activeConnections = metrics.getActiveConnections();
        long maxConnections = metrics.getMaxConnections();
        
        if (maxConnections <= 0) {
            return 1.0; // 没有连接数限制，给满分
        }
        
        double connectionRatio = (double) activeConnections / maxConnections;
        
        // 连接数比例越低，评分越高
        double score = Math.max(0.0, 1.0 - (connectionRatio / MAX_CONNECTION_RATIO));
        return Math.pow(score, 0.7);
    }
    
    /**
     * 计算QPS评分
     */
    private double calculateQpsScore(InstanceMetrics metrics) {
        double qps = metrics.getQps();
        
        if (qps <= 0) {
            return 0.5; // 没有QPS数据，给中等评分
        }
        
        // QPS适中时评分最高，过高或过低都会降低评分
        double optimalQps = MAX_QPS * 0.7; // 假设70%的QPS是最优的
        double deviation = Math.abs(qps - optimalQps) / optimalQps;
        double score = Math.max(0.0, 1.0 - deviation);
        
        return Math.pow(score, 1.5);
    }
}