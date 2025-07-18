package com.taobao.gateway.router;

import com.taobao.gateway.router.Route;

/**
 * 路由匹配器接口
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface RouteMatcher {
    
    /**
     * 匹配路由
     * 
     * @param path 请求路径
     * @param method HTTP方法
     * @return 匹配的路由，如果没有匹配则返回null
     */
    Route match(String path, String method);
    
    /**
     * 添加路由
     * 
     * @param route 路由配置
     */
    void addRoute(Route route);
    
    /**
     * 移除路由
     * 
     * @param routeId 路由ID
     */
    void removeRoute(String routeId);
    
    /**
     * 更新路由
     * 
     * @param route 路由配置
     */
    void updateRoute(Route route);
    
    /**
     * 获取所有路由
     * 
     * @return 路由列表
     */
    java.util.List<Route> getAllRoutes();
} 