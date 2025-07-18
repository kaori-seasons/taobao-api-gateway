package com.taobao.gateway.router;

/**
 * 路由类型枚举
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public enum RouteType {

    /**
     * 精确匹配
     */
    EXACT("精确匹配"),

    /**
     * 前缀匹配
     */
    PREFIX("前缀匹配"),

    /**
     * 正则匹配
     */
    REGEX("正则匹配");

    private final String description;

    RouteType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 