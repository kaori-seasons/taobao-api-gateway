package com.taobao.gateway.cache.eviction;

import java.util.Collection;
import java.util.Map;

/**
 * 缓存驱逐策略接口
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface EvictionPolicy<K, V> {
    
    /**
     * 获取缓存值
     * 
     * @param key 键
     * @return 值
     */
    V get(K key);
    
    /**
     * 设置缓存值
     * 
     * @param key 键
     * @param value 值
     */
    void put(K key, V value);
    
    /**
     * 删除缓存值
     * 
     * @param key 键
     * @return 被删除的值
     */
    V remove(K key);
    
    /**
     * 批量获取缓存值
     * 
     * @param keys 键集合
     * @return 键值对映射
     */
    Map<K, V> getAll(Collection<K> keys);
    
    /**
     * 批量设置缓存值
     * 
     * @param map 键值对映射
     */
    void putAll(Map<K, V> map);
    
    /**
     * 批量删除缓存值
     * 
     * @param keys 键集合
     */
    void removeAll(Collection<K> keys);
    
    /**
     * 检查键是否存在
     * 
     * @param key 键
     * @return 是否存在
     */
    boolean containsKey(K key);
    
    /**
     * 获取缓存大小
     * 
     * @return 缓存大小
     */
    long size();
    
    /**
     * 清空缓存
     */
    void clear();
    
    /**
     * 获取最大容量
     * 
     * @return 最大容量
     */
    long getMaxSize();
    
    /**
     * 设置最大容量
     * 
     * @param maxSize 最大容量
     */
    void setMaxSize(long maxSize);
    
    /**
     * 获取驱逐统计信息
     * 
     * @return 驱逐统计信息
     */
    EvictionStats getEvictionStats();
} 