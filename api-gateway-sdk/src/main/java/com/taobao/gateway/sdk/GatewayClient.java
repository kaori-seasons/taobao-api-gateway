package com.taobao.gateway.sdk;

import com.taobao.gateway.sdk.config.GatewayClientConfig;
import com.taobao.gateway.sdk.exception.GatewayException;
import com.taobao.gateway.sdk.model.GatewayRequest;
import com.taobao.gateway.sdk.model.GatewayResponse;
import com.taobao.gateway.sdk.transport.HttpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * API网关客户端主类
 * <p>
 * 用于向API网关发送同步和异步请求，支持自定义配置和资源释放。
 * </p>
 *
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class GatewayClient {

    private static final Logger logger = LoggerFactory.getLogger(GatewayClient.class);

    /** 客户端配置 */
    private final GatewayClientConfig config;
    /** HTTP传输层 */
    private final HttpTransport transport;
    /** 线程池（用于异步请求） */
    private final ExecutorService executorService;

    /**
     * 构造函数
     *
     * @param config 客户端配置
     */
    public GatewayClient(GatewayClientConfig config) {
        this.config = config;
        this.transport = new HttpTransport(config);
        this.executorService = Executors.newFixedThreadPool(config.getThreadPoolSize());
    }

    /**
     * 同步发送请求到API网关
     *
     * @param request 网关请求对象
     * @return 网关响应对象
     * @throws GatewayException 发送失败时抛出
     */
    public GatewayResponse send(GatewayRequest request) throws GatewayException {
        logger.debug("同步发送请求: {}", request);
        return transport.send(request);
    }

    /**
     * 异步发送请求到API网关
     *
     * @param request 网关请求对象
     * @return CompletableFuture<GatewayResponse> 异步响应
     */
    public CompletableFuture<GatewayResponse> sendAsync(GatewayRequest request) {
        logger.debug("异步发送请求: {}", request);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return transport.send(request);
            } catch (Exception e) {
                logger.error("异步请求失败: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    /**
     * 关闭客户端，释放资源
     */
    public void close() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (transport != null) {
            transport.close();
        }
        logger.info("网关客户端已关闭");
    }

    /**
     * 获取客户端配置
     *
     * @return GatewayClientConfig
     */
    public GatewayClientConfig getConfig() {
        return config;
    }

    /**
     * 获取HTTP传输层
     *
     * @return HttpTransport
     */
    public HttpTransport getTransport() {
        return transport;
    }
} 