package com.taobao.gateway.loadbalancer.adaptive.impl;

import com.taobao.gateway.loadbalancer.LoadBalancerType;
import com.taobao.gateway.loadbalancer.ServiceInstance;
import com.taobao.gateway.loadbalancer.adaptive.*;
import com.taobao.gateway.loadbalancer.adaptive.strategy.ScoreBasedAdaptiveStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 自适应负载均衡器实现
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
public class AdaptiveLoadBalancerImpl implements AdaptiveLoadBalancer {
    
    private static final Logger logger = LoggerFactory.getLogger(AdaptiveLoadBalancerImpl.class);
    
    /**
     * 负载均衡器名称
     */
    private final String name;
    
    /**
     * 自适应配置
     */
    private final AdaptiveLoadBalanceConfig config;
    
    /**
     * 自适应策略
     */
    private final AdaptiveLoadBalanceStrategy strategy;
    
    /**
     * 实例指标映射
     */
    private final ConcurrentHashMap<String, InstanceMetrics> instanceMetricsMap;
    
    /**
     * 负载均衡统计
     */
    private final AdaptiveLoadBalanceStats stats;
    
    /**
     * 定时任务
     */
    private ScheduledFuture<?> healthCheckTask;
    private ScheduledFuture<?> metricsUpdateTask;
    
    /**
     * 调度器
     */
    private final ScheduledExecutorService scheduler;
    
    /**
     * 构造函数
     */
    public AdaptiveLoadBalancerImpl(String name, AdaptiveLoadBalanceConfig config, 
                                   ScheduledExecutorService scheduler) {
        this.name = name;
        this.config = config;
        this.scheduler = scheduler;
        this.instanceMetricsMap = new ConcurrentHashMap<>();
        this.stats = new AdaptiveLoadBalanceStats();
        
        // 根据配置选择策略
        this.strategy = createStrategy(config.getStrategyType());
        
        // 启动定时任务
        if (config.isEnabled()) {
            startScheduledTasks();
        }
        
        logger.info("自适应负载均衡器初始化完成: {}, 策略: {}", name, config.getStrategyType());
    }
    
    @Override
    public ServiceInstance select(String serviceName, List<ServiceInstance> instances) {
        return select(serviceName, instances, null);
    }
    
    @Override
    public ServiceInstance select(String serviceName, List<ServiceInstance> instances, String requestKey) {
        if (instances == null || instances.isEmpty()) {
            logger.warn("服务实例列表为空: {}", serviceName);
            return null;
        }
        
        stats.incrementTotalRequests();
        
        // 健康检查，过滤掉不健康的实例
        List<ServiceInstance> healthyInstances = healthCheck(serviceName, instances);
        
        if (healthyInstances.isEmpty()) {
            logger.warn("没有健康的服务实例: {}", serviceName);
            stats.incrementFailedRequests();
            return null;
        }
        
        // 使用自适应策略选择实例
        ServiceInstance selected = strategy.select(serviceName, healthyInstances, 
                instanceMetricsMap, requestKey);
        
        if (selected != null) {
            // 更新统计信息
            AdaptiveLoadBalanceStats.InstanceStats instanceStats = 
                    stats.getInstanceStats(selected.getInstanceId());
            instanceStats.incrementRequestCount();
            instanceStats.setLastRequestTime(System.currentTimeMillis());
            
            logger.debug("自适应负载均衡选择实例: service={}, instance={}", 
                    serviceName, selected.getInstanceId());
        } else {
            stats.incrementFailedRequests();
            logger.warn("自适应负载均衡选择失败: service={}", serviceName);
        }
        
        return selected;
    }
    
    @Override
    public LoadBalancerType getType() {
        return LoadBalancerType.ADAPTIVE;
    }
    
    @Override
    public AdaptiveLoadBalanceStrategy getStrategy() {
        return strategy;
    }
    
    @Override
    public void updateInstanceMetrics(String serviceName, String instanceId, InstanceMetrics metrics) {
        if (metrics != null) {
            instanceMetricsMap.put(instanceId, metrics);
            logger.debug("更新实例指标: service={}, instance={}, metrics={}", 
                    serviceName, instanceId, metrics);
        }
    }
    
    @Override
    public AdaptiveLoadBalanceConfig getAdaptiveConfig() {
        return config;
    }
    
    @Override
    public AdaptiveLoadBalanceStats getStats() {
        return stats;
    }
    
    @Override
    public List<ServiceInstance> healthCheck(String serviceName, List<ServiceInstance> instances) {
        stats.incrementHealthCheckCount();
        
        return instances.stream()
                .filter(instance -> {
                    InstanceMetrics metrics = instanceMetricsMap.get(instance.getInstanceId());
                    if (metrics == null) {
                        // 如果没有指标数据，认为实例健康
                        return true;
                    }
                    
                    boolean healthy = metrics.isHealthy();
                    if (!healthy) {
                        logger.warn("实例不健康: service={}, instance={}, metrics={}", 
                                serviceName, instance.getInstanceId(), metrics);
                    }
                    
                    return healthy;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 启动定时任务
     */
    private void startScheduledTasks() {
        // 启动健康检查任务
        healthCheckTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                performHealthCheck();
            } catch (Exception e) {
                logger.error("健康检查任务执行异常", e);
            }
        }, config.getHealthCheckInterval(), config.getHealthCheckInterval(), TimeUnit.MILLISECONDS);
        
        // 启动指标更新任务
        metricsUpdateTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                performMetricsUpdate();
            } catch (Exception e) {
                logger.error("指标更新任务执行异常", e);
            }
        }, config.getMetricsUpdateInterval(), config.getMetricsUpdateInterval(), TimeUnit.MILLISECONDS);
        
        logger.info("启动定时任务 - 健康检查间隔: {}ms, 指标更新间隔: {}ms", 
                config.getHealthCheckInterval(), config.getMetricsUpdateInterval());
    }
    
    /**
     * 执行健康检查
     */
    private void performHealthCheck() {
        // 清理过期的指标数据
        long currentTime = System.currentTimeMillis();
        long expireTime = currentTime - 300000; // 5分钟过期
        
        instanceMetricsMap.entrySet().removeIf(entry -> {
            InstanceMetrics metrics = entry.getValue();
            boolean expired = metrics.getLastUpdateTime() < expireTime;
            if (expired) {
                logger.debug("清理过期指标数据: instance={}", entry.getKey());
            }
            return expired;
        });
        
        logger.debug("健康检查完成，当前实例数: {}", instanceMetricsMap.size());
    }
    
    /**
     * 执行指标更新
     */
    private void performMetricsUpdate() {
        // 这里可以添加指标数据的聚合和计算逻辑
        // 例如：计算平均响应时间、错误率等
        
        logger.debug("指标更新完成，当前实例数: {}", instanceMetricsMap.size());
    }
    
    /**
     * 创建自适应策略
     */
    private AdaptiveLoadBalanceStrategy createStrategy(AdaptiveLoadBalanceStrategyType type) {
        switch (type) {
            case SCORE_BASED:
                return new ScoreBasedAdaptiveStrategy();
            case RESPONSE_TIME_BASED:
                return new ResponseTimeBasedAdaptiveStrategy();
            case LOAD_BASED:
                return new LoadBasedAdaptiveStrategy();
            case ERROR_RATE_BASED:
                return new ErrorRateBasedAdaptiveStrategy();
            case HYBRID:
                return new HybridAdaptiveStrategy();
            default:
                logger.warn("未知的自适应策略类型: {}, 使用综合评分策略", type);
                return new ScoreBasedAdaptiveStrategy();
        }
    }
    
    /**
     * 关闭负载均衡器
     */
    public void shutdown() {
        if (healthCheckTask != null && !healthCheckTask.isCancelled()) {
            healthCheckTask.cancel(false);
        }
        if (metricsUpdateTask != null && !metricsUpdateTask.isCancelled()) {
            metricsUpdateTask.cancel(false);
        }
        logger.info("自适应负载均衡器已关闭: {}", name);
    }
    
    // 其他策略的简单实现
    private static class ResponseTimeBasedAdaptiveStrategy implements AdaptiveLoadBalanceStrategy {
        @Override
        public ServiceInstance select(String serviceName, List<ServiceInstance> instances, 
                                    Map<String, InstanceMetrics> instanceMetrics) {
            return select(serviceName, instances, instanceMetrics, null);
        }
        
        @Override
        public ServiceInstance select(String serviceName, List<ServiceInstance> instances, 
                                    Map<String, InstanceMetrics> instanceMetrics, String requestKey) {
            return instances.stream()
                    .min((i1, i2) -> {
                        InstanceMetrics m1 = instanceMetrics.get(i1.getInstanceId());
                        InstanceMetrics m2 = instanceMetrics.get(i2.getInstanceId());
                        
                        double rt1 = m1 != null ? m1.getResponseTime() : Double.MAX_VALUE;
                        double rt2 = m2 != null ? m2.getResponseTime() : Double.MAX_VALUE;
                        
                        return Double.compare(rt1, rt2);
                    })
                    .orElse(instances.get(0));
        }
        
        @Override
        public AdaptiveLoadBalanceStrategyType getType() {
            return AdaptiveLoadBalanceStrategyType.RESPONSE_TIME_BASED;
        }
        
        @Override
        public double calculateWeight(ServiceInstance instance, InstanceMetrics metrics) {
            if (metrics == null) return 1.0;
            double responseTime = metrics.getResponseTime();
            return responseTime > 0 ? 1000.0 / responseTime : 1.0;
        }
    }
    
    private static class LoadBasedAdaptiveStrategy implements AdaptiveLoadBalanceStrategy {
        @Override
        public ServiceInstance select(String serviceName, List<ServiceInstance> instances, 
                                    Map<String, InstanceMetrics> instanceMetrics) {
            return select(serviceName, instances, instanceMetrics, null);
        }
        
        @Override
        public ServiceInstance select(String serviceName, List<ServiceInstance> instances, 
                                    Map<String, InstanceMetrics> instanceMetrics, String requestKey) {
            return instances.stream()
                    .min((i1, i2) -> {
                        InstanceMetrics m1 = instanceMetrics.get(i1.getInstanceId());
                        InstanceMetrics m2 = instanceMetrics.get(i2.getInstanceId());
                        
                        double load1 = m1 != null ? m1.calculateLoadScore() : Double.MAX_VALUE;
                        double load2 = m2 != null ? m2.calculateLoadScore() : Double.MAX_VALUE;
                        
                        return Double.compare(load1, load2);
                    })
                    .orElse(instances.get(0));
        }
        
        @Override
        public AdaptiveLoadBalanceStrategyType getType() {
            return AdaptiveLoadBalanceStrategyType.LOAD_BASED;
        }
        
        @Override
        public double calculateWeight(ServiceInstance instance, InstanceMetrics metrics) {
            if (metrics == null) return 1.0;
            double loadScore = metrics.calculateLoadScore();
            return Math.max(0.1, 100.0 - loadScore);
        }
    }
    
    private static class ErrorRateBasedAdaptiveStrategy implements AdaptiveLoadBalanceStrategy {
        @Override
        public ServiceInstance select(String serviceName, List<ServiceInstance> instances, 
                                    Map<String, InstanceMetrics> instanceMetrics) {
            return select(serviceName, instances, instanceMetrics, null);
        }
        
        @Override
        public ServiceInstance select(String serviceName, List<ServiceInstance> instances, 
                                    Map<String, InstanceMetrics> instanceMetrics, String requestKey) {
            return instances.stream()
                    .min((i1, i2) -> {
                        InstanceMetrics m1 = instanceMetrics.get(i1.getInstanceId());
                        InstanceMetrics m2 = instanceMetrics.get(i2.getInstanceId());
                        
                        double er1 = m1 != null ? m1.getErrorRate() : Double.MAX_VALUE;
                        double er2 = m2 != null ? m2.getErrorRate() : Double.MAX_VALUE;
                        
                        return Double.compare(er1, er2);
                    })
                    .orElse(instances.get(0));
        }
        
        @Override
        public AdaptiveLoadBalanceStrategyType getType() {
            return AdaptiveLoadBalanceStrategyType.ERROR_RATE_BASED;
        }
        
        @Override
        public double calculateWeight(ServiceInstance instance, InstanceMetrics metrics) {
            if (metrics == null) return 1.0;
            double errorRate = metrics.getErrorRate();
            return Math.max(0.1, 1.0 - errorRate);
        }
    }
    
    private static class HybridAdaptiveStrategy implements AdaptiveLoadBalanceStrategy {
        @Override
        public ServiceInstance select(String serviceName, List<ServiceInstance> instances, 
                                    Map<String, InstanceMetrics> instanceMetrics) {
            return select(serviceName, instances, instanceMetrics, null);
        }
        
        @Override
        public ServiceInstance select(String serviceName, List<ServiceInstance> instances, 
                                    Map<String, InstanceMetrics> instanceMetrics, String requestKey) {
            return instances.stream()
                    .max((i1, i2) -> {
                        double score1 = calculateWeight(i1, instanceMetrics.get(i1.getInstanceId()));
                        double score2 = calculateWeight(i2, instanceMetrics.get(i2.getInstanceId()));
                        return Double.compare(score1, score2);
                    })
                    .orElse(instances.get(0));
        }
        
        @Override
        public AdaptiveLoadBalanceStrategyType getType() {
            return AdaptiveLoadBalanceStrategyType.HYBRID;
        }
        
        @Override
        public double calculateWeight(ServiceInstance instance, InstanceMetrics metrics) {
            if (metrics == null) return 1.0;
            
            // 综合评分：响应时间、负载、错误率
            double responseTimeScore = 1000.0 / Math.max(metrics.getResponseTime(), 1.0);
            double loadScore = Math.max(0.1, 100.0 - metrics.calculateLoadScore());
            double errorScore = Math.max(0.1, 1.0 - metrics.getErrorRate());
            
            return responseTimeScore * 0.4 + loadScore * 0.4 + errorScore * 0.2;
        }
    }
}