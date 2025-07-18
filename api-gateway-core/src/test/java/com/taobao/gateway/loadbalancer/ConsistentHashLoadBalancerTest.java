package com.taobao.gateway.loadbalancer;

import com.taobao.gateway.loadbalancer.impl.ConsistentHashLoadBalancer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 一致性哈希负载均衡器测试类
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class ConsistentHashLoadBalancerTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ConsistentHashLoadBalancerTest.class);
    
    private ConsistentHashLoadBalancer loadBalancer;
    private List<ServiceInstance> instances;
    
    @BeforeEach
    void setUp() {
        // 创建一致性哈希负载均衡器，使用较少的虚拟节点便于测试
        loadBalancer = new ConsistentHashLoadBalancer(50);
        
        // 创建测试服务实例
        instances = new ArrayList<>();
        instances.add(createServiceInstance("instance-1", "service-1", "192.168.1.1", 8080, 100));
        instances.add(createServiceInstance("instance-2", "service-1", "192.168.1.2", 8080, 100));
        instances.add(createServiceInstance("instance-3", "service-1", "192.168.1.3", 8080, 100));
        instances.add(createServiceInstance("instance-4", "service-1", "192.168.1.4", 8080, 100));
    }
    
    @Test
    void testBasicLoadBalancing() {
        logger.info("测试基本负载均衡功能");
        
        String serviceName = "test-service";
        Map<String, Integer> distribution = new HashMap<>();
        
        // 进行多次选择，统计分布情况
        for (int i = 0; i < 1000; i++) {
            String requestKey = "request-" + i;
            ServiceInstance selected = loadBalancer.select(serviceName, instances, requestKey);
            assertNotNull(selected, "应该能选择到服务实例");
            
            String instanceId = selected.getId();
            distribution.put(instanceId, distribution.getOrDefault(instanceId, 0) + 1);
        }
        
        // 验证所有实例都被选择到
        assertEquals(4, distribution.size(), "应该选择到所有4个实例");
        
        // 输出分布情况
        logger.info("负载分布情况: {}", distribution);
        
        // 验证分布相对均匀（允许一定的偏差）
        int minCount = distribution.values().stream().mapToInt(Integer::intValue).min().orElse(0);
        int maxCount = distribution.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        double ratio = (double) minCount / maxCount;
        
        assertTrue(ratio > 0.5, "负载分布应该相对均匀，最小/最大比例应该大于0.5，实际: " + ratio);
        logger.info("负载分布比例: {}/{} = {}", minCount, maxCount, ratio);
    }
    
    @Test
    void testConsistency() {
        logger.info("测试一致性特性");
        
        String serviceName = "consistency-test";
        Map<String, ServiceInstance> requestToInstance = new HashMap<>();
        
        // 第一轮选择
        for (int i = 0; i < 100; i++) {
            String requestKey = "request-" + i;
            ServiceInstance selected = loadBalancer.select(serviceName, instances, requestKey);
            requestToInstance.put(requestKey, selected);
        }
        
        // 第二轮选择（实例列表相同）
        int consistentCount = 0;
        for (int i = 0; i < 100; i++) {
            String requestKey = "request-" + i;
            ServiceInstance selected = loadBalancer.select(serviceName, instances, requestKey);
            ServiceInstance previous = requestToInstance.get(requestKey);
            
            if (selected.equals(previous)) {
                consistentCount++;
            }
        }
        
        // 验证一致性（应该100%一致，因为实例列表没有变化）
        assertEquals(100, consistentCount, "实例列表相同时，选择结果应该完全一致");
        logger.info("一致性测试通过，100个请求中有{}个保持一致", consistentCount);
    }
    
    @Test
    void testNodeRemoval() {
        logger.info("测试节点移除的一致性");
        
        String serviceName = "node-removal-test";
        Map<String, ServiceInstance> beforeRemoval = new HashMap<>();
        
        // 记录移除节点前的选择结果
        for (int i = 0; i < 100; i++) {
            String requestKey = "request-" + i;
            ServiceInstance selected = loadBalancer.select(serviceName, instances, requestKey);
            beforeRemoval.put(requestKey, selected);
        }
        
        // 移除一个节点
        List<ServiceInstance> reducedInstances = new ArrayList<>(instances);
        reducedInstances.remove(0); // 移除第一个实例
        
        // 记录移除节点后的选择结果
        Map<String, ServiceInstance> afterRemoval = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            String requestKey = "request-" + i;
            ServiceInstance selected = loadBalancer.select(serviceName, reducedInstances, requestKey);
            afterRemoval.put(requestKey, selected);
        }
        
        // 统计需要重新分配的比例
        int reallocatedCount = 0;
        for (int i = 0; i < 100; i++) {
            String requestKey = "request-" + i;
            ServiceInstance before = beforeRemoval.get(requestKey);
            ServiceInstance after = afterRemoval.get(requestKey);
            
            if (!before.equals(after)) {
                reallocatedCount++;
            }
        }
        
        // 验证重新分配的比例应该在合理范围内（一致性哈希的优势）
        double reallocationRatio = (double) reallocatedCount / 100;
        logger.info("节点移除后重新分配比例: {}/100 = {}", reallocatedCount, reallocationRatio);
        
        // 理论上，移除1个节点时，重新分配比例应该接近 1/4 = 25%
        // 但由于虚拟节点的存在，实际比例可能有所不同
        assertTrue(reallocationRatio < 0.5, "重新分配比例应该小于50%，实际: " + reallocationRatio);
    }
    
    @Test
    void testNodeAddition() {
        logger.info("测试节点添加的一致性");
        
        String serviceName = "node-addition-test";
        Map<String, ServiceInstance> beforeAddition = new HashMap<>();
        
        // 记录添加节点前的选择结果
        for (int i = 0; i < 100; i++) {
            String requestKey = "request-" + i;
            ServiceInstance selected = loadBalancer.select(serviceName, instances, requestKey);
            beforeAddition.put(requestKey, selected);
        }
        
        // 添加一个新节点
        List<ServiceInstance> expandedInstances = new ArrayList<>(instances);
        expandedInstances.add(createServiceInstance("instance-5", "service-1", "192.168.1.5", 8080, 100));
        
        // 记录添加节点后的选择结果
        Map<String, ServiceInstance> afterAddition = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            String requestKey = "request-" + i;
            ServiceInstance selected = loadBalancer.select(serviceName, expandedInstances, requestKey);
            afterAddition.put(requestKey, selected);
        }
        
        // 统计需要重新分配的比例
        int reallocatedCount = 0;
        for (int i = 0; i < 100; i++) {
            String requestKey = "request-" + i;
            ServiceInstance before = beforeAddition.get(requestKey);
            ServiceInstance after = afterAddition.get(requestKey);
            
            if (!before.equals(after)) {
                reallocatedCount++;
            }
        }
        
        double reallocationRatio = (double) reallocatedCount / 100;
        logger.info("节点添加后重新分配比例: {}/100 = {}", reallocatedCount, reallocationRatio);
        
        // 验证重新分配的比例应该在合理范围内
        assertTrue(reallocationRatio < 0.5, "重新分配比例应该小于50%，实际: " + reallocationRatio);
    }
    
    @Test
    void testWeightedDistribution() {
        logger.info("测试权重感知的负载分布");
        
        // 创建不同权重的实例
        List<ServiceInstance> weightedInstances = new ArrayList<>();
        weightedInstances.add(createServiceInstance("weight-1", "weighted-service", "192.168.1.1", 8080, 50));
        weightedInstances.add(createServiceInstance("weight-2", "weighted-service", "192.168.1.2", 8080, 100));
        weightedInstances.add(createServiceInstance("weight-3", "weighted-service", "192.168.1.3", 8080, 200));
        
        String serviceName = "weighted-test";
        Map<String, Integer> distribution = new HashMap<>();
        
        // 进行多次选择
        for (int i = 0; i < 1000; i++) {
            String requestKey = "request-" + i;
            ServiceInstance selected = loadBalancer.select(serviceName, weightedInstances, requestKey);
            String instanceId = selected.getId();
            distribution.put(instanceId, distribution.getOrDefault(instanceId, 0) + 1);
        }
        
        logger.info("权重分布情况: {}", distribution);
        
        // 验证权重较高的实例被选择次数更多
        int weight1Count = distribution.getOrDefault("weight-1", 0);
        int weight2Count = distribution.getOrDefault("weight-2", 0);
        int weight3Count = distribution.getOrDefault("weight-3", 0);
        
        // 权重比例为 1:2:4，选择次数应该大致符合这个比例
        assertTrue(weight3Count > weight2Count, "权重最高的实例应该被选择次数最多");
        assertTrue(weight2Count > weight1Count, "权重中等的实例应该被选择次数多于权重最低的实例");
        
        logger.info("权重验证通过: weight-1={}, weight-2={}, weight-3={}", weight1Count, weight2Count, weight3Count);
    }
    
    @Test
    void testEmptyInstanceList() {
        logger.info("测试空实例列表的处理");
        
        String serviceName = "empty-test";
        ServiceInstance selected = loadBalancer.select(serviceName, new ArrayList<>(), "request-0");
        
        assertNull(selected, "空实例列表应该返回null");
    }
    
    @Test
    void testUnhealthyInstances() {
        logger.info("测试不健康实例的过滤");
        
        // 创建包含不健康实例的列表
        List<ServiceInstance> mixedInstances = new ArrayList<>();
        ServiceInstance healthy1 = createServiceInstance("healthy-1", "mixed-service", "192.168.1.1", 8080, 100);
        ServiceInstance healthy2 = createServiceInstance("healthy-2", "mixed-service", "192.168.1.2", 8080, 100);
        ServiceInstance unhealthy = createServiceInstance("unhealthy-1", "mixed-service", "192.168.1.3", 8080, 100);
        unhealthy.setHealthy(false);
        
        mixedInstances.add(healthy1);
        mixedInstances.add(healthy2);
        mixedInstances.add(unhealthy);
        
        String serviceName = "mixed-test";
        Map<String, Integer> distribution = new HashMap<>();
        
        // 进行多次选择
        for (int i = 0; i < 100; i++) {
            String requestKey = "request-" + i;
            ServiceInstance selected = loadBalancer.select(serviceName, mixedInstances, requestKey);
            assertNotNull(selected, "应该能选择到健康的服务实例");
            assertTrue(selected.isHealthy(), "选择的实例应该是健康的");
            
            String instanceId = selected.getId();
            distribution.put(instanceId, distribution.getOrDefault(instanceId, 0) + 1);
        }
        
        // 验证只选择了健康的实例
        assertEquals(2, distribution.size(), "应该只选择到2个健康实例");
        assertFalse(distribution.containsKey("unhealthy-1"), "不应该选择到不健康的实例");
        
        logger.info("健康实例过滤测试通过，分布: {}", distribution);
    }
    
    /**
     * 创建服务实例的辅助方法
     */
    private ServiceInstance createServiceInstance(String id, String serviceName, String host, int port, int weight) {
        ServiceInstance instance = new ServiceInstance(id, serviceName, host, port);
        instance.setWeight(weight);
        instance.setHealthy(true);
        instance.setEnabled(true);
        return instance;
    }
} 