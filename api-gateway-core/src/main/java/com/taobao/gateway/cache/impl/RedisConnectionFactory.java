package com.taobao.gateway.cache.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

/**
 * Redis连接工厂
 * 提供Redis连接池管理功能
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class RedisConnectionFactory {

    private static final Logger logger = LoggerFactory.getLogger(RedisConnectionFactory.class);

    /**
     * Redis连接池
     */
    private final JedisPool jedisPool;

    /**
     * 构造函数
     */
    public RedisConnectionFactory() {
        this("localhost", 6379, null, 0);
    }

    /**
     * 构造函数
     * 
     * @param host Redis主机地址
     * @param port Redis端口
     */
    public RedisConnectionFactory(String host, int port) {
        this(host, port, null, 0);
    }

    /**
     * 构造函数
     * 
     * @param host Redis主机地址
     * @param port Redis端口
     * @param password Redis密码
     * @param database Redis数据库索引
     */
    public RedisConnectionFactory(String host, int port, String password, int database) {
        this(host, port, password, database, createDefaultPoolConfig());
    }

    /**
     * 构造函数
     * 
     * @param host Redis主机地址
     * @param port Redis端口
     * @param password Redis密码
     * @param database Redis数据库索引
     * @param poolConfig 连接池配置
     */
    public RedisConnectionFactory(String host, int port, String password, int database, JedisPoolConfig poolConfig) {
        this.jedisPool = new JedisPool(poolConfig, host, port, 2000, password, database);
        logger.info("初始化Redis连接工厂: {}:{}, database: {}", host, port, database);
    }

    /**
     * 获取Redis连接
     * 
     * @return Redis连接
     */
    public Jedis getConnection() {
        return jedisPool.getResource();
    }

    /**
     * 关闭Redis连接
     * 
     * @param jedis Redis连接
     */
    public void closeConnection(Jedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }

    /**
     * 关闭连接池
     */
    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            logger.info("关闭Redis连接池");
        }
    }

    /**
     * 获取连接池
     * 
     * @return 连接池
     */
    public JedisPool getJedisPool() {
        return jedisPool;
    }

    /**
     * 检查连接池状态
     * 
     * @return 是否正常
     */
    public boolean isHealthy() {
        try (Jedis jedis = getConnection()) {
            String result = jedis.ping();
            return "PONG".equals(result);
        } catch (Exception e) {
            logger.error("Redis连接池健康检查失败", e);
            return false;
        }
    }

    /**
     * 获取连接池统计信息
     * 
     * @return 统计信息
     */
    public String getPoolStats() {
        return jedisPool.toString();
    }

    /**
     * 创建默认连接池配置
     * 
     * @return 连接池配置
     */
    private static JedisPoolConfig createDefaultPoolConfig() {
        JedisPoolConfig config = new JedisPoolConfig();
        
        // 连接池配置
        config.setMaxTotal(100);                    // 最大连接数
        config.setMaxIdle(20);                      // 最大空闲连接数
        config.setMinIdle(5);                       // 最小空闲连接数
        config.setMaxWait(Duration.ofSeconds(3));   // 最大等待时间
        config.setTestOnBorrow(true);               // 借用连接时测试
        config.setTestOnReturn(true);               // 归还连接时测试
        config.setTestWhileIdle(true);              // 空闲时测试
        config.setTimeBetweenEvictionRuns(Duration.ofSeconds(30)); // 空闲连接检测间隔
        config.setMinEvictableIdleTime(Duration.ofMinutes(5));     // 最小空闲时间
        config.setNumTestsPerEvictionRun(3);        // 每次检测的连接数
        
        return config;
    }

    /**
     * 创建自定义连接池配置
     * 
     * @param maxTotal 最大连接数
     * @param maxIdle 最大空闲连接数
     * @param minIdle 最小空闲连接数
     * @param maxWaitSeconds 最大等待时间（秒）
     * @return 连接池配置
     */
    public static JedisPoolConfig createCustomPoolConfig(int maxTotal, int maxIdle, int minIdle, int maxWaitSeconds) {
        JedisPoolConfig config = new JedisPoolConfig();
        
        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setMaxWait(Duration.ofSeconds(maxWaitSeconds));
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setTestWhileIdle(true);
        config.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        config.setMinEvictableIdleTime(Duration.ofMinutes(5));
        config.setNumTestsPerEvictionRun(3);
        
        return config;
    }
}
