package com.taobao.gateway.cache.impl;

import com.taobao.gateway.cache.Cache;
import com.taobao.gateway.cache.CacheConfig;
import com.taobao.gateway.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认缓存管理器实现
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class DefaultCacheManager implements CacheManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultCacheManager.class);
    
    /**
     * 缓存实例映射
     */
    private final Map<String, Cache> caches = new ConcurrentHashMap<>();
    
    /**
     * 默认缓存配置
     */
    private final CacheConfig defaultConfig;
    
    /**
     * Redis连接工厂
     */
    private final RedisConnectionFactory redisConnectionFactory;
    
    public DefaultCacheManager() {
        this(new CacheConfig("default"));
    }
    
    public DefaultCacheManager(CacheConfig defaultConfig) {
        this(defaultConfig, null);
    }
    
    public DefaultCacheManager(CacheConfig defaultConfig, RedisConnectionFactory redisConnectionFactory) {
        this.defaultConfig = defaultConfig;
        this.redisConnectionFactory = redisConnectionFactory;
        logger.info("初始化默认缓存管理器");
    }
    
    @Override
    public Cache getCache(String cacheName) {
        return caches.computeIfAbsent(cacheName, name -> createCache(name, defaultConfig));
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String cacheName, Class<K> keyType, Class<V> valueType) {
        return (Cache<K, V>) getCache(cacheName);
    }
    
    @Override
    public Cache createCache(String cacheName, CacheConfig config) {
        Cache existingCache = caches.get(cacheName);
        if (existingCache != null) {
            logger.warn("缓存已存在: {}", cacheName);
            return existingCache;
        }
        
        Cache cache = buildCache(cacheName, config);
        caches.put(cacheName, cache);
        logger.info("创建缓存: {}", cacheName);
        return cache;
    }
    
    @Override
    public void removeCache(String cacheName) {
        Cache cache = caches.remove(cacheName);
        if (cache != null) {
            cache.clear();
            logger.info("删除缓存: {}", cacheName);
        }
    }
    
    @Override
    public Collection<String> getCacheNames() {
        return caches.keySet();
    }
    
    @Override
    public void clearAll() {
        caches.values().forEach(Cache::clear);
        caches.clear();
        logger.info("清空所有缓存");
    }
    
    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("cacheCount", caches.size());
        stats.put("cacheNames", getCacheNames());
        
        Map<String, Object> cacheStats = new java.util.HashMap<>();
        for (Map.Entry<String, Cache> entry : caches.entrySet()) {
            cacheStats.put(entry.getKey(), entry.getValue().getStats());
        }
        stats.put("caches", cacheStats);
        
        return stats;
    }
    
    /**
     * 构建缓存实例
     * 
     * @param cacheName 缓存名称
     * @param config 缓存配置
     * @return 缓存实例
     */
    @SuppressWarnings("unchecked")
    private Cache buildCache(String cacheName, CacheConfig config) {
        // 合并默认配置
        CacheConfig mergedConfig = mergeConfig(config);
        
        if (mergedConfig.isL1Enabled() && mergedConfig.isL2Enabled()) {
            // 创建二级缓存
            Cache<Object, Object> l1Cache = createL1Cache(cacheName + "_l1", mergedConfig);
            
            if (redisConnectionFactory != null) {
                RedisCache<Object, Object> l2Cache = new RedisCache<>(cacheName + "_l2", mergedConfig, redisConnectionFactory);
                return new L2Cache<>(cacheName, mergedConfig, l1Cache, l2Cache);
            } else {
                logger.warn("Redis连接工厂未配置，仅使用一级缓存: {}", cacheName);
                return l1Cache;
            }
        } else if (mergedConfig.isL1Enabled()) {
            // 只创建一级缓存
            return createL1Cache(cacheName, mergedConfig);
        } else if (mergedConfig.isL2Enabled()) {
            // 只创建二级缓存
            if (redisConnectionFactory != null) {
                return new RedisCache<>(cacheName, mergedConfig, redisConnectionFactory);
            } else {
                logger.warn("Redis连接工厂未配置，创建空缓存: {}", cacheName);
                return new EmptyCache<>(cacheName);
            }
        } else {
            // 都不启用，创建空缓存
            logger.warn("缓存未启用: {}", cacheName);
            return new EmptyCache<>(cacheName);
        }
    }
    
    /**
     * 创建一级缓存
     * 
     * @param cacheName 缓存名称
     * @param config 缓存配置
     * @return 一级缓存实例
     */
    @SuppressWarnings("unchecked")
    private Cache<Object, Object> createL1Cache(String cacheName, CacheConfig config) {
        // 根据驱逐策略选择缓存实现
        switch (config.getEvictionPolicy()) {
            case LRU:
            case LFU:
            case FIFO:
            case RANDOM:
                // 使用自定义驱逐策略实现
                return new EvictionBasedCache<>(cacheName, config);
            default:
                // 使用Caffeine实现
                return new CaffeineCache<>(cacheName, config);
        }
    }
    
    /**
     * 合并配置
     * 
     * @param config 用户配置
     * @return 合并后的配置
     */
    private CacheConfig mergeConfig(CacheConfig config) {
        if (config == null) {
            return defaultConfig;
        }
        
        // 这里可以添加配置合并逻辑
        // 目前简单返回用户配置，如果为空则返回默认配置
        return config;
    }
    
    /**
     * 空缓存实现（当缓存未启用时使用）
     */
    private static class EmptyCache<K, V> implements Cache<K, V> {
        private final String name;
        
        public EmptyCache(String name) {
            this.name = name;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public V get(K key) {
            return null;
        }
        
        @Override
        public V get(K key, com.taobao.gateway.cache.CacheLoader<K, V> loader) {
            try {
                return loader.load(key);
            } catch (Exception e) {
                logger.error("加载缓存值异常: key={}", key, e);
                return null;
            }
        }
        
        @Override
        public Map<K, V> getAll(Collection<K> keys) {
            return new java.util.HashMap<>();
        }
        
        @Override
        public Map<K, V> getAll(Collection<K> keys, com.taobao.gateway.cache.CacheLoader<K, V> loader) {
            try {
                return loader.loadAll(keys);
            } catch (Exception e) {
                logger.error("批量加载缓存值异常: keys={}", keys, e);
                return new java.util.HashMap<>();
            }
        }
        
        @Override
        public void put(K key, V value) {
            // 空实现
        }
        
        @Override
        public void put(K key, V value, long expireTime, java.util.concurrent.TimeUnit timeUnit) {
            // 空实现
        }
        
        @Override
        public void putAll(Map<K, V> map) {
            // 空实现
        }
        
        @Override
        public V remove(K key) {
            return null;
        }
        
        @Override
        public void removeAll(Collection<K> keys) {
            // 空实现
        }
        
        @Override
        public boolean containsKey(K key) {
            return false;
        }
        
        @Override
        public long size() {
            return 0;
        }
        
        @Override
        public void clear() {
            // 空实现
        }
        
        @Override
        public com.taobao.gateway.cache.CacheStats getStats() {
            return new com.taobao.gateway.cache.CacheStats();
        }
        
        @Override
        public void refresh(K key) {
            // 空实现
        }
        
        @Override
        public void refreshAll(Collection<K> keys) {
            // 空实现
        }
    }
}
