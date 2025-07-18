package com.taobao.gateway.filter;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * 过滤器接口
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface Filter {

    /**
     * 执行过滤逻辑
     * 
     * @param request HTTP请求
     * @param chain 过滤器链
     * @return HTTP响应
     */
    FullHttpResponse doFilter(FullHttpRequest request, FilterChain chain);

    /**
     * 获取过滤器顺序（数字越小优先级越高）
     * 
     * @return 过滤器顺序
     */
    int getOrder();

    /**
     * 是否启用该过滤器
     * 
     * @return 是否启用
     */
    default boolean isEnabled() {
        return true;
    }
} 