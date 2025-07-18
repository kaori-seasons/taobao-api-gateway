package com.taobao.gateway.loadbalancer;

import com.taobao.gateway.loadbalancer.impl.LeastConnectionsLoadBalancer;
import com.taobao.gateway.loadbalancer.impl.RoundRobinLoadBalancer;
import com.taobao.gateway.loadbalancer.impl.WeightedRoundRobinLoadBalancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 负载均衡器工厂
 * 负责创建和管理不同类型的负载均衡器实例
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class LoadBalancerFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(LoadBalancerFactory.class);
    
    /**
     * 负载均衡器实例缓存
     */
    private final Map<String, LoadBalancer> loadBalancerCache = new ConcurrentHashMap<>();
    
    /**
     * 创建负载均衡器
     * 
     * @param type 负载均衡器类型
     * @return 负载均衡器实例
     */
    public LoadBalancer createLoadBalancer(LoadBalancerType type) {
        return createLoadBalancer(type, null);
    }
    
    /**
     * 创建负载均衡器
     * 
     * @param type 负载均衡器类型
     * @param name 负载均衡器名称
     * @return 负载均衡器实例
     */
    public LoadBalancer createLoadBalancer(LoadBalancerType type, String name) {
        String key = name != null ? name : type.name();
        
        return loadBalancerCache.computeIfAbsent(key, k -> {
            LoadBalancer loadBalancer = doCreateLoadBalancer(type);
            logger.info("创建负载均衡器: {} - {}", type, key);
            return loadBalancer;
        });
    }
    
    /**
     * 创建负载均衡器实例
     * 
     * @param type 负载均衡器类型
     * @return 负载均衡器实例
     */
    private LoadBalancer doCreateLoadBalancer(LoadBalancerType type) {
        switch (type) {
            case ROUND_ROBIN:
                return new RoundRobinLoadBalancer();
            case WEIGHTED_ROUND_ROBIN:
                return new WeightedRoundRobinLoadBalancer();
            case LEAST_CONNECTIONS:
                return new LeastConnectionsLoadBalancer();
            default:
                logger.warn("未知的负载均衡器类型: {}, 使用轮询算法", type);
                return new RoundRobinLoadBalancer();
        }
    }
    
    /**
     * 获取负载均衡器
     * 
     * @param name 负载均衡器名称
     * @return 负载均衡器实例，如果不存在则返回null
     */
    public LoadBalancer getLoadBalancer(String name) {
        return loadBalancerCache.get(name);
    }
    
    /**
     * 移除负载均衡器
     * 
     * @param name 负载均衡器名称
     * @return 被移除的负载均衡器实例
     */
    public LoadBalancer removeLoadBalancer(String name) {
        LoadBalancer removed = loadBalancerCache.remove(name);
        if (removed != null) {
            logger.info("移除负载均衡器: {}", name);
        }
        return removed;
    }
    
    /**
     * 清空所有负载均衡器
     */
    public void clear() {
        loadBalancerCache.clear();
        logger.info("清空所有负载均衡器");
    }
    
    /**
     * 获取负载均衡器数量
     * 
     * @return 负载均衡器数量
     */
    public int size() {
        return loadBalancerCache.size();
    }
    
    /**
     * 检查是否存在指定名称的负载均衡器
     * 
     * @param name 负载均衡器名称
     * @return 是否存在
     */
    public boolean contains(String name) {
        return loadBalancerCache.containsKey(name);
    }
    
    /**
     * 获取所有负载均衡器名称
     * 
     * @return 负载均衡器名称集合
     */
    public java.util.Set<String> getLoadBalancerNames() {
        return loadBalancerCache.keySet();
    }
} 