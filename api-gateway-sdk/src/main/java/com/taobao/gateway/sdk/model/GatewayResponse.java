package com.taobao.gateway.sdk.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 网关响应模型
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class GatewayResponse {
    
    /**
     * 响应状态码
     */
    private int statusCode;
    
    /**
     * 响应头
     */
    private Map<String, String> headers;
    
    /**
     * 响应体
     */
    private String body;
    
    /**
     * 响应时间（毫秒）
     */
    private long responseTime;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 默认构造函数
     */
    public GatewayResponse() {
        this.headers = new HashMap<>();
    }
    
    /**
     * 构造函数
     * 
     * @param statusCode 状态码
     * @param body 响应体
     */
    public GatewayResponse(int statusCode, String body) {
        this();
        this.statusCode = statusCode;
        this.body = body;
        this.success = statusCode >= 200 && statusCode < 300;
    }
    
    /**
     * 创建成功响应
     * 
     * @param statusCode 状态码
     * @param body 响应体
     * @return 响应对象
     */
    public static GatewayResponse success(int statusCode, String body) {
        GatewayResponse response = new GatewayResponse(statusCode, body);
        response.setSuccess(true);
        return response;
    }
    
    /**
     * 创建错误响应
     * 
     * @param statusCode 状态码
     * @param errorMessage 错误信息
     * @return 响应对象
     */
    public static GatewayResponse error(int statusCode, String errorMessage) {
        GatewayResponse response = new GatewayResponse(statusCode, null);
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
    }
    
    /**
     * 添加响应头
     * 
     * @param key 键
     * @param value 值
     * @return 当前响应对象
     */
    public GatewayResponse addHeader(String key, String value) {
        this.headers.put(key, value);
        return this;
    }
    
    /**
     * 设置响应时间
     * 
     * @param responseTime 响应时间（毫秒）
     * @return 当前响应对象
     */
    public GatewayResponse setResponseTime(long responseTime) {
        this.responseTime = responseTime;
        return this;
    }
    
    // Getter和Setter方法
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
    
    public long getResponseTime() {
        return responseTime;
    }
    
    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    @Override
    public String toString() {
        return "GatewayResponse{" +
                "statusCode=" + statusCode +
                ", headers=" + headers +
                ", body='" + body + '\'' +
                ", responseTime=" + responseTime +
                ", errorMessage='" + errorMessage + '\'' +
                ", success=" + success +
                '}';
    }
} 