package com.taobao.gateway.sdk.transport;

import com.taobao.gateway.sdk.model.GatewayResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP传输处理器
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class HttpTransportHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpTransportHandler.class);
    
    /**
     * 响应Future的AttributeKey
     */
    public static final AttributeKey<CompletableFuture<GatewayResponse>> RESPONSE_FUTURE_KEY = 
            AttributeKey.valueOf("responseFuture");
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) {
        try {
            // 获取响应Future
            CompletableFuture<GatewayResponse> future = ctx.channel().attr(RESPONSE_FUTURE_KEY).get();
            if (future == null) {
                logger.warn("未找到响应Future");
                return;
            }
            
            // 解析响应
            GatewayResponse gatewayResponse = parseResponse(response);
            
            // 完成Future
            future.complete(gatewayResponse);
            
        } catch (Exception e) {
            logger.error("处理HTTP响应失败: {}", e.getMessage(), e);
            CompletableFuture<GatewayResponse> future = ctx.channel().attr(RESPONSE_FUTURE_KEY).get();
            if (future != null) {
                future.completeExceptionally(e);
            }
        }
    }
    
    /**
     * 解析HTTP响应
     * 
     * @param response HTTP响应
     * @return 网关响应
     */
    private GatewayResponse parseResponse(FullHttpResponse response) {
        // 获取状态码
        int statusCode = response.status().code();
        
        // 获取响应体
        ByteBuf content = response.content();
        String body = content.readableBytes() > 0 ? 
                content.toString(StandardCharsets.UTF_8) : null;
        
        // 创建网关响应
        GatewayResponse gatewayResponse = new GatewayResponse(statusCode, body);
        
        // 设置响应头
        HttpHeaders headers = response.headers();
        headers.forEach(entry -> gatewayResponse.addHeader(entry.getKey(), entry.getValue()));
        
        // 设置响应时间
        gatewayResponse.setResponseTime(System.currentTimeMillis());
        
        return gatewayResponse;
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("HTTP传输处理器异常: {}", cause.getMessage(), cause);
        
        // 通知Future异常
        CompletableFuture<GatewayResponse> future = ctx.channel().attr(RESPONSE_FUTURE_KEY).get();
        if (future != null) {
            future.completeExceptionally(cause);
        }
        
        ctx.close();
    }
}
