package com.taobao.gateway.sdk.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 网关请求模型
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class GatewayRequest {
    
    /**
     * 请求ID
     */
    private String requestId;
    
    /**
     * 请求路径
     */
    private String path;
    
    /**
     * HTTP方法
     */
    private String method;
    
    /**
     * 请求头
     */
    private Map<String, String> headers;
    
    /**
     * 查询参数
     */
    private Map<String, String> queryParams;
    
    /**
     * 请求体
     */
    private String body;
    
    /**
     * 超时时间（毫秒）
     */
    private long timeout = 30000;
    
    /**
     * 是否重试
     */
    private boolean retry = true;
    
    /**
     * 重试次数
     */
    private int retryCount = 3;
    
    /**
     * 默认构造函数
     */
    public GatewayRequest() {
        this.headers = new HashMap<>();
        this.queryParams = new HashMap<>();
    }
    
    /**
     * 构造函数
     * 
     * @param path 请求路径
     * @param method HTTP方法
     */
    public GatewayRequest(String path, String method) {
        this();
        this.path = path;
        this.method = method;
    }
    
    /**
     * 添加请求头
     * 
     * @param key 键
     * @param value 值
     * @return 当前请求对象
     */
    public GatewayRequest addHeader(String key, String value) {
        this.headers.put(key, value);
        return this;
    }
    
    /**
     * 添加查询参数
     * 
     * @param key 键
     * @param value 值
     * @return 当前请求对象
     */
    public GatewayRequest addQueryParam(String key, String value) {
        this.queryParams.put(key, value);
        return this;
    }
    
    /**
     * 设置请求体
     * 
     * @param body 请求体
     * @return 当前请求对象
     */
    public GatewayRequest withBody(String body) {
        this.body = body;
        return this;
    }
    
    /**
     * 设置超时时间
     * 
     * @param timeout 超时时间（毫秒）
     * @return 当前请求对象
     */
    public GatewayRequest withTimeout(long timeout) {
        this.timeout = timeout;
        return this;
    }
    
    /**
     * 设置重试配置
     * 
     * @param retry 是否重试
     * @param retryCount 重试次数
     * @return 当前请求对象
     */
    public GatewayRequest setRetry(boolean retry, int retryCount) {
        this.retry = retry;
        this.retryCount = retryCount;
        return this;
    }
    
    // Getter和Setter方法
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    public Map<String, String> getQueryParams() {
        return queryParams;
    }
    
    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }
    
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
    
    public long getTimeout() {
        return timeout;
    }
    
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    
    public boolean isRetry() {
        return retry;
    }
    
    public void setRetry(boolean retry) {
        this.retry = retry;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    @Override
    public String toString() {
        return "GatewayRequest{" +
                "requestId='" + requestId + '\'' +
                ", path='" + path + '\'' +
                ", method='" + method + '\'' +
                ", headers=" + headers +
                ", queryParams=" + queryParams +
                ", body='" + body + '\'' +
                ", timeout=" + timeout +
                ", retry=" + retry +
                ", retryCount=" + retryCount +
                '}';
    }
} 