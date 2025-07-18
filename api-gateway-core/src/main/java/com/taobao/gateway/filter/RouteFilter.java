package com.taobao.gateway.filter;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 路由过滤器
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class RouteFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RouteFilter.class);

    @Override
    public FullHttpResponse doFilter(FullHttpRequest request, FilterChain chain) {
        // 这里暂时直接执行下一个过滤器，后续会实现真正的路由逻辑
        logger.debug("路由过滤器处理请求: {}", request.uri());
        
        // 执行下一个过滤器
        return chain.doFilter(request);
    }

    @Override
    public int getOrder() {
        return 200; // 路由过滤器优先级中等
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
} 