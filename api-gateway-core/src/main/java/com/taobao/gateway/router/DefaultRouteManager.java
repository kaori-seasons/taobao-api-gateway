package com.taobao.gateway.router;

import com.taobao.gateway.router.impl.DefaultRouteMatcher;
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
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
     * 路由匹配器
     */
    @Autowired
    private DefaultRouteMatcher routeMatcher;

    @Override
    public RouteResult route(FullHttpRequest request) {
        String path = request.uri();
        String method = request.method().name();
        logger.debug("路由请求: {} {}", method, path);

        // 使用路由匹配器查找匹配的路由规则
        Route matchedRoute = routeMatcher.match(path, method);
        
        if (matchedRoute != null) {
            logger.debug("找到匹配的路由: {}", matchedRoute);
            return RouteResult.success(matchedRoute);
        } else {
            logger.warn("未找到匹配的路由: {} {}", method, path);
            return RouteResult.failure("No route found for path: " + path);
        }
    }

    @Override
    public void addRoute(Route route) {
        if (route != null && route.getPath() != null) {
            routeMatcher.addRoute(route);
            logger.info("添加路由规则: {}", route);
        }
    }

    @Override
    public void removeRoute(String path) {
        // 通过路径查找路由ID，然后移除
        List<Route> allRoutes = routeMatcher.getAllRoutes();
        for (Route route : allRoutes) {
            if (route.getPath().equals(path)) {
                routeMatcher.removeRoute(route.getId());
                logger.info("移除路由规则: {}", route);
                break;
            }
        }
    }

    @Override
    public void updateRoute(Route route) {
        if (route != null && route.getPath() != null) {
            routeMatcher.updateRoute(route);
            logger.info("更新路由规则: {}", route);
        }
    }

    @Override
    public Route getRoute(String path) {
        // 通过路径查找路由
        List<Route> allRoutes = routeMatcher.getAllRoutes();
        for (Route route : allRoutes) {
            if (route.getPath().equals(path)) {
                return route;
            }
        }
        return null;
    }

    /**
     * 获取所有路由规则
     */
    public List<Route> getAllRoutes() {
        return routeMatcher.getAllRoutes();
    }

    /**
     * 清空所有路由规则
     */
    public void clearRoutes() {
        // 获取所有路由并逐个移除
        List<Route> allRoutes = routeMatcher.getAllRoutes();
        for (Route route : allRoutes) {
            routeMatcher.removeRoute(route.getId());
        }
        logger.info("清空所有路由规则");
    }
} 