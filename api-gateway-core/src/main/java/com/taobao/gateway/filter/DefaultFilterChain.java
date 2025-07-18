package com.taobao.gateway.filter;

import com.taobao.gateway.router.RouteManager;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 默认过滤器链实现
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class DefaultFilterChain implements FilterChain {

    private static final Logger logger = LoggerFactory.getLogger(DefaultFilterChain.class);

    @Autowired
    private RouteManager routeManager;

    private final List<Filter> filters = new CopyOnWriteArrayList<>();
    private int currentIndex = 0;

    @PostConstruct
    public void init() {
        // 添加默认过滤器
        addFilter(new LogFilter());
        addFilter(new RouteFilter());
    }

    @Override
    public FullHttpResponse doFilter(FullHttpRequest request) {
        currentIndex = 0;
        return doFilterInternal(request);
    }

    /**
     * 内部过滤器执行方法
     */
    private FullHttpResponse doFilterInternal(FullHttpRequest request) {
        if (currentIndex >= filters.size()) {
            // 所有过滤器执行完毕，进行路由转发
            return routeRequest(request);
        }

        Filter filter = filters.get(currentIndex++);
        
        if (!filter.isEnabled()) {
            // 跳过禁用的过滤器
            return doFilterInternal(request);
        }

        try {
            logger.debug("执行过滤器: {}", filter.getClass().getSimpleName());
            return filter.doFilter(request, this);
        } catch (Exception e) {
            logger.error("过滤器执行异常: {}", filter.getClass().getSimpleName(), e);
            return createErrorResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Filter Error");
        }
    }

    /**
     * 路由请求到后端服务
     */
    private FullHttpResponse routeRequest(FullHttpRequest request) {
        try {
            // 这里暂时返回一个简单的响应，后续会实现真正的路由逻辑
            String responseBody = "Gateway Response: " + request.uri();
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(responseBody, CharsetUtil.UTF_8)
            );
            
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            
            return response;
        } catch (Exception e) {
            logger.error("路由请求时发生错误", e);
            return createErrorResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Routing Error");
        }
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

    @Override
    public void addFilter(Filter filter) {
        filters.add(filter);
        // 按顺序排序
        filters.sort(Comparator.comparingInt(Filter::getOrder));
        logger.info("添加过滤器: {}, 顺序: {}", filter.getClass().getSimpleName(), filter.getOrder());
    }

    @Override
    public void removeFilter(Filter filter) {
        filters.remove(filter);
        logger.info("移除过滤器: {}", filter.getClass().getSimpleName());
    }

    @Override
    public void clearFilters() {
        filters.clear();
        logger.info("清空所有过滤器");
    }

    /**
     * 获取所有过滤器
     */
    public List<Filter> getFilters() {
        return new ArrayList<>(filters);
    }
} 