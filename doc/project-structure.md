# 项目结构说明

## 目录结构

```
taobao-api-gateway/
├── api-gateway-core/                    # 网关核心服务
│   ├── src/main/java/
│   │   └── com/taobao/gateway/
│   │       ├── GatewayApplication.java  # 启动类
│   │       ├── config/                  # 配置类
│   │       │   ├── NettyConfig.java     # Netty配置
│   │       │   ├── ThreadPoolConfig.java # 线程池配置
│   │       │   └── RedisConfig.java     # Redis配置
│   │       ├── handler/                 # 处理器
│   │       │   ├── HttpRequestHandler.java # HTTP请求处理
│   │       │   ├── RouteHandler.java    # 路由处理
│   │       │   └── ResponseHandler.java # 响应处理
│   │       ├── filter/                  # 过滤器
│   │       │   ├── AuthFilter.java      # 认证过滤
│   │       │   ├── RateLimitFilter.java # 限流过滤
│   │       │   └── LogFilter.java       # 日志过滤
│   │       ├── router/                  # 路由器
│   │       │   ├── RouteManager.java    # 路由管理
│   │       │   ├── LoadBalancer.java    # 负载均衡
│   │       │   └── CircuitBreaker.java  # 熔断器
│   │       ├── cache/                   # 缓存
│   │       │   ├── CacheManager.java    # 缓存管理
│   │       │   └── RedisCache.java      # Redis缓存
│   │       ├── metrics/                 # 监控指标
│   │       │   ├── MetricsCollector.java # 指标收集
│   │       │   └── PrometheusExporter.java # Prometheus导出
│   │       └── util/                    # 工具类
│   │           ├── HttpUtil.java        # HTTP工具
│   │           ├── JsonUtil.java        # JSON工具
│   │           └── TimeUtil.java        # 时间工具
│   ├── src/main/resources/
│   │   ├── application.yml              # 应用配置
│   │   ├── logback.xml                  # 日志配置
│   │   └── routes.json                  # 路由配置
│   └── pom.xml                          # Maven配置
├── api-gateway-sdk/                     # SDK包
│   ├── src/main/java/
│   │   └── com/taobao/gateway/sdk/
│   │       ├── GatewayClient.java       # 网关客户端
│   │       ├── RouteRegistry.java       # 路由注册
│   │       └── MetricsReporter.java     # 指标上报
│   └── pom.xml
├── api-gateway-center/                  # 注册中心
│   ├── src/main/java/
│   │   └── com/taobao/gateway/center/
│   │       ├── CenterApplication.java   # 启动类
│   │       ├── controller/              # 控制器
│   │       │   ├── RouteController.java # 路由控制器
│   │       │   └── ServiceController.java # 服务控制器
│   │       ├── service/                 # 服务层
│   │       │   ├── RouteService.java    # 路由服务
│   │       │   └── ServiceRegistry.java # 服务注册
│   │       └── repository/              # 数据访问层
│   │           ├── RouteRepository.java # 路由仓库
│   │           └── ServiceRepository.java # 服务仓库
│   └── pom.xml
├── api-gateway-admin/                   # 管理后台
│   ├── src/main/java/
│   │   └── com/taobao/gateway/admin/
│   │       ├── AdminApplication.java    # 启动类
│   │       ├── controller/              # 控制器
│   │       │   ├── DashboardController.java # 仪表板
│   │       │   ├── ConfigController.java # 配置管理
│   │       │   └── MonitorController.java # 监控管理
│   │       ├── service/                 # 服务层
│   │       │   ├── ConfigService.java   # 配置服务
│   │       │   └── MonitorService.java  # 监控服务
│   │       └── web/                     # 前端资源
│   │           ├── index.html           # 主页面
│   │           ├── css/                 # 样式文件
│   │           └── js/                  # JavaScript文件
│   └── pom.xml
├── docs/                                # 文档
│   ├── README.md                        # 项目说明
│   ├── design.md                        # 设计文档
│   ├── api.md                           # API文档
│   └── deployment.md                    # 部署文档
├── scripts/                             # 脚本
│   ├── start.sh                         # 启动脚本
│   ├── stop.sh                          # 停止脚本
│   ├── deploy.sh                        # 部署脚本
│   └── monitor.sh                       # 监控脚本
├── tests/                               # 测试
│   ├── unit/                            # 单元测试
│   ├── integration/                     # 集成测试
│   └── performance/                     # 性能测试
├── docker/                              # Docker配置
│   ├── Dockerfile                       # Docker镜像
│   ├── docker-compose.yml               # 容器编排
│   └── k8s/                             # Kubernetes配置
├── .gitignore                           # Git忽略文件
├── pom.xml                              # 父POM
└── README.md                            # 项目说明
```

## 模块说明

### 1. api-gateway-core (核心服务)
网关的核心服务模块，负责：
- HTTP请求的接收和处理
- 路由转发和负载均衡
- 限流熔断保护
- 监控指标收集

**主要组件：**
- `NettyConfig`: Netty服务器配置
- `HttpRequestHandler`: HTTP请求处理器
- `RouteManager`: 路由管理器
- `LoadBalancer`: 负载均衡器
- `CircuitBreaker`: 熔断器
- `CacheManager`: 缓存管理器
- `MetricsCollector`: 指标收集器

### 2. api-gateway-sdk (SDK包)
提供给后端服务使用的SDK，负责：
- 服务注册和发现
- 路由配置上报
- 监控指标上报

**主要组件：**
- `GatewayClient`: 网关客户端
- `RouteRegistry`: 路由注册器
- `MetricsReporter`: 指标上报器

### 3. api-gateway-center (注册中心)
服务注册中心，负责：
- 服务注册和发现
- 路由配置管理
- 服务健康检查

**主要组件：**
- `RouteController`: 路由管理接口
- `ServiceController`: 服务管理接口
- `RouteService`: 路由服务
- `ServiceRegistry`: 服务注册器

### 4. api-gateway-admin (管理后台)
可视化管理界面，负责：
- 路由配置管理
- 监控数据展示
- 系统配置管理

**主要组件：**
- `DashboardController`: 仪表板接口
- `ConfigController`: 配置管理接口
- `MonitorController`: 监控管理接口
- `ConfigService`: 配置服务
- `MonitorService`: 监控服务

## 技术栈

### 后端技术
- **框架**: Spring Boot 2.7+
- **网络**: Netty 4.1+
- **数据库**: MySQL 8.0+
- **缓存**: Redis 6.0+
- **监控**: Prometheus + Grafana
- **日志**: Logback + ELK

### 前端技术
- **框架**: Vue.js 3.0+
- **UI库**: Element Plus
- **图表**: ECharts
- **构建**: Vite

### 部署技术
- **容器**: Docker
- **编排**: Docker Compose / Kubernetes
- **CI/CD**: Jenkins / GitLab CI

## 开发规范

### 代码规范
- 遵循阿里巴巴Java开发手册
- 使用统一的代码格式化工具
- 编写完整的单元测试
- 添加详细的代码注释

### 提交规范
```
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
test: 测试相关
chore: 构建过程或辅助工具的变动
```

### 分支管理
- `main`: 主分支，用于生产环境
- `develop`: 开发分支，用于集成测试
- `feature/*`: 功能分支，用于新功能开发
- `hotfix/*`: 热修复分支，用于紧急修复

## 性能优化

### 代码层面
- 使用对象池减少GC压力
- 异步处理提高并发能力
- 缓存减少重复计算
- 连接池复用网络连接

### 配置层面
- JVM参数优化
- 线程池参数调优
- 网络参数优化
- 缓存参数调优

### 部署层面
- 水平扩展提高吞吐量
- 负载均衡分散压力
- 监控告警及时发现问题
- 日志分析优化性能瓶颈 