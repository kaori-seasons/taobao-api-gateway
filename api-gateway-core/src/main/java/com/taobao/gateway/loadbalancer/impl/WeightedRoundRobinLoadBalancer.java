package com.taobao.gateway.loadbalancer.impl;

import com.taobao.gateway.loadbalancer.LoadBalancer;
import com.taobao.gateway.loadbalancer.LoadBalancerType;
import com.taobao.gateway.loadbalancer.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 权重轮询负载均衡器实现
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class WeightedRoundRobinLoadBalancer implements LoadBalancer {
    
    private static final Logger logger = LoggerFactory.getLogger(WeightedRoundRobinLoadBalancer.class);
    
    /** 轮询计数器 */
    private final AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    public ServiceInstance select(String serviceName, List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            logger.warn("没有可用的服务实例: {}", serviceName);
            return null;
        }
        
        // 过滤出健康的实例
        List<ServiceInstance> healthyInstances = instances.stream()
                .filter(ServiceInstance::isHealthy)
                .filter(ServiceInstance::isEnabled)
                .collect(Collectors.toList());
        
        if (healthyInstances.isEmpty()) {
            logger.warn("没有健康的服务实例: {}", serviceName);
            return null;
        }
        
        // 计算总权重
        int totalWeight = healthyInstances.stream()
                .mapToInt(ServiceInstance::getWeight)
                .sum();
        
        if (totalWeight <= 0) {
            logger.warn("所有实例权重都为0: {}", serviceName);
            return null;
        }
        
        // 权重轮询选择
        int currentWeight = counter.getAndIncrement() % totalWeight;
        int weightSum = 0;
        
        for (ServiceInstance instance : healthyInstances) {
            weightSum += instance.getWeight();
            if (currentWeight < weightSum) {
                logger.debug("权重轮询选择服务实例: {} -> {}", serviceName, instance);
                return instance;
            }
        }
        
        // 兜底选择第一个实例
        ServiceInstance selected = healthyInstances.get(0);
        logger.debug("权重轮询选择服务实例(兜底): {} -> {}", serviceName, selected);
        return selected;
    }
    
    @Override
    public LoadBalancerType getType() {
        return LoadBalancerType.WEIGHTED_ROUND_ROBIN;
    }
} 