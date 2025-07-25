package com.taobao.gateway.cache.eviction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * FIFO（先进先出）驱逐策略实现
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class FIFOEvictionPolicy<K, V> implements EvictionPolicy<K, V> {
    
    private static final Logger logger = LoggerFactory.getLogger(FIFOEvictionPolicy.class);
    
    /**
     * 最大容量
     */
    private long maxSize;
    
    /**
     * 缓存映射
     */
    private final Map<K, V> cache;
    
    /**
     * 键的顺序队列（先进先出）
     */
    private final LinkedList<K> keyQueue;
    
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
    public FIFOEvictionPolicy(long maxSize) {
        this.maxSize = maxSize;
        this.cache = new ConcurrentHashMap<>();
        this.keyQueue = new LinkedList<>();
    }
    
    @Override
    public V get(K key) {
        lock.readLock().lock();
        try {
            V value = cache.get(key);
            if (value != null) {
                stats.incrementHitCount();
                logger.debug("FIFO缓存命中: key={}", key);
            } else {
                stats.incrementMissCount();
                logger.debug("FIFO缓存未命中: key={}", key);
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
            if (cache.containsKey(key)) {
                // 更新现有值，不改变顺序
                cache.put(key, value);
                logger.debug("FIFO缓存更新: key={}", key);
            } else {
                // 检查是否需要驱逐
                if (cache.size() >= maxSize) {
                    evict();
                }
                
                // 添加新值
                cache.put(key, value);
                keyQueue.addLast(key);
                logger.debug("FIFO缓存添加: key={}", key);
            }
            
            stats.incrementPutCount();
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
                keyQueue.remove(key);
                stats.incrementRemoveCount();
                logger.debug("FIFO缓存删除: key={}", key);
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
            logger.debug("FIFO缓存批量获取: keys={}, hits={}", keys.size(), result.size());
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void putAll(Map<K, V> map) {
        lock.writeLock().lock();
        try {
            for (Map.Entry<K, V> entry : map.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
            logger.debug("FIFO缓存批量设置: size={}", map.size());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void removeAll(Collection<K> keys) {
        lock.writeLock().lock();
        try {
            for (K key : keys) {
                remove(key);
            }
            logger.debug("FIFO缓存批量删除: keys={}", keys.size());
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
            keyQueue.clear();
            logger.info("FIFO缓存清空");
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
     * 驱逐最早进入的项
     */
    private void evict() {
        if (!keyQueue.isEmpty()) {
            K keyToEvict = keyQueue.removeFirst();
            cache.remove(keyToEvict);
            stats.incrementEvictionCount();
            logger.debug("FIFO驱逐: key={}", keyToEvict);
        }
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
    
    /**
     * 获取键队列（用于调试）
     */
    public LinkedList<K> getKeyQueue() {
        lock.readLock().lock();
        try {
            return new LinkedList<>(keyQueue);
        } finally {
            lock.readLock().unlock();
        }
    }
} 