package com.taobao.gateway.cache;

import java.util.Map;

/**
 * 缓存加载器接口
 * 用于加载缓存数据
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@FunctionalInterface
public interface CacheLoader<K, V> {
    
    /**
     * 加载单个值
     * 
     * @param key 键
     * @return 值
     * @throws Exception 加载异常
     */
    V load(K key) throws Exception;
    
    /**
     * 批量加载值
     * 默认实现为循环调用单个加载方法
     * 
     * @param keys 键集合
     * @return 键值对映射
     * @throws Exception 加载异常
     */
    default Map<K, V> loadAll(Iterable<K> keys) throws Exception {
        Map<K, V> result = new java.util.HashMap<>();
        for (K key : keys) {
            V value = load(key);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }
} 