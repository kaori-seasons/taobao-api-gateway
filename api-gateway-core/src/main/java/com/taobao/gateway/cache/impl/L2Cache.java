package com.taobao.gateway.cache.impl;

import com.taobao.gateway.cache.CacheConfig;
import com.taobao.gateway.cache.CacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 二级缓存实现
 * 整合一级缓存（Caffeine）和二级缓存（Redis）
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class L2Cache<K, V> implements com.taobao.gateway.cache.Cache<K, V> {
    
    private static final Logger logger = LoggerFactory.getLogger(L2Cache.class);
    
    private final String name;
    private final CaffeineCache<K, V> l1Cache;
    private final RedisCache<K, V> l2Cache;
    private final CacheStats stats;
    private final CacheConfig config;
    
    public L2Cache(String name, CacheConfig config, CaffeineCache<K, V> l1Cache, RedisCache<K, V> l2Cache) {
        this.name = name;
        this.config = config;
        this.l1Cache = l1Cache;
        this.l2Cache = l2Cache;
        this.stats = new CacheStats();
        
        logger.info("创建二级缓存: {}", name);
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public V get(K key) {
        // 1. 先查询一级缓存
        V value = l1Cache.get(key);
        if (value != null) {
            stats.recordHits(1);
            stats.recordL1Hit();
            logger.debug("二级缓存L1命中: key={}", key);
            return value;
        }
        
        // 2. 一级缓存未命中，查询二级缓存
        if (config.isL2Enabled()) {
            value = l2Cache.get(key);
            if (value != null) {
                // 将二级缓存的值回填到一级缓存
                l1Cache.put(key, value);
                stats.recordHits(1);
                stats.recordL2Hit();
                logger.debug("二级缓存L2命中: key={}", key);
                return value;
            }
        }
        
        // 3. 两级缓存都未命中
        stats.recordMisses(1);
        logger.debug("二级缓存未命中: key={}", key);
        return null;
    }
    
    @Override
    public V get(K key, com.taobao.gateway.cache.CacheLoader<K, V> loader) {
        // 1. 先查询一级缓存
        V value = l1Cache.get(key);
        if (value != null) {
            stats.recordHits(1);
            stats.recordL1Hit();
            logger.debug("二级缓存L1命中: key={}", key);
            return value;
        }
        
        // 2. 一级缓存未命中，查询二级缓存
        if (config.isL2Enabled()) {
            value = l2Cache.get(key);
            if (value != null) {
                // 将二级缓存的值回填到一级缓存
                l1Cache.put(key, value);
                stats.recordHits(1);
                stats.recordL2Hit();
                logger.debug("二级缓存L2命中: key={}", key);
                return value;
            }
        }
        
        // 3. 两级缓存都未命中，使用加载器加载
        try {
            long startTime = System.nanoTime();
            value = loader.load(key);
            long loadTime = System.nanoTime() - startTime;
            
            if (value != null) {
                // 根据更新模式决定写入策略
                switch (config.getUpdateMode()) {
                    case WRITE_THROUGH:
                        // 同时写入一级和二级缓存
                        l1Cache.put(key, value);
                        if (config.isL2Enabled()) {
                            l2Cache.put(key, value);
                        }
                        break;
                    case WRITE_BACK:
                        // 只写入一级缓存，异步写入二级缓存
                        l1Cache.put(key, value);
                        // TODO: 异步写入二级缓存
                        break;
                    case WRITE_AROUND:
                        // 只写入一级缓存
                        l1Cache.put(key, value);
                        break;
                }
                
                stats.recordLoadSuccess(loadTime);
                logger.debug("二级缓存加载成功: key={}, loadTime={}ms", key, loadTime / 1_000_000);
            } else {
                stats.recordLoadFailure(loadTime);
                logger.debug("二级缓存加载失败: key={}, loadTime={}ms", key, loadTime / 1_000_000);
            }
            
            return value;
        } catch (Exception e) {
            stats.recordMisses(1);
            logger.error("加载缓存值异常: key={}", key, e);
            return null;
        }
    }
    
    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        Map<K, V> result = new java.util.HashMap<>();
        if (keys.isEmpty()) {
            return result;
        }
        
        // 1. 先查询一级缓存
        Map<K, V> l1Result = l1Cache.getAll(keys);
        result.putAll(l1Result);
        
        // 2. 查询一级缓存未命中的键
        Collection<K> missingKeys = keys.stream()
                .filter(key -> !result.containsKey(key))
                .collect(java.util.stream.Collectors.toList());
        
        if (!missingKeys.isEmpty() && config.isL2Enabled()) {
            // 3. 查询二级缓存
            Map<K, V> l2Result = l2Cache.getAll(missingKeys);
            
            // 4. 将二级缓存的结果回填到一级缓存
            for (Map.Entry<K, V> entry : l2Result.entrySet()) {
                l1Cache.put(entry.getKey(), entry.getValue());
            }
            
            result.putAll(l2Result);
        }
        
        long hitCount = result.size();
        long missCount = keys.size() - hitCount;
        
        if (hitCount > 0) {
            stats.recordHits(hitCount);
        }
        if (missCount > 0) {
            stats.recordMisses(missCount);
        }
        
        logger.debug("二级缓存批量获取: keys={}, hits={}, misses={}", keys.size(), hitCount, missCount);
        return result;
    }
    
    @Override
    public Map<K, V> getAll(Collection<K> keys, com.taobao.gateway.cache.CacheLoader<K, V> loader) {
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
                    // 根据更新模式决定写入策略
                    switch (config.getUpdateMode()) {
                        case WRITE_THROUGH:
                            l1Cache.putAll(loadedValues);
                            if (config.isL2Enabled()) {
                                l2Cache.putAll(loadedValues);
                            }
                            break;
                        case WRITE_BACK:
                            l1Cache.putAll(loadedValues);
                            // TODO: 异步写入二级缓存
                            break;
                        case WRITE_AROUND:
                            l1Cache.putAll(loadedValues);
                            break;
                    }
                    
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
                
                logger.debug("二级缓存批量加载: keys={}, hits={}, misses={}, loadTime={}ms", 
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
        // 根据更新模式决定写入策略
        switch (config.getUpdateMode()) {
            case WRITE_THROUGH:
                l1Cache.put(key, value);
                if (config.isL2Enabled()) {
                    l2Cache.put(key, value);
                }
                break;
            case WRITE_BACK:
                l1Cache.put(key, value);
                // TODO: 异步写入二级缓存
                break;
            case WRITE_AROUND:
                l1Cache.put(key, value);
                break;
        }
        
        logger.debug("二级缓存设置: key={}", key);
    }
    
    @Override
    public void put(K key, V value, long expireTime, TimeUnit timeUnit) {
        // 根据更新模式决定写入策略
        switch (config.getUpdateMode()) {
            case WRITE_THROUGH:
                l1Cache.put(key, value, expireTime, timeUnit);
                if (config.isL2Enabled()) {
                    l2Cache.put(key, value, expireTime, timeUnit);
                }
                break;
            case WRITE_BACK:
                l1Cache.put(key, value, expireTime, timeUnit);
                // TODO: 异步写入二级缓存
                break;
            case WRITE_AROUND:
                l1Cache.put(key, value, expireTime, timeUnit);
                break;
        }
        
        logger.debug("二级缓存设置(带过期时间): key={}, expireTime={}{}", key, expireTime, timeUnit);
    }
    
    @Override
    public void putAll(Map<K, V> map) {
        // 根据更新模式决定写入策略
        switch (config.getUpdateMode()) {
            case WRITE_THROUGH:
                l1Cache.putAll(map);
                if (config.isL2Enabled()) {
                    l2Cache.putAll(map);
                }
                break;
            case WRITE_BACK:
                l1Cache.putAll(map);
                // TODO: 异步写入二级缓存
                break;
            case WRITE_AROUND:
                l1Cache.putAll(map);
                break;
        }
        
        logger.debug("二级缓存批量设置: size={}", map.size());
    }
    
    @Override
    public V remove(K key) {
        V value = l1Cache.remove(key);
        if (config.isL2Enabled()) {
            l2Cache.remove(key);
        }
        logger.debug("二级缓存删除: key={}", key);
        return value;
    }
    
    @Override
    public void removeAll(Collection<K> keys) {
        l1Cache.removeAll(keys);
        if (config.isL2Enabled()) {
            l2Cache.removeAll(keys);
        }
        logger.debug("二级缓存批量删除: keys={}", keys.size());
    }
    
    @Override
    public boolean containsKey(K key) {
        return l1Cache.containsKey(key) || (config.isL2Enabled() && l2Cache.containsKey(key));
    }
    
    @Override
    public long size() {
        return l1Cache.size() + (config.isL2Enabled() ? l2Cache.size() : 0);
    }
    
    @Override
    public void clear() {
        l1Cache.clear();
        if (config.isL2Enabled()) {
            l2Cache.clear();
        }
        logger.info("二级缓存清空: {}", name);
    }
    
    @Override
    public CacheStats getStats() {
        // 合并一级和二级缓存的统计信息
        CacheStats l1Stats = l1Cache.getStats();
        CacheStats l2Stats = l2Cache.getStats();
        
        stats.setSize(size());
        return stats;
    }
    
    @Override
    public void refresh(K key) {
        l1Cache.refresh(key);
        if (config.isL2Enabled()) {
            l2Cache.refresh(key);
        }
        logger.debug("二级缓存刷新: key={}", key);
    }
    
    @Override
    public void refreshAll(Collection<K> keys) {
        l1Cache.refreshAll(keys);
        if (config.isL2Enabled()) {
            l2Cache.refreshAll(keys);
        }
        logger.debug("二级缓存批量刷新: keys={}", keys.size());
    }
    
    /**
     * 获取一级缓存
     */
    public CaffeineCache<K, V> getL1Cache() {
        return l1Cache;
    }
    
    /**
     * 获取二级缓存
     */
    public RedisCache<K, V> getL2Cache() {
        return l2Cache;
    }
}
