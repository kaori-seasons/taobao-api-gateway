package com.taobao.gateway;

import com.taobao.gateway.config.NettyConfig;
import com.taobao.gateway.server.NettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * API网关启动类
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.taobao.gateway")
public class GatewayApplication {

    private static final Logger logger = LoggerFactory.getLogger(GatewayApplication.class);

    public static void main(String[] args) {
        logger.info("正在启动API网关服务...");
        
        // 启动Spring Boot应用
        ApplicationContext context = SpringApplication.run(GatewayApplication.class, args);
        
        // 获取Netty服务器配置
        NettyConfig nettyConfig = context.getBean(NettyConfig.class);
        
        // 启动Netty服务器
        NettyServer nettyServer = context.getBean(NettyServer.class);
        try {
            nettyServer.start();
            logger.info("API网关服务启动成功，监听端口: {}", nettyConfig.getPort());
        } catch (Exception e) {
            logger.error("API网关服务启动失败", e);
            System.exit(1);
        }
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("正在关闭API网关服务...");
            try {
                nettyServer.stop();
                logger.info("API网关服务已关闭");
            } catch (Exception e) {
                logger.error("关闭API网关服务时发生错误", e);
            }
        }));
    }
} 