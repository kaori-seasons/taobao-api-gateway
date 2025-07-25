package com.taobao.gateway.cache.eviction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 驱逐策略测试类
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class EvictionPolicyTest {
    
    private static final int MAX_SIZE = 3;
    
    @Test
    public void testLRUEvictionPolicy() {
        LRUEvictionPolicy<String, String> lruCache = new LRUEvictionPolicy<>(MAX_SIZE);
        
        // 添加数据
        lruCache.put("A", "value1");
        lruCache.put("B", "value2");
        lruCache.put("C", "value3");
        
        assertEquals(3, lruCache.size());
        assertEquals("value1", lruCache.get("A"));
        assertEquals("value2", lruCache.get("B"));
        assertEquals("value3", lruCache.get("C"));
        
        // 添加第四个元素，应该驱逐A
        lruCache.put("D", "value4");
        assertEquals(3, lruCache.size());
        assertNull(lruCache.get("A")); // A被驱逐
        assertEquals("value2", lruCache.get("B"));
        assertEquals("value3", lruCache.get("C"));
        assertEquals("value4", lruCache.get("D"));
        
        // 访问B，然后添加E，应该驱逐C
        lruCache.get("B"); // 访问B，使其变为最近使用
        lruCache.put("E", "value5");
        assertEquals(3, lruCache.size());
        assertNull(lruCache.get("C")); // C被驱逐
        assertEquals("value2", lruCache.get("B"));
        assertEquals("value4", lruCache.get("D"));
        assertEquals("value5", lruCache.get("E"));
        
        // 检查统计信息
        EvictionStats stats = lruCache.getEvictionStats();
        assertEquals(2, stats.getEvictionCount());
        assertTrue(stats.getHitCount() > 0);
    }
    
    @Test
    public void testLFUEvictionPolicy() {
        LFUEvictionPolicy<String, String> lfuCache = new LFUEvictionPolicy<>(MAX_SIZE);
        
        // 添加数据
        lfuCache.put("A", "value1");
        lfuCache.put("B", "value2");
        lfuCache.put("C", "value3");
        
        assertEquals(3, lfuCache.size());
        
        // 访问A和B多次，C只访问一次
        lfuCache.get("A");
        lfuCache.get("A");
        lfuCache.get("B");
        lfuCache.get("B");
        lfuCache.get("C"); // C只访问一次
        
        // 添加第四个元素，应该驱逐C（使用频率最低）
        lfuCache.put("D", "value4");
        assertEquals(3, lfuCache.size());
        assertNull(lfuCache.get("C")); // C被驱逐
        assertEquals("value1", lfuCache.get("A"));
        assertEquals("value2", lfuCache.get("B"));
        assertEquals("value4", lfuCache.get("D"));
        
        // 检查统计信息
        EvictionStats stats = lfuCache.getEvictionStats();
        assertEquals(1, stats.getEvictionCount());
        assertTrue(stats.getHitCount() > 0);
    }
    
    @Test
    public void testFIFOEvictionPolicy() {
        FIFOEvictionPolicy<String, String> fifoCache = new FIFOEvictionPolicy<>(MAX_SIZE);
        
        // 添加数据
        fifoCache.put("A", "value1");
        fifoCache.put("B", "value2");
        fifoCache.put("C", "value3");
        
        assertEquals(3, fifoCache.size());
        
        // 访问A，但不会改变其位置
        fifoCache.get("A");
        
        // 添加第四个元素，应该驱逐A（最早进入）
        fifoCache.put("D", "value4");
        assertEquals(3, fifoCache.size());
        assertNull(fifoCache.get("A")); // A被驱逐
        assertEquals("value2", fifoCache.get("B"));
        assertEquals("value3", fifoCache.get("C"));
        assertEquals("value4", fifoCache.get("D"));
        
        // 再次添加元素，应该驱逐B
        fifoCache.put("E", "value5");
        assertEquals(3, fifoCache.size());
        assertNull(fifoCache.get("B")); // B被驱逐
        assertEquals("value3", fifoCache.get("C"));
        assertEquals("value4", fifoCache.get("D"));
        assertEquals("value5", fifoCache.get("E"));
        
        // 检查统计信息
        EvictionStats stats = fifoCache.getEvictionStats();
        assertEquals(2, stats.getEvictionCount());
        assertTrue(stats.getHitCount() > 0);
    }
    
    @Test
    public void testRandomEvictionPolicy() {
        RandomEvictionPolicy<String, String> randomCache = new RandomEvictionPolicy<>(MAX_SIZE);
        
        // 添加数据
        randomCache.put("A", "value1");
        randomCache.put("B", "value2");
        randomCache.put("C", "value3");
        
        assertEquals(3, randomCache.size());
        
        // 添加第四个元素，随机驱逐一个
        randomCache.put("D", "value4");
        assertEquals(3, randomCache.size());
        
        // 检查统计信息
        EvictionStats stats = randomCache.getEvictionStats();
        assertEquals(1, stats.getEvictionCount());
        assertTrue(stats.getHitCount() >= 0);
    }
    
    @Test
    public void testBatchOperations() {
        LRUEvictionPolicy<String, String> cache = new LRUEvictionPolicy<>(MAX_SIZE);
        
        // 批量设置
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("A", "value1");
        data.put("B", "value2");
        data.put("C", "value3");
        cache.putAll(data);
        
        assertEquals(3, cache.size());
        
        // 批量获取
        java.util.List<String> keys = java.util.Arrays.asList("A", "B", "C");
        java.util.Map<String, String> result = cache.getAll(keys);
        assertEquals(3, result.size());
        assertEquals("value1", result.get("A"));
        assertEquals("value2", result.get("B"));
        assertEquals("value3", result.get("C"));
        
        // 批量删除
        cache.removeAll(java.util.Arrays.asList("A", "B"));
        assertEquals(1, cache.size());
        assertNull(cache.get("A"));
        assertNull(cache.get("B"));
        assertEquals("value3", cache.get("C"));
    }
    
    @Test
    public void testUpdateExistingKey() {
        LRUEvictionPolicy<String, String> cache = new LRUEvictionPolicy<>(MAX_SIZE);
        
        // 添加数据
        cache.put("A", "value1");
        assertEquals("value1", cache.get("A"));
        
        // 更新现有键
        cache.put("A", "value1_updated");
        assertEquals("value1_updated", cache.get("A"));
        assertEquals(1, cache.size()); // 大小不变
        
        // 检查统计信息
        EvictionStats stats = cache.getEvictionStats();
        assertEquals(0, stats.getEvictionCount()); // 没有驱逐
        assertTrue(stats.getPutCount() > 0);
    }
    
    @Test
    public void testClearCache() {
        LRUEvictionPolicy<String, String> cache = new LRUEvictionPolicy<>(MAX_SIZE);
        
        // 添加数据
        cache.put("A", "value1");
        cache.put("B", "value2");
        cache.put("C", "value3");
        
        assertEquals(3, cache.size());
        
        // 清空缓存
        cache.clear();
        assertEquals(0, cache.size());
        assertNull(cache.get("A"));
        assertNull(cache.get("B"));
        assertNull(cache.get("C"));
    }
    
    @Test
    public void testContainsKey() {
        LRUEvictionPolicy<String, String> cache = new LRUEvictionPolicy<>(MAX_SIZE);
        
        assertFalse(cache.containsKey("A"));
        
        cache.put("A", "value1");
        assertTrue(cache.containsKey("A"));
        
        cache.remove("A");
        assertFalse(cache.containsKey("A"));
    }
    
    @Test
    public void testMaxSizeUpdate() {
        LRUEvictionPolicy<String, String> cache = new LRUEvictionPolicy<>(2);
        
        // 添加数据
        cache.put("A", "value1");
        cache.put("B", "value2");
        cache.put("C", "value3"); // 会驱逐A
        
        assertEquals(2, cache.size());
        assertNull(cache.get("A"));
        
        // 增加最大容量
        cache.setMaxSize(3);
        cache.put("D", "value4");
        assertEquals(3, cache.size());
        assertEquals("value2", cache.get("B"));
        assertEquals("value3", cache.get("C"));
        assertEquals("value4", cache.get("D"));
    }
}