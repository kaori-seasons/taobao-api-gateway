package com.taobao.gateway.discovery;

import com.taobao.gateway.loadbalancer.ServiceInstance;

import java.util.List;

/**
 * 服务发现接口
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface ServiceDiscovery {
    
    /**
     * 注册服务实例
     * 
     * @param instance 服务实例
     */
    void register(ServiceInstance instance);
    
    /**
     * 注销服务实例
     * 
     * @param instanceId 实例ID
     */
    void unregister(String instanceId);
    
    /**
     * 获取服务实例列表
     * 
     * @param serviceName 服务名称
     * @return 服务实例列表
     */
    List<ServiceInstance> getInstances(String serviceName);
    
    /**
     * 获取所有服务名称
     * 
     * @return 服务名称列表
     */
    List<String> getServiceNames();
    
    /**
     * 健康检查
     * 
     * @param instanceId 实例ID
     * @return 是否健康
     */
    boolean healthCheck(String instanceId);
    
    /**
     * 启动服务发现
     */
    void start();
    
    /**
     * 停止服务发现
     */
    void stop();
} 