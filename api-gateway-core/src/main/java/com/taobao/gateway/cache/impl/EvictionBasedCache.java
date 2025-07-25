package com.taobao.gateway.cache.impl;

import com.taobao.gateway.cache.Cache;
import com.taobao.gateway.cache.CacheConfig;
import com.taobao.gateway.cache.CacheStats;
import com.taobao.gateway.cache.eviction.EvictionPolicy;
import com.taobao.gateway.cache.eviction.EvictionPolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于驱逐策略的缓存实现
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class EvictionBasedCache<K, V> implements Cache<K, V> {
    
    private static final Logger logger = LoggerFactory.getLogger(EvictionBasedCache.class);
    
    /**
     * 缓存名称
     */
    private final String name;
    
    /**
     * 驱逐策略
     */
    private final EvictionPolicy<K, V> evictionPolicy;
    
    /**
     * 缓存统计信息
     */
    private final CacheStats stats;
    
    /**
     * 缓存配置
     */
    private final CacheConfig config;
    
    /**
     * 构造函数
     * 
     * @param name 缓存名称
     * @param config 缓存配置
     */
    public EvictionBasedCache(String name, CacheConfig config) {
        this.name = name;
        this.config = config;
        this.stats = new CacheStats();
        this.evictionPolicy = EvictionPolicyFactory.createEvictionPolicy(config);
        
        logger.info("创建基于驱逐策略的缓存: name={}, policy={}, maxSize={}", 
                name, config.getEvictionPolicy(), config.getL1MaxSize());
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public V get(K key) {
        V value = evictionPolicy.get(key);
        if (value != null) {
            stats.recordHits(1);
            logger.debug("驱逐策略缓存命中: key={}", key);
        } else {
            stats.recordMisses(1);
            logger.debug("驱逐策略缓存未命中: key={}", key);
        }
        return value;
    }
    
    @Override
    public V get(K key, com.taobao.gateway.cache.CacheLoader<K, V> loader) {
        // 先尝试从缓存获取
        V value = get(key);
        if (value != null) {
            return value;
        }
        
        // 缓存未命中，使用加载器加载
        try {
            long startTime = System.nanoTime();
            value = loader.load(key);
            long loadTime = System.nanoTime() - startTime;
            
            if (value != null) {
                // 将加载的值放入缓存
                put(key, value);
                stats.recordLoadSuccess(loadTime);
                stats.recordHits(1);
                logger.debug("驱逐策略缓存加载成功: key={}, loadTime={}ms", key, loadTime / 1_000_000);
            } else {
                stats.recordLoadFailure(loadTime);
                stats.recordMisses(1);
                logger.debug("驱逐策略缓存加载失败: key={}, loadTime={}ms", key, loadTime / 1_000_000);
            }
            
            return value;
        } catch (Exception e) {
            stats.recordMisses(1);
            logger.error("获取缓存值异常: key={}", key, e);
            return null;
        }
    }
    
    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        Map<K, V> result = evictionPolicy.getAll(keys);
        long hitCount = result.size();
        long missCount = keys.size() - hitCount;
        
        if (hitCount > 0) {
            stats.recordHits(hitCount);
        }
        if (missCount > 0) {
            stats.recordMisses(missCount);
        }
        
        logger.debug("驱逐策略缓存批量获取: keys={}, hits={}, misses={}", keys.size(), hitCount, missCount);
        return result;
    }
    
    @Override
    public Map<K, V> getAll(Collection<K> keys, com.taobao.gateway.cache.CacheLoader<K, V> loader) {
        // 先尝试从缓存获取
        Map<K, V> result = getAll(keys);
        Collection<K> missingKeys = keys.stream()
                .filter(key -> !result.containsKey(key))
                .collect(java.util.stream.Collectors.toList());
        
        if (!missingKeys.isEmpty()) {
            try {
                long startTime = System.nanoTime();
                Map<K, V> loadedValues = loader.loadAll(missingKeys);
                long loadTime = System.nanoTime() - startTime;
                
                if (!loadedValues.isEmpty()) {
                    // 将加载的值放入缓存
                    putAll(loadedValues);
                    result.putAll(loadedValues);
                    stats.recordLoadSuccess(loadTime);
                }
                
                long hitCount = loadedValues.size();
                long missCount = missingKeys.size() - hitCount;
                
                if (hitCount > 0) {
                    stats.recordHits(hitCount);
                }
                if (missCount > 0) {
                    stats.recordLoadFailure(loadTime);
                    stats.recordMisses(missCount);
                }
                
                logger.debug("驱逐策略缓存批量加载: keys={}, hits={}, misses={}, loadTime={}ms", 
                        missingKeys.size(), hitCount, missCount, loadTime / 1_000_000);
            } catch (Exception e) {
                stats.recordMisses(missingKeys.size());
                logger.error("批量加载缓存值异常: keys={}", missingKeys, e);
            }
        }
        
        return result;
    }
    
    @Override
    public void put(K key, V value) {
        evictionPolicy.put(key, value);
        stats.setSize(evictionPolicy.size());
        logger.debug("驱逐策略缓存设置: key={}", key);
    }
    
    @Override
    public void put(K key, V value, long expireTime, TimeUnit timeUnit) {
        // 驱逐策略缓存不支持单个key的过期时间，使用默认设置
        put(key, value);
        logger.debug("驱逐策略缓存设置(带过期时间): key={}, expireTime={}{}", key, expireTime, timeUnit);
    }
    
    @Override
    public void putAll(Map<K, V> map) {
        evictionPolicy.putAll(map);
        stats.setSize(evictionPolicy.size());
        logger.debug("驱逐策略缓存批量设置: size={}", map.size());
    }
    
    @Override
    public V remove(K key) {
        V value = evictionPolicy.remove(key);
        stats.setSize(evictionPolicy.size());
        logger.debug("驱逐策略缓存删除: key={}", key);
        return value;
    }
    
    @Override
    public void removeAll(Collection<K> keys) {
        evictionPolicy.removeAll(keys);
        stats.setSize(evictionPolicy.size());
        logger.debug("驱逐策略缓存批量删除: keys={}", keys.size());
    }
    
    @Override
    public boolean containsKey(K key) {
        return evictionPolicy.containsKey(key);
    }
    
    @Override
    public long size() {
        long size = evictionPolicy.size();
        stats.setSize(size);
        return size;
    }
    
    @Override
    public void clear() {
        evictionPolicy.clear();
        stats.setSize(0);
        logger.info("驱逐策略缓存清空: {}", name);
    }
    
    @Override
    public CacheStats getStats() {
        stats.setSize(evictionPolicy.size());
        return stats;
    }
    
    @Override
    public void refresh(K key) {
        evictionPolicy.remove(key);
        logger.debug("驱逐策略缓存刷新: key={}", key);
    }
    
    @Override
    public void refreshAll(Collection<K> keys) {
        evictionPolicy.removeAll(keys);
        logger.debug("驱逐策略缓存批量刷新: keys={}", keys.size());
    }
    
    /**
     * 获取驱逐策略
     */
    public EvictionPolicy<K, V> getEvictionPolicy() {
        return evictionPolicy;
    }
    
    /**
     * 获取驱逐统计信息
     */
    public com.taobao.gateway.cache.eviction.EvictionStats getEvictionStats() {
        return evictionPolicy.getEvictionStats();
    }
    
    /**
     * 获取缓存配置
     */
    public CacheConfig getConfig() {
        return config;
    }
}