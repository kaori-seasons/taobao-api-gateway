package com.taobao.gateway.adaptive;

import com.taobao.gateway.loadbalancer.LoadBalancerFactory;
import com.taobao.gateway.loadbalancer.adaptive.AdaptiveLoadBalancer;
import com.taobao.gateway.loadbalancer.adaptive.AdaptiveLoadBalanceConfig;
import com.taobao.gateway.loadbalancer.adaptive.impl.AdaptiveLoadBalancerImpl;
import com.taobao.gateway.ratelimit.RateLimiter;
import com.taobao.gateway.ratelimit.adaptive.AdaptiveRateLimiter;
import com.taobao.gateway.ratelimit.adaptive.AdaptiveRateLimitConfig;
import com.taobao.gateway.ratelimit.adaptive.impl.AdaptiveRateLimiterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 自适应限流和负载均衡自动配置类
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
@Configuration
@EnableConfigurationProperties({AdaptiveRateLimitConfig.class, AdaptiveLoadBalanceConfig.class})
@ConditionalOnProperty(prefix = "gateway.adaptive", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdaptiveAutoConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(AdaptiveAutoConfiguration.class);
    
    /**
     * 创建调度器
     */
    @Bean
    public ScheduledExecutorService adaptiveScheduler() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "adaptive-scheduler");
            t.setDaemon(true);
            return t;
        });
        
        logger.info("创建自适应调度器");
        return scheduler;
    }
    
    /**
     * 创建自适应限流器
     */
    @Bean
    @ConditionalOnProperty(prefix = "gateway.adaptive.ratelimit", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AdaptiveRateLimiter adaptiveRateLimiter(AdaptiveRateLimitConfig config, 
                                                   ScheduledExecutorService scheduler) {
        AdaptiveRateLimiter rateLimiter = new AdaptiveRateLimiterImpl("adaptive-rate-limiter", config, scheduler);
        logger.info("创建自适应限流器: {}", config);
        return rateLimiter;
    }
    
    /**
     * 创建自适应负载均衡器
     */
    @Bean
    @ConditionalOnProperty(prefix = "gateway.adaptive.loadbalancer", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AdaptiveLoadBalancer adaptiveLoadBalancer(AdaptiveLoadBalanceConfig config, 
                                                     ScheduledExecutorService scheduler) {
        AdaptiveLoadBalancer loadBalancer = new AdaptiveLoadBalancerImpl("adaptive-load-balancer", config, scheduler);
        logger.info("创建自适应负载均衡器: {}", config);
        return loadBalancer;
    }
    
    /**
     * 注册自适应负载均衡器到工厂
     */
    @Bean
    public AdaptiveLoadBalancer adaptiveLoadBalancerRegistration(AdaptiveLoadBalancer adaptiveLoadBalancer) {
        return adaptiveLoadBalancer;
    }
} 