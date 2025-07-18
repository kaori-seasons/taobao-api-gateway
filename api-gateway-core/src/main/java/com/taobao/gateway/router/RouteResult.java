package com.taobao.gateway.router;

/**
 * 路由结果类
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class RouteResult {

    /**
     * 是否匹配成功
     */
    private boolean matched;

    /**
     * 匹配的路由规则
     */
    private Route route;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 匹配的路径参数
     */
    private String[] pathParameters;

    public RouteResult() {
    }

    public RouteResult(boolean matched, Route route) {
        this.matched = matched;
        this.route = route;
    }

    public RouteResult(boolean matched, String errorMessage) {
        this.matched = matched;
        this.errorMessage = errorMessage;
    }

    /**
     * 创建成功结果
     */
    public static RouteResult success(Route route) {
        return new RouteResult(true, route);
    }

    /**
     * 创建失败结果
     */
    public static RouteResult failure(String errorMessage) {
        return new RouteResult(false, errorMessage);
    }

    // Getter和Setter方法
    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String[] getPathParameters() {
        return pathParameters;
    }

    public void setPathParameters(String[] pathParameters) {
        this.pathParameters = pathParameters;
    }

    @Override
    public String toString() {
        return "RouteResult{" +
                "matched=" + matched +
                ", route=" + route +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
} 