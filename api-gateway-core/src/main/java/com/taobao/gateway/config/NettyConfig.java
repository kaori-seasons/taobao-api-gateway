package com.taobao.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Netty服务器配置类
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Configuration
@ConfigurationProperties(prefix = "netty")
public class NettyConfig {

    /**
     * 服务器监听端口
     */
    private int port = 8080;

    /**
     * Boss线程数（接收连接的线程）
     */
    private int bossThreads = 1;

    /**
     * Worker线程数（处理IO的线程）
     */
    private int workerThreads = 16;

    /**
     * 连接队列大小
     */
    private int backlog = 1024;

    /**
     * 连接超时时间（毫秒）
     */
    private int connectionTimeout = 30000;

    /**
     * 读取超时时间（毫秒）
     */
    private int readTimeout = 60000;

    /**
     * 写入超时时间（毫秒）
     */
    private int writeTimeout = 60000;

    /**
     * 是否启用TCP_NODELAY
     */
    private boolean tcpNoDelay = true;

    /**
     * 是否启用SO_KEEPALIVE
     */
    private boolean keepAlive = true;

    /**
     * 是否启用SO_REUSEADDR
     */
    private boolean reuseAddr = true;

    // Getter和Setter方法
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getBossThreads() {
        return bossThreads;
    }

    public void setBossThreads(int bossThreads) {
        this.bossThreads = bossThreads;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getWriteTimeout() {
        return writeTimeout;
    }

    public void setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public boolean isReuseAddr() {
        return reuseAddr;
    }

    public void setReuseAddr(boolean reuseAddr) {
        this.reuseAddr = reuseAddr;
    }

    @Override
    public String toString() {
        return "NettyConfig{" +
                "port=" + port +
                ", bossThreads=" + bossThreads +
                ", workerThreads=" + workerThreads +
                ", backlog=" + backlog +
                ", connectionTimeout=" + connectionTimeout +
                ", readTimeout=" + readTimeout +
                ", writeTimeout=" + writeTimeout +
                ", tcpNoDelay=" + tcpNoDelay +
                ", keepAlive=" + keepAlive +
                ", reuseAddr=" + reuseAddr +
                '}';
    }
} 