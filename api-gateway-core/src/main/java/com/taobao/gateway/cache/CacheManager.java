package com.taobao.gateway.cache;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 缓存管理器接口
 * 定义缓存的基本操作
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface CacheManager {
    
    /**
     * 获取缓存
     * 
     * @param cacheName 缓存名称
     * @return 缓存实例
     */
    Cache getCache(String cacheName);
    
    /**
     * 获取缓存（带类型）
     * 
     * @param cacheName 缓存名称
     * @param keyType 键类型
     * @param valueType 值类型
     * @return 缓存实例
     */
    <K, V> Cache<K, V> getCache(String cacheName, Class<K> keyType, Class<V> valueType);
    
    /**
     * 创建缓存
     * 
     * @param cacheName 缓存名称
     * @param config 缓存配置
     * @return 缓存实例
     */
    Cache createCache(String cacheName, CacheConfig config);
    
    /**
     * 删除缓存
     * 
     * @param cacheName 缓存名称
     */
    void removeCache(String cacheName);
    
    /**
     * 获取所有缓存名称
     * 
     * @return 缓存名称集合
     */
    Collection<String> getCacheNames();
    
    /**
     * 清空所有缓存
     */
    void clearAll();
    
    /**
     * 获取缓存统计信息
     * 
     * @return 统计信息
     */
    Map<String, Object> getStats();
}
