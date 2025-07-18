package com.taobao.gateway.cache.impl;

import redis.clients.jedis.Jedis;

/**
 * Redis连接工厂接口
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface RedisConnectionFactory {
    
    /**
     * 获取Redis连接
     * 
     * @return Jedis连接
     */
    Jedis getConnection();
    
    /**
     * 释放Redis连接
     * 
     * @param jedis Jedis连接
     */
    void releaseConnection(Jedis jedis);
    
    /**
     * 关闭连接工厂
     */
    void close();
}
