package com.taobao.gateway.filter;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * 过滤器链接口
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface FilterChain {

    /**
     * 执行过滤器链
     * 
     * @param request HTTP请求
     * @return HTTP响应
     */
    FullHttpResponse doFilter(FullHttpRequest request);

    /**
     * 添加过滤器
     * 
     * @param filter 过滤器
     */
    void addFilter(Filter filter);

    /**
     * 移除过滤器
     * 
     * @param filter 过滤器
     */
    void removeFilter(Filter filter);

    /**
     * 清空所有过滤器
     */
    void clearFilters();
} 