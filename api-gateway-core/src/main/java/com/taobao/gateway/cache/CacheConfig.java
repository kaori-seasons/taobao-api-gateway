package com.taobao.gateway.cache;

import java.time.Duration;

/**
 * 缓存配置类
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class CacheConfig {
    
    /**
     * 缓存名称
     */
    private String name;
    
    /**
     * 是否启用一级缓存（本地缓存）
     */
    private boolean l1Enabled = true;
    
    /**
     * 是否启用二级缓存（Redis缓存）
     */
    private boolean l2Enabled = true;
    
    /**
     * 一级缓存最大大小
     */
    private long l1MaxSize = 1000;
    
    /**
     * 一级缓存过期时间
     */
    private Duration l1ExpireAfterWrite = Duration.ofMinutes(30);
    
    /**
     * 一级缓存访问后过期时间
     */
    private Duration l1ExpireAfterAccess = Duration.ofMinutes(10);
    
    /**
     * 二级缓存过期时间
     */
    private Duration l2ExpireAfterWrite = Duration.ofHours(2);
    
    /**
     * 是否启用缓存统计
     */
    private boolean statsEnabled = true;
    
    /**
     * 是否启用缓存预热
     */
    private boolean warmUpEnabled = false;
    
    /**
     * 缓存键前缀
     */
    private String keyPrefix = "";
    
    /**
     * 是否启用空值缓存
     */
    private boolean cacheNullValues = false;
    
    /**
     * 缓存更新模式
     */
    private CacheUpdateMode updateMode = CacheUpdateMode.WRITE_THROUGH;
    
    /**
     * 缓存驱逐策略
     */
    private CacheEvictionPolicy evictionPolicy = CacheEvictionPolicy.LRU;
    
    /**
     * Redis配置
     */
    private RedisConfig redis = new RedisConfig();
    
    public CacheConfig() {
    }
    
    public CacheConfig(String name) {
        this.name = name;
    }
    
    // Getter和Setter方法
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
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
    
    public long getL1MaxSize() {
        return l1MaxSize;
    }
    
    public void setL1MaxSize(long l1MaxSize) {
        this.l1MaxSize = l1MaxSize;
    }
    
    public Duration getL1ExpireAfterWrite() {
        return l1ExpireAfterWrite;
    }
    
    public void setL1ExpireAfterWrite(Duration l1ExpireAfterWrite) {
        this.l1ExpireAfterWrite = l1ExpireAfterWrite;
    }
    
    public Duration getL1ExpireAfterAccess() {
        return l1ExpireAfterAccess;
    }
    
    public void setL1ExpireAfterAccess(Duration l1ExpireAfterAccess) {
        this.l1ExpireAfterAccess = l1ExpireAfterAccess;
    }
    
    public Duration getL2ExpireAfterWrite() {
        return l2ExpireAfterWrite;
    }
    
    public void setL2ExpireAfterWrite(Duration l2ExpireAfterWrite) {
        this.l2ExpireAfterWrite = l2ExpireAfterWrite;
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
    
    public String getKeyPrefix() {
        return keyPrefix;
    }
    
    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }
    
    public boolean isCacheNullValues() {
        return cacheNullValues;
    }
    
    public void setCacheNullValues(boolean cacheNullValues) {
        this.cacheNullValues = cacheNullValues;
    }
    
    public CacheUpdateMode getUpdateMode() {
        return updateMode;
    }
    
    public void setUpdateMode(CacheUpdateMode updateMode) {
        this.updateMode = updateMode;
    }
    
    public CacheEvictionPolicy getEvictionPolicy() {
        return evictionPolicy;
    }
    
    public void setEvictionPolicy(CacheEvictionPolicy evictionPolicy) {
        this.evictionPolicy = evictionPolicy;
    }
    
    public RedisConfig getRedis() {
        return redis;
    }
    
    public void setRedis(RedisConfig redis) {
        this.redis = redis;
    }
    
    /**
     * Redis配置类
     */
    public static class RedisConfig {
        /**
         * 是否启用Redis
         */
        private boolean enabled = true;
        
        /**
         * Redis主机地址
         */
        private String host = "localhost";
        
        /**
         * Redis端口
         */
        private int port = 6379;
        
        /**
         * Redis密码
         */
        private String password;
        
        /**
         * Redis数据库索引
         */
        private int database = 0;
        
        /**
         * 连接超时时间（毫秒）
         */
        private int connectionTimeout = 2000;
        
        /**
         * 最大连接数
         */
        private int maxTotal = 100;
        
        /**
         * 最大空闲连接数
         */
        private int maxIdle = 20;
        
        /**
         * 最小空闲连接数
         */
        private int minIdle = 5;
        
        /**
         * 最大等待时间（毫秒）
         */
        private int maxWait = 3000;
        
        // Getter和Setter方法
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getHost() {
            return host;
        }
        
        public void setHost(String host) {
            this.host = host;
        }
        
        public int getPort() {
            return port;
        }
        
        public void setPort(int port) {
            this.port = port;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public int getDatabase() {
            return database;
        }
        
        public void setDatabase(int database) {
            this.database = database;
        }
        
        public int getConnectionTimeout() {
            return connectionTimeout;
        }
        
        public void setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }
        
        public int getMaxTotal() {
            return maxTotal;
        }
        
        public void setMaxTotal(int maxTotal) {
            this.maxTotal = maxTotal;
        }
        
        public int getMaxIdle() {
            return maxIdle;
        }
        
        public void setMaxIdle(int maxIdle) {
            this.maxIdle = maxIdle;
        }
        
        public int getMinIdle() {
            return minIdle;
        }
        
        public void setMinIdle(int minIdle) {
            this.minIdle = minIdle;
        }
        
        public int getMaxWait() {
            return maxWait;
        }
        
        public void setMaxWait(int maxWait) {
            this.maxWait = maxWait;
        }
    }
    
    /**
     * 缓存更新模式枚举
     */
    public enum CacheUpdateMode {
        /**
         * 写穿模式：同时更新一级和二级缓存
         */
        WRITE_THROUGH,
        
        /**
         * 写回模式：先更新一级缓存，异步更新二级缓存
         */
        WRITE_BACK,
        
        /**
         * 写分配模式：只更新一级缓存，不更新二级缓存
         */
        WRITE_AROUND
    }
    
    /**
     * 缓存驱逐策略枚举
     */
    public enum CacheEvictionPolicy {
        /**
         * 最近最少使用
         */
        LRU,
        
        /**
         * 最近最常使用
         */
        LFU,
        
        /**
         * 先进先出
         */
        FIFO,
        
        /**
         * 随机驱逐
         */
        RANDOM
    }
}
