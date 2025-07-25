package com.taobao.gateway.dispatcher;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * 多Reactor分发层配置类
 * 支持百亿级别请求的高性能多Reactor模型配置
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
@Configuration
@ConfigurationProperties(prefix = "gateway.multi-reactor")
public class MultiReactorDispatcherConfig {

    /**
     * 是否启用多Reactor模式
     */
    private boolean enabled = true;

    /**
     * 多Reactor配置列表
     */
    private List<ReactorConfig> reactors;

    /**
     * 负载均衡策略
     */
    private String loadBalanceStrategy = "ROUND_ROBIN";

    /**
     * 是否启用连接复用
     */
    private boolean connectionReuse = true;

    /**
     * 连接池配置
     */
    private ConnectionPoolConfig connectionPool;

    /**
     * 路由匹配配置
     */
    private RouteMatchConfig routeMatch;

    /**
     * 性能优化配置
     */
    private PerformanceConfig performance;

    /**
     * 监控配置
     */
    private MetricsConfig metrics;

    /**
     * 单个Reactor配置
     */
    public static class ReactorConfig {
        /**
         * Reactor ID
         */
        private String id;

        /**
         * 监听端口
         */
        private int port;

        /**
         * 主Reactor线程数
         */
        private int mainReactorThreads = 1;

        /**
         * 子Reactor线程数
         */
        private int subReactorThreads = 16;

        /**
         * 业务线程池大小
         */
        private int businessThreadPoolSize = 32;

        /**
         * 连接队列大小
         */
        private int backlog = 1024;

        /**
         * 权重（用于负载均衡）
         */
        private int weight = 100;

        /**
         * 是否启用
         */
        private boolean enabled = true;

        // Getter和Setter方法
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public int getMainReactorThreads() { return mainReactorThreads; }
        public void setMainReactorThreads(int mainReactorThreads) { this.mainReactorThreads = mainReactorThreads; }
        public int getSubReactorThreads() { return subReactorThreads; }
        public void setSubReactorThreads(int subReactorThreads) { this.subReactorThreads = subReactorThreads; }
        public int getBusinessThreadPoolSize() { return businessThreadPoolSize; }
        public void setBusinessThreadPoolSize(int businessThreadPoolSize) { this.businessThreadPoolSize = businessThreadPoolSize; }
        public int getBacklog() { return backlog; }
        public void setBacklog(int backlog) { this.backlog = backlog; }
        public int getWeight() { return weight; }
        public void setWeight(int weight) { this.weight = weight; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    /**
     * 连接池配置
     */
    public static class ConnectionPoolConfig {
        /**
         * 最大连接数
         */
        private int maxConnections = 100000;

        /**
         * 最小连接数
         */
        private int minConnections = 1000;

        /**
         * 连接超时时间（毫秒）
         */
        private int connectionTimeout = 5000;

        /**
         * 空闲连接超时时间（毫秒）
         */
        private int idleTimeout = 300000;

        /**
         * 连接验证间隔（毫秒）
         */
        private int validationInterval = 30000;

        /**
         * 是否启用连接验证
         */
        private boolean validationEnabled = true;

        // Getter和Setter方法
        public int getMaxConnections() { return maxConnections; }
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
        public int getMinConnections() { return minConnections; }
        public void setMinConnections(int minConnections) { this.minConnections = minConnections; }
        public int getConnectionTimeout() { return connectionTimeout; }
        public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }
        public int getIdleTimeout() { return idleTimeout; }
        public void setIdleTimeout(int idleTimeout) { this.idleTimeout = idleTimeout; }
        public int getValidationInterval() { return validationInterval; }
        public void setValidationInterval(int validationInterval) { this.validationInterval = validationInterval; }
        public boolean isValidationEnabled() { return validationEnabled; }
        public void setValidationEnabled(boolean validationEnabled) { this.validationEnabled = validationEnabled; }
    }

    /**
     * 路由匹配配置
     */
    public static class RouteMatchConfig {
        /**
         * 是否启用字典树匹配
         */
        private boolean trieEnabled = true;

        /**
         * 字典树节点缓存大小
         */
        private int trieCacheSize = 100000;

        /**
         * 是否启用正则表达式匹配
         */
        private boolean regexEnabled = true;

        /**
         * 正则表达式缓存大小
         */
        private int regexCacheSize = 10000;

        /**
         * 是否启用通配符匹配
         */
        private boolean wildcardEnabled = true;

        /**
         * 匹配算法优先级
         */
        private List<String> matchPriority = List.of("TRIE", "REGEX", "WILDCARD", "EXACT");

        /**
         * 是否启用匹配结果缓存
         */
        private boolean resultCacheEnabled = true;

        /**
         * 匹配结果缓存大小
         */
        private int resultCacheSize = 100000;

        /**
         * 匹配结果缓存过期时间（毫秒）
         */
        private long resultCacheExpire = 300000;

        // Getter和Setter方法
        public boolean isTrieEnabled() { return trieEnabled; }
        public void setTrieEnabled(boolean trieEnabled) { this.trieEnabled = trieEnabled; }
        public int getTrieCacheSize() { return trieCacheSize; }
        public void setTrieCacheSize(int trieCacheSize) { this.trieCacheSize = trieCacheSize; }
        public boolean isRegexEnabled() { return regexEnabled; }
        public void setRegexEnabled(boolean regexEnabled) { this.regexEnabled = regexEnabled; }
        public int getRegexCacheSize() { return regexCacheSize; }
        public void setRegexCacheSize(int regexCacheSize) { this.regexCacheSize = regexCacheSize; }
        public boolean isWildcardEnabled() { return wildcardEnabled; }
        public void setWildcardEnabled(boolean wildcardEnabled) { this.wildcardEnabled = wildcardEnabled; }
        public List<String> getMatchPriority() { return matchPriority; }
        public void setMatchPriority(List<String> matchPriority) { this.matchPriority = matchPriority; }
        public boolean isResultCacheEnabled() { return resultCacheEnabled; }
        public void setResultCacheEnabled(boolean resultCacheEnabled) { this.resultCacheEnabled = resultCacheEnabled; }
        public int getResultCacheSize() { return resultCacheSize; }
        public void setResultCacheSize(int resultCacheSize) { this.resultCacheSize = resultCacheSize; }
        public long getResultCacheExpire() { return resultCacheExpire; }
        public void setResultCacheExpire(long resultCacheExpire) { this.resultCacheExpire = resultCacheExpire; }
    }

    /**
     * 性能优化配置
     */
    public static class PerformanceConfig {
        /**
         * 是否启用零拷贝
         */
        private boolean zeroCopy = true;

        /**
         * 是否启用直接内存
         */
        private boolean directBuffer = true;

        /**
         * 是否启用内存池
         */
        private boolean memoryPool = true;

        /**
         * 内存池大小（MB）
         */
        private int memoryPoolSize = 1024;

        /**
         * 是否启用对象池
         */
        private boolean objectPool = true;

        /**
         * 对象池大小
         */
        private int objectPoolSize = 10000;

        /**
         * 是否启用批量处理
         */
        private boolean batchProcessing = true;

        /**
         * 批量处理大小
         */
        private int batchSize = 1000;

        /**
         * 是否启用异步处理
         */
        private boolean asyncProcessing = true;

        /**
         * 异步处理线程池大小
         */
        private int asyncThreadPoolSize = 64;

        // Getter和Setter方法
        public boolean isZeroCopy() { return zeroCopy; }
        public void setZeroCopy(boolean zeroCopy) { this.zeroCopy = zeroCopy; }
        public boolean isDirectBuffer() { return directBuffer; }
        public void setDirectBuffer(boolean directBuffer) { this.directBuffer = directBuffer; }
        public boolean isMemoryPool() { return memoryPool; }
        public void setMemoryPool(boolean memoryPool) { this.memoryPool = memoryPool; }
        public int getMemoryPoolSize() { return memoryPoolSize; }
        public void setMemoryPoolSize(int memoryPoolSize) { this.memoryPoolSize = memoryPoolSize; }
        public boolean isObjectPool() { return objectPool; }
        public void setObjectPool(boolean objectPool) { this.objectPool = objectPool; }
        public int getObjectPoolSize() { return objectPoolSize; }
        public void setObjectPoolSize(int objectPoolSize) { this.objectPoolSize = objectPoolSize; }
        public boolean isBatchProcessing() { return batchProcessing; }
        public void setBatchProcessing(boolean batchProcessing) { this.batchProcessing = batchProcessing; }
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        public boolean isAsyncProcessing() { return asyncProcessing; }
        public void setAsyncProcessing(boolean asyncProcessing) { this.asyncProcessing = asyncProcessing; }
        public int getAsyncThreadPoolSize() { return asyncThreadPoolSize; }
        public void setAsyncThreadPoolSize(int asyncThreadPoolSize) { this.asyncThreadPoolSize = asyncThreadPoolSize; }
    }

    /**
     * 监控配置
     */
    public static class MetricsConfig {
        /**
         * 是否启用性能监控
         */
        private boolean performanceEnabled = true;

        /**
         * 是否启用业务监控
         */
        private boolean businessEnabled = true;

        /**
         * 是否启用资源监控
         */
        private boolean resourceEnabled = true;

        /**
         * 监控数据采样率（0-1）
         */
        private double samplingRate = 1.0;

        /**
         * 监控数据保留时间（小时）
         */
        private int retentionHours = 24;

        /**
         * 是否启用实时告警
         */
        private boolean alertEnabled = true;

        // Getter和Setter方法
        public boolean isPerformanceEnabled() { return performanceEnabled; }
        public void setPerformanceEnabled(boolean performanceEnabled) { this.performanceEnabled = performanceEnabled; }
        public boolean isBusinessEnabled() { return businessEnabled; }
        public void setBusinessEnabled(boolean businessEnabled) { this.businessEnabled = businessEnabled; }
        public boolean isResourceEnabled() { return resourceEnabled; }
        public void setResourceEnabled(boolean resourceEnabled) { this.resourceEnabled = resourceEnabled; }
        public double getSamplingRate() { return samplingRate; }
        public void setSamplingRate(double samplingRate) { this.samplingRate = samplingRate; }
        public int getRetentionHours() { return retentionHours; }
        public void setRetentionHours(int retentionHours) { this.retentionHours = retentionHours; }
        public boolean isAlertEnabled() { return alertEnabled; }
        public void setAlertEnabled(boolean alertEnabled) { this.alertEnabled = alertEnabled; }
    }

    // Getter和Setter方法
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public List<ReactorConfig> getReactors() { return reactors; }
    public void setReactors(List<ReactorConfig> reactors) { this.reactors = reactors; }
    public String getLoadBalanceStrategy() { return loadBalanceStrategy; }
    public void setLoadBalanceStrategy(String loadBalanceStrategy) { this.loadBalanceStrategy = loadBalanceStrategy; }
    public boolean isConnectionReuse() { return connectionReuse; }
    public void setConnectionReuse(boolean connectionReuse) { this.connectionReuse = connectionReuse; }
    public ConnectionPoolConfig getConnectionPool() { return connectionPool; }
    public void setConnectionPool(ConnectionPoolConfig connectionPool) { this.connectionPool = connectionPool; }
    public RouteMatchConfig getRouteMatch() { return routeMatch; }
    public void setRouteMatch(RouteMatchConfig routeMatch) { this.routeMatch = routeMatch; }
    public PerformanceConfig getPerformance() { return performance; }
    public void setPerformance(PerformanceConfig performance) { this.performance = performance; }
    public MetricsConfig getMetrics() { return metrics; }
    public void setMetrics(MetricsConfig metrics) { this.metrics = metrics; }

    @Override
    public String toString() {
        return "MultiReactorDispatcherConfig{" +
                "enabled=" + enabled +
                ", reactors=" + reactors +
                ", loadBalanceStrategy='" + loadBalanceStrategy + '\'' +
                ", connectionReuse=" + connectionReuse +
                ", connectionPool=" + connectionPool +
                ", routeMatch=" + routeMatch +
                ", performance=" + performance +
                ", metrics=" + metrics +
                '}';
    }
} 