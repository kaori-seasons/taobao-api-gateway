package com.taobao.gateway.cache.example;

import com.taobao.gateway.cache.Cache;
import com.taobao.gateway.cache.CacheLoader;
import com.taobao.gateway.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 缓存使用示例
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class CacheExample {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheExample.class);
    
    @Autowired
    private CacheManager cacheManager;
    
    /**
     * 用户缓存示例
     */
    public void userCacheExample() {
        // 获取用户缓存
        Cache<String, User> userCache = cacheManager.getCache("user", String.class, User.class);
        
        // 设置用户信息
        User user = new User("1", "张三", "zhangsan@example.com");
        userCache.put("1", user);
        
        // 获取用户信息
        User cachedUser = userCache.get("1");
        logger.info("从缓存获取用户: {}", cachedUser);
        
        // 使用加载器获取用户
        User loadedUser = userCache.get("2", key -> {
            // 模拟从数据库加载用户
            logger.info("从数据库加载用户: {}", key);
            return new User(key, "李四", "lisi@example.com");
        });
        logger.info("加载的用户: {}", loadedUser);
        
        // 批量操作
        Map<String, User> users = Map.of(
            "3", new User("3", "王五", "wangwu@example.com"),
            "4", new User("4", "赵六", "zhaoliu@example.com")
        );
        userCache.putAll(users);
        
        Map<String, User> cachedUsers = userCache.getAll(users.keySet());
        logger.info("批量获取用户: {}", cachedUsers);
    }
    
    /**
     * 配置缓存示例
     */
    public void configCacheExample() {
        // 获取配置缓存
        Cache<String, String> configCache = cacheManager.getCache("config", String.class, String.class);
        
        // 设置配置
        configCache.put("app.name", "API Gateway");
        configCache.put("app.version", "1.0.0");
        configCache.put("app.debug", "true");
        
        // 获取配置
        String appName = configCache.get("app.name");
        String appVersion = configCache.get("app.version");
        String debug = configCache.get("app.debug");
        
        logger.info("应用配置: name={}, version={}, debug={}", appName, appVersion, debug);
        
        // 使用加载器获取配置
        String dbConfig = configCache.get("database.url", key -> {
            // 模拟从配置文件加载
            logger.info("从配置文件加载: {}", key);
            return "jdbc:mysql://localhost:3306/gateway";
        });
        logger.info("数据库配置: {}", dbConfig);
    }
    
    /**
     * 路由缓存示例
     */
    public void routeCacheExample() {
        // 获取路由缓存
        Cache<String, Route> routeCache = cacheManager.getCache("route", String.class, Route.class);
        
        // 设置路由
        Route route1 = new Route("/api/users", "GET", "http://user-service:8080/users");
        Route route2 = new Route("/api/orders", "POST", "http://order-service:8080/orders");
        
        routeCache.put("/api/users", route1);
        routeCache.put("/api/orders", route2);
        
        // 获取路由
        Route cachedRoute = routeCache.get("/api/users");
        logger.info("从缓存获取路由: {}", cachedRoute);
        
        // 使用加载器获取路由
        Route loadedRoute = routeCache.get("/api/products", key -> {
            // 模拟从路由表加载
            logger.info("从路由表加载: {}", key);
            return new Route(key, "GET", "http://product-service:8080/products");
        });
        logger.info("加载的路由: {}", loadedRoute);
    }
    
    /**
     * 用户实体类
     */
    public static class User {
        private String id;
        private String name;
        private String email;
        
        public User(String id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
        
        // Getter和Setter方法
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        @Override
        public String toString() {
            return "User{id='" + id + "', name='" + name + "', email='" + email + "'}";
        }
    }
    
    /**
     * 路由实体类
     */
    public static class Route {
        private String path;
        private String method;
        private String target;
        
        public Route(String path, String method, String target) {
            this.path = path;
            this.method = method;
            this.target = target;
        }
        
        // Getter和Setter方法
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        
        @Override
        public String toString() {
            return "Route{path='" + path + "', method='" + method + "', target='" + target + "'}";
        }
    }
} 