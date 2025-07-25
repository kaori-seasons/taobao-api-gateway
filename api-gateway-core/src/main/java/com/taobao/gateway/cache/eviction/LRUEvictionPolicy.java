package com.taobao.gateway.cache.eviction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * LRU（最近最少使用）驱逐策略实现
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class LRUEvictionPolicy<K, V> implements EvictionPolicy<K, V> {
    
    private static final Logger logger = LoggerFactory.getLogger(LRUEvictionPolicy.class);
    
    /**
     * 最大容量
     */
    private long maxSize;
    
    /**
     * LRU缓存映射
     */
    private final Map<K, V> cache;
    
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
    public LRUEvictionPolicy(long maxSize) {
        this.maxSize = maxSize;
        this.cache = new LinkedHashMap<K, V>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                boolean shouldRemove = size() > LRUEvictionPolicy.this.maxSize;
                if (shouldRemove) {
                    stats.incrementEvictionCount();
                    logger.debug("LRU驱逐: key={}", eldest.getKey());
                }
                return shouldRemove;
            }
        };
    }
    
    @Override
    public V get(K key) {
        lock.readLock().lock();
        try {
            V value = cache.get(key);
            if (value != null) {
                stats.incrementHitCount();
                logger.debug("LRU缓存命中: key={}", key);
            } else {
                stats.incrementMissCount();
                logger.debug("LRU缓存未命中: key={}", key);
            }
            return value;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void put(K key, V value) {
        lock.writeLock().lock();
        try {
            cache.put(key, value);
            stats.incrementPutCount();
            logger.debug("LRU缓存设置: key={}", key);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public V remove(K key) {
        lock.writeLock().lock();
        try {
            V value = cache.remove(key);
            if (value != null) {
                stats.incrementRemoveCount();
                logger.debug("LRU缓存删除: key={}", key);
            }
            return value;
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
                V value = cache.get(key);
                if (value != null) {
                    result.put(key, value);
                    stats.incrementHitCount();
                } else {
                    stats.incrementMissCount();
                }
            }
            logger.debug("LRU缓存批量获取: keys={}, hits={}", keys.size(), result.size());
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void putAll(Map<K, V> map) {
        lock.writeLock().lock();
        try {
            cache.putAll(map);
            stats.incrementPutCount();
            logger.debug("LRU缓存批量设置: size={}", map.size());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void removeAll(Collection<K> keys) {
        lock.writeLock().lock();
        try {
            for (K key : keys) {
                if (cache.remove(key) != null) {
                    stats.incrementRemoveCount();
                }
            }
            logger.debug("LRU缓存批量删除: keys={}", keys.size());
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
            logger.info("LRU缓存清空");
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
     * 获取缓存映射（用于调试）
     */
    public Map<K, V> getCacheMap() {
        lock.readLock().lock();
        try {
            return new java.util.HashMap<>(cache);
        } finally {
            lock.readLock().unlock();
        }
    }
}