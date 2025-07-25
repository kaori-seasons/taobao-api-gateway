 # 自适应限流和负载均衡设计文档

## 概述

本文档描述了淘宝API网关中自适应限流和自适应负载均衡的设计和实现。参考了[掘金文章](https://juejin.cn/post/7217346593501921337)中的自适应算法思想，结合当前系统的实际情况，实现了基于多维度指标的自适应调节机制。

## 设计目标

1. **自适应限流**：根据系统负载、响应时间、错误率等指标动态调整限流阈值
2. **自适应负载均衡**：根据服务实例的健康状态、性能指标动态调整负载分配策略
3. **高可用性**：确保在系统压力变化时能够自动调节，保持系统稳定性
4. **可观测性**：提供详细的统计信息和监控指标

## 架构设计

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    自适应控制系统                              │
├─────────────────────────────────────────────────────────────┤
│  系统指标收集  │  实例指标收集  │  策略计算  │  执行调节        │
├─────────────────────────────────────────────────────────────┤
│  自适应限流器  │  自适应负载均衡器  │  监控统计  │  配置管理      │
└─────────────────────────────────────────────────────────────┘
```

### 核心组件

1. **SystemMetrics**：系统指标收集器
2. **InstanceMetrics**：实例指标收集器
3. **AdaptiveStrategy**：自适应策略接口
4. **AdaptiveRateLimiter**：自适应限流器
5. **AdaptiveLoadBalancer**：自适应负载均衡器

## 自适应限流设计

### 系统指标

- **CPU使用率**：系统CPU使用率 (0-100%)
- **内存使用率**：系统内存使用率 (0-100%)
- **响应时间**：平均响应时间 (毫秒)
- **错误率**：请求错误率 (0-1)
- **连接数**：当前活跃连接数
- **QPS**：每秒查询数

### 自适应策略

#### 1. 综合自适应策略 (COMPREHENSIVE)

基于多维度指标的综合评分算法：

```java
压力分数 = CPU权重 × CPU评分 + 内存权重 × 内存评分 + 
          响应时间权重 × 响应时间评分 + 错误率权重 × 错误率评分 + 
          负载权重 × 负载评分
```

**权重配置**：
- CPU权重：0.25
- 内存权重：0.20
- 响应时间权重：0.25
- 错误率权重：0.20
- 负载权重：0.10

**调整策略**：
- 高压力 (>0.8)：激进调整，降低限流阈值30%
- 中等压力 (0.6-0.8)：适度调整，降低限流阈值15%
- 低压力 (0.4-0.6)：保守调整，降低限流阈值5%
- 正常压力 (<0.4)：微调，提高限流阈值2%

#### 2. 其他策略

- **CPU_BASED**：基于CPU使用率的自适应策略
- **RESPONSE_TIME_BASED**：基于响应时间的自适应策略
- **ERROR_RATE_BASED**：基于错误率的自适应策略
- **LOAD_SCORE_BASED**：基于负载分数的自适应策略

### 平滑调整机制

为了避免限流阈值剧烈波动，实现了平滑调整机制：

```java
新阈值 = 旧阈值 × (1 - 平滑因子) + 计算阈值 × 平滑因子
```

默认平滑因子为0.3，可根据实际情况调整。

## 自适应负载均衡设计

### 实例指标

- **响应时间**：实例平均响应时间 (毫秒)
- **错误率**：实例请求错误率 (0-1)
- **活跃连接数**：当前活跃连接数
- **CPU使用率**：实例CPU使用率 (0-100%)
- **内存使用率**：实例内存使用率 (0-100%)
- **QPS**：实例每秒查询数
- **健康状态**：实例健康状态 (HEALTHY/UNHEALTHY/UNKNOWN)

### 自适应策略

#### 1. 综合评分策略 (SCORE_BASED)

基于多维度指标的综合评分算法：

```java
综合评分 = 响应时间评分 × 0.35 + 负载评分 × 0.25 + 
          错误率评分 × 0.20 + 连接数评分 × 0.10 + QPS评分 × 0.10
```

**评分计算**：
- **响应时间评分**：响应时间越短，评分越高
- **负载评分**：负载越低，评分越高
- **错误率评分**：错误率越低，评分越高
- **连接数评分**：连接数比例越低，评分越高
- **QPS评分**：QPS适中时评分最高

#### 2. 其他策略

- **RESPONSE_TIME_BASED**：基于响应时间的负载均衡
- **LOAD_BASED**：基于负载的负载均衡
- **ERROR_RATE_BASED**：基于错误率的负载均衡
- **HYBRID**：混合策略

### 健康检查机制

定期对服务实例进行健康检查：

1. **指标检查**：检查响应时间、错误率等指标
2. **连接检查**：检查实例连接状态
3. **故障转移**：自动将流量从故障实例转移到健康实例

### 权重动态调整

根据实例性能动态调整权重：

```java
新权重 = 基础权重 × 性能系数
性能系数 = f(响应时间, 错误率, 负载, 连接数)
```

## 配置管理

### 自适应限流配置

```yaml
gateway:
  adaptive:
    ratelimit:
      enabled: true
      strategy-type: COMPREHENSIVE
      base-limit: 1000
      min-limit: 100
      max-limit: 10000
      adjustment-step: 0.1
      adjustment-interval: 5000
      cpu-threshold: 80.0
      memory-threshold: 80.0
      response-time-threshold: 1000.0
      error-rate-threshold: 0.1
      load-score-threshold: 80.0
      smooth-adjustment: true
      smooth-factor: 0.3
      history-window-size: 10
```

### 自适应负载均衡配置

```yaml
gateway:
  adaptive:
    loadbalancer:
      enabled: true
      strategy-type: SCORE_BASED
      health-check-interval: 30000
      metrics-update-interval: 5000
      response-time-threshold: 1000.0
      error-rate-threshold: 0.1
      load-threshold: 80.0
      weight-adjustment-factor: 0.1
      min-weight: 0.1
      max-weight: 10.0
      smooth-adjustment: true
      smooth-factor: 0.3
      failover-enabled: true
      failover-threshold: 3
      warmup-enabled: true
      warmup-time: 60000
```

## 监控和统计

### 自适应限流统计

- **总请求数**：处理的请求总数
- **通过请求数**：通过限流的请求数
- **被限流请求数**：被限流的请求数
- **当前限流阈值**：当前生效的限流阈值
- **调整次数**：限流阈值调整次数
- **平均响应时间**：系统平均响应时间
- **错误率**：系统错误率

### 自适应负载均衡统计

- **总请求数**：负载均衡处理的请求总数
- **成功请求数**：成功处理的请求数
- **失败请求数**：失败的请求数
- **平均响应时间**：平均响应时间
- **权重调整次数**：权重调整次数
- **健康检查次数**：健康检查次数
- **故障转移次数**：故障转移次数

## 使用示例

### 自适应限流使用

```java
@Autowired
private AdaptiveRateLimiter adaptiveRateLimiter;

// 尝试获取令牌
boolean allowed = adaptiveRateLimiter.tryAcquire("user-123");

// 更新系统指标
SystemMetrics metrics = new SystemMetrics();
metrics.setCpuUsage(75.0);
metrics.setMemoryUsage(80.0);
metrics.setAvgResponseTime(800.0);
metrics.setErrorRate(0.05);
adaptiveRateLimiter.updateMetrics(metrics);

// 获取统计信息
AdaptiveRateLimitStats stats = adaptiveRateLimiter.getStats();
```

### 自适应负载均衡使用

```java
@Autowired
private AdaptiveLoadBalancer adaptiveLoadBalancer;

// 选择服务实例
ServiceInstance selected = adaptiveLoadBalancer.select("user-service", instances);

// 更新实例指标
InstanceMetrics metrics = new InstanceMetrics("instance-1");
metrics.setResponseTime(200.0);
metrics.setErrorRate(0.02);
metrics.setCpuUsage(60.0);
adaptiveLoadBalancer.updateInstanceMetrics("user-service", "instance-1", metrics);

// 获取统计信息
AdaptiveLoadBalanceStats stats = adaptiveLoadBalancer.getStats();
```

## 性能优化

### 1. 异步处理

- 指标收集和策略计算采用异步处理
- 使用定时任务定期更新指标和调整策略

### 2. 缓存机制

- 缓存计算结果，避免重复计算
- 使用本地缓存存储历史数据

### 3. 批量处理

- 批量更新指标数据
- 批量调整多个实例的权重

### 4. 平滑调整

- 避免策略剧烈变化
- 使用平滑因子控制调整幅度

## 故障处理

### 1. 降级策略

- 当自适应策略失效时，降级到基础策略
- 当指标收集失败时，使用默认值

### 2. 容错机制

- 异常捕获和处理
- 自动恢复机制

### 3. 监控告警

- 关键指标监控
- 异常情况告警

## 扩展性设计

### 1. 策略扩展

- 支持自定义自适应策略
- 支持策略组合和切换

### 2. 指标扩展

- 支持自定义指标收集
- 支持指标聚合和计算

### 3. 配置扩展

- 支持动态配置更新
- 支持配置热加载

## 总结

自适应限流和负载均衡系统通过实时监控系统指标和实例性能，动态调整限流策略和负载分配，实现了智能化的流量控制和服务治理。系统具有良好的可扩展性、可观测性和容错能力，能够有效应对高并发场景下的系统压力变化。

参考文章：[掘金 - 自适应算法在微服务架构中的应用](https://juejin.cn/post/7217346593501921337)