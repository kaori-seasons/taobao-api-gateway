package com.taobao.gateway.cache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存配置属性类
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Configuration
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {
    
    /**
     * Redis配置
     */
    private Redis redis = new Redis();
    
    /**
     * 本地缓存配置
     */
    private Local local = new Local();
    
    /**
     * 默认缓存配置
     */
    private Default defaultConfig = new Default();
    
    /**
     * 自定义缓存配置
     */
    private Map<String, Object> caches = new HashMap<>();
    
    public Redis getRedis() {
        return redis;
    }
    
    public void setRedis(Redis redis) {
        this.redis = redis;
    }
    
    public Local getLocal() {
        return local;
    }
    
    public void setLocal(Local local) {
        this.local = local;
    }
    
    public Default getDefaultConfig() {
        return defaultConfig;
    }
    
    public void setDefaultConfig(Default defaultConfig) {
        this.defaultConfig = defaultConfig;
    }
    
    public Map<String, Object> getCaches() {
        return caches;
    }
    
    public void setCaches(Map<String, Object> caches) {
        this.caches = caches;
    }
    
    /**
     * Redis配置
     */
    public static class Redis {
        /**
         * 默认TTL（秒）
         */
        private Duration defaultTtl = Duration.ofHours(1);
        
        /**
         * 键前缀
         */
        private String keyPrefix = "gateway:";
        
        /**
         * 是否启用
         */
        private boolean enabled = true;
        
        public Duration getDefaultTtl() {
            return defaultTtl;
        }
        
        public void setDefaultTtl(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
        }
        
        public String getKeyPrefix() {
            return keyPrefix;
        }
        
        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
    
    /**
     * 本地缓存配置
     */
    public static class Local {
        /**
         * 最大大小
         */
        private long maxSize = 1000;
        
        /**
         * 写入后过期时间
         */
        private Duration expireAfterWrite = Duration.ofMinutes(30);
        
        /**
         * 访问后过期时间
         */
        private Duration expireAfterAccess = Duration.ofMinutes(10);
        
        /**
         * 是否启用
         */
        private boolean enabled = true;
        
        public long getMaxSize() {
            return maxSize;
        }
        
        public void setMaxSize(long maxSize) {
            this.maxSize = maxSize;
        }
        
        public Duration getExpireAfterWrite() {
            return expireAfterWrite;
        }
        
        public void setExpireAfterWrite(Duration expireAfterWrite) {
            this.expireAfterWrite = expireAfterWrite;
        }
        
        public Duration getExpireAfterAccess() {
            return expireAfterAccess;
        }
        
        public void setExpireAfterAccess(Duration expireAfterAccess) {
            this.expireAfterAccess = expireAfterAccess;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
    
    /**
     * 默认配置
     */
    public static class Default {
        /**
         * 是否启用一级缓存
         */
        private boolean l1Enabled = true;
        
        /**
         * 是否启用二级缓存
         */
        private boolean l2Enabled = true;
        
        /**
         * 是否启用统计
         */
        private boolean statsEnabled = true;
        
        /**
         * 是否启用预热
         */
        private boolean warmUpEnabled = false;
        
        /**
         * 是否缓存空值
         */
        private boolean cacheNullValues = false;
        
        /**
         * 更新模式
         */
        private String updateMode = "WRITE_THROUGH";
        
        /**
         * 驱逐策略
         */
        private String evictionPolicy = "LRU";
        
        public boolean isL1Enabled() {
            return l1Enabled;
        }
        
        public void setL1Enabled(boolean l1Enabled) {
            this.l1Enabled = l1Enabled;
        }
        
        public boolean isL2Enabled() {
            return l2Enabled;
        }
        
        public void setL2Enabled(boolean l2Enabled) {
            this.l2Enabled = l2Enabled;
        }
        
        public boolean isStatsEnabled() {
            return statsEnabled;
        }
        
        public void setStatsEnabled(boolean statsEnabled) {
            this.statsEnabled = statsEnabled;
        }
        
        public boolean isWarmUpEnabled() {
            return warmUpEnabled;
        }
        
        public void setWarmUpEnabled(boolean warmUpEnabled) {
            this.warmUpEnabled = warmUpEnabled;
        }
        
        public boolean isCacheNullValues() {
            return cacheNullValues;
        }
        
        public void setCacheNullValues(boolean cacheNullValues) {
            this.cacheNullValues = cacheNullValues;
        }
        
        public String getUpdateMode() {
            return updateMode;
        }
        
        public void setUpdateMode(String updateMode) {
            this.updateMode = updateMode;
        }
        
        public String getEvictionPolicy() {
            return evictionPolicy;
        }
        
        public void setEvictionPolicy(String evictionPolicy) {
            this.evictionPolicy = evictionPolicy;
        }
    }
} 