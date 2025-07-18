package com.taobao.gateway.sdk.exception;

/**
 * 网关异常
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class GatewayException extends Exception {
    
    /**
     * 错误码
     */
    private String errorCode;
    
    /**
     * 默认构造函数
     */
    public GatewayException() {
        super();
    }
    
    /**
     * 构造函数
     * 
     * @param message 错误信息
     */
    public GatewayException(String message) {
        super(message);
    }
    
    /**
     * 构造函数
     * 
     * @param message 错误信息
     * @param cause 原因
     */
    public GatewayException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 构造函数
     * 
     * @param cause 原因
     */
    public GatewayException(Throwable cause) {
        super(cause);
    }
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误信息
     */
    public GatewayException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误信息
     * @param cause 原因
     */
    public GatewayException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * 获取错误码
     * 
     * @return 错误码
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * 设置错误码
     * 
     * @param errorCode 错误码
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
} 