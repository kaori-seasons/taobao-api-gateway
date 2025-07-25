package com.taobao.gateway.dispatcher;

import com.taobao.gateway.dispatcher.impl.DefaultRequestDispatcher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 分发层自动配置类
 * 用于Spring Boot自动配置分发层组件
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Configuration
@EnableConfigurationProperties(DispatcherConfig.class)
@ConditionalOnProperty(prefix = "gateway.dispatcher", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DispatcherAutoConfiguration {

    /**
     * 配置请求分发器
     */
    @Bean
    public DefaultRequestDispatcher requestDispatcher() {
        return new DefaultRequestDispatcher();
    }

    /**
     * 配置分发层服务器
     */
    @Bean
    public NettyDispatcherServer nettyDispatcherServer() {
        return new NettyDispatcherServer();
    }
} 