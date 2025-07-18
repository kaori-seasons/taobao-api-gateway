package com.taobao.gateway.discovery.impl;

import com.taobao.gateway.discovery.ServiceDiscovery;
import com.taobao.gateway.loadbalancer.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 默认服务发现实现
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class DefaultServiceDiscovery implements ServiceDiscovery {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultServiceDiscovery.class);
    
    /** 服务实例存储：服务名称 -> 实例列表 */
    private final Map<String, List<ServiceInstance>> serviceInstances = new ConcurrentHashMap<>();
    
    /** 实例存储：实例ID -> 实例 */
    private final Map<String, ServiceInstance> instances = new ConcurrentHashMap<>();
    
    /** 健康检查调度器 */
    private final ScheduledExecutorService healthCheckScheduler = Executors.newScheduledThreadPool(1);
    
    /** 是否已启动 */
    private volatile boolean started = false;
    
    @Override
    public void register(ServiceInstance instance) {
        logger.info("注册服务实例: {}", instance);
        
        // 添加到实例存储
        instances.put(instance.getId(), instance);
        
        // 添加到服务实例存储
        serviceInstances.computeIfAbsent(instance.getServiceName(), k -> new CopyOnWriteArrayList<>())
                .add(instance);
        
        logger.info("服务实例注册成功: {}", instance.getId());
    }
    
    @Override
    public void unregister(String instanceId) {
        logger.info("注销服务实例: {}", instanceId);
        
        ServiceInstance instance = instances.remove(instanceId);
        if (instance != null) {
            List<ServiceInstance> instanceList = serviceInstances.get(instance.getServiceName());
            if (instanceList != null) {
                instanceList.removeIf(inst -> inst.getId().equals(instanceId));
                
                // 如果服务没有实例了，删除服务
                if (instanceList.isEmpty()) {
                    serviceInstances.remove(instance.getServiceName());
                }
            }
            logger.info("服务实例注销成功: {}", instanceId);
        } else {
            logger.warn("服务实例不存在: {}", instanceId);
        }
    }
    
    @Override
    public List<ServiceInstance> getInstances(String serviceName) {
        List<ServiceInstance> instanceList = serviceInstances.get(serviceName);
        if (instanceList == null) {
            return List.of();
        }
        
        // 返回健康的实例
        return instanceList.stream()
                .filter(ServiceInstance::isHealthy)
                .filter(ServiceInstance::isEnabled)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<String> getServiceNames() {
        return List.copyOf(serviceInstances.keySet());
    }
    
    @Override
    public boolean healthCheck(String instanceId) {
        ServiceInstance instance = instances.get(instanceId);
        if (instance == null) {
            return false;
        }
        
        // 简单的健康检查：检查最后活跃时间
        long currentTime = System.currentTimeMillis();
        long lastActiveTime = instance.getLastActiveTime();
        long timeout = 30000; // 30秒超时
        
        boolean healthy = (currentTime - lastActiveTime) < timeout;
        
        if (!healthy) {
            logger.warn("服务实例健康检查失败: {}", instanceId);
            instance.setHealthy(false);
        }
        
        return healthy;
    }
    
    @Override
    public void start() {
        if (started) {
            logger.warn("服务发现已经启动");
            return;
        }
        
        logger.info("启动服务发现");
        
        // 启动健康检查任务
        healthCheckScheduler.scheduleAtFixedRate(this::performHealthCheck, 0, 30, TimeUnit.SECONDS);
        
        started = true;
        logger.info("服务发现启动成功");
    }
    
    @Override
    public void stop() {
        if (!started) {
            logger.warn("服务发现未启动");
            return;
        }
        
        logger.info("停止服务发现");
        
        // 停止健康检查任务
        healthCheckScheduler.shutdown();
        try {
            if (!healthCheckScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                healthCheckScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            healthCheckScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        started = false;
        logger.info("服务发现停止成功");
    }
    
    /**
     * 执行健康检查
     */
    private void performHealthCheck() {
        try {
            logger.debug("开始执行健康检查");
            
            for (ServiceInstance instance : instances.values()) {
                boolean healthy = healthCheck(instance.getId());
                if (healthy && !instance.isHealthy()) {
                    instance.setHealthy(true);
                    logger.info("服务实例恢复健康: {}", instance.getId());
                }
            }
            
            logger.debug("健康检查完成");
        } catch (Exception e) {
            logger.error("健康检查异常", e);
        }
    }
    
    /**
     * 获取服务发现统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalServices", serviceInstances.size());
        stats.put("totalInstances", instances.size());
        
        // 统计健康实例数
        long healthyInstances = instances.values().stream()
                .filter(ServiceInstance::isHealthy)
                .count();
        stats.put("healthyInstances", healthyInstances);
        
        // 统计启用实例数
        long enabledInstances = instances.values().stream()
                .filter(ServiceInstance::isEnabled)
                .count();
        stats.put("enabledInstances", enabledInstances);
        
        return stats;
    }
} 