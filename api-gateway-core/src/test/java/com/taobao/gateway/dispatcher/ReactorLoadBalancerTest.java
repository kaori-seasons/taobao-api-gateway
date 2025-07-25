package com.taobao.gateway.dispatcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ReactorLoadBalancer测试类
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
public class ReactorLoadBalancerTest {

    private ReactorLoadBalancer loadBalancer;
    private MultiReactorDispatcherServer.ReactorInstance reactor1;
    private MultiReactorDispatcherServer.ReactorInstance reactor2;
    private MultiReactorDispatcherServer.ReactorInstance reactor3;

    @BeforeEach
    public void setUp() {
        loadBalancer = new ReactorLoadBalancer();
        
        // 创建模拟的Reactor实例
        reactor1 = createMockReactor("reactor-1", 100);
        reactor2 = createMockReactor("reactor-2", 200);
        reactor3 = createMockReactor("reactor-3", 300);
    }

    /**
     * 创建模拟的Reactor实例
     */
    private MultiReactorDispatcherServer.ReactorInstance createMockReactor(String id, int weight) {
        return new MultiReactorDispatcherServer.ReactorInstance(
                id, 8080, null, null, null, weight
        );
    }

    @Test
    public void testRoundRobinStrategy() {
        loadBalancer.setStrategy(ReactorLoadBalancer.LoadBalanceStrategy.ROUND_ROBIN);
        
        loadBalancer.addReactor(reactor1);
        loadBalancer.addReactor(reactor2);
        loadBalancer.addReactor(reactor3);

        // 测试轮询选择
        assertEquals("reactor-1", loadBalancer.select().getId());
        assertEquals("reactor-2", loadBalancer.select().getId());
        assertEquals("reactor-3", loadBalancer.select().getId());
        assertEquals("reactor-1", loadBalancer.select().getId()); // 回到第一个
    }

    @Test
    public void testLeastConnectionsStrategy() {
        loadBalancer.setStrategy(ReactorLoadBalancer.LoadBalanceStrategy.LEAST_CONNECTIONS);
        
        loadBalancer.addReactor(reactor1);
        loadBalancer.addReactor(reactor2);
        loadBalancer.addReactor(reactor3);

        // 设置不同的连接数
        reactor1.incrementConnections(); // 1
        reactor2.incrementConnections(); // 1
        reactor2.incrementConnections(); // 2
        reactor3.incrementConnections(); // 1
        reactor3.incrementConnections(); // 2
        reactor3.incrementConnections(); // 3

        // 应该选择连接数最少的reactor1
        assertEquals("reactor-1", loadBalancer.select().getId());
    }

    @Test
    public void testWeightedRoundRobinStrategy() {
        loadBalancer.setStrategy(ReactorLoadBalancer.LoadBalanceStrategy.WEIGHTED_ROUND_ROBIN);
        
        loadBalancer.addReactor(reactor1); // 权重100
        loadBalancer.addReactor(reactor2); // 权重200
        loadBalancer.addReactor(reactor3); // 权重300

        // 统计选择结果
        Map<String, Integer> selectionCount = new HashMap<>();
        int totalSelections = 600; // 总权重

        for (int i = 0; i < totalSelections; i++) {
            String selectedId = loadBalancer.select().getId();
            selectionCount.put(selectedId, selectionCount.getOrDefault(selectedId, 0) + 1);
        }

        // 验证权重分配
        assertTrue(selectionCount.get("reactor-1") > 0);
        assertTrue(selectionCount.get("reactor-2") > 0);
        assertTrue(selectionCount.get("reactor-3") > 0);
        
        // reactor3的权重最高，应该被选择最多
        assertTrue(selectionCount.get("reactor-3") >= selectionCount.get("reactor-2"));
        assertTrue(selectionCount.get("reactor-2") >= selectionCount.get("reactor-1"));
    }

    @Test
    public void testWeightedRandomStrategy() {
        loadBalancer.setStrategy(ReactorLoadBalancer.LoadBalanceStrategy.WEIGHTED_RANDOM);
        
        loadBalancer.addReactor(reactor1); // 权重100
        loadBalancer.addReactor(reactor2); // 权重200
        loadBalancer.addReactor(reactor3); // 权重300

        // 统计选择结果
        Map<String, Integer> selectionCount = new HashMap<>();
        int totalSelections = 1000;

        for (int i = 0; i < totalSelections; i++) {
            String selectedId = loadBalancer.select().getId();
            selectionCount.put(selectedId, selectionCount.getOrDefault(selectedId, 0) + 1);
        }

        // 验证所有Reactor都被选择过
        assertTrue(selectionCount.get("reactor-1") > 0);
        assertTrue(selectionCount.get("reactor-2") > 0);
        assertTrue(selectionCount.get("reactor-3") > 0);
    }

    @Test
    public void testConsistentHashStrategy() {
        loadBalancer.setStrategy(ReactorLoadBalancer.LoadBalanceStrategy.CONSISTENT_HASH);
        
        loadBalancer.addReactor(reactor1);
        loadBalancer.addReactor(reactor2);
        loadBalancer.addReactor(reactor3);

        // 测试一致性哈希
        String clientIp1 = "192.168.1.1";
        String clientIp2 = "192.168.1.2";
        
        String selected1 = loadBalancer.selectByClientIp(clientIp1).getId();
        String selected2 = loadBalancer.selectByClientIp(clientIp2).getId();
        
        // 相同IP应该总是选择相同的Reactor
        assertEquals(selected1, loadBalancer.selectByClientIp(clientIp1).getId());
        assertEquals(selected2, loadBalancer.selectByClientIp(clientIp2).getId());
    }

    @Test
    public void testRandomStrategy() {
        loadBalancer.setStrategy(ReactorLoadBalancer.LoadBalanceStrategy.RANDOM);
        
        loadBalancer.addReactor(reactor1);
        loadBalancer.addReactor(reactor2);
        loadBalancer.addReactor(reactor3);

        // 统计选择结果
        Map<String, Integer> selectionCount = new HashMap<>();
        int totalSelections = 1000;

        for (int i = 0; i < totalSelections; i++) {
            String selectedId = loadBalancer.select().getId();
            selectionCount.put(selectedId, selectionCount.getOrDefault(selectedId, 0) + 1);
        }

        // 验证所有Reactor都被选择过
        assertTrue(selectionCount.get("reactor-1") > 0);
        assertTrue(selectionCount.get("reactor-2") > 0);
        assertTrue(selectionCount.get("reactor-3") > 0);
    }

    @Test
    public void testEmptyLoadBalancer() {
        // 测试空负载均衡器
        assertNull(loadBalancer.select());
        assertEquals(0, loadBalancer.getReactorCount());
        
        ReactorLoadBalancer.LoadBalanceStats stats = loadBalancer.getStats();
        assertEquals(0, stats.getReactorCount());
        assertEquals(0, stats.getTotalConnections());
    }

    @Test
    public void testAddAndRemoveReactor() {
        loadBalancer.addReactor(reactor1);
        assertEquals(1, loadBalancer.getReactorCount());
        
        loadBalancer.addReactor(reactor2);
        assertEquals(2, loadBalancer.getReactorCount());
        
        loadBalancer.removeReactor("reactor-1");
        assertEquals(1, loadBalancer.getReactorCount());
        assertEquals("reactor-2", loadBalancer.select().getId());
        
        loadBalancer.clear();
        assertEquals(0, loadBalancer.getReactorCount());
        assertNull(loadBalancer.select());
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        loadBalancer.addReactor(reactor1);
        loadBalancer.addReactor(reactor2);
        loadBalancer.addReactor(reactor3);
        
        int threadCount = 10;
        int selectionsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger totalSelections = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < selectionsPerThread; j++) {
                        MultiReactorDispatcherServer.ReactorInstance selected = loadBalancer.select();
                        assertNotNull(selected);
                        totalSelections.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        assertEquals(threadCount * selectionsPerThread, totalSelections.get());
    }

    @Test
    public void testLoadBalanceStats() {
        loadBalancer.addReactor(reactor1);
        loadBalancer.addReactor(reactor2);
        loadBalancer.addReactor(reactor3);
        
        // 设置不同的连接数
        reactor1.incrementConnections();
        reactor1.incrementConnections(); // 2
        reactor2.incrementConnections(); // 1
        reactor3.incrementConnections();
        reactor3.incrementConnections();
        reactor3.incrementConnections(); // 3
        
        ReactorLoadBalancer.LoadBalanceStats stats = loadBalancer.getStats();
        
        assertEquals(ReactorLoadBalancer.LoadBalanceStrategy.ROUND_ROBIN, stats.getStrategy());
        assertEquals(3, stats.getReactorCount());
        assertEquals(6, stats.getTotalConnections());
        assertEquals(2, stats.getAverageConnections());
        assertEquals(1, stats.getMinConnections());
        assertEquals(3, stats.getMaxConnections());
    }

    @Test
    public void testStrategyChange() {
        loadBalancer.addReactor(reactor1);
        loadBalancer.addReactor(reactor2);
        
        // 测试轮询策略
        loadBalancer.setStrategy(ReactorLoadBalancer.LoadBalanceStrategy.ROUND_ROBIN);
        assertEquals("reactor-1", loadBalancer.select().getId());
        assertEquals("reactor-2", loadBalancer.select().getId());
        
        // 切换到随机策略
        loadBalancer.setStrategy(ReactorLoadBalancer.LoadBalanceStrategy.RANDOM);
        assertNotNull(loadBalancer.select());
        assertNotNull(loadBalancer.select());
        
        // 验证策略已更改
        ReactorLoadBalancer.LoadBalanceStats stats = loadBalancer.getStats();
        assertEquals(ReactorLoadBalancer.LoadBalanceStrategy.RANDOM, stats.getStrategy());
    }
}