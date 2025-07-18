package com.taobao.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Configuration
@ConfigurationProperties(prefix = "thread-pool")
public class ThreadPoolConfig {

    /**
     * 核心线程数
     */
    private int corePoolSize = 20;

    /**
     * 最大线程数
     */
    private int maxPoolSize = 100;

    /**
     * 队列容量
     */
    private int queueCapacity = 1000;

    /**
     * 线程空闲时间（秒）
     */
    private int keepAliveSeconds = 60;

    /**
     * 线程名前缀
     */
    private String threadNamePrefix = "gateway-";

    /**
     * 是否允许核心线程超时
     */
    private boolean allowCoreThreadTimeOut = true;

    /**
     * 等待所有任务结束后再关闭线程池
     */
    private boolean waitForTasksToCompleteOnShutdown = true;

    /**
     * 等待时间（秒）
     */
    private int awaitTerminationSeconds = 60;

    /**
     * 拒绝策略
     */
    private String rejectedExecutionHandler = "CALLER_RUNS";

    /**
     * 业务线程池
     */
    @Bean("businessTaskExecutor")
    public Executor businessTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix(threadNamePrefix + "business-");
        executor.setAllowCoreThreadTimeOut(allowCoreThreadTimeOut);
        executor.setWaitForTasksToCompleteOnShutdown(waitForTasksToCompleteOnShutdown);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.setRejectedExecutionHandler(getRejectedExecutionHandler());
        executor.initialize();
        return executor;
    }

    /**
     * 路由处理线程池
     */
    @Bean("routeTaskExecutor")
    public Executor routeTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize / 2);
        executor.setMaxPoolSize(maxPoolSize / 2);
        executor.setQueueCapacity(queueCapacity / 2);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix(threadNamePrefix + "route-");
        executor.setAllowCoreThreadTimeOut(allowCoreThreadTimeOut);
        executor.setWaitForTasksToCompleteOnShutdown(waitForTasksToCompleteOnShutdown);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.setRejectedExecutionHandler(getRejectedExecutionHandler());
        executor.initialize();
        return executor;
    }

    /**
     * 监控线程池
     */
    @Bean("monitorTaskExecutor")
    public Executor monitorTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix(threadNamePrefix + "monitor-");
        executor.setAllowCoreThreadTimeOut(allowCoreThreadTimeOut);
        executor.setWaitForTasksToCompleteOnShutdown(waitForTasksToCompleteOnShutdown);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.setRejectedExecutionHandler(getRejectedExecutionHandler());
        executor.initialize();
        return executor;
    }

    /**
     * 获取拒绝策略
     */
    private ThreadPoolExecutor.CallerRunsPolicy getRejectedExecutionHandler() {
        return new ThreadPoolExecutor.CallerRunsPolicy();
    }

    // Getter和Setter方法
    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public int getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    public void setKeepAliveSeconds(int keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }

    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    public boolean isAllowCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
        this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
    }

    public boolean isWaitForTasksToCompleteOnShutdown() {
        return waitForTasksToCompleteOnShutdown;
    }

    public void setWaitForTasksToCompleteOnShutdown(boolean waitForTasksToCompleteOnShutdown) {
        this.waitForTasksToCompleteOnShutdown = waitForTasksToCompleteOnShutdown;
    }

    public int getAwaitTerminationSeconds() {
        return awaitTerminationSeconds;
    }

    public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
        this.awaitTerminationSeconds = awaitTerminationSeconds;
    }

    public String getRejectedExecutionHandlerName() {
        return rejectedExecutionHandler;
    }

    public void setRejectedExecutionHandler(String rejectedExecutionHandler) {
        this.rejectedExecutionHandler = rejectedExecutionHandler;
    }
} 