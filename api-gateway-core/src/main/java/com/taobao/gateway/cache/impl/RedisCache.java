package com.taobao.gateway.cache.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.gateway.cache.CacheConfig;
import com.taobao.gateway.cache.CacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的二级缓存实现
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class RedisCache<K, V> implements com.taobao.gateway.cache.Cache<K, V> {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisCache.class);
    
    private final String name;
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final CacheStats stats;
    private final CacheConfig config;
    private final String keyPrefix;
    
    public RedisCache(String name, CacheConfig config, JedisPool jedisPool) {
        this.name = name;
        this.config = config;
        this.jedisPool = jedisPool;
        this.objectMapper = new ObjectMapper();
        this.stats = new CacheStats();
        this.keyPrefix = config.getKeyPrefix() + ":" + name + ":";
        
        logger.info("创建Redis缓存: {}, 键前缀: {}", name, keyPrefix);
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public V get(K key) {
        String redisKey = buildKey(key);
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(redisKey);
            if (value != null) {
                V deserializedValue = deserialize(value);
                if (deserializedValue != null) {
                    stats.recordHits(1);
                    stats.recordL2Hit();
                    logger.debug("二级缓存命中: key={}", key);
                    return deserializedValue;
                }
            }
            
            stats.recordMisses(1);
            logger.debug("二级缓存未命中: key={}", key);
            return null;
        } catch (Exception e) {
            stats.recordMisses(1);
            logger.error("获取Redis缓存异常: key={}", key, e);
            return null;
        }
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
                stats.recordL2Hit();
                logger.debug("二级缓存加载成功: key={}, loadTime={}ms", key, loadTime / 1_000_000);
            } else {
                stats.recordLoadFailure(loadTime);
                stats.recordMisses(1);
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
        Map<K, V> result = new HashMap<>();
        if (keys.isEmpty()) {
            return result;
        }
        
        List<String> redisKeyList = keys.stream()
                .map(this::buildKey)
                .collect(java.util.stream.Collectors.toList());
        String[] redisKeys = redisKeyList.toArray(new String[0]);
        
        try (Jedis jedis = jedisPool.getResource()) {
            List<String> values = jedis.mget(redisKeys);
            int index = 0;
            for (K key : keys) {
                String value = values.get(index++);
                if (value != null) {
                    V deserializedValue = deserialize(value);
                    if (deserializedValue != null) {
                        result.put(key, deserializedValue);
                    }
                }
            }
            
            long hitCount = result.size();
            long missCount = keys.size() - hitCount;
            
            if (hitCount > 0) {
                stats.recordHits(hitCount);
                stats.recordL2Hit();
            }
            if (missCount > 0) {
                stats.recordMisses(missCount);
            }
            
            logger.debug("二级缓存批量获取: keys={}, hits={}, misses={}", keys.size(), hitCount, missCount);
            return result;
        } catch (Exception e) {
            stats.recordMisses(keys.size());
            logger.error("批量获取Redis缓存异常: keys={}", keys, e);
            return result;
        }
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
                    putAll(loadedValues);
                    result.putAll(loadedValues);
                    stats.recordLoadSuccess(loadTime);
                }
                
                long hitCount = loadedValues.size();
                long missCount = missingKeys.size() - hitCount;
                
                if (hitCount > 0) {
                    stats.recordHits(hitCount);
                    stats.recordL2Hit();
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
        put(key, value, config.getL2ExpireAfterWrite().toSeconds(), TimeUnit.SECONDS);
    }
    
    @Override
    public void put(K key, V value, long expireTime, TimeUnit timeUnit) {
        if (value == null && !config.isCacheNullValues()) {
            return;
        }
        
        String redisKey = buildKey(key);
        try (Jedis jedis = jedisPool.getResource()) {
            String serializedValue = serialize(value);
            if (serializedValue != null) {
                int expireSeconds = (int) timeUnit.toSeconds(expireTime);
                jedis.setex(redisKey, expireSeconds, serializedValue);
                stats.setSize(jedis.dbSize());
                logger.debug("二级缓存设置: key={}, expireTime={}s", key, expireSeconds);
            }
        } catch (Exception e) {
            logger.error("设置Redis缓存异常: key={}", key, e);
        }
    }
    
    @Override
    public void putAll(Map<K, V> map) {
        if (map.isEmpty()) {
            return;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> redisMap = new HashMap<>();
            for (Map.Entry<K, V> entry : map.entrySet()) {
                if (entry.getValue() != null || config.isCacheNullValues()) {
                    String serializedValue = serialize(entry.getValue());
                    if (serializedValue != null) {
                        redisMap.put(buildKey(entry.getKey()), serializedValue);
                    }
                }
            }
            
            if (!redisMap.isEmpty()) {
                // 使用管道批量设置
                try (var pipeline = jedis.pipelined()) {
                    for (Map.Entry<String, String> entry : redisMap.entrySet()) {
                        pipeline.setex(entry.getKey(), (int) config.getL2ExpireAfterWrite().toSeconds(), entry.getValue());
                    }
                    pipeline.sync();
                }
                stats.setSize(jedis.dbSize());
                logger.debug("二级缓存批量设置: size={}", redisMap.size());
            }
        } catch (Exception e) {
            logger.error("批量设置Redis缓存异常: map={}", map, e);
        }
    }
    
    @Override
    public V remove(K key) {
        String redisKey = buildKey(key);
        try (Jedis jedis = jedisPool.getResource()) {
            V value = get(key);
            jedis.del(redisKey);
            stats.setSize(jedis.dbSize());
            logger.debug("二级缓存删除: key={}", key);
            return value;
        } catch (Exception e) {
            logger.error("删除Redis缓存异常: key={}", key, e);
            return null;
        }
    }
    
    @Override
    public void removeAll(Collection<K> keys) {
        if (keys.isEmpty()) {
            return;
        }
        
        List<String> redisKeyList = keys.stream()
                .map(this::buildKey)
                .collect(java.util.stream.Collectors.toList());
        String[] redisKeys = redisKeyList.toArray(new String[0]);
        
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(redisKeys);
            stats.setSize(jedis.dbSize());
            logger.debug("二级缓存批量删除: keys={}", keys.size());
        } catch (Exception e) {
            logger.error("批量删除Redis缓存异常: keys={}", keys, e);
        }
    }
    
    @Override
    public boolean containsKey(K key) {
        String redisKey = buildKey(key);
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(redisKey);
        } catch (Exception e) {
            logger.error("检查Redis缓存键存在异常: key={}", key, e);
            return false;
        }
    }
    
    @Override
    public long size() {
        try (Jedis jedis = jedisPool.getResource()) {
            long size = jedis.dbSize();
            stats.setSize(size);
            return size;
        } catch (Exception e) {
            logger.error("获取Redis缓存大小异常", e);
            return 0;
        }
    }
    
    @Override
    public void clear() {
        try (Jedis jedis = jedisPool.getResource()) {
            String pattern = keyPrefix + "*";
            long deletedCount = 0;
            String cursor = "0";
            do {
                ScanResult<String> scanResult = jedis.scan(cursor, new ScanParams().match(pattern).count(100));
                cursor = scanResult.getCursor();
                if (!scanResult.getResult().isEmpty()) {
                    deletedCount += jedis.del(scanResult.getResult().toArray(new String[0]));
                }
            } while (!"0".equals(cursor));
            
            stats.setSize(jedis.dbSize());
            logger.info("二级缓存清空: {}, 删除键数: {}", name, deletedCount);
        } catch (Exception e) {
            logger.error("清空Redis缓存异常: {}", name, e);
        }
    }
    
    @Override
    public CacheStats getStats() {
        stats.setSize(size());
        return stats;
    }
    
    @Override
    public void refresh(K key) {
        remove(key);
        logger.debug("二级缓存刷新: key={}", key);
    }
    
    @Override
    public void refreshAll(Collection<K> keys) {
        removeAll(keys);
        logger.debug("二级缓存批量刷新: keys={}", keys.size());
    }
    
    /**
     * 构建Redis键
     */
    private String buildKey(K key) {
        return keyPrefix + key.toString();
    }
    
    /**
     * 序列化值
     */
    private String serialize(V value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            logger.error("序列化缓存值失败: value={}", value, e);
            return null;
        }
    }
    
    /**
     * 反序列化值
     */
    @SuppressWarnings("unchecked")
    private V deserialize(String value) {
        try {
            return (V) objectMapper.readValue(value, Object.class);
        } catch (JsonProcessingException e) {
            logger.error("反序列化缓存值失败: value={}", value, e);
            return null;
        }
    }
}
