package com.taobao.gateway.filter;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 日志过滤器
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class LogFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(LogFilter.class);

    @Override
    public FullHttpResponse doFilter(FullHttpRequest request, FilterChain chain) {
        long startTime = System.currentTimeMillis();
        
        // 记录请求日志
        logger.info("收到请求: {} {} - 客户端: {}", 
                request.method(), request.uri(), request.headers().get("User-Agent", "Unknown"));

        // 执行下一个过滤器
        FullHttpResponse response = chain.doFilter(request);
        
        // 计算处理时间
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 记录响应日志
        logger.info("请求处理完成: {} {} - 状态: {} - 耗时: {}ms", 
                request.method(), request.uri(), response.status(), duration);
        
        return response;
    }

    @Override
    public int getOrder() {
        return 100; // 日志过滤器优先级较高
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
} 