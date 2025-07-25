package com.taobao.gateway.adaptive.example;

import com.taobao.gateway.loadbalancer.ServiceInstance;
import com.taobao.gateway.loadbalancer.adaptive.AdaptiveLoadBalancer;
import com.taobao.gateway.loadbalancer.adaptive.InstanceMetrics;
import com.taobao.gateway.ratelimit.adaptive.AdaptiveRateLimiter;
import com.taobao.gateway.ratelimit.adaptive.SystemMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 自适应限流和负载均衡使用示例
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
@Component
public class AdaptiveExample {
    
    private static final Logger logger = LoggerFactory.getLogger(AdaptiveExample.class);
    
    @Autowired
    private AdaptiveRateLimiter adaptiveRateLimiter;
    
    @Autowired
    private AdaptiveLoadBalancer adaptiveLoadBalancer;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    /**
     * 演示自适应限流功能
     */
    public void demonstrateAdaptiveRateLimit() {
        logger.info("开始演示自适应限流功能");
        
        // 启动系统指标模拟器
        startSystemMetricsSimulator();
        
        // 模拟请求
        for (int i = 0; i < 100; i++) {
            String requestKey = "user-" + (i % 10);
            boolean allowed = adaptiveRateLimiter.tryAcquire(requestKey);
            
            if (allowed) {
                logger.debug("请求通过: {}", requestKey);
            } else {
                logger.debug("请求被限流: {}", requestKey);
            }
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // 打印统计信息
        logger.info("自适应限流统计: {}", adaptiveRateLimiter.getStats());
    }
    
    /**
     * 演示自适应负载均衡功能
     */
    public void demonstrateAdaptiveLoadBalance() {
        logger.info("开始演示自适应负载均衡功能");
        
        // 创建模拟的服务实例
        List<ServiceInstance> instances = createMockServiceInstances();
        
        // 启动实例指标模拟器
        startInstanceMetricsSimulator(instances);
        
        // 模拟负载均衡选择
        for (int i = 0; i < 50; i++) {
            String requestKey = "request-" + i;
            ServiceInstance selected = adaptiveLoadBalancer.select("user-service", instances, requestKey);
            
            if (selected != null) {
                logger.debug("负载均衡选择实例: {}", selected.getInstanceId());
            } else {
                logger.warn("负载均衡选择失败");
            }
            
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // 打印统计信息
        logger.info("自适应负载均衡统计: {}", adaptiveLoadBalancer.getStats());
    }
    
    /**
     * 启动系统指标模拟器
     */
    private void startSystemMetricsSimulator() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                SystemMetrics metrics = new SystemMetrics();
                
                // 模拟CPU使用率变化
                double cpuUsage = 30 + Math.random() * 70; // 30-100%
                metrics.setCpuUsage(cpuUsage);
                
                // 模拟内存使用率变化
                double memoryUsage = 40 + Math.random() * 50; // 40-90%
                metrics.setMemoryUsage(memoryUsage);
                
                // 模拟响应时间变化
                double responseTime = 100 + Math.random() * 1500; // 100-1600ms
                metrics.setAvgResponseTime(responseTime);
                
                // 模拟错误率变化
                double errorRate = Math.random() * 0.2; // 0-20%
                metrics.setErrorRate(errorRate);
                
                // 模拟连接数变化
                long currentConnections = (long) (Math.random() * 1000);
                metrics.setCurrentConnections(currentConnections);
                metrics.setMaxConnections(1000);
                
                // 模拟QPS变化
                double qps = 100 + Math.random() * 900; // 100-1000 QPS
                metrics.setQps(qps);
                
                // 更新自适应限流器
                adaptiveRateLimiter.updateMetrics(metrics);
                
                logger.debug("更新系统指标: {}", metrics);
                
            } catch (Exception e) {
                logger.error("系统指标模拟器异常", e);
            }
        }, 0, 5, TimeUnit.SECONDS);
    }
    
    /**
     * 启动实例指标模拟器
     */
    private void startInstanceMetricsSimulator(List<ServiceInstance> instances) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                for (ServiceInstance instance : instances) {
                    InstanceMetrics metrics = new InstanceMetrics(instance.getInstanceId());
                    
                    // 模拟响应时间变化
                    double responseTime = 50 + Math.random() * 1000; // 50-1050ms
                    metrics.setResponseTime(responseTime);
                    
                    // 模拟错误率变化
                    double errorRate = Math.random() * 0.15; // 0-15%
                    metrics.setErrorRate(errorRate);
                    
                    // 模拟连接数变化
                    long activeConnections = (long) (Math.random() * 500);
                    metrics.setActiveConnections(activeConnections);
                    metrics.setMaxConnections(500);
                    
                    // 模拟QPS变化
                    double qps = 50 + Math.random() * 450; // 50-500 QPS
                    metrics.setQps(qps);
                    
                    // 模拟CPU使用率变化
                    double cpuUsage = 20 + Math.random() * 60; // 20-80%
                    metrics.setCpuUsage(cpuUsage);
                    
                    // 模拟内存使用率变化
                    double memoryUsage = 30 + Math.random() * 50; // 30-80%
                    metrics.setMemoryUsage(memoryUsage);
                    
                    // 设置健康状态
                    if (errorRate > 0.1 || responseTime > 1000) {
                        metrics.setHealthStatus(InstanceMetrics.HealthStatus.UNHEALTHY);
                    } else {
                        metrics.setHealthStatus(InstanceMetrics.HealthStatus.HEALTHY);
                    }
                    
                    // 更新自适应负载均衡器
                    adaptiveLoadBalancer.updateInstanceMetrics("user-service", 
                            instance.getInstanceId(), metrics);
                    
                    logger.debug("更新实例指标: instance={}, metrics={}", 
                            instance.getInstanceId(), metrics);
                }
                
            } catch (Exception e) {
                logger.error("实例指标模拟器异常", e);
            }
        }, 0, 3, TimeUnit.SECONDS);
    }
    
    /**
     * 创建模拟的服务实例
     */
    private List<ServiceInstance> createMockServiceInstances() {
        List<ServiceInstance> instances = new ArrayList<>();
        
        for (int i = 1; i <= 5; i++) {
            ServiceInstance instance = new ServiceInstance();
            instance.setInstanceId("user-service-" + i);
            instance.setServiceName("user-service");
            instance.setHost("192.168.1." + (100 + i));
            instance.setPort(8080 + i);
            instance.setWeight(1.0);
            instance.setEnabled(true);
            
            instances.add(instance);
        }
        
        return instances;
    }
    
    /**
     * 演示综合场景
     */
    public void demonstrateComprehensiveScenario() {
        logger.info("开始演示综合场景");
        
        // 启动系统指标模拟器
        startSystemMetricsSimulator();
        
        // 创建模拟的服务实例
        List<ServiceInstance> instances = createMockServiceInstances();
        
        // 启动实例指标模拟器
        startInstanceMetricsSimulator(instances);
        
        // 模拟综合场景
        for (int i = 0; i < 200; i++) {
            String requestKey = "user-" + (i % 20);
            
            // 1. 自适应限流检查
            boolean rateLimitAllowed = adaptiveRateLimiter.tryAcquire(requestKey);
            
            if (rateLimitAllowed) {
                // 2. 自适应负载均衡选择
                ServiceInstance selected = adaptiveLoadBalancer.select("user-service", instances, requestKey);
                
                if (selected != null) {
                    logger.debug("请求处理成功: user={}, instance={}", requestKey, selected.getInstanceId());
                    
                    // 模拟请求处理时间
                    try {
                        Thread.sleep(50 + (long) (Math.random() * 100));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    logger.warn("负载均衡选择失败: user={}", requestKey);
                }
            } else {
                logger.debug("请求被限流: user={}", requestKey);
            }
            
            // 每10个请求打印一次统计信息
            if (i % 10 == 0) {
                logger.info("处理进度: {}/200, 限流统计: {}, 负载均衡统计: {}", 
                        i, adaptiveRateLimiter.getStats(), adaptiveLoadBalancer.getStats());
            }
        }
        
        logger.info("综合场景演示完成");
    }
    
    /**
     * 关闭示例
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}