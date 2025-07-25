package com.taobao.gateway.adaptive;

import com.taobao.gateway.loadbalancer.ServiceInstance;
import com.taobao.gateway.loadbalancer.adaptive.AdaptiveLoadBalancer;
import com.taobao.gateway.loadbalancer.adaptive.AdaptiveLoadBalanceConfig;
import com.taobao.gateway.loadbalancer.adaptive.InstanceMetrics;
import com.taobao.gateway.loadbalancer.adaptive.impl.AdaptiveLoadBalancerImpl;
import com.taobao.gateway.ratelimit.adaptive.AdaptiveRateLimitConfig;
import com.taobao.gateway.ratelimit.adaptive.AdaptiveRateLimiter;
import com.taobao.gateway.ratelimit.adaptive.SystemMetrics;
import com.taobao.gateway.ratelimit.adaptive.impl.AdaptiveRateLimiterImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 自适应限流和负载均衡测试类
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
public class AdaptiveTest {
    
    private AdaptiveRateLimiter rateLimiter;
    private AdaptiveLoadBalancer loadBalancer;
    private ScheduledExecutorService scheduler;
    
    @BeforeEach
    public void setUp() {
        scheduler = Executors.newScheduledThreadPool(4);
        
        // 创建自适应限流器
        AdaptiveRateLimitConfig rateLimitConfig = new AdaptiveRateLimitConfig();
        rateLimitConfig.setEnabled(true);
        rateLimitConfig.setStrategyType(AdaptiveRateLimitConfig.AdaptiveStrategyType.COMPREHENSIVE);
        rateLimitConfig.setBaseLimit(1000);
        rateLimitConfig.setMinLimit(100);
        rateLimitConfig.setMaxLimit(10000);
        rateLimiter = new AdaptiveRateLimiterImpl("test-rate-limiter", rateLimitConfig, scheduler);
        
        // 创建自适应负载均衡器
        AdaptiveLoadBalanceConfig loadBalanceConfig = new AdaptiveLoadBalanceConfig();
        loadBalanceConfig.setEnabled(true);
        loadBalanceConfig.setStrategyType(AdaptiveLoadBalanceConfig.AdaptiveLoadBalanceStrategyType.SCORE_BASED);
        loadBalancer = new AdaptiveLoadBalancerImpl("test-load-balancer", loadBalanceConfig, scheduler);
    }
    
    @AfterEach
    public void tearDown() {
        if (scheduler != null) {
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
    
    @Test
    public void testAdaptiveRateLimit() {
        // 测试基础限流功能
        assertTrue(rateLimiter.tryAcquire("test-key"));
        assertTrue(rateLimiter.tryAcquire("test-key", 5));
        
        // 测试系统指标更新
        SystemMetrics metrics = new SystemMetrics();
        metrics.setCpuUsage(50.0);
        metrics.setMemoryUsage(60.0);
        metrics.setAvgResponseTime(500.0);
        metrics.setErrorRate(0.05);
        metrics.setCurrentConnections(500);
        metrics.setMaxConnections(1000);
        metrics.setQps(800.0);
        
        rateLimiter.updateMetrics(metrics);
        
        // 验证指标计算
        assertFalse(metrics.isOverloaded());
        assertTrue(metrics.isHealthy());
        assertTrue(metrics.calculateLoadScore() > 0);
        
        // 测试高负载场景
        SystemMetrics highLoadMetrics = new SystemMetrics();
        highLoadMetrics.setCpuUsage(90.0);
        highLoadMetrics.setMemoryUsage(85.0);
        highLoadMetrics.setAvgResponseTime(1500.0);
        highLoadMetrics.setErrorRate(0.15);
        
        rateLimiter.updateMetrics(highLoadMetrics);
        
        assertTrue(highLoadMetrics.isOverloaded());
        assertFalse(highLoadMetrics.isHealthy());
        
        // 验证统计信息
        assertNotNull(rateLimiter.getStats());
        assertTrue(rateLimiter.getStats().getTotalRequests() > 0);
    }
    
    @Test
    public void testAdaptiveLoadBalance() {
        // 创建测试实例
        List<ServiceInstance> instances = createTestInstances();
        
        // 测试基础负载均衡功能
        ServiceInstance selected = loadBalancer.select("test-service", instances);
        assertNotNull(selected);
        assertTrue(instances.contains(selected));
        
        // 测试实例指标更新
        InstanceMetrics metrics = new InstanceMetrics("instance-1");
        metrics.setResponseTime(200.0);
        metrics.setErrorRate(0.02);
        metrics.setCpuUsage(60.0);
        metrics.setMemoryUsage(70.0);
        metrics.setActiveConnections(300);
        metrics.setMaxConnections(500);
        metrics.setQps(400.0);
        metrics.setHealthStatus(InstanceMetrics.HealthStatus.HEALTHY);
        
        loadBalancer.updateInstanceMetrics("test-service", "instance-1", metrics);
        
        // 验证指标计算
        assertTrue(metrics.isHealthy());
        assertFalse(metrics.isOverloaded());
        assertTrue(metrics.calculateScore() > 0);
        
        // 测试不健康实例
        InstanceMetrics unhealthyMetrics = new InstanceMetrics("instance-2");
        unhealthyMetrics.setResponseTime(2000.0);
        unhealthyMetrics.setErrorRate(0.25);
        unhealthyMetrics.setCpuUsage(95.0);
        unhealthyMetrics.setHealthStatus(InstanceMetrics.HealthStatus.UNHEALTHY);
        
        loadBalancer.updateInstanceMetrics("test-service", "instance-2", unhealthyMetrics);
        
        assertFalse(unhealthyMetrics.isHealthy());
        assertTrue(unhealthyMetrics.isOverloaded());
        
        // 测试健康检查
        List<ServiceInstance> healthyInstances = loadBalancer.healthCheck("test-service", instances);
        assertNotNull(healthyInstances);
        
        // 验证统计信息
        assertNotNull(loadBalancer.getStats());
        assertTrue(loadBalancer.getStats().getTotalRequests() > 0);
    }
    
    @Test
    public void testComprehensiveScenario() throws InterruptedException {
        // 创建测试实例
        List<ServiceInstance> instances = createTestInstances();
        
        // 模拟正常负载场景
        SystemMetrics normalMetrics = new SystemMetrics();
        normalMetrics.setCpuUsage(40.0);
        normalMetrics.setMemoryUsage(50.0);
        normalMetrics.setAvgResponseTime(300.0);
        normalMetrics.setErrorRate(0.02);
        normalMetrics.setCurrentConnections(400);
        normalMetrics.setMaxConnections(1000);
        normalMetrics.setQps(600.0);
        
        rateLimiter.updateMetrics(normalMetrics);
        
        // 模拟正常实例指标
        for (ServiceInstance instance : instances) {
            InstanceMetrics metrics = new InstanceMetrics(instance.getInstanceId());
            metrics.setResponseTime(150.0 + Math.random() * 200);
            metrics.setErrorRate(Math.random() * 0.05);
            metrics.setCpuUsage(30.0 + Math.random() * 40);
            metrics.setMemoryUsage(40.0 + Math.random() * 30);
            metrics.setHealthStatus(InstanceMetrics.HealthStatus.HEALTHY);
            
            loadBalancer.updateInstanceMetrics("test-service", instance.getInstanceId(), metrics);
        }
        
        // 执行综合测试
        int successCount = 0;
        int rateLimitCount = 0;
        
        for (int i = 0; i < 100; i++) {
            String requestKey = "user-" + (i % 10);
            
            // 限流检查
            if (rateLimiter.tryAcquire(requestKey)) {
                // 负载均衡选择
                ServiceInstance selected = loadBalancer.select("test-service", instances, requestKey);
                if (selected != null) {
                    successCount++;
                }
            } else {
                rateLimitCount++;
            }
            
            // 模拟请求间隔
            Thread.sleep(10);
        }
        
        // 验证结果
        assertTrue(successCount > 0, "应该有成功的请求");
        assertTrue(rateLimitCount >= 0, "限流计数应该非负");
        
        // 验证统计信息
        assertTrue(rateLimiter.getStats().getTotalRequests() > 0);
        assertTrue(loadBalancer.getStats().getTotalRequests() > 0);
        
        System.out.println("综合测试结果 - 成功请求: " + successCount + ", 限流请求: " + rateLimitCount);
        System.out.println("限流统计: " + rateLimiter.getStats());
        System.out.println("负载均衡统计: " + loadBalancer.getStats());
    }
    
    @Test
    public void testHighLoadScenario() throws InterruptedException {
        // 创建测试实例
        List<ServiceInstance> instances = createTestInstances();
        
        // 模拟高负载场景
        SystemMetrics highLoadMetrics = new SystemMetrics();
        highLoadMetrics.setCpuUsage(85.0);
        highLoadMetrics.setMemoryUsage(80.0);
        highLoadMetrics.setAvgResponseTime(1200.0);
        highLoadMetrics.setErrorRate(0.12);
        highLoadMetrics.setCurrentConnections(900);
        highLoadMetrics.setMaxConnections(1000);
        highLoadMetrics.setQps(950.0);
        
        rateLimiter.updateMetrics(highLoadMetrics);
        
        // 模拟部分实例不健康
        for (int i = 0; i < instances.size(); i++) {
            ServiceInstance instance = instances.get(i);
            InstanceMetrics metrics = new InstanceMetrics(instance.getInstanceId());
            
            if (i < 2) {
                // 健康实例
                metrics.setResponseTime(200.0 + Math.random() * 300);
                metrics.setErrorRate(Math.random() * 0.05);
                metrics.setCpuUsage(50.0 + Math.random() * 30);
                metrics.setHealthStatus(InstanceMetrics.HealthStatus.HEALTHY);
            } else {
                // 不健康实例
                metrics.setResponseTime(1500.0 + Math.random() * 1000);
                metrics.setErrorRate(0.1 + Math.random() * 0.2);
                metrics.setCpuUsage(80.0 + Math.random() * 20);
                metrics.setHealthStatus(InstanceMetrics.HealthStatus.UNHEALTHY);
            }
            
            loadBalancer.updateInstanceMetrics("test-service", instance.getInstanceId(), metrics);
        }
        
        // 执行高负载测试
        int successCount = 0;
        int rateLimitCount = 0;
        
        for (int i = 0; i < 200; i++) {
            String requestKey = "user-" + (i % 20);
            
            // 限流检查
            if (rateLimiter.tryAcquire(requestKey)) {
                // 负载均衡选择
                ServiceInstance selected = loadBalancer.select("test-service", instances, requestKey);
                if (selected != null) {
                    successCount++;
                }
            } else {
                rateLimitCount++;
            }
            
            // 模拟请求间隔
            Thread.sleep(5);
        }
        
        // 验证高负载下的行为
        assertTrue(rateLimitCount > 0, "高负载下应该有请求被限流");
        assertTrue(successCount > 0, "应该有部分请求成功");
        
        System.out.println("高负载测试结果 - 成功请求: " + successCount + ", 限流请求: " + rateLimitCount);
        System.out.println("限流统计: " + rateLimiter.getStats());
        System.out.println("负载均衡统计: " + loadBalancer.getStats());
    }
    
    /**
     * 创建测试实例
     */
    private List<ServiceInstance> createTestInstances() {
        List<ServiceInstance> instances = new ArrayList<>();
        
        for (int i = 1; i <= 5; i++) {
            ServiceInstance instance = new ServiceInstance();
            instance.setInstanceId("instance-" + i);
            instance.setServiceName("test-service");
            instance.setHost("192.168.1." + (100 + i));
            instance.setPort(8080 + i);
            instance.setWeight(1.0);
            instance.setEnabled(true);
            
            instances.add(instance);
        }
        
        return instances;
    }
}