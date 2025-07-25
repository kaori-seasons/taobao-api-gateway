package com.taobao.gateway.loadbalancer.adaptive;

import com.taobao.gateway.loadbalancer.ServiceInstance;

import java.util.List;
import java.util.Map;

/**
 * 自适应负载均衡策略接口
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
public interface AdaptiveLoadBalanceStrategy {
    
    /**
     * 选择服务实例
     * 
     * @param serviceName 服务名称
     * @param instances 服务实例列表
     * @param instanceMetrics 实例指标映射
     * @return 选择的服务实例
     */
    ServiceInstance select(String serviceName, List<ServiceInstance> instances, 
                          Map<String, InstanceMetrics> instanceMetrics);
    
    /**
     * 选择服务实例（带请求key）
     * 
     * @param serviceName 服务名称
     * @param instances 服务实例列表
     * @param instanceMetrics 实例指标映射
     * @param requestKey 请求key
     * @return 选择的服务实例
     */
    ServiceInstance select(String serviceName, List<ServiceInstance> instances, 
                          Map<String, InstanceMetrics> instanceMetrics, String requestKey);
    
    /**
     * 获取策略类型
     * 
     * @return 策略类型
     */
    AdaptiveLoadBalanceStrategyType getType();
    
    /**
     * 计算实例权重
     * 
     * @param instance 服务实例
     * @param metrics 实例指标
     * @return 计算后的权重
     */
    double calculateWeight(ServiceInstance instance, InstanceMetrics metrics);
} 