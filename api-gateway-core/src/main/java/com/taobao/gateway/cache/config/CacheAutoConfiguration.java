package com.taobao.gateway.cache.config;

import com.taobao.gateway.cache.CacheManager;
import com.taobao.gateway.cache.impl.DefaultCacheManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 缓存自动配置类
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Configuration
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {
    
    /**
     * 创建Redis连接池
     */
    @Bean
    @ConditionalOnMissingBean
    public JedisPool jedisPool(CacheProperties properties) {
        if (!properties.getRedis().isEnabled()) {
            return null;
        }
        
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(5);
        poolConfig.setMaxWaitMillis(3000);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        
        // 这里应该从配置中读取Redis连接信息
        // 暂时使用默认配置
        return new JedisPool(poolConfig, "localhost", 6379, 3000);
    }
    
    /**
     * 创建缓存管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager(CacheProperties properties, JedisPool jedisPool) {
        return new DefaultCacheManager();
    }
} 