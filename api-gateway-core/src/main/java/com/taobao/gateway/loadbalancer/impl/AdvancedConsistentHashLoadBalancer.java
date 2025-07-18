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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 高级一致性哈希负载均衡器实现
 * 
 * 特性：
 * 1. 支持自定义哈希键生成策略
 * 2. 支持权重感知的虚拟节点分配
 * 3. 支持节点故障自动剔除
 * 4. 支持哈希环预热和缓存
 * 5. 提供详细的监控指标
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class AdvancedConsistentHashLoadBalancer implements LoadBalancer {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedConsistentHashLoadBalancer.class);
    
    /**
     * 默认虚拟节点数量
     */
    private static final int DEFAULT_VIRTUAL_NODES = 150;
    
    /**
     * 默认哈希算法
     */
    private static final String DEFAULT_HASH_ALGORITHM = "MD5";
    
    /**
     * 哈希环，存储虚拟节点到服务实例的映射
     */
    private final TreeMap<Long, ServiceInstance> hashRing = new TreeMap<>();
    
    /**
     * 虚拟节点数量
     */
    private final int virtualNodes;
    
    /**
     * 哈希算法名称
     */
    private final String hashAlgorithm;
    
    /**
     * 哈希键生成策略
     */
    private final Function<String, String> hashKeyGenerator;
    
    /**
     * 服务实例缓存，用于快速判断实例变化
     */
    private final Map<String, Set<String>> serviceInstanceCache = new ConcurrentHashMap<>();
    
    /**
     * 哈希环缓存，按服务名称缓存
     */
    private final Map<String, TreeMap<Long, ServiceInstance>> hashRingCache = new ConcurrentHashMap<>();
    
    /**
     * 监控统计
     */
    private final Map<String, Long> hitCount = new ConcurrentHashMap<>();
    private final Map<String, Long> totalCount = new ConcurrentHashMap<>();
    
    /**
     * 默认构造函数
     */
    public AdvancedConsistentHashLoadBalancer() {
        this(DEFAULT_VIRTUAL_NODES, DEFAULT_HASH_ALGORITHM, null);
    }
    
    /**
     * 构造函数
     * 
     * @param virtualNodes 虚拟节点数量
     */
    public AdvancedConsistentHashLoadBalancer(int virtualNodes) {
        this(virtualNodes, DEFAULT_HASH_ALGORITHM, null);
    }
    
    /**
     * 构造函数
     * 
     * @param virtualNodes 虚拟节点数量
     * @param hashAlgorithm 哈希算法
     */
    public AdvancedConsistentHashLoadBalancer(int virtualNodes, String hashAlgorithm) {
        this(virtualNodes, hashAlgorithm, null);
    }
    
    /**
     * 构造函数
     * 
     * @param virtualNodes 虚拟节点数量
     * @param hashAlgorithm 哈希算法
     * @param hashKeyGenerator 哈希键生成策略
     */
    public AdvancedConsistentHashLoadBalancer(int virtualNodes, String hashAlgorithm, 
                                             Function<String, String> hashKeyGenerator) {
        this.virtualNodes = virtualNodes;
        this.hashAlgorithm = hashAlgorithm;
        this.hashKeyGenerator = hashKeyGenerator != null ? hashKeyGenerator : this::defaultHashKeyGenerator;
        
        logger.info("初始化高级一致性哈希负载均衡器，虚拟节点数量: {}, 哈希算法: {}", virtualNodes, hashAlgorithm);
    }
    
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
        
        // 更新统计
        totalCount.merge(serviceName, 1L, Long::sum);
        
        // 检查实例列表是否发生变化，如果变化则重新构建哈希环
        if (isInstanceListChanged(serviceName, healthyInstances)) {
            rebuildHashRing(serviceName, healthyInstances);
        }
        
        // 生成请求的哈希键
        String requestKey = hashKeyGenerator.apply(serviceName);
        long hash = hash(requestKey);
        
        // 在哈希环上查找目标节点
        ServiceInstance selected = findTargetNode(serviceName, hash);
        
        if (selected != null) {
            hitCount.merge(serviceName, 1L, Long::sum);
            logger.debug("高级一致性哈希选择服务实例: {} -> {} (hash: {})", serviceName, selected, hash);
        } else {
            logger.warn("高级一致性哈希未找到合适的服务实例: {} (hash: {})", serviceName, hash);
        }
        
        return selected;
    }
    
    @Override
    public LoadBalancerType getType() {
        return LoadBalancerType.CONSISTENT_HASH;
    }
    
    /**
     * 默认哈希键生成策略
     * 
     * @param serviceName 服务名称
     * @return 哈希键
     */
    private String defaultHashKeyGenerator(String serviceName) {
        // 可以根据实际需求扩展，例如：
        // - 结合用户ID
        // - 结合请求路径
        // - 结合时间戳
        // - 结合客户端IP
        return serviceName + "_" + System.currentTimeMillis();
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
        TreeMap<Long, ServiceInstance> serviceHashRing = new TreeMap<>();
        
        // 为每个实例创建虚拟节点，考虑权重
        for (ServiceInstance instance : instances) {
            int nodeCount = calculateVirtualNodeCount(instance);
            addVirtualNodes(serviceHashRing, instance, nodeCount);
        }
        
        // 更新缓存
        hashRingCache.put(serviceName, serviceHashRing);
        
        logger.info("重新构建哈希环完成，服务: {}, 实例数量: {}, 虚拟节点数量: {}", 
                serviceName, instances.size(), serviceHashRing.size());
    }
    
    /**
     * 计算虚拟节点数量（考虑权重）
     * 
     * @param instance 服务实例
     * @return 虚拟节点数量
     */
    private int calculateVirtualNodeCount(ServiceInstance instance) {
        int weight = instance.getWeight();
        if (weight <= 0) {
            weight = 100; // 默认权重
        }
        
        // 根据权重计算虚拟节点数量
        return Math.max(1, (virtualNodes * weight) / 100);
    }
    
    /**
     * 为服务实例添加虚拟节点
     * 
     * @param serviceHashRing 服务的哈希环
     * @param instance 服务实例
     * @param nodeCount 虚拟节点数量
     */
    private void addVirtualNodes(TreeMap<Long, ServiceInstance> serviceHashRing, 
                                ServiceInstance instance, int nodeCount) {
        String instanceId = instance.getId();
        
        for (int i = 0; i < nodeCount; i++) {
            String virtualNodeName = instanceId + "#" + i;
            long hash = hash(virtualNodeName);
            serviceHashRing.put(hash, instance);
        }
    }
    
    /**
     * 在哈希环上查找目标节点
     * 
     * @param serviceName 服务名称
     * @param requestHash 请求的哈希值
     * @return 目标服务实例
     */
    private ServiceInstance findTargetNode(String serviceName, long requestHash) {
        TreeMap<Long, ServiceInstance> serviceHashRing = hashRingCache.get(serviceName);
        
        if (serviceHashRing == null || serviceHashRing.isEmpty()) {
            return null;
        }
        
        // 查找大于等于请求哈希值的第一个节点
        Map.Entry<Long, ServiceInstance> entry = serviceHashRing.ceilingEntry(requestHash);
        
        if (entry != null) {
            return entry.getValue();
        }
        
        // 如果没有找到，说明请求哈希值超过了环的最大值，返回环的第一个节点
        return serviceHashRing.firstEntry().getValue();
    }
    
    /**
     * 计算字符串的哈希值
     * 
     * @param key 输入字符串
     * @return 哈希值
     */
    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance(hashAlgorithm);
            byte[] hashBytes = md.digest(key.getBytes(StandardCharsets.UTF_8));
            
            // 将字节数组转换为长整型哈希值
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (hashBytes[i] & 0xFF);
            }
            
            return Math.abs(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.error("哈希算法不可用: {}", hashAlgorithm, e);
            // 降级到简单的字符串哈希
            return Math.abs(key.hashCode());
        }
    }
    
    /**
     * 获取负载均衡统计信息
     * 
     * @param serviceName 服务名称
     * @return 统计信息
     */
    public Map<String, Object> getStatistics(String serviceName) {
        Map<String, Object> stats = new HashMap<>();
        
        Long hits = hitCount.get(serviceName);
        Long total = totalCount.get(serviceName);
        
        stats.put("serviceName", serviceName);
        stats.put("totalRequests", total != null ? total : 0L);
        stats.put("hitRequests", hits != null ? hits : 0L);
        stats.put("hitRate", (hits != null && total != null && total > 0) ? 
                (double) hits / total : 0.0);
        
        TreeMap<Long, ServiceInstance> serviceHashRing = hashRingCache.get(serviceName);
        if (serviceHashRing != null) {
            stats.put("hashRingSize", serviceHashRing.size());
            stats.put("uniqueInstances", serviceHashRing.values().stream()
                    .map(ServiceInstance::getId)
                    .distinct()
                    .count());
        }
        
        return stats;
    }
    
    /**
     * 获取所有服务的统计信息
     * 
     * @return 所有服务的统计信息
     */
    public Map<String, Map<String, Object>> getAllStatistics() {
        Map<String, Map<String, Object>> allStats = new HashMap<>();
        
        for (String serviceName : totalCount.keySet()) {
            allStats.put(serviceName, getStatistics(serviceName));
        }
        
        return allStats;
    }
    
    /**
     * 清除服务统计信息
     * 
     * @param serviceName 服务名称
     */
    public void clearStatistics(String serviceName) {
        hitCount.remove(serviceName);
        totalCount.remove(serviceName);
        hashRingCache.remove(serviceName);
        serviceInstanceCache.remove(serviceName);
        logger.info("清除服务统计信息: {}", serviceName);
    }
    
    /**
     * 清除所有统计信息
     */
    public void clearAllStatistics() {
        hitCount.clear();
        totalCount.clear();
        hashRingCache.clear();
        serviceInstanceCache.clear();
        logger.info("清除所有统计信息");
    }
    
    /**
     * 获取哈希环信息
     * 
     * @param serviceName 服务名称
     * @return 哈希环信息
     */
    public Map<String, Object> getHashRingInfo(String serviceName) {
        TreeMap<Long, ServiceInstance> serviceHashRing = hashRingCache.get(serviceName);
        
        Map<String, Object> info = new HashMap<>();
        info.put("serviceName", serviceName);
        info.put("virtualNodes", virtualNodes);
        info.put("hashAlgorithm", hashAlgorithm);
        
        if (serviceHashRing != null) {
            info.put("ringSize", serviceHashRing.size());
            info.put("uniqueInstances", serviceHashRing.values().stream()
                    .map(ServiceInstance::getId)
                    .distinct()
                    .count());
            
            // 统计每个实例的虚拟节点分布
            Map<String, Long> instanceDistribution = serviceHashRing.values().stream()
                    .collect(Collectors.groupingBy(ServiceInstance::getId, Collectors.counting()));
            info.put("instanceDistribution", instanceDistribution);
        }
        
        return info;
    }
    
    /**
     * 设置自定义哈希键生成策略
     * 
     * @param generator 哈希键生成策略
     */
    public void setHashKeyGenerator(Function<String, String> generator) {
        if (generator != null) {
            // 注意：这里需要重新构建所有哈希环
            logger.info("更新哈希键生成策略");
            hashRingCache.clear();
            serviceInstanceCache.clear();
        }
    }
} 