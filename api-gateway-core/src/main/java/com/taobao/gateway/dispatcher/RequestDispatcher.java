package com.taobao.gateway.dispatcher;

import java.util.concurrent.CompletableFuture;

/**
 * 请求分发器接口
 * 定义分发层的核心分发逻辑
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface RequestDispatcher {

    /**
     * 分发请求
     * 
     * @param context 请求上下文
     * @return 异步处理结果
     */
    CompletableFuture<RequestContext> dispatch(RequestContext context);

    /**
     * 处理请求
     * 
     * @param context 请求上下文
     * @return 异步处理结果
     */
    CompletableFuture<RequestContext> process(RequestContext context);

    /**
     * 路由请求
     * 
     * @param context 请求上下文
     * @return 异步处理结果
     */
    CompletableFuture<RequestContext> route(RequestContext context);

    /**
     * 转发请求
     * 
     * @param context 请求上下文
     * @return 异步处理结果
     */
    CompletableFuture<RequestContext> forward(RequestContext context);

    /**
     * 处理响应
     * 
     * @param context 请求上下文
     * @return 异步处理结果
     */
    CompletableFuture<RequestContext> handleResponse(RequestContext context);

    /**
     * 处理异常
     * 
     * @param context 请求上下文
     * @param throwable 异常信息
     * @return 异步处理结果
     */
    CompletableFuture<RequestContext> handleException(RequestContext context, Throwable throwable);
} 