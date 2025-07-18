package com.taobao.gateway.loadbalancer;

import java.util.List;

/**
 * 负载均衡器接口
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface LoadBalancer {
    
    /**
     * 选择服务实例
     * 
     * @param serviceName 服务名称
     * @param instances 服务实例列表
     * @return 选择的服务实例
     */
    ServiceInstance select(String serviceName, List<ServiceInstance> instances);
    
    /**
     * 选择一个服务实例（带请求key，适用于一致性哈希等场景）
     * @param serviceName 服务名称
     * @param instances 服务实例列表
     * @param requestKey 用于哈希的请求特征key（如用户ID、请求路径等）
     * @return 选中的服务实例
     */
    default ServiceInstance select(String serviceName, List<ServiceInstance> instances, String requestKey) {
        // 默认实现兼容老接口
        return select(serviceName, instances);
    }
    
    /**
     * 获取负载均衡器类型
     * 
     * @return 负载均衡器类型
     */
    LoadBalancerType getType();
} 