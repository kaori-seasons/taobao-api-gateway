package com.taobao.gateway.router;

import io.netty.handler.codec.http.FullHttpRequest;

/**
 * 路由管理器接口
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface RouteManager {

    /**
     * 路由请求
     * 
     * @param request HTTP请求
     * @return 路由结果
     */
    RouteResult route(FullHttpRequest request);

    /**
     * 添加路由规则
     * 
     * @param route 路由规则
     */
    void addRoute(Route route);

    /**
     * 移除路由规则
     * 
     * @param path 路径
     */
    void removeRoute(String path);

    /**
     * 更新路由规则
     * 
     * @param route 路由规则
     */
    void updateRoute(Route route);

    /**
     * 获取路由规则
     * 
     * @param path 路径
     * @return 路由规则
     */
    Route getRoute(String path);
} 