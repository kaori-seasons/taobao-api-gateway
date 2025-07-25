package com.taobao.gateway.loadbalancer.adaptive;

import com.taobao.gateway.loadbalancer.LoadBalancer;
import com.taobao.gateway.loadbalancer.ServiceInstance;

import java.util.List;

/**
 * 自适应负载均衡器接口
 * 基于服务实例的健康状态、响应时间、负载等指标动态调整负载均衡策略
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
public interface AdaptiveLoadBalancer extends LoadBalancer {
    
    /**
     * 获取自适应策略
     * 
     * @return 自适应策略
     */
    AdaptiveLoadBalanceStrategy getStrategy();
    
    /**
     * 更新服务实例指标
     * 
     * @param serviceName 服务名称
     * @param instanceId 实例ID
     * @param metrics 实例指标
     */
    void updateInstanceMetrics(String serviceName, String instanceId, InstanceMetrics metrics);
    
    /**
     * 获取自适应配置
     * 
     * @return 自适应配置
     */
    AdaptiveLoadBalanceConfig getAdaptiveConfig();
    
    /**
     * 获取负载均衡统计信息
     * 
     * @return 统计信息
     */
    AdaptiveLoadBalanceStats getStats();
    
    /**
     * 健康检查
     * 
     * @param serviceName 服务名称
     * @param instances 服务实例列表
     * @return 健康的实例列表
     */
    List<ServiceInstance> healthCheck(String serviceName, List<ServiceInstance> instances);
} 