package com.taobao.gateway.loadbalancer.impl;

import com.taobao.gateway.loadbalancer.LoadBalancer;
import com.taobao.gateway.loadbalancer.LoadBalancerType;
import com.taobao.gateway.loadbalancer.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 一致性哈希负载均衡器实现
 * 
 * 一致性哈希算法的核心思想：
 * 1. 将哈希空间组织成一个虚拟的圆环（哈希环）
 * 2. 将服务节点通过哈希算法映射到环上
 * 3. 将请求通过哈希算法映射到环上
 * 4. 按顺时针方向找到第一个节点作为目标节点
 * 
 * 优势：
 * - 节点变化时只有少量请求需要重新分配
 * - 支持虚拟节点，提高负载均衡效果
 * - 适合缓存场景，减少缓存失效
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class ConsistentHashLoadBalancer implements LoadBalancer {
    
    private static final Logger logger = LoggerFactory.getLogger(ConsistentHashLoadBalancer.class);
    
    /**
     * 默认虚拟节点数量
     */
    private static final int DEFAULT_VIRTUAL_NODES = 150;
    
    /**
     * 哈希算法名称
     */
    private static final String HASH_ALGORITHM = "MD5";
    
    /**
     * 哈希环，存储虚拟节点到服务实例的映射
     */
    private final TreeMap<Long, ServiceInstance> hashRing = new TreeMap<>();
    
    /**
     * 虚拟节点数量
     */
    private final int virtualNodes;
    
    /**
     * 服务实例缓存，用于快速判断实例变化
     */
    private final Map<String, Set<String>> serviceInstanceCache = new ConcurrentHashMap<>();
    
    /**
     * 默认构造函数，使用默认虚拟节点数量
     */
    public ConsistentHashLoadBalancer() {
        this(DEFAULT_VIRTUAL_NODES);
    }
    
    /**
     * 构造函数
     * 
     * @param virtualNodes 虚拟节点数量
     */
    public ConsistentHashLoadBalancer(int virtualNodes) {
        this.virtualNodes = virtualNodes;
        logger.info("初始化一致性哈希负载均衡器，虚拟节点数量: {}", virtualNodes);
    }
    
    @Override
    public ServiceInstance select(String serviceName, List<ServiceInstance> instances, String requestKey) {
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
        
        // 检查实例列表是否发生变化，如果变化则重新构建哈希环
        if (isInstanceListChanged(serviceName, healthyInstances)) {
            rebuildHashRing(serviceName, healthyInstances);
        }
        
        // 生成请求的哈希键
        long hash = hash(requestKey);
        
        // 在哈希环上查找目标节点
        ServiceInstance selected = findTargetNode(hash);
        
        if (selected != null) {
            logger.debug("一致性哈希选择服务实例: {} -> {} (hash: {})", serviceName, selected, hash);
        } else {
            logger.warn("一致性哈希未找到合适的服务实例: {} (hash: {})", serviceName, hash);
        }
        
        return selected;
    }
    
    @Override
    public ServiceInstance select(String serviceName, List<ServiceInstance> instances) {
        // 兼容老接口，使用serviceName作为requestKey
        return select(serviceName, instances, serviceName);
    }
    
    @Override
    public LoadBalancerType getType() {
        return LoadBalancerType.CONSISTENT_HASH;
    }
    
    /**
     * 检查服务实例列表是否发生变化
     * 
     * @param serviceName 服务名称
     * @param instances 当前实例列表
     * @return 是否发生变化
     */
    private boolean isInstanceListChanged(String serviceName, List<ServiceInstance> instances) {
        Set<String> currentInstanceIds = instances.stream()
                .map(ServiceInstance::getId)
                .collect(Collectors.toSet());
        
        Set<String> cachedInstanceIds = serviceInstanceCache.get(serviceName);
        
        if (cachedInstanceIds == null || !cachedInstanceIds.equals(currentInstanceIds)) {
            serviceInstanceCache.put(serviceName, currentInstanceIds);
            return true;
        }
        
        return false;
    }
    
    /**
     * 重新构建哈希环
     * 
     * @param serviceName 服务名称
     * @param instances 服务实例列表
     */
    private void rebuildHashRing(String serviceName, List<ServiceInstance> instances) {
        synchronized (hashRing) {
            // 清空当前哈希环
            hashRing.clear();
            
            // 为每个实例创建虚拟节点
            for (ServiceInstance instance : instances) {
                addVirtualNodes(instance);
            }
            
            logger.info("重新构建哈希环完成，服务: {}, 实例数量: {}, 虚拟节点数量: {}", 
                    serviceName, instances.size(), hashRing.size());
        }
    }
    
    /**
     * 为服务实例添加虚拟节点
     * 
     * @param instance 服务实例
     */
    private void addVirtualNodes(ServiceInstance instance) {
        String instanceId = instance.getId();
        int weight = instance.getWeight();
        
        // 根据权重计算虚拟节点数量，权重越高，虚拟节点越多
        int nodeCount = Math.max(1, (virtualNodes * weight) / 100);
        
        // 为每个实例创建多个虚拟节点，提高负载均衡效果
        for (int i = 0; i < nodeCount; i++) {
            String virtualNodeName = instanceId + "#" + i;
            long hash = hash(virtualNodeName);
            hashRing.put(hash, instance);
        }
    }
    
    /**
     * 在哈希环上查找目标节点
     * 
     * @param requestHash 请求的哈希值
     * @return 目标服务实例
     */
    private ServiceInstance findTargetNode(long requestHash) {
        synchronized (hashRing) {
            // 查找大于等于请求哈希值的第一个节点
            Map.Entry<Long, ServiceInstance> entry = hashRing.ceilingEntry(requestHash);
            
            if (entry != null) {
                return entry.getValue();
            }
            
            // 如果没有找到，说明请求哈希值超过了环的最大值，返回环的第一个节点
            if (!hashRing.isEmpty()) {
                return hashRing.firstEntry().getValue();
            }
            
            return null;
        }
    }
    
    /**
     * 计算字符串的哈希值
     * 
     * @param key 输入字符串
     * @return 哈希值
     */
    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = md.digest(key.getBytes(StandardCharsets.UTF_8));
            
            // 将字节数组转换为长整型哈希值
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (hashBytes[i] & 0xFF);
            }
            
            return Math.abs(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.error("哈希算法不可用: {}", HASH_ALGORITHM, e);
            // 降级到简单的字符串哈希
            return Math.abs(key.hashCode());
        }
    }
    
    /**
     * 获取哈希环信息（用于调试和监控）
     * 
     * @return 哈希环信息
     */
    public Map<String, Object> getHashRingInfo() {
        synchronized (hashRing) {
            Map<String, Object> info = new HashMap<>();
            info.put("virtualNodes", virtualNodes);
            info.put("ringSize", hashRing.size());
            info.put("uniqueInstances", hashRing.values().stream()
                    .map(ServiceInstance::getId)
                    .distinct()
                    .count());
            info.put("hashAlgorithm", HASH_ALGORITHM);
            return info;
        }
    }
    
    /**
     * 获取虚拟节点数量
     * 
     * @return 虚拟节点数量
     */
    public int getVirtualNodes() {
        return virtualNodes;
    }
    
    /**
     * 获取哈希环大小
     * 
     * @return 哈希环大小
     */
    public int getHashRingSize() {
        synchronized (hashRing) {
            return hashRing.size();
        }
    }
} 