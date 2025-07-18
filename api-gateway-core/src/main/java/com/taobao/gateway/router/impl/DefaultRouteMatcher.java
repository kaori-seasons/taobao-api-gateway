package com.taobao.gateway.router.impl;

import com.taobao.gateway.router.Route;
import com.taobao.gateway.router.RouteMatcher;
import com.taobao.gateway.router.RouteType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * 默认路由匹配器实现
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class DefaultRouteMatcher implements RouteMatcher {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultRouteMatcher.class);
    
    /** 路由缓存：路径 -> 路由列表 */
    private final Map<String, List<Route>> routeCache = new ConcurrentHashMap<>();
    
    /** 所有路由列表 */
    private final List<Route> allRoutes = new CopyOnWriteArrayList<>();
    
    /** 正则表达式缓存 */
    private final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();
    
    @Override
    public Route match(String path, String method) {
        logger.debug("匹配路由: {} {}", method, path);
        
        // 1. 精确匹配
        Route exactRoute = matchExact(path, method);
        if (exactRoute != null) {
            logger.debug("精确匹配成功: {}", exactRoute);
            return exactRoute;
        }
        
        // 2. 前缀匹配
        Route prefixRoute = matchPrefix(path, method);
        if (prefixRoute != null) {
            logger.debug("前缀匹配成功: {}", prefixRoute);
            return prefixRoute;
        }
        
        // 3. 正则匹配
        Route regexRoute = matchRegex(path, method);
        if (regexRoute != null) {
            logger.debug("正则匹配成功: {}", regexRoute);
            return regexRoute;
        }
        
        logger.debug("没有找到匹配的路由: {} {}", method, path);
        return null;
    }
    
    /**
     * 精确匹配
     */
    private Route matchExact(String path, String method) {
        String key = method + ":" + path;
        List<Route> routes = routeCache.get(key);
        if (routes != null) {
            for (Route route : routes) {
                if (route.getType() == RouteType.EXACT && 
                    route.getPath().equals(path) && 
                    route.getMethod().equals(method)) {
                    return route;
                }
            }
        }
        return null;
    }
    
    /**
     * 前缀匹配
     */
    private Route matchPrefix(String path, String method) {
        for (Route route : allRoutes) {
            if (route.getType() == RouteType.PREFIX && 
                route.getMethod().equals(method) && 
                path.startsWith(route.getPath())) {
                return route;
            }
        }
        return null;
    }
    
    /**
     * 正则匹配
     */
    private Route matchRegex(String path, String method) {
        for (Route route : allRoutes) {
            if (route.getType() == RouteType.REGEX && 
                route.getMethod().equals(method)) {
                Pattern pattern = getPattern(route.getPath());
                if (pattern.matcher(path).matches()) {
                    return route;
                }
            }
        }
        return null;
    }
    
    /**
     * 获取正则表达式模式（带缓存）
     */
    private Pattern getPattern(String regex) {
        return patternCache.computeIfAbsent(regex, Pattern::compile);
    }
    
    @Override
    public void addRoute(Route route) {
        logger.info("添加路由: {}", route);
        
        // 添加到路由列表
        allRoutes.add(route);
        
        // 更新缓存
        updateCache(route);
        
        logger.info("路由添加成功，当前路由总数: {}", allRoutes.size());
    }
    
    @Override
    public void removeRoute(String routeId) {
        logger.info("移除路由: {}", routeId);
        
        // 从路由列表中移除
        allRoutes.removeIf(route -> route.getId().equals(routeId));
        
        // 清除缓存
        clearCache();
        
        // 重新构建缓存
        for (Route route : allRoutes) {
            updateCache(route);
        }
        
        logger.info("路由移除成功，当前路由总数: {}", allRoutes.size());
    }
    
    @Override
    public void updateRoute(Route route) {
        logger.info("更新路由: {}", route);
        
        // 移除旧路由
        removeRoute(route.getId());
        
        // 添加新路由
        addRoute(route);
        
        logger.info("路由更新成功");
    }
    
    @Override
    public List<Route> getAllRoutes() {
        return new CopyOnWriteArrayList<>(allRoutes);
    }
    
    /**
     * 更新路由缓存
     */
    private void updateCache(Route route) {
        String key = route.getMethod() + ":" + route.getPath();
        routeCache.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(route);
    }
    
    /**
     * 清除路由缓存
     */
    private void clearCache() {
        routeCache.clear();
        patternCache.clear();
    }
    
    /**
     * 获取路由统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalRoutes", allRoutes.size());
        stats.put("cacheSize", routeCache.size());
        stats.put("patternCacheSize", patternCache.size());
        
        // 按类型统计
        long exactCount = allRoutes.stream().filter(r -> r.getType() == RouteType.EXACT).count();
        long prefixCount = allRoutes.stream().filter(r -> r.getType() == RouteType.PREFIX).count();
        long regexCount = allRoutes.stream().filter(r -> r.getType() == RouteType.REGEX).count();
        
        stats.put("exactRoutes", exactCount);
        stats.put("prefixRoutes", prefixCount);
        stats.put("regexRoutes", regexCount);
        
        return stats;
    }
} 