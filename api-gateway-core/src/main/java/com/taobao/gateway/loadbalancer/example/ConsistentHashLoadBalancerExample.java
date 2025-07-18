package com.taobao.gateway.loadbalancer.example;

import com.taobao.gateway.loadbalancer.LoadBalancer;
import com.taobao.gateway.loadbalancer.LoadBalancerFactory;
import com.taobao.gateway.loadbalancer.LoadBalancerType;
import com.taobao.gateway.loadbalancer.ServiceInstance;
import com.taobao.gateway.loadbalancer.impl.AdvancedConsistentHashLoadBalancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 一致性哈希负载均衡器使用示例
 * 
 * 本示例展示了如何：
 * 1. 使用工厂创建一致性哈希负载均衡器
 * 2. 使用高级一致性哈希负载均衡器
 * 3. 自定义哈希键生成策略
 * 4. 监控负载均衡效果
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class ConsistentHashLoadBalancerExample {
    
    private static final Logger logger = LoggerFactory.getLogger(ConsistentHashLoadBalancerExample.class);
    
    @Autowired
    private LoadBalancerFactory loadBalancerFactory;
    
    /**
     * 基本使用示例
     */
    public void basicUsageExample() {
        logger.info("=== 基本一致性哈希负载均衡器使用示例 ===");
        
        // 1. 使用工厂创建一致性哈希负载均衡器
        LoadBalancer loadBalancer = loadBalancerFactory.createLoadBalancer(LoadBalancerType.CONSISTENT_HASH);
        
        // 2. 创建服务实例
        List<ServiceInstance> instances = createServiceInstances();
        
        // 3. 进行负载均衡选择
        String serviceName = "user-service";
        Map<String, Integer> distribution = new HashMap<>();
        
        for (int i = 0; i < 1000; i++) {
            ServiceInstance selected = loadBalancer.select(serviceName, instances);
            if (selected != null) {
                String instanceId = selected.getId();
                distribution.put(instanceId, distribution.getOrDefault(instanceId, 0) + 1);
            }
        }
        
        logger.info("基本一致性哈希负载分布: {}", distribution);
    }
    
    /**
     * 高级一致性哈希负载均衡器使用示例
     */
    public void advancedUsageExample() {
        logger.info("=== 高级一致性哈希负载均衡器使用示例 ===");
        
        // 1. 创建高级一致性哈希负载均衡器
        AdvancedConsistentHashLoadBalancer advancedLoadBalancer = new AdvancedConsistentHashLoadBalancer(200, "SHA-256");
        
        // 2. 创建服务实例
        List<ServiceInstance> instances = createServiceInstances();
        
        // 3. 进行负载均衡选择
        String serviceName = "order-service";
        for (int i = 0; i < 500; i++) {
            ServiceInstance selected = advancedLoadBalancer.select(serviceName, instances);
            if (selected != null) {
                logger.debug("选择服务实例: {} -> {}", serviceName, selected.getId());
            }
        }
        
        // 4. 获取统计信息
        Map<String, Object> stats = advancedLoadBalancer.getStatistics(serviceName);
        logger.info("高级一致性哈希统计信息: {}", stats);
        
        // 5. 获取哈希环信息
        Map<String, Object> hashRingInfo = advancedLoadBalancer.getHashRingInfo(serviceName);
        logger.info("哈希环信息: {}", hashRingInfo);
    }
    
    /**
     * 自定义哈希键生成策略示例
     */
    public void customHashKeyGeneratorExample() {
        logger.info("=== 自定义哈希键生成策略示例 ===");
        
        // 1. 创建自定义哈希键生成策略
        Function<String, String> customHashKeyGenerator = serviceName -> {
            // 这里可以根据实际业务需求生成哈希键
            // 例如：结合用户ID、请求路径、时间戳等
            String userId = "user-" + (System.currentTimeMillis() % 1000); // 模拟用户ID
            String requestPath = "/api/v1/users"; // 模拟请求路径
            return serviceName + "_" + userId + "_" + requestPath;
        };
        
        // 2. 创建高级一致性哈希负载均衡器
        AdvancedConsistentHashLoadBalancer loadBalancer = new AdvancedConsistentHashLoadBalancer(
                150, "MD5", customHashKeyGenerator);
        
        // 3. 创建服务实例
        List<ServiceInstance> instances = createServiceInstances();
        
        // 4. 进行负载均衡选择
        String serviceName = "payment-service";
        Map<String, Integer> distribution = new HashMap<>();
        
        for (int i = 0; i < 1000; i++) {
            ServiceInstance selected = loadBalancer.select(serviceName, instances);
            if (selected != null) {
                String instanceId = selected.getId();
                distribution.put(instanceId, distribution.getOrDefault(instanceId, 0) + 1);
            }
        }
        
        logger.info("自定义哈希键生成策略负载分布: {}", distribution);
    }
    
    /**
     * 节点变化一致性测试示例
     */
    public void nodeChangeConsistencyExample() {
        logger.info("=== 节点变化一致性测试示例 ===");
        
        // 1. 创建负载均衡器
        AdvancedConsistentHashLoadBalancer loadBalancer = new AdvancedConsistentHashLoadBalancer(100);
        
        // 2. 创建初始服务实例
        List<ServiceInstance> initialInstances = createServiceInstances();
        String serviceName = "inventory-service";
        
        // 3. 记录初始选择结果
        Map<String, ServiceInstance> initialSelections = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            String requestKey = "request-" + i;
            ServiceInstance selected = loadBalancer.select(serviceName, initialInstances);
            initialSelections.put(requestKey, selected);
        }
        
        // 4. 添加新节点
        List<ServiceInstance> expandedInstances = new ArrayList<>(initialInstances);
        ServiceInstance newInstance = new ServiceInstance("instance-5", serviceName, "192.168.1.5", 8080);
        newInstance.setWeight(100);
        newInstance.setHealthy(true);
        newInstance.setEnabled(true);
        expandedInstances.add(newInstance);
        
        // 5. 记录添加节点后的选择结果
        Map<String, ServiceInstance> expandedSelections = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            String requestKey = "request-" + i;
            ServiceInstance selected = loadBalancer.select(serviceName, expandedInstances);
            expandedSelections.put(requestKey, selected);
        }
        
        // 6. 统计重新分配的比例
        int reallocatedCount = 0;
        for (int i = 0; i < 100; i++) {
            String requestKey = "request-" + i;
            ServiceInstance initial = initialSelections.get(requestKey);
            ServiceInstance expanded = expandedSelections.get(requestKey);
            
            if (!initial.equals(expanded)) {
                reallocatedCount++;
            }
        }
        
        double reallocationRatio = (double) reallocatedCount / 100;
        logger.info("节点添加后重新分配比例: {}/100 = {}", reallocatedCount, reallocationRatio);
        
        // 7. 验证一致性哈希的优势
        assert reallocationRatio < 0.5 : "重新分配比例应该小于50%，实际: " + reallocationRatio;
        logger.info("一致性哈希优势验证通过，重新分配比例: {}", reallocationRatio);
    }
    
    /**
     * 权重感知负载均衡示例
     */
    public void weightAwareLoadBalancingExample() {
        logger.info("=== 权重感知负载均衡示例 ===");
        
        // 1. 创建不同权重的服务实例
        List<ServiceInstance> weightedInstances = new ArrayList<>();
        
        ServiceInstance lowWeight = new ServiceInstance("low-weight", "weighted-service", "192.168.1.1", 8080);
        lowWeight.setWeight(50);
        lowWeight.setHealthy(true);
        lowWeight.setEnabled(true);
        
        ServiceInstance mediumWeight = new ServiceInstance("medium-weight", "weighted-service", "192.168.1.2", 8080);
        mediumWeight.setWeight(100);
        mediumWeight.setHealthy(true);
        mediumWeight.setEnabled(true);
        
        ServiceInstance highWeight = new ServiceInstance("high-weight", "weighted-service", "192.168.1.3", 8080);
        highWeight.setWeight(200);
        highWeight.setHealthy(true);
        highWeight.setEnabled(true);
        
        weightedInstances.add(lowWeight);
        weightedInstances.add(mediumWeight);
        weightedInstances.add(highWeight);
        
        // 2. 创建负载均衡器
        AdvancedConsistentHashLoadBalancer loadBalancer = new AdvancedConsistentHashLoadBalancer(150);
        
        // 3. 进行负载均衡选择
        String serviceName = "weighted-service";
        Map<String, Integer> distribution = new HashMap<>();
        
        for (int i = 0; i < 1000; i++) {
            ServiceInstance selected = loadBalancer.select(serviceName, weightedInstances);
            if (selected != null) {
                String instanceId = selected.getId();
                distribution.put(instanceId, distribution.getOrDefault(instanceId, 0) + 1);
            }
        }
        
        logger.info("权重感知负载分布: {}", distribution);
        
        // 4. 验证权重效果
        int lowCount = distribution.getOrDefault("low-weight", 0);
        int mediumCount = distribution.getOrDefault("medium-weight", 0);
        int highCount = distribution.getOrDefault("high-weight", 0);
        
        logger.info("权重验证: low={}, medium={}, high={}", lowCount, mediumCount, highCount);
        logger.info("权重比例验证: low/medium={}, medium/high={}", 
                (double) lowCount / mediumCount, (double) mediumCount / highCount);
    }
    
    /**
     * 监控和统计示例
     */
    public void monitoringAndStatisticsExample() {
        logger.info("=== 监控和统计示例 ===");
        
        // 1. 创建负载均衡器
        AdvancedConsistentHashLoadBalancer loadBalancer = new AdvancedConsistentHashLoadBalancer(100);
        
        // 2. 创建服务实例
        List<ServiceInstance> instances = createServiceInstances();
        String serviceName = "monitoring-service";
        
        // 3. 模拟多个服务的负载均衡
        String[] services = {"user-service", "order-service", "payment-service"};
        
        for (String service : services) {
            for (int i = 0; i < 200; i++) {
                loadBalancer.select(service, instances);
            }
        }
        
        // 4. 获取所有服务的统计信息
        Map<String, Map<String, Object>> allStats = loadBalancer.getAllStatistics();
        logger.info("所有服务统计信息: {}", allStats);
        
        // 5. 获取特定服务的哈希环信息
        for (String service : services) {
            Map<String, Object> hashRingInfo = loadBalancer.getHashRingInfo(service);
            logger.info("服务 {} 的哈希环信息: {}", service, hashRingInfo);
        }
        
        // 6. 清除统计信息
        loadBalancer.clearAllStatistics();
        logger.info("统计信息已清除");
    }
    
    /**
     * 创建测试服务实例
     */
    private List<ServiceInstance> createServiceInstances() {
        List<ServiceInstance> instances = new ArrayList<>();
        
        for (int i = 1; i <= 4; i++) {
            ServiceInstance instance = new ServiceInstance(
                    "instance-" + i,
                    "test-service",
                    "192.168.1." + i,
                    8080
            );
            instance.setWeight(100);
            instance.setHealthy(true);
            instance.setEnabled(true);
            instances.add(instance);
        }
        
        return instances;
    }
    
    /**
     * 运行所有示例
     */
    public void runAllExamples() {
        try {
            basicUsageExample();
            Thread.sleep(1000);
            
            advancedUsageExample();
            Thread.sleep(1000);
            
            customHashKeyGeneratorExample();
            Thread.sleep(1000);
            
            nodeChangeConsistencyExample();
            Thread.sleep(1000);
            
            weightAwareLoadBalancingExample();
            Thread.sleep(1000);
            
            monitoringAndStatisticsExample();
            
            logger.info("所有示例运行完成");
        } catch (InterruptedException e) {
            logger.error("示例运行被中断", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("示例运行出错", e);
        }
    }
} 