package com.taobao.gateway.loadbalancer.impl;

import com.taobao.gateway.loadbalancer.LoadBalancer;
import com.taobao.gateway.loadbalancer.LoadBalancerType;
import com.taobao.gateway.loadbalancer.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 最小连接数负载均衡器实现
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class LeastConnectionsLoadBalancer implements LoadBalancer {
    
    private static final Logger logger = LoggerFactory.getLogger(LeastConnectionsLoadBalancer.class);
    
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
        
        // 选择连接数最少的实例
        ServiceInstance selected = healthyInstances.stream()
                .min((a, b) -> {
                    int connectionsA = a.getCurrentConnections();
                    int connectionsB = b.getCurrentConnections();
                    
                    // 如果连接数相同，按权重排序
                    if (connectionsA == connectionsB) {
                        return Integer.compare(b.getWeight(), a.getWeight());
                    }
                    
                    return Integer.compare(connectionsA, connectionsB);
                })
                .orElse(healthyInstances.get(0));
        
        logger.debug("最小连接数选择服务实例: {} -> {}", serviceName, selected);
        return selected;
    }
    
    @Override
    public LoadBalancerType getType() {
        return LoadBalancerType.LEAST_CONNECTIONS;
    }
} 