package com.taobao.gateway.router;

import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认路由管理器实现
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class DefaultRouteManager implements RouteManager {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRouteManager.class);

    /**
     * 路由规则缓存
     */
    private final Map<String, Route> routeCache = new ConcurrentHashMap<>();

    @Override
    public RouteResult route(FullHttpRequest request) {
        String path = request.uri();
        logger.debug("路由请求: {}", path);

        // 查找匹配的路由规则
        Route matchedRoute = findMatchedRoute(path);
        
        if (matchedRoute != null) {
            logger.debug("找到匹配的路由: {}", matchedRoute);
            return RouteResult.success(matchedRoute);
        } else {
            logger.warn("未找到匹配的路由: {}", path);
            return RouteResult.failure("No route found for path: " + path);
        }
    }

    @Override
    public void addRoute(Route route) {
        if (route != null && route.getPath() != null) {
            routeCache.put(route.getPath(), route);
            logger.info("添加路由规则: {}", route);
        }
    }

    @Override
    public void removeRoute(String path) {
        Route removedRoute = routeCache.remove(path);
        if (removedRoute != null) {
            logger.info("移除路由规则: {}", removedRoute);
        }
    }

    @Override
    public void updateRoute(Route route) {
        if (route != null && route.getPath() != null) {
            routeCache.put(route.getPath(), route);
            logger.info("更新路由规则: {}", route);
        }
    }

    @Override
    public Route getRoute(String path) {
        return routeCache.get(path);
    }

    /**
     * 查找匹配的路由规则
     */
    private Route findMatchedRoute(String path) {
        // 首先尝试精确匹配
        Route exactRoute = routeCache.get(path);
        if (exactRoute != null && exactRoute.isEnabled()) {
            return exactRoute;
        }

        // 然后尝试前缀匹配
        for (Route route : routeCache.values()) {
            if (!route.isEnabled()) {
                continue;
            }

            if (route.getType() == RouteType.PREFIX && path.startsWith(route.getPath())) {
                return route;
            }
        }

        // 最后尝试正则匹配（这里暂时不实现，后续会添加）
        // TODO: 实现正则匹配

        return null;
    }

    /**
     * 获取所有路由规则
     */
    public Map<String, Route> getAllRoutes() {
        return new ConcurrentHashMap<>(routeCache);
    }

    /**
     * 清空所有路由规则
     */
    public void clearRoutes() {
        routeCache.clear();
        logger.info("清空所有路由规则");
    }
} 