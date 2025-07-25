package com.taobao.gateway.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 缓存服务类
 * 提供高级缓存功能和便捷的操作方法
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

    @Autowired
    private CacheManager cacheManager;

    /**
     * 获取缓存值
     * 
     * @param cacheName 缓存名称
     * @param key 键
     * @return 值
     */
    public <K, V> V get(String cacheName, K key) {
        Cache<K, V> cache = cacheManager.getCache(cacheName);
        return cache.get(key);
    }

    /**
     * 获取缓存值，如果不存在则使用加载器加载
     * 
     * @param cacheName 缓存名称
     * @param key 键
     * @param loader 加载器
     * @return 值
     */
    public <K, V> V get(String cacheName, K key, Function<K, V> loader) {
        Cache<K, V> cache = cacheManager.getCache(cacheName);
        return cache.get(key, loader::apply);
    }

    /**
     * 批量获取缓存值
     * 
     * @param cacheName 缓存名称
     * @param keys 键集合
     * @return 键值对映射
     */
    public <K, V> Map<K, V> getAll(String cacheName, Collection<K> keys) {
        Cache<K, V> cache = cacheManager.getCache(cacheName);
        return cache.getAll(keys);
    }

    /**
     * 批量获取缓存值，如果不存在则使用加载器加载
     * 
     * @param cacheName 缓存名称
     * @param keys 键集合
     * @param loader 加载器
     * @return 键值对映射
     */
    public <K, V> Map<K, V> getAll(String cacheName, Collection<K> keys, Function<Collection<K>, Map<K, V>> loader) {
        Cache<K, V> cache = cacheManager.getCache(cacheName);
        return cache.getAll(keys, new CacheLoader<K, V>() {
            @Override
            public V load(K key) throws Exception {
                // 这个方法不会被调用，因为我们重写了loadAll方法
                return null;
            }
            
            @Override
            public Map<K, V> loadAll(Iterable<K> keys) throws Exception {
                return loader.apply((Collection<K>) keys);
            }
        });
    }

    /**
     * 设置缓存值
     * 
     * @param cacheName 缓存名称
     * @param key 键
     * @param value 值
     */
    public <K, V> void put(String cacheName, K key, V value) {
        Cache<K, V> cache = cacheManager.getCache(cacheName);
        cache.put(key, value);
    }

    /**
     * 设置缓存值（带过期时间）
     * 
     * @param cacheName 缓存名称
     * @param key 键
     * @param value 值
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     */
    public <K, V> void put(String cacheName, K key, V value, long expireTime, TimeUnit timeUnit) {
        Cache<K, V> cache = cacheManager.getCache(cacheName);
        cache.put(key, value, expireTime, timeUnit);
    }

    /**
     * 批量设置缓存值
     * 
     * @param cacheName 缓存名称
     * @param map 键值对映射
     */
    public <K, V> void putAll(String cacheName, Map<K, V> map) {
        Cache<K, V> cache = cacheManager.getCache(cacheName);
        cache.putAll(map);
    }

    /**
     * 删除缓存值
     * 
     * @param cacheName 缓存名称
     * @param key 键
     * @return 被删除的值
     */
    public <K, V> V remove(String cacheName, K key) {
        Cache<K, V> cache = cacheManager.getCache(cacheName);
        return cache.remove(key);
    }

    /**
     * 批量删除缓存值
     * 
     * @param cacheName 缓存名称
     * @param keys 键集合
     */
    public <K, V> void removeAll(String cacheName, Collection<K> keys) {
        Cache<K, V> cache = cacheManager.getCache(cacheName);
        cache.removeAll(keys);
    }

    /**
     * 检查键是否存在
     * 
     * @param cacheName 缓存名称
     * @param key 键
     * @return 是否存在
     */
    public <K, V> boolean containsKey(String cacheName, K key) {
        Cache<K, V> cache = cacheManager.getCache(cacheName);
        return cache.containsKey(key);
    }

    /**
     * 获取缓存大小
     * 
     * @param cacheName 缓存名称
     * @return 缓存大小
     */
    public <K, V> long size(String cacheName) {
        Cache<K, V> cache = cacheManager.getCache(cacheName);
        return cache.size();
    }

    /**
     * 清空缓存
     * 
     * @param cacheName 缓存名称
     */
    public <K, V> void clear(String cacheName) {
        Cache<K, V> cache = cacheManager.getCache(cacheName);
        cache.clear();
    }

    /**
     * 刷新缓存
     * 
     * @param cacheName 缓存名称
     * @param key 键
     */
    public <K, V> void refresh(String cacheName, K key) {
        Cache<K, V> cache = cacheManager.getCache(cacheName);
        cache.refresh(key);
    }

    /**
     * 批量刷新缓存
     * 
     * @param cacheName 缓存名称
     * @param keys 键集合
     */
    public <K, V> void refreshAll(String cacheName, Collection<K> keys) {
        Cache<K, V> cache = cacheManager.getCache(cacheName);
        cache.refreshAll(keys);
    }

    /**
     * 获取缓存统计信息
     * 
     * @param cacheName 缓存名称
     * @return 统计信息
     */
    public <K, V> CacheStats getStats(String cacheName) {
        Cache<K, V> cache = cacheManager.getCache(cacheName);
        return cache.getStats();
    }

    /**
     * 获取所有缓存统计信息
     * 
     * @return 统计信息映射
     */
    public Map<String, Object> getAllStats() {
        return cacheManager.getStats();
    }

    /**
     * 创建缓存
     * 
     * @param cacheName 缓存名称
     * @param config 缓存配置
     * @return 缓存实例
     */
    public <K, V> Cache<K, V> createCache(String cacheName, CacheConfig config) {
        return cacheManager.createCache(cacheName, config);
    }

    /**
     * 删除缓存
     * 
     * @param cacheName 缓存名称
     */
    public void removeCache(String cacheName) {
        cacheManager.removeCache(cacheName);
    }

    /**
     * 获取所有缓存名称
     * 
     * @return 缓存名称集合
     */
    public Collection<String> getCacheNames() {
        return cacheManager.getCacheNames();
    }

    /**
     * 清空所有缓存
     */
    public void clearAll() {
        cacheManager.clearAll();
    }

    /**
     * 缓存操作工具类
     */
    public static class CacheOps {
        
        /**
         * 缓存操作结果
         */
        public static class CacheResult<T> {
            private final boolean success;
            private final T value;
            private final String error;

            private CacheResult(boolean success, T value, String error) {
                this.success = success;
                this.value = value;
                this.error = error;
            }

            public static <T> CacheResult<T> success(T value) {
                return new CacheResult<>(true, value, null);
            }

            public static <T> CacheResult<T> failure(String error) {
                return new CacheResult<>(false, null, error);
            }

            public boolean isSuccess() {
                return success;
            }

            public T getValue() {
                return value;
            }

            public String getError() {
                return error;
            }
        }

        /**
         * 安全获取缓存值
         * 
         * @param cache 缓存实例
         * @param key 键
         * @return 操作结果
         */
        public static <K, V> CacheResult<V> safeGet(Cache<K, V> cache, K key) {
            try {
                V value = cache.get(key);
                return CacheResult.success(value);
            } catch (Exception e) {
                logger.error("获取缓存值失败: key={}", key, e);
                return CacheResult.failure(e.getMessage());
            }
        }

        /**
         * 安全设置缓存值
         * 
         * @param cache 缓存实例
         * @param key 键
         * @param value 值
         * @return 操作结果
         */
        public static <K, V> CacheResult<Void> safePut(Cache<K, V> cache, K key, V value) {
            try {
                cache.put(key, value);
                return CacheResult.success(null);
            } catch (Exception e) {
                logger.error("设置缓存值失败: key={}", key, e);
                return CacheResult.failure(e.getMessage());
            }
        }

        /**
         * 安全删除缓存值
         * 
         * @param cache 缓存实例
         * @param key 键
         * @return 操作结果
         */
        public static <K, V> CacheResult<V> safeRemove(Cache<K, V> cache, K key) {
            try {
                V value = cache.remove(key);
                return CacheResult.success(value);
            } catch (Exception e) {
                logger.error("删除缓存值失败: key={}", key, e);
                return CacheResult.failure(e.getMessage());
            }
        }
    }
} 