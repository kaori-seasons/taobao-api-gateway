package com.taobao.gateway.dispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reactor负载均衡器
 * 支持多种负载均衡策略，用于在多Reactor实例间分配连接和请求
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
public class ReactorLoadBalancer {

    private static final Logger logger = LoggerFactory.getLogger(ReactorLoadBalancer.class);

    /**
     * Reactor实例映射
     */
    private final ConcurrentHashMap<String, ReactorInstance> reactors;

    /**
     * 负载均衡策略
     */
    private LoadBalanceStrategy strategy;

    /**
     * 轮询计数器
     */
    private final AtomicInteger roundRobinCounter;

    /**
     * 权重轮询计数器
     */
    private final AtomicLong weightedCounter;

    /**
     * 负载均衡策略枚举
     */
    public enum LoadBalanceStrategy {
        /**
         * 轮询策略
         */
        ROUND_ROBIN,
        
        /**
         * 最少连接数策略
         */
        LEAST_CONNECTIONS,
        
        /**
         * 权重轮询策略
         */
        WEIGHTED_ROUND_ROBIN,
        
        /**
         * 权重随机策略
         */
        WEIGHTED_RANDOM,
        
        /**
         * 一致性哈希策略
         */
        CONSISTENT_HASH,
        
        /**
         * 随机策略
         */
        RANDOM
    }

    /**
     * 构造函数
     */
    public ReactorLoadBalancer() {
        this(LoadBalanceStrategy.ROUND_ROBIN);
    }

    /**
     * 构造函数
     * 
     * @param strategy 负载均衡策略
     */
    public ReactorLoadBalancer(LoadBalanceStrategy strategy) {
        this.reactors = new ConcurrentHashMap<>();
        this.strategy = strategy;
        this.roundRobinCounter = new AtomicInteger(0);
        this.weightedCounter = new AtomicLong(0);
    }

    /**
     * 添加Reactor实例
     * 
     * @param reactor Reactor实例
     */
    public void addReactor(ReactorInstance reactor) {
        reactors.put(reactor.getId(), reactor);
        logger.info("添加Reactor实例到负载均衡器: {}, 当前实例数: {}", reactor.getId(), reactors.size());
    }

    /**
     * 移除Reactor实例
     * 
     * @param reactorId Reactor实例ID
     */
    public void removeReactor(String reactorId) {
        ReactorInstance removed = reactors.remove(reactorId);
        if (removed != null) {
            logger.info("从负载均衡器移除Reactor实例: {}, 当前实例数: {}", reactorId, reactors.size());
        }
    }

    /**
     * 清空所有Reactor实例
     */
    public void clear() {
        reactors.clear();
        roundRobinCounter.set(0);
        weightedCounter.set(0);
        logger.info("清空负载均衡器中的所有Reactor实例");
    }

    /**
     * 选择Reactor实例
     * 
     * @return 选中的Reactor实例
     */
    public ReactorInstance select() {
        if (reactors.isEmpty()) {
            logger.warn("没有可用的Reactor实例");
            return null;
        }

        ReactorInstance selected = null;
        switch (strategy) {
            case ROUND_ROBIN:
                selected = selectRoundRobin();
                break;
            case LEAST_CONNECTIONS:
                selected = selectLeastConnections();
                break;
            case WEIGHTED_ROUND_ROBIN:
                selected = selectWeightedRoundRobin();
                break;
            case WEIGHTED_RANDOM:
                selected = selectWeightedRandom();
                break;
            case CONSISTENT_HASH:
                selected = selectConsistentHash();
                break;
            case RANDOM:
                selected = selectRandom();
                break;
            default:
                selected = selectRoundRobin();
                break;
        }

        if (selected != null) {
            logger.debug("负载均衡器选择Reactor实例: {}, 策略: {}", selected.getId(), strategy);
        }

        return selected;
    }

    /**
     * 轮询选择Reactor实例
     */
    private ReactorInstance selectRoundRobin() {
        ReactorInstance[] instances = reactors.values().toArray(new ReactorInstance[0]);
        int index = roundRobinCounter.getAndIncrement() % instances.length;
        return instances[index];
    }

    /**
     * 最少连接数选择Reactor实例
     */
    private ReactorInstance selectLeastConnections() {
        return reactors.values().stream()
                .min((r1, r2) -> Integer.compare(r1.getActiveConnections(), r2.getActiveConnections()))
                .orElse(null);
    }

    /**
     * 权重轮询选择Reactor实例
     */
    private ReactorInstance selectWeightedRoundRobin() {
        if (reactors.isEmpty()) {
            return null;
        }

        // 计算总权重
        int totalWeight = reactors.values().stream().mapToInt(ReactorInstance::getWeight).sum();
        if (totalWeight <= 0) {
            return selectRoundRobin();
        }

        // 权重轮询算法
        long counter = weightedCounter.getAndIncrement();
        int currentWeight = 0;
        
        for (ReactorInstance reactor : reactors.values()) {
            currentWeight += reactor.getWeight();
            if (counter % totalWeight < currentWeight) {
                return reactor;
            }
        }

        // 兜底返回第一个
        return reactors.values().iterator().next();
    }

    /**
     * 权重随机选择Reactor实例
     */
    private ReactorInstance selectWeightedRandom() {
        if (reactors.isEmpty()) {
            return null;
        }

        // 计算总权重
        int totalWeight = reactors.values().stream().mapToInt(ReactorInstance::getWeight).sum();
        if (totalWeight <= 0) {
            return selectRandom();
        }

        // 权重随机算法
        int random = (int) (Math.random() * totalWeight);
        int currentWeight = 0;
        
        for (ReactorInstance reactor : reactors.values()) {
            currentWeight += reactor.getWeight();
            if (random < currentWeight) {
                return reactor;
            }
        }

        // 兜底返回第一个
        return reactors.values().iterator().next();
    }

    /**
     * 一致性哈希选择Reactor实例
     */
    private ReactorInstance selectConsistentHash() {
        if (reactors.isEmpty()) {
            return null;
        }

        // 简单的一致性哈希实现，使用当前时间作为哈希键
        String hashKey = String.valueOf(System.currentTimeMillis());
        int hash = hashKey.hashCode();
        
        ReactorInstance[] instances = reactors.values().toArray(new ReactorInstance[0]);
        int index = Math.abs(hash) % instances.length;
        
        return instances[index];
    }

    /**
     * 随机选择Reactor实例
     */
    private ReactorInstance selectRandom() {
        if (reactors.isEmpty()) {
            return null;
        }

        ReactorInstance[] instances = reactors.values().toArray(new ReactorInstance[0]);
        int index = (int) (Math.random() * instances.length);
        return instances[index];
    }

    /**
     * 根据客户端IP选择Reactor实例（用于一致性哈希）
     * 
     * @param clientIp 客户端IP
     * @return 选中的Reactor实例
     */
    public ReactorInstance selectByClientIp(String clientIp) {
        if (reactors.isEmpty()) {
            return null;
        }

        int hash = clientIp.hashCode();
        ReactorInstance[] instances = reactors.values().toArray(new ReactorInstance[0]);
        int index = Math.abs(hash) % instances.length;
        
        ReactorInstance selected = instances[index];
        logger.debug("根据客户端IP选择Reactor实例: clientIp={}, reactor={}", clientIp, selected.getId());
        
        return selected;
    }

    /**
     * 根据请求ID选择Reactor实例（用于一致性哈希）
     * 
     * @param requestId 请求ID
     * @return 选中的Reactor实例
     */
    public ReactorInstance selectByRequestId(String requestId) {
        if (reactors.isEmpty()) {
            return null;
        }

        int hash = requestId.hashCode();
        ReactorInstance[] instances = reactors.values().toArray(new ReactorInstance[0]);
        int index = Math.abs(hash) % instances.length;
        
        ReactorInstance selected = instances[index];
        logger.debug("根据请求ID选择Reactor实例: requestId={}, reactor={}", requestId, selected.getId());
        
        return selected;
    }

    /**
     * 获取所有Reactor实例
     * 
     * @return Reactor实例列表
     */
    public List<ReactorInstance> getAllReactors() {
        return List.copyOf(reactors.values());
    }

    /**
     * 获取Reactor实例数量
     * 
     * @return 实例数量
     */
    public int getReactorCount() {
        return reactors.size();
    }

    /**
     * 获取负载均衡策略
     * 
     * @return 负载均衡策略
     */
    public LoadBalanceStrategy getStrategy() {
        return strategy;
    }

    /**
     * 设置负载均衡策略
     * 
     * @param strategy 负载均衡策略
     */
    public void setStrategy(LoadBalanceStrategy strategy) {
        this.strategy = strategy;
        logger.info("设置负载均衡策略: {}", strategy);
    }

    /**
     * 获取负载均衡统计信息
     * 
     * @return 统计信息
     */
    public LoadBalanceStats getStats() {
        LoadBalanceStats stats = new LoadBalanceStats();
        stats.setStrategy(strategy);
        stats.setReactorCount(reactors.size());
        stats.setTotalConnections(reactors.values().stream().mapToInt(ReactorInstance::getActiveConnections).sum());
        stats.setAverageConnections(reactors.isEmpty() ? 0 : 
                stats.getTotalConnections() / reactors.size());
        
        // 计算连接数分布
        int minConnections = reactors.values().stream().mapToInt(ReactorInstance::getActiveConnections).min().orElse(0);
        int maxConnections = reactors.values().stream().mapToInt(ReactorInstance::getActiveConnections).max().orElse(0);
        stats.setMinConnections(minConnections);
        stats.setMaxConnections(maxConnections);
        
        return stats;
    }

    /**
     * 负载均衡统计信息
     */
    public static class LoadBalanceStats {
        private LoadBalanceStrategy strategy;
        private int reactorCount;
        private int totalConnections;
        private int averageConnections;
        private int minConnections;
        private int maxConnections;

        // Getter和Setter方法
        public LoadBalanceStrategy getStrategy() { return strategy; }
        public void setStrategy(LoadBalanceStrategy strategy) { this.strategy = strategy; }
        
        public int getReactorCount() { return reactorCount; }
        public void setReactorCount(int reactorCount) { this.reactorCount = reactorCount; }
        
        public int getTotalConnections() { return totalConnections; }
        public void setTotalConnections(int totalConnections) { this.totalConnections = totalConnections; }
        
        public int getAverageConnections() { return averageConnections; }
        public void setAverageConnections(int averageConnections) { this.averageConnections = averageConnections; }
        
        public int getMinConnections() { return minConnections; }
        public void setMinConnections(int minConnections) { this.minConnections = minConnections; }
        
        public int getMaxConnections() { return maxConnections; }
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }

        @Override
        public String toString() {
            return "LoadBalanceStats{" +
                    "strategy=" + strategy +
                    ", reactorCount=" + reactorCount +
                    ", totalConnections=" + totalConnections +
                    ", averageConnections=" + averageConnections +
                    ", minConnections=" + minConnections +
                    ", maxConnections=" + maxConnections +
                    '}';
        }
    }
}