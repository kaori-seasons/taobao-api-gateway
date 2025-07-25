package com.taobao.gateway.cache;

import com.taobao.gateway.cache.impl.DefaultCacheManager;
import com.taobao.gateway.cache.impl.RedisConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Jedis;

/**
 * 缓存模块自动配置类
 * 提供Spring Boot自动配置支持
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Configuration
@EnableConfigurationProperties(CacheConfig.class)
@ConditionalOnProperty(prefix = "cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheAutoConfiguration {

    /**
     * 配置Redis连接工厂
     */
    @Bean
    @ConditionalOnClass(Jedis.class)
    @ConditionalOnProperty(prefix = "cache.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedisConnectionFactory redisConnectionFactory(CacheConfig cacheConfig) {
        CacheConfig.RedisConfig redisConfig = cacheConfig.getRedis();
        if (redisConfig != null) {
            return new RedisConnectionFactory(
                    redisConfig.getHost(),
                    redisConfig.getPort(),
                    redisConfig.getPassword(),
                    redisConfig.getDatabase()
            );
        }
        return new RedisConnectionFactory();
    }

    /**
     * 配置缓存管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager(CacheConfig cacheConfig, RedisConnectionFactory redisConnectionFactory) {
        return new DefaultCacheManager(cacheConfig, redisConnectionFactory);
    }

    /**
     * 配置缓存管理器（无Redis）
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "cache.redis", name = "enabled", havingValue = "false")
    public CacheManager cacheManagerWithoutRedis(CacheConfig cacheConfig) {
        return new DefaultCacheManager(cacheConfig, null);
    }
} 