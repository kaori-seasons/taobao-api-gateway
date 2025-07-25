package com.taobao.gateway.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * 缓存管理控制器
 * 提供REST API接口来管理缓存
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/cache")
public class CacheController {

    @Autowired
    private CacheService cacheService;

    /**
     * 获取所有缓存名称
     */
    @GetMapping("/names")
    public ResponseEntity<Collection<String>> getCacheNames() {
        return ResponseEntity.ok(cacheService.getCacheNames());
    }

    /**
     * 获取缓存统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAllStats() {
        return ResponseEntity.ok(cacheService.getAllStats());
    }

    /**
     * 获取指定缓存的统计信息
     */
    @GetMapping("/{cacheName}/stats")
    public ResponseEntity<CacheStats> getCacheStats(@PathVariable String cacheName) {
        CacheStats stats = cacheService.getStats(cacheName);
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取缓存值
     */
    @GetMapping("/{cacheName}/{key}")
    public ResponseEntity<Object> getCacheValue(@PathVariable String cacheName, @PathVariable String key) {
        Object value = cacheService.get(cacheName, key);
        if (value != null) {
            return ResponseEntity.ok(value);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 设置缓存值
     */
    @PostMapping("/{cacheName}/{key}")
    public ResponseEntity<Void> setCacheValue(
            @PathVariable String cacheName,
            @PathVariable String key,
            @RequestBody Object value) {
        cacheService.put(cacheName, key, value);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除缓存值
     */
    @DeleteMapping("/{cacheName}/{key}")
    public ResponseEntity<Object> removeCacheValue(@PathVariable String cacheName, @PathVariable String key) {
        Object removedValue = cacheService.remove(cacheName, key);
        if (removedValue != null) {
            return ResponseEntity.ok(removedValue);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 检查缓存键是否存在
     */
    @GetMapping("/{cacheName}/{key}/exists")
    public ResponseEntity<Boolean> containsKey(@PathVariable String cacheName, @PathVariable String key) {
        boolean exists = cacheService.containsKey(cacheName, key);
        return ResponseEntity.ok(exists);
    }

    /**
     * 获取缓存大小
     */
    @GetMapping("/{cacheName}/size")
    public ResponseEntity<Long> getCacheSize(@PathVariable String cacheName) {
        long size = cacheService.size(cacheName);
        return ResponseEntity.ok(size);
    }

    /**
     * 清空指定缓存
     */
    @DeleteMapping("/{cacheName}")
    public ResponseEntity<Void> clearCache(@PathVariable String cacheName) {
        cacheService.clear(cacheName);
        return ResponseEntity.ok().build();
    }

    /**
     * 清空所有缓存
     */
    @DeleteMapping
    public ResponseEntity<Void> clearAllCaches() {
        cacheService.clearAll();
        return ResponseEntity.ok().build();
    }

    /**
     * 刷新缓存
     */
    @PostMapping("/{cacheName}/{key}/refresh")
    public ResponseEntity<Void> refreshCache(@PathVariable String cacheName, @PathVariable String key) {
        cacheService.refresh(cacheName, key);
        return ResponseEntity.ok().build();
    }

    /**
     * 批量获取缓存值
     */
    @PostMapping("/{cacheName}/batch-get")
    public ResponseEntity<Map<String, Object>> batchGet(
            @PathVariable String cacheName,
            @RequestBody Collection<String> keys) {
        Map<String, Object> values = cacheService.getAll(cacheName, keys);
        return ResponseEntity.ok(values);
    }

    /**
     * 批量设置缓存值
     */
    @PostMapping("/{cacheName}/batch-set")
    public ResponseEntity<Void> batchSet(
            @PathVariable String cacheName,
            @RequestBody Map<String, Object> keyValueMap) {
        cacheService.putAll(cacheName, keyValueMap);
        return ResponseEntity.ok().build();
    }

    /**
     * 批量删除缓存值
     */
    @PostMapping("/{cacheName}/batch-remove")
    public ResponseEntity<Void> batchRemove(
            @PathVariable String cacheName,
            @RequestBody Collection<String> keys) {
        cacheService.removeAll(cacheName, keys);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除缓存
     */
    @DeleteMapping("/{cacheName}/remove")
    public ResponseEntity<Void> removeCache(@PathVariable String cacheName) {
        cacheService.removeCache(cacheName);
        return ResponseEntity.ok().build();
    }

    /**
     * 缓存健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "cacheCount", cacheService.getCacheNames().size(),
                "timestamp", System.currentTimeMillis()
        );
        return ResponseEntity.ok(health);
    }
} 