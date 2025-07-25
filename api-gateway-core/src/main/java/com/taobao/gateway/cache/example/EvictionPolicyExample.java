package com.taobao.gateway.cache.example;

import com.taobao.gateway.cache.CacheConfig;
import com.taobao.gateway.cache.CacheService;
import com.taobao.gateway.cache.eviction.EvictionPolicyFactory;
import com.taobao.gateway.cache.eviction.EvictionPolicy;
import com.taobao.gateway.cache.eviction.EvictionStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

/**
 * 驱逐策略使用示例
 * 展示如何使用不同的驱逐策略
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class EvictionPolicyExample {

    private static final Logger logger = LoggerFactory.getLogger(EvictionPolicyExample.class);

    @Autowired
    private CacheService cacheService;

    /**
     * LRU驱逐策略示例
     */
    public void lruExample() {
        logger.info("=== LRU驱逐策略示例 ===");

        // 创建LRU缓存配置
        CacheConfig config = new CacheConfig("lru-cache");
        config.setL1Enabled(true);
        config.setL2Enabled(false);
        config.setL1MaxSize(3);
        config.setEvictionPolicy(CacheConfig.CacheEvictionPolicy.LRU);

        // 创建缓存
        var cache = cacheService.createCache("lru-cache", config);

        // 添加数据
        cache.put("A", "value1");
        cache.put("B", "value2");
        cache.put("C", "value3");

        logger.info("初始状态: size={}", cache.size());

        // 访问A，使其变为最近使用
        cache.get("A");

        // 添加D，应该驱逐B（最近最少使用）
        cache.put("D", "value4");

        logger.info("添加D后: A={}, B={}, C={}, D={}", 
                cache.get("A"), cache.get("B"), cache.get("C"), cache.get("D"));

        // 检查统计信息
        var stats = cache.getStats();
        logger.info("LRU缓存统计: {}", stats);
    }

    /**
     * LFU驱逐策略示例
     */
    public void lfuExample() {
        logger.info("=== LFU驱逐策略示例 ===");

        // 创建LFU缓存配置
        CacheConfig config = new CacheConfig("lfu-cache");
        config.setL1Enabled(true);
        config.setL2Enabled(false);
        config.setL1MaxSize(3);
        config.setEvictionPolicy(CacheConfig.CacheEvictionPolicy.LFU);

        // 创建缓存
        var cache = cacheService.createCache("lfu-cache", config);

        // 添加数据
        cache.put("A", "value1");
        cache.put("B", "value2");
        cache.put("C", "value3");

        logger.info("初始状态: size={}", cache.size());

        // 访问A和B多次，C只访问一次
        cache.get("A");
        cache.get("A");
        cache.get("B");
        cache.get("B");
        cache.get("C"); // C只访问一次

        // 添加D，应该驱逐C（使用频率最低）
        cache.put("D", "value4");

        logger.info("添加D后: A={}, B={}, C={}, D={}", 
                cache.get("A"), cache.get("B"), cache.get("C"), cache.get("D"));

        // 检查统计信息
        var stats = cache.getStats();
        logger.info("LFU缓存统计: {}", stats);
    }

    /**
     * FIFO驱逐策略示例
     */
    public void fifoExample() {
        logger.info("=== FIFO驱逐策略示例 ===");

        // 创建FIFO缓存配置
        CacheConfig config = new CacheConfig("fifo-cache");
        config.setL1Enabled(true);
        config.setL2Enabled(false);
        config.setL1MaxSize(3);
        config.setEvictionPolicy(CacheConfig.CacheEvictionPolicy.FIFO);

        // 创建缓存
        var cache = cacheService.createCache("fifo-cache", config);

        // 添加数据
        cache.put("A", "value1");
        cache.put("B", "value2");
        cache.put("C", "value3");

        logger.info("初始状态: size={}", cache.size());

        // 访问A，但不会改变其位置
        cache.get("A");

        // 添加D，应该驱逐A（最早进入）
        cache.put("D", "value4");

        logger.info("添加D后: A={}, B={}, C={}, D={}", 
                cache.get("A"), cache.get("B"), cache.get("C"), cache.get("D"));

        // 检查统计信息
        var stats = cache.getStats();
        logger.info("FIFO缓存统计: {}", stats);
    }

    /**
     * RANDOM驱逐策略示例
     */
    public void randomExample() {
        logger.info("=== RANDOM驱逐策略示例 ===");

        // 创建RANDOM缓存配置
        CacheConfig config = new CacheConfig("random-cache");
        config.setL1Enabled(true);
        config.setL2Enabled(false);
        config.setL1MaxSize(3);
        config.setEvictionPolicy(CacheConfig.CacheEvictionPolicy.RANDOM);

        // 创建缓存
        var cache = cacheService.createCache("random-cache", config);

        // 添加数据
        cache.put("A", "value1");
        cache.put("B", "value2");
        cache.put("C", "value3");

        logger.info("初始状态: size={}", cache.size());

        // 添加D，随机驱逐一个
        cache.put("D", "value4");

        logger.info("添加D后: A={}, B={}, C={}, D={}", 
                cache.get("A"), cache.get("B"), cache.get("C"), cache.get("D"));

        // 检查统计信息
        var stats = cache.getStats();
        logger.info("RANDOM缓存统计: {}", stats);
    }

    /**
     * 性能对比示例
     */
    public void performanceComparison() {
        logger.info("=== 驱逐策略性能对比示例 ===");

        String[] policies = {"LRU", "LFU", "FIFO", "RANDOM"};
        int maxSize = 1000;
        int operations = 10000;

        for (String policyName : policies) {
            CacheConfig.CacheEvictionPolicy policy = CacheConfig.CacheEvictionPolicy.valueOf(policyName);
            
            // 创建缓存配置
            CacheConfig config = new CacheConfig(policyName.toLowerCase() + "-cache");
            config.setL1Enabled(true);
            config.setL2Enabled(false);
            config.setL1MaxSize(maxSize);
            config.setEvictionPolicy(policy);

            // 创建缓存
            var cache = cacheService.createCache(config.getName(), config);

            // 性能测试
            long startTime = System.currentTimeMillis();
            
            Random random = new Random();
            for (int i = 0; i < operations; i++) {
                String key = "key" + random.nextInt(maxSize * 2);
                String value = "value" + i;
                
                if (random.nextBoolean()) {
                    // 50%概率进行写操作
                    cache.put(key, value);
                } else {
                    // 50%概率进行读操作
                    cache.get(key);
                }
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 获取统计信息
            var stats = cache.getStats();
            var evictionStats = getEvictionStats(cache);

            logger.info("{}策略性能测试:", policyName);
            logger.info("  操作次数: {}", operations);
            logger.info("  耗时: {}ms", duration);
            logger.info("  平均操作时间: {}ms", (double) duration / operations);
            logger.info("  命中率: {:.2f}%", stats.getHitRate() * 100);
            logger.info("  驱逐次数: {}", evictionStats.getEvictionCount());
            logger.info("  最终大小: {}", cache.size());
        }
    }

    /**
     * 批量操作示例
     */
    public void batchOperationsExample() {
        logger.info("=== 批量操作示例 ===");

        // 创建缓存
        CacheConfig config = new CacheConfig("batch-cache");
        config.setL1Enabled(true);
        config.setL2Enabled(false);
        config.setL1MaxSize(100);
        config.setEvictionPolicy(CacheConfig.CacheEvictionPolicy.LRU);

        var cache = cacheService.createCache("batch-cache", config);

        // 批量设置
        Map<String, String> data = new java.util.HashMap<>();
        for (int i = 0; i < 50; i++) {
            data.put("key" + i, "value" + i);
        }
        cache.putAll(data);

        logger.info("批量设置后: size={}", cache.size());

        // 批量获取
        var keys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        var result = cache.getAll(keys);

        logger.info("批量获取结果: {}", result);

        // 批量删除
        cache.removeAll(Arrays.asList("key1", "key2", "key3"));

        logger.info("批量删除后: size={}", cache.size());
        logger.info("key1存在: {}", cache.containsKey("key1"));
        logger.info("key4存在: {}", cache.containsKey("key4"));
    }

    /**
     * 获取驱逐统计信息
     */
    private EvictionStats getEvictionStats(com.taobao.gateway.cache.Cache<?, ?> cache) {
        // 这里需要根据具体的缓存实现来获取驱逐统计信息
        // 由于接口限制，这里返回一个空的统计信息
        return new EvictionStats();
    }

    /**
     * 运行所有示例
     */
    public void runAllExamples() {
        logger.info("开始运行驱逐策略示例...");

        try {
            lruExample();
            Thread.sleep(1000);

            lfuExample();
            Thread.sleep(1000);

            fifoExample();
            Thread.sleep(1000);

            randomExample();
            Thread.sleep(1000);

            performanceComparison();
            Thread.sleep(1000);

            batchOperationsExample();

            logger.info("所有驱逐策略示例运行完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("示例运行被中断", e);
        } catch (Exception e) {
            logger.error("示例运行出错", e);
        }
    }
}