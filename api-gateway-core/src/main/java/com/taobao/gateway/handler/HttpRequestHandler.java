package com.taobao.gateway.handler;

import com.taobao.gateway.filter.FilterChain;
import com.taobao.gateway.router.RouteManager;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * HTTP请求处理器
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);

    @Autowired
    private FilterChain filterChain;

    @Autowired
    private RouteManager routeManager;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        logger.debug("收到HTTP请求: {} {}", request.method(), request.uri());

        // 异步处理请求
        CompletableFuture<FullHttpResponse> future = CompletableFuture.supplyAsync(() -> {
            try {
                // 执行过滤器链
                return filterChain.doFilter(request);
            } catch (Exception e) {
                logger.error("处理请求时发生错误", e);
                return createErrorResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
            }
        });
        
        future.thenAccept(response -> {
            // 发送响应
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }).exceptionally(throwable -> {
            logger.error("异步处理请求时发生错误", throwable);
            FullHttpResponse errorResponse = createErrorResponse(
                    HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
            ctx.writeAndFlush(errorResponse).addListener(ChannelFutureListener.CLOSE);
            return null;
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("HTTP请求处理异常", cause);
        
        // 发送错误响应
        FullHttpResponse response = createErrorResponse(
                HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                logger.warn("连接空闲超时，关闭连接");
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("客户端连接建立: {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("客户端连接断开: {}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }

    /**
     * 创建错误响应
     */
    private FullHttpResponse createErrorResponse(HttpResponseStatus status, String message) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer(message, CharsetUtil.UTF_8)
        );
        
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        
        return response;
    }
} 