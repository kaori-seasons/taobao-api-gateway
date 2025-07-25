# 工业IoT网关使用指南

## 概述

工业IoT网关是基于现有高性能API网关架构扩展的工业物联网解决方案，支持多协议转换、边缘计算、实时数据处理等功能。该网关能够无缝接入Modbus、OPC UA、BACnet等主流工业协议，并通过JSON中间格式实现与云端平台的数据交互。

## 核心特性

### 🚀 高性能
- 基于Netty异步非阻塞IO架构
- 支持大规模设备并发接入
- 毫秒级协议转换响应
- 多级缓存优化

### 🔌 多协议支持
- **Modbus RTU/TCP**: 支持线圈、离散输入、输入寄存器、保持寄存器
- **OPC UA**: 支持客户端和服务器模式
- **BACnet**: 支持楼宇自动化协议
- **自定义协议**: 支持扩展自定义协议适配器

### 🧠 边缘计算
- 本地规则引擎
- 实时数据处理
- 异常检测和告警
- 数据聚合和计算

### 🔒 安全可靠
- TLS/DTLS加密传输
- X.509证书认证
- RBAC权限控制
- 操作审计日志

### 📊 监控运维
- 实时设备状态监控
- 性能指标收集
- 告警机制
- 可视化配置管理

## 快速开始

### 1. 环境准备

#### 系统要求
- JDK 11+
- Maven 3.6+
- Redis 6.0+
- 支持RS485/RS232的硬件设备

#### 依赖安装
```bash
# 安装系统依赖
sudo apt-get update
sudo apt-get install -y libmodbus-dev libserialport-dev

# 克隆项目
git clone https://github.com/your-repo/taobao-api-gateway.git
cd taobao-api-gateway
```

### 2. 配置设备

#### 基础配置
在 `application.yml` 中配置IoT网关：

```yaml
gateway:
  iot:
    enabled: true
    gateway-id: "iot-gateway-001"
    gateway-name: "工业IoT网关"
```

#### 设备配置
配置Modbus设备示例：

```yaml
devices:
  - device-id: "temp_sensor_001"
    name: "温度传感器001"
    protocol-type: "MODBUS_RTU"
    connection:
      type: "RS485"
      serial-port: "/dev/ttyUSB0"
      baud-rate: 9600
      data-bits: 8
      stop-bits: 1
      parity: "NONE"
    data-points:
      temperature:
        address: "30001"
        data-type: "FLOAT32"
        unit: "°C"
        conversion:
          formula: "value / 10"
          scale: 0.1
      humidity:
        address: "30002"
        data-type: "FLOAT32"
        unit: "%RH"
        conversion:
          formula: "value / 10"
          scale: 0.1
    rules:
      - name: "温度告警"
        condition: "temperature > 50"
        action: "send_alert"
        level: "WARNING"
```

#### 协议配置
```yaml
protocols:
  modbus:
    timeout: 3000
    retries: 3
    inter-frame-delay: 10
    max-connections: 10
    connection-pool:
      max-idle: 5
      max-active: 20
      min-idle: 2
      max-wait: 5000
```

### 3. 启动网关

#### 编译项目
```bash
mvn clean package -DskipTests
```

#### 启动应用
```bash
java -jar api-gateway-core/target/api-gateway-core.jar
```

#### 验证启动
```bash
# 检查网关状态
curl http://localhost:8080/actuator/health

# 查看设备列表
curl http://localhost:8080/api/iot/devices

# 查看设备数据
curl http://localhost:8080/api/iot/devices/temp_sensor_001/data
```

## 设备接入指南

### Modbus设备接入

#### 1. 硬件连接
- **RS485连接**: 使用USB转RS485适配器
- **TCP连接**: 直接网络连接
- **串口连接**: 使用USB转串口适配器

#### 2. 设备配置
```yaml
- device-id: "plc_001"
  name: "PLC控制器001"
  protocol-type: "MODBUS_TCP"
  connection:
    type: "TCP"
    host: "192.168.1.100"
    port: 502
  data-points:
    status:
      address: "00001"
      data-type: "BOOL"
      unit: ""
    speed:
      address: "40001"
      data-type: "INT16"
      unit: "rpm"
    temperature:
      address: "30001"
      data-type: "FLOAT32"
      unit: "°C"
      conversion:
        formula: "value / 10"
        scale: 0.1
```

#### 3. 数据读取
```java
// 读取单个数据点
DeviceData data = deviceManager.getDeviceData("plc_001", 
    ReadRequest.builder()
        .address("30001")
        .count(1)
        .build());

// 读取多个数据点
DeviceData data = deviceManager.getDeviceData("plc_001", 
    ReadRequest.builder()
        .address("30001")
        .count(10)
        .build());
```

### OPC UA设备接入

#### 1. 设备配置
```yaml
- device-id: "opcua_server_001"
  name: "OPC UA服务器001"
  protocol-type: "OPC_UA"
  connection:
    type: "TCP"
    host: "192.168.1.200"
    port: 4840
    security-policy: "Basic256Sha256"
    message-security-mode: "SignAndEncrypt"
  data-points:
    temperature:
      node-id: "ns=2;s=Temperature"
      data-type: "FLOAT32"
      unit: "°C"
    pressure:
      node-id: "ns=2;s=Pressure"
      data-type: "FLOAT32"
      unit: "Pa"
```

### 自定义协议接入

#### 1. 实现协议适配器
```java
@Component
public class CustomProtocolAdapter implements ProtocolAdapter {
    
    @Override
    public boolean connect(DeviceConfig config) {
        // 实现设备连接逻辑
        return true;
    }
    
    @Override
    public DeviceData readData(ReadRequest request) {
        // 实现数据读取逻辑
        DeviceData data = new DeviceData();
        // ... 读取数据
        return data;
    }
    
    @Override
    public boolean writeData(WriteRequest request) {
        // 实现数据写入逻辑
        return true;
    }
}
```

#### 2. 注册适配器
```java
@Component
public class CustomProtocolFactory implements ProtocolFactory {
    
    @Override
    public String getProtocolType() {
        return "CUSTOM";
    }
    
    @Override
    public ProtocolAdapter createAdapter(DeviceConfig config) {
        return new CustomProtocolAdapter();
    }
}
```

## 边缘计算配置

### 规则引擎配置

#### 1. 条件规则
```yaml
rules:
  - name: "温度告警"
    condition: "temperature > 50"
    action: "send_alert"
    level: "WARNING"
    enabled: true
    
  - name: "设备离线检测"
    condition: "last_update_time < now() - 300"
    action: "device_offline_alert"
    level: "HIGH"
    enabled: true
```

#### 2. 数据聚合规则
```yaml
rules:
  - name: "小时平均值"
    type: "AGGREGATION"
    condition: "every 1 hour"
    action: "calculate_average"
    fields: ["temperature", "humidity"]
    enabled: true
```

#### 3. 异常检测规则
```yaml
rules:
  - name: "异常值检测"
    type: "ANOMALY"
    condition: "value > mean + 3 * std"
    action: "anomaly_alert"
    level: "MEDIUM"
    enabled: true
```

### 自定义规则实现

#### 1. 实现规则处理器
```java
@Component
public class CustomRuleHandler implements RuleHandler {
    
    @Override
    public String getRuleType() {
        return "CUSTOM";
    }
    
    @Override
    public RuleResult execute(RuleContext context) {
        // 实现自定义规则逻辑
        DeviceData data = context.getDeviceData();
        String condition = context.getCondition();
        
        // 执行规则判断
        boolean result = evaluateCondition(data, condition);
        
        return RuleResult.builder()
            .success(result)
            .message("Custom rule executed")
            .build();
    }
}
```

## 安全配置

### TLS加密配置

#### 1. 证书配置
```yaml
security:
  enabled: true
  tls-enabled: true
  certificate-path: "/path/to/certificate.pem"
  private-key-path: "/path/to/private-key.pem"
  ca-certificate-path: "/path/to/ca-certificate.pem"
```

#### 2. 加密算法配置
```yaml
security:
  encryption-algorithm: "AES"
  key-size: 256
  cipher-mode: "GCM"
```

### 访问控制配置

#### 1. 用户权限配置
```yaml
security:
  users:
    - username: "admin"
      password: "admin123"
      roles: ["ADMIN"]
      permissions: ["*"]
    
    - username: "operator"
      password: "operator123"
      roles: ["OPERATOR"]
      permissions: ["device:read", "device:write"]
```

#### 2. 设备权限配置
```yaml
security:
  device-permissions:
    - device-id: "temp_sensor_001"
      users: ["admin", "operator"]
      permissions: ["read", "write"]
    
    - device-id: "plc_001"
      users: ["admin"]
      permissions: ["read", "write", "control"]
```

## 监控和运维

### 监控指标

#### 1. 设备监控
```bash
# 查看设备连接状态
curl http://localhost:8080/api/iot/metrics/devices/status

# 查看设备性能指标
curl http://localhost:8080/api/iot/metrics/devices/performance

# 查看协议转换性能
curl http://localhost:8080/api/iot/metrics/protocols/conversion
```

#### 2. 系统监控
```bash
# 查看系统资源使用
curl http://localhost:8080/actuator/metrics/system.cpu.usage

# 查看内存使用
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# 查看连接数
curl http://localhost:8080/actuator/metrics/tomcat.sessions.active.current
```

### 告警配置

#### 1. 告警规则配置
```yaml
monitor:
  alert-enabled: true
  alert-webhook: "http://alert-server/webhook"
  rules:
    - name: "设备离线告警"
      condition: "device_status == 'OFFLINE'"
      level: "HIGH"
      channels: ["email", "sms", "webhook"]
      
    - name: "数据异常告警"
      condition: "data_anomaly_score > 0.8"
      level: "MEDIUM"
      channels: ["email", "webhook"]
```

#### 2. 告警通知配置
```yaml
monitor:
  notifications:
    email:
      enabled: true
      smtp-host: "smtp.example.com"
      smtp-port: 587
      username: "alert@example.com"
      password: "password"
      recipients: ["admin@example.com", "operator@example.com"]
    
    webhook:
      enabled: true
      url: "http://webhook-server/notify"
      timeout: 5000
```

### 日志配置

#### 1. 日志级别配置
```yaml
logging:
  level:
    com.taobao.gateway.iot: DEBUG
    com.taobao.gateway.iot.protocol: INFO
    com.taobao.gateway.iot.device: INFO
    com.taobao.gateway.iot.security: WARN
```

#### 2. 日志文件配置
```yaml
logging:
  file:
    name: logs/iot-gateway.log
    max-size: 100MB
    max-history: 30
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

## 性能优化

### 连接池优化

#### 1. 连接池配置
```yaml
protocols:
  modbus:
    connection-pool:
      max-idle: 5
      max-active: 20
      min-idle: 2
      max-wait: 5000
      test-on-borrow: true
      test-on-return: true
```

#### 2. 超时配置
```yaml
protocols:
  modbus:
    timeout: 3000
    retries: 3
    inter-frame-delay: 10
```

### 缓存优化

#### 1. 本地缓存配置
```yaml
cache:
  local-cache-size: 1000
  local-cache-expire-seconds: 300
  eviction-policy: "LRU"
```

#### 2. Redis缓存配置
```yaml
cache:
  redis-enabled: true
  redis-host: "localhost"
  redis-port: 6379
  redis-database: 1
  redis-pool:
    max-active: 20
    max-idle: 10
    min-idle: 5
    max-wait: 3000
```

## 故障排查

### 常见问题

#### 1. 设备连接失败
```bash
# 检查设备配置
curl http://localhost:8080/api/iot/devices/device_id/config

# 检查连接状态
curl http://localhost:8080/api/iot/devices/device_id/status

# 查看连接日志
tail -f logs/iot-gateway.log | grep "device_id"
```

#### 2. 数据读取异常
```bash
# 检查数据点配置
curl http://localhost:8080/api/iot/devices/device_id/datapoints

# 测试数据读取
curl http://localhost:8080/api/iot/devices/device_id/test-read

# 查看协议转换日志
tail -f logs/iot-gateway.log | grep "protocol"
```

#### 3. 性能问题
```bash
# 查看性能指标
curl http://localhost:8080/api/iot/metrics/performance

# 查看连接池状态
curl http://localhost:8080/api/iot/metrics/connection-pool

# 查看缓存命中率
curl http://localhost:8080/api/iot/metrics/cache
```

### 调试模式

#### 1. 启用调试日志
```yaml
logging:
  level:
    com.taobao.gateway.iot: DEBUG
    com.taobao.gateway.iot.protocol: DEBUG
    com.taobao.gateway.iot.device: DEBUG
```

#### 2. 启用协议调试
```yaml
protocols:
  modbus:
    debug-enabled: true
    log-raw-data: true
```

## 扩展开发

### 自定义协议开发

#### 1. 实现协议适配器
```java
@Component
public class CustomProtocolAdapter implements ProtocolAdapter {
    
    @Override
    public boolean connect(DeviceConfig config) {
        // 实现连接逻辑
        return true;
    }
    
    @Override
    public DeviceData readData(ReadRequest request) {
        // 实现读取逻辑
        return new DeviceData();
    }
    
    @Override
    public boolean writeData(WriteRequest request) {
        // 实现写入逻辑
        return true;
    }
}
```

#### 2. 注册协议工厂
```java
@Component
public class CustomProtocolFactory implements ProtocolFactory {
    
    @Override
    public String getProtocolType() {
        return "CUSTOM";
    }
    
    @Override
    public ProtocolAdapter createAdapter(DeviceConfig config) {
        return new CustomProtocolAdapter();
    }
}
```

### 自定义规则开发

#### 1. 实现规则处理器
```java
@Component
public class CustomRuleHandler implements RuleHandler {
    
    @Override
    public String getRuleType() {
        return "CUSTOM";
    }
    
    @Override
    public RuleResult execute(RuleContext context) {
        // 实现规则逻辑
        return RuleResult.success();
    }
}
```

## 总结

工业IoT网关提供了完整的工业物联网解决方案，支持多协议接入、边缘计算、安全通信等功能。通过合理的配置和优化，可以满足不同工业场景的需求。

### 主要优势
1. **高性能**: 基于Netty的高性能架构
2. **多协议**: 支持主流工业协议
3. **易扩展**: 模块化设计，支持自定义扩展
4. **安全可靠**: 多层次安全机制
5. **运维友好**: 完善的监控和告警

### 适用场景
- 工业自动化
- 能源管理
- 楼宇控制
- 智能制造
- 设备监控

通过本指南，您可以快速搭建和配置工业IoT网关，实现工业设备的数据采集、处理和云端对接。