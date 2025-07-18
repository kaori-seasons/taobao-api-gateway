package com.taobao.gateway.loadbalancer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 一致性哈希负载均衡器配置
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
@ConfigurationProperties(prefix = "gateway.loadbalancer.consistent-hash")
public class ConsistentHashConfig {
    
    /**
     * 是否启用一致性哈希负载均衡器
     */
    private boolean enabled = true;
    
    /**
     * 虚拟节点数量
     */
    private int virtualNodes = 150;
    
    /**
     * 哈希算法名称
     */
    private String hashAlgorithm = "MD5";
    
    /**
     * 是否启用权重感知
     */
    private boolean weightAware = true;
    
    /**
     * 是否启用哈希环缓存
     */
    private boolean enableCache = true;
    
    /**
     * 哈希环缓存过期时间（毫秒）
     */
    private long cacheExpireTime = 300000; // 5分钟
    
    /**
     * 是否启用监控统计
     */
    private boolean enableStatistics = true;
    
    /**
     * 统计信息清理间隔（毫秒）
     */
    private long statisticsCleanupInterval = 3600000; // 1小时
    
    /**
     * 默认构造函数
     */
    public ConsistentHashConfig() {
    }
    
    // Getter和Setter方法
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getVirtualNodes() {
        return virtualNodes;
    }
    
    public void setVirtualNodes(int virtualNodes) {
        this.virtualNodes = virtualNodes;
    }
    
    public String getHashAlgorithm() {
        return hashAlgorithm;
    }
    
    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }
    
    public boolean isWeightAware() {
        return weightAware;
    }
    
    public void setWeightAware(boolean weightAware) {
        this.weightAware = weightAware;
    }
    
    public boolean isEnableCache() {
        return enableCache;
    }
    
    public void setEnableCache(boolean enableCache) {
        this.enableCache = enableCache;
    }
    
    public long getCacheExpireTime() {
        return cacheExpireTime;
    }
    
    public void setCacheExpireTime(long cacheExpireTime) {
        this.cacheExpireTime = cacheExpireTime;
    }
    
    public boolean isEnableStatistics() {
        return enableStatistics;
    }
    
    public void setEnableStatistics(boolean enableStatistics) {
        this.enableStatistics = enableStatistics;
    }
    
    public long getStatisticsCleanupInterval() {
        return statisticsCleanupInterval;
    }
    
    public void setStatisticsCleanupInterval(long statisticsCleanupInterval) {
        this.statisticsCleanupInterval = statisticsCleanupInterval;
    }
    
    @Override
    public String toString() {
        return "ConsistentHashConfig{" +
                "enabled=" + enabled +
                ", virtualNodes=" + virtualNodes +
                ", hashAlgorithm='" + hashAlgorithm + '\'' +
                ", weightAware=" + weightAware +
                ", enableCache=" + enableCache +
                ", cacheExpireTime=" + cacheExpireTime +
                ", enableStatistics=" + enableStatistics +
                ", statisticsCleanupInterval=" + statisticsCleanupInterval +
                '}';
    }
} 