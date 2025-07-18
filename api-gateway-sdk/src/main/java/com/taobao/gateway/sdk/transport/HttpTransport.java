package com.taobao.gateway.sdk.transport;

import com.taobao.gateway.sdk.config.GatewayClientConfig;
import com.taobao.gateway.sdk.exception.GatewayException;
import com.taobao.gateway.sdk.model.GatewayRequest;
import com.taobao.gateway.sdk.model.GatewayResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * HTTP传输层实现
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class HttpTransport {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpTransport.class);
    
    private final GatewayClientConfig config;
    private final EventLoopGroup eventLoopGroup;
    private final Bootstrap bootstrap;
    
    /**
     * 构造函数
     * 
     * @param config 客户端配置
     */
    public HttpTransport(GatewayClientConfig config) {
        this.config = config;
        this.eventLoopGroup = new NioEventLoopGroup(config.getThreadPoolSize());
        this.bootstrap = createBootstrap();
    }
    
    /**
     * 创建Bootstrap
     * 
     * @return Bootstrap实例
     */
    private Bootstrap createBootstrap() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) config.getConnectTimeout().toMillis())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new IdleStateHandler(
                                (int) config.getConnectionIdleTimeout().getSeconds(),
                                0, 0, TimeUnit.SECONDS));
                        pipeline.addLast(new HttpClientCodec());
                        pipeline.addLast(new HttpObjectAggregator(65536));
                        pipeline.addLast(new HttpTransportHandler());
                    }
                });
        return bootstrap;
    }
    
    /**
     * 发送请求
     * 
     * @param request 网关请求
     * @return 网关响应
     * @throws GatewayException 网关异常
     */
    public GatewayResponse send(GatewayRequest request) throws GatewayException {
        try {
            URI uri = new URI(config.getGatewayUrl() + request.getPath());
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : (config.isEnableSsl() ? 443 : 80);
            
            // 创建HTTP请求
            FullHttpRequest httpRequest = createHttpRequest(request, uri);
            
            // 连接服务器
            ChannelFuture connectFuture = bootstrap.connect(host, port);
            Channel channel = connectFuture.sync().channel();
            
            // 发送请求
            CompletableFuture<GatewayResponse> responseFuture = new CompletableFuture<>();
            channel.attr(HttpTransportHandler.RESPONSE_FUTURE_KEY).set(responseFuture);
            
            channel.writeAndFlush(httpRequest).sync();
            
            // 等待响应
            GatewayResponse response = responseFuture.get(request.getTimeout(), TimeUnit.MILLISECONDS);
            
            // 关闭连接
            channel.close().sync();
            
            return response;
            
        } catch (Exception e) {
            logger.error("发送HTTP请求失败: {}", e.getMessage(), e);
            throw new GatewayException("发送HTTP请求失败", e);
        }
    }
    
    /**
     * 创建HTTP请求
     * 
     * @param request 网关请求
     * @param uri URI
     * @return HTTP请求
     */
    private FullHttpRequest createHttpRequest(GatewayRequest request, URI uri) {
        // 构建查询字符串
        StringBuilder queryString = new StringBuilder();
        if (request.getQueryParams() != null && !request.getQueryParams().isEmpty()) {
            queryString.append("?");
            request.getQueryParams().forEach((key, value) -> {
                if (queryString.length() > 1) {
                    queryString.append("&");
                }
                queryString.append(key).append("=").append(value);
            });
        }
        
        // 创建HTTP请求
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        String uriString = uri.getPath() + queryString.toString();
        
        FullHttpRequest httpRequest;
        if (request.getBody() != null && !request.getBody().isEmpty()) {
            ByteBuf content = Unpooled.copiedBuffer(request.getBody(), StandardCharsets.UTF_8);
            httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uriString, content);
        } else {
            httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uriString);
        }
        
        // 设置请求头
        httpRequest.headers().set(HttpHeaderNames.HOST, uri.getHost());
        httpRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        httpRequest.headers().set(HttpHeaderNames.ACCEPT, "*/*");
        httpRequest.headers().set(HttpHeaderNames.USER_AGENT, "Taobao-Gateway-Client/1.0.0");
        
        if (request.getBody() != null && !request.getBody().isEmpty()) {
            httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.getBody().length());
        }
        
        // 添加自定义请求头
        if (request.getHeaders() != null) {
            request.getHeaders().forEach(httpRequest.headers()::set);
        }
        
        // 添加认证信息
        if (config.getApiKey() != null) {
            httpRequest.headers().set("X-API-Key", config.getApiKey());
        }
        if (config.getAppId() != null) {
            httpRequest.headers().set("X-App-Id", config.getAppId());
        }
        
        return httpRequest;
    }
    
    /**
     * 关闭传输层
     */
    public void close() {
        if (eventLoopGroup != null && !eventLoopGroup.isShutdown()) {
            eventLoopGroup.shutdownGracefully();
        }
        logger.info("HTTP传输层已关闭");
    }
} 