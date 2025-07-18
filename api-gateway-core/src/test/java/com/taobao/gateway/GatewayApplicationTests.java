package com.taobao.gateway;

import com.taobao.gateway.config.NettyConfig;
import com.taobao.gateway.config.ThreadPoolConfig;
import com.taobao.gateway.filter.FilterChain;
import com.taobao.gateway.filter.DefaultFilterChain;
import com.taobao.gateway.filter.LogFilter;
import com.taobao.gateway.filter.RouteFilter;
import com.taobao.gateway.router.RouteManager;
import com.taobao.gateway.router.DefaultRouteManager;
import com.taobao.gateway.server.NettyServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 网关应用测试类
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootTest
@ActiveProfiles("test")
class GatewayApplicationTests {

    @Autowired(required = false)
    private NettyConfig nettyConfig;
    
    @Autowired(required = false)
    private ThreadPoolConfig threadPoolConfig;
    
    @Autowired(required = false)
    private NettyServer nettyServer;
    
    @Autowired(required = false)
    private FilterChain filterChain;
    
    @Autowired(required = false)
    private RouteManager routeManager;

    @Test
    void contextLoads() {
        // 测试Spring Boot应用上下文是否正常加载
        assertNotNull(nettyConfig, "NettyConfig should be loaded");
        assertNotNull(threadPoolConfig, "ThreadPoolConfig should be loaded");
    }
    
    @Test
    void testNettyConfig() {
        // 测试Netty配置
        assertNotNull(nettyConfig, "NettyConfig should not be null");
        assertEquals(8080, nettyConfig.getPort(), "Default port should be 8080");
        assertEquals(1, nettyConfig.getBossThreads(), "Boss threads should be 1");
        assertEquals(16, nettyConfig.getWorkerThreads(), "Worker threads should be 16");
    }
    
    @Test
    void testThreadPoolConfig() {
        // 测试线程池配置
        assertNotNull(threadPoolConfig, "ThreadPoolConfig should not be null");
        assertEquals(20, threadPoolConfig.getCorePoolSize(), "Core pool size should be 20");
        assertEquals(100, threadPoolConfig.getMaxPoolSize(), "Max pool size should be 100");
        assertEquals(1000, threadPoolConfig.getQueueCapacity(), "Queue capacity should be 1000");
    }
    
    @Test
    void testFilterChain() {
        // 测试过滤器链
        assertNotNull(filterChain, "FilterChain should not be null");
        
        // 验证过滤器链包含必要的过滤器
        assertTrue(((DefaultFilterChain) filterChain).getFilters().stream()
                .anyMatch(filter -> filter instanceof LogFilter), 
                "FilterChain should contain LogFilter");
        
        assertTrue(((DefaultFilterChain) filterChain).getFilters().stream()
                .anyMatch(filter -> filter instanceof RouteFilter), 
                "FilterChain should contain RouteFilter");
    }
    
    @Test
    void testRouteManager() {
        // 测试路由管理器
        assertNotNull(routeManager, "RouteManager should not be null");
        assertTrue(routeManager instanceof DefaultRouteManager, 
                "RouteManager should be instance of DefaultRouteManager");
    }
    
    @Test
    void testNettyServer() {
        // 测试Netty服务器
        assertNotNull(nettyServer, "NettyServer should not be null");
        // 注意：在实际测试中，我们不会启动服务器，只是验证配置
    }
} 