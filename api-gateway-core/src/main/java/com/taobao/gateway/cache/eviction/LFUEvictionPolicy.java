package com.taobao.gateway.cache.eviction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * LFU（最近最常使用）驱逐策略实现
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class LFUEvictionPolicy<K, V> implements EvictionPolicy<K, V> {
    
    private static final Logger logger = LoggerFactory.getLogger(LFUEvictionPolicy.class);
    
    /**
     * 最大容量
     */
    private long maxSize;
    
    /**
     * 缓存映射
     */
    private final Map<K, CacheEntry<V>> cache;
    
    /**
     * 频率映射（频率 -> 键集合）
     */
    private final Map<Long, java.util.Set<K>> frequencyMap;
    
    /**
     * 最小频率
     */
    private long minFrequency = 1;
    
    /**
     * 读写锁，保证线程安全
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * 驱逐统计信息
     */
    private final EvictionStats stats = new EvictionStats();
    
    /**
     * 构造函数
     * 
     * @param maxSize 最大容量
     */
    public LFUEvictionPolicy(long maxSize) {
        this.maxSize = maxSize;
        this.cache = new ConcurrentHashMap<>();
        this.frequencyMap = new ConcurrentHashMap<>();
        this.frequencyMap.put(1L, ConcurrentHashMap.newKeySet());
    }
    
    @Override
    public V get(K key) {
        lock.readLock().lock();
        try {
            CacheEntry<V> entry = cache.get(key);
            if (entry != null) {
                // 增加访问频率
                incrementFrequency(key, entry);
                stats.incrementHitCount();
                logger.debug("LFU缓存命中: key={}, frequency={}", key, entry.frequency);
                return entry.value;
            } else {
                stats.incrementMissCount();
                logger.debug("LFU缓存未命中: key={}", key);
                return null;
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void put(K key, V value) {
        lock.writeLock().lock();
        try {
            // 检查是否需要驱逐
            if (cache.size() >= maxSize && !cache.containsKey(key)) {
                evict();
            }
            
            CacheEntry<V> entry = cache.get(key);
            if (entry != null) {
                // 更新现有值
                entry.value = value;
                incrementFrequency(key, entry);
            } else {
                // 添加新值
                entry = new CacheEntry<>(value, 1);
                cache.put(key, entry);
                frequencyMap.get(1L).add(key);
            }
            
            stats.incrementPutCount();
            logger.debug("LFU缓存设置: key={}, frequency={}", key, entry.frequency);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public V remove(K key) {
        lock.writeLock().lock();
        try {
            CacheEntry<V> entry = cache.remove(key);
            if (entry != null) {
                // 从频率映射中移除
                frequencyMap.get(entry.frequency).remove(key);
                if (frequencyMap.get(entry.frequency).isEmpty()) {
                    frequencyMap.remove(entry.frequency);
                    // 更新最小频率
                    if (entry.frequency == minFrequency && !frequencyMap.isEmpty()) {
                        minFrequency = frequencyMap.keySet().stream().min(Long::compareTo).orElse(1L);
                    }
                }
                stats.incrementRemoveCount();
                logger.debug("LFU缓存删除: key={}", key);
                return entry.value;
            }
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        lock.readLock().lock();
        try {
            Map<K, V> result = new java.util.HashMap<>();
            for (K key : keys) {
                CacheEntry<V> entry = cache.get(key);
                if (entry != null) {
                    result.put(key, entry.value);
                    incrementFrequency(key, entry);
                    stats.incrementHitCount();
                } else {
                    stats.incrementMissCount();
                }
            }
            logger.debug("LFU缓存批量获取: keys={}, hits={}", keys.size(), result.size());
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void putAll(Map<K, V> map) {
        lock.writeLock().lock();
        try {
            for (Map.Entry<K, V> entry : map.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
            logger.debug("LFU缓存批量设置: size={}", map.size());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void removeAll(Collection<K> keys) {
        lock.writeLock().lock();
        try {
            for (K key : keys) {
                remove(key);
            }
            logger.debug("LFU缓存批量删除: keys={}", keys.size());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public boolean containsKey(K key) {
        lock.readLock().lock();
        try {
            return cache.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public long size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
            frequencyMap.clear();
            frequencyMap.put(1L, ConcurrentHashMap.newKeySet());
            minFrequency = 1;
            logger.info("LFU缓存清空");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public long getMaxSize() {
        return maxSize;
    }
    
    @Override
    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }
    
    @Override
    public EvictionStats getEvictionStats() {
        return stats;
    }
    
    /**
     * 增加访问频率
     */
    private void incrementFrequency(K key, CacheEntry<V> entry) {
        // 从当前频率映射中移除
        frequencyMap.get(entry.frequency).remove(key);
        
        // 如果当前频率映射为空，移除它
        if (frequencyMap.get(entry.frequency).isEmpty()) {
            frequencyMap.remove(entry.frequency);
            // 更新最小频率
            if (entry.frequency == minFrequency && !frequencyMap.isEmpty()) {
                minFrequency = frequencyMap.keySet().stream().min(Long::compareTo).orElse(1L);
            }
        }
        
        // 增加频率
        entry.frequency++;
        
        // 添加到新的频率映射
        frequencyMap.computeIfAbsent(entry.frequency, k -> ConcurrentHashMap.newKeySet()).add(key);
    }
    
    /**
     * 驱逐最少使用的项
     */
    private void evict() {
        java.util.Set<K> minFreqKeys = frequencyMap.get(minFrequency);
        if (minFreqKeys != null && !minFreqKeys.isEmpty()) {
            // 选择第一个键进行驱逐（可以优化为随机选择）
            K keyToEvict = minFreqKeys.iterator().next();
            remove(keyToEvict);
            stats.incrementEvictionCount();
            logger.debug("LFU驱逐: key={}, frequency={}", keyToEvict, minFrequency);
        }
    }
    
    /**
     * 缓存条目
     */
    private static class CacheEntry<V> {
        V value;
        long frequency;
        
        CacheEntry(V value, long frequency) {
            this.value = value;
            this.frequency = frequency;
        }
    }
    
    /**
     * 获取缓存映射（用于调试）
     */
    public Map<K, V> getCacheMap() {
        lock.readLock().lock();
        try {
            Map<K, V> result = new java.util.HashMap<>();
            for (Map.Entry<K, CacheEntry<V>> entry : cache.entrySet()) {
                result.put(entry.getKey(), entry.getValue().value);
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
} 