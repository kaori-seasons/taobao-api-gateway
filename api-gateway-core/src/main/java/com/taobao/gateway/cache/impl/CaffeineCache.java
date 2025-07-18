package com.taobao.gateway.cache.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.taobao.gateway.cache.CacheConfig;
import com.taobao.gateway.cache.CacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 基于Caffeine的一级缓存实现
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class CaffeineCache<K, V> implements com.taobao.gateway.cache.Cache<K, V> {
    
    private static final Logger logger = LoggerFactory.getLogger(CaffeineCache.class);
    
    private final String name;
    private final Cache<K, V> cache;
    private final CacheStats stats;
    private final CacheConfig config;
    
    public CaffeineCache(String name, CacheConfig config) {
        this.name = name;
        this.config = config;
        this.stats = new CacheStats();
        
        // 构建Caffeine缓存
        @SuppressWarnings("unchecked")
        Caffeine<K, V> builder = (Caffeine<K, V>) Caffeine.newBuilder()
                .maximumSize(config.getL1MaxSize())
                .expireAfterWrite(config.getL1ExpireAfterWrite().toNanos(), TimeUnit.NANOSECONDS)
                .expireAfterAccess(config.getL1ExpireAfterAccess().toNanos(), TimeUnit.NANOSECONDS);
        
        // 根据驱逐策略设置
        switch (config.getEvictionPolicy()) {
            case LRU:
                // Caffeine默认就是LRU，无需额外配置
                break;
            case LFU:
                builder.recordStats();
                break;
            case FIFO:
                // Caffeine不支持FIFO，使用LRU代替
                logger.warn("Caffeine不支持FIFO策略，使用LRU代替");
                break;
            case RANDOM:
                // Caffeine不支持随机驱逐，使用LRU代替
                logger.warn("Caffeine不支持随机驱逐策略，使用LRU代替");
                break;
        }
        
        // 添加统计监听器
        if (config.isStatsEnabled()) {
            builder.recordStats();
        }
        
        // 暂时移除驱逐监听器，避免类型推断问题
        // TODO: 后续优化驱逐监听器
        
        this.cache = builder.build();
        
        logger.info("创建Caffeine缓存: {}, 最大大小: {}, 过期时间: {}", 
                name, config.getL1MaxSize(), config.getL1ExpireAfterWrite());
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public V get(K key) {
        V value = cache.getIfPresent(key);
        if (value != null) {
            stats.recordHits(1);
            stats.recordL1Hit();
            logger.debug("一级缓存命中: key={}", key);
        } else {
            stats.recordMisses(1);
            logger.debug("一级缓存未命中: key={}", key);
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
                stats.recordL1Hit();
                logger.debug("一级缓存加载成功: key={}, loadTime={}ms", key, loadTime / 1_000_000);
            } else {
                stats.recordLoadFailure(loadTime);
                stats.recordMisses(1);
                logger.debug("一级缓存加载失败: key={}, loadTime={}ms", key, loadTime / 1_000_000);
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
        Map<K, V> result = cache.getAllPresent(keys);
        long hitCount = result.size();
        long missCount = keys.size() - hitCount;
        
        if (hitCount > 0) {
            stats.recordHits(hitCount);
            stats.recordL1Hit();
        }
        if (missCount > 0) {
            stats.recordMisses(missCount);
        }
        
        logger.debug("一级缓存批量获取: keys={}, hits={}, misses={}", keys.size(), hitCount, missCount);
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
                    stats.recordL1Hit();
                }
                if (missCount > 0) {
                    stats.recordLoadFailure(loadTime);
                    stats.recordMisses(missCount);
                }
                
                logger.debug("一级缓存批量加载: keys={}, hits={}, misses={}, loadTime={}ms", 
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
        cache.put(key, value);
        stats.setSize(cache.estimatedSize());
        logger.debug("一级缓存设置: key={}", key);
    }
    
    @Override
    public void put(K key, V value, long expireTime, TimeUnit timeUnit) {
        // Caffeine不支持单个key的过期时间，使用默认过期时间
        put(key, value);
        logger.debug("一级缓存设置(带过期时间): key={}, expireTime={}{}", key, expireTime, timeUnit);
    }
    
    @Override
    public void putAll(Map<K, V> map) {
        cache.putAll(map);
        stats.setSize(cache.estimatedSize());
        logger.debug("一级缓存批量设置: size={}", map.size());
    }
    
    @Override
    public V remove(K key) {
        V value = cache.getIfPresent(key);
        cache.invalidate(key);
        stats.setSize(cache.estimatedSize());
        logger.debug("一级缓存删除: key={}", key);
        return value;
    }
    
    @Override
    public void removeAll(Collection<K> keys) {
        cache.invalidateAll(keys);
        stats.setSize(cache.estimatedSize());
        logger.debug("一级缓存批量删除: keys={}", keys.size());
    }
    
    @Override
    public boolean containsKey(K key) {
        return cache.getIfPresent(key) != null;
    }
    
    @Override
    public long size() {
        long size = cache.estimatedSize();
        stats.setSize(size);
        return size;
    }
    
    @Override
    public void clear() {
        cache.invalidateAll();
        stats.setSize(0);
        logger.info("一级缓存清空: {}", name);
    }
    
    @Override
    public CacheStats getStats() {
        stats.setSize(cache.estimatedSize());
        return stats;
    }
    
    @Override
    public void refresh(K key) {
        cache.invalidate(key);
        logger.debug("一级缓存刷新: key={}", key);
    }
    
    @Override
    public void refreshAll(Collection<K> keys) {
        cache.invalidateAll(keys);
        logger.debug("一级缓存批量刷新: keys={}", keys.size());
    }
    
    /**
     * 获取Caffeine缓存实例
     */
    public Cache<K, V> getCaffeineCache() {
        return cache;
    }
} 