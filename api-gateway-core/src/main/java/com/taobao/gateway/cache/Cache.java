package com.taobao.gateway.cache;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 缓存接口
 * 定义缓存的基本操作方法
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface Cache<K, V> {
    
    /**
     * 获取缓存名称
     * 
     * @return 缓存名称
     */
    String getName();
    
    /**
     * 获取缓存值
     * 
     * @param key 键
     * @return 值
     */
    V get(K key);
    
    /**
     * 获取缓存值，如果不存在则使用加载器加载
     * 
     * @param key 键
     * @param loader 加载器
     * @return 值
     */
    V get(K key, CacheLoader<K, V> loader);
    
    /**
     * 批量获取缓存值
     * 
     * @param keys 键集合
     * @return 键值对映射
     */
    Map<K, V> getAll(Collection<K> keys);
    
    /**
     * 批量获取缓存值，如果不存在则使用加载器加载
     * 
     * @param keys 键集合
     * @param loader 加载器
     * @return 键值对映射
     */
    Map<K, V> getAll(Collection<K> keys, CacheLoader<K, V> loader);
    
    /**
     * 设置缓存值
     * 
     * @param key 键
     * @param value 值
     */
    void put(K key, V value);
    
    /**
     * 设置缓存值（带过期时间）
     * 
     * @param key 键
     * @param value 值
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     */
    void put(K key, V value, long expireTime, TimeUnit timeUnit);
    
    /**
     * 批量设置缓存值
     * 
     * @param map 键值对映射
     */
    void putAll(Map<K, V> map);
    
    /**
     * 删除缓存值
     * 
     * @param key 键
     * @return 被删除的值
     */
    V remove(K key);
    
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
     * 获取缓存统计信息
     * 
     * @return 统计信息
     */
    CacheStats getStats();
    
    /**
     * 刷新缓存
     * 
     * @param key 键
     */
    void refresh(K key);
    
    /**
     * 批量刷新缓存
     * 
     * @param keys 键集合
     */
    void refreshAll(Collection<K> keys);
}
