package com.taobao.gateway.router;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 路由规则类
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class Route {

    /**
     * 路由ID
     */
    private String id;

    /**
     * 路径
     */
    private String path;

    /**
     * 目标服务地址
     */
    private String target;

    /**
     * 权重
     */
    private int weight = 100;

    /**
     * 超时时间（毫秒）
     */
    private int timeout = 30000;

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * 路由类型（精确匹配、前缀匹配、正则匹配）
     */
    private RouteType type = RouteType.EXACT;

    /**
     * 额外参数
     */
    private Map<String, String> parameters = new ConcurrentHashMap<>();

    public Route() {
    }

    public Route(String id, String path, String target) {
        this.id = id;
        this.path = path;
        this.target = target;
    }

    // Getter和Setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public RouteType getType() {
        return type;
    }

    public void setType(RouteType type) {
        this.type = type;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "Route{" +
                "id='" + id + '\'' +
                ", path='" + path + '\'' +
                ", target='" + target + '\'' +
                ", weight=" + weight +
                ", timeout=" + timeout +
                ", enabled=" + enabled +
                ", type=" + type +
                '}';
    }
} 