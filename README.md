# MQTT Spring Boot Starter 开发文档

## 目录
- [功能特性](#功能特性)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [核心组件](#核心组件)
- [使用示例](#使用示例)
- [高级功能](#高级功能)
- [故障排除](#故障排除)
- [最佳实践](#最佳实践)

## 项目概述

**MQTT Spring Boot Starter** 是一个基于 Spring Boot 的自动配置 Starter，为 MQTT 协议提供企业级的集成解决方案。该项目封装了 Eclipse Paho MQTT 客户端，提供了简单易用的注解驱动开发模式，同时具备生产环境所需的高可用特性。

## 功能特性

### 核心功能
- ✅ 自动配置 MQTT 客户端
- ✅ SSL/TLS 安全连接支持
- ✅ 自动重连机制
- ✅ 注解方式订阅消息
- ✅ 消息发布模板
- ✅ 完整的重试机制
- ✅ 连接状态监控

### 高级特性
- 🔄 指数退避重试策略
- 📊 连接健康监控
- 🔒 多层级安全配置
- 🚀 异步消息处理
- 📝 完整的日志记录
- 💡 智能异常处理

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.github.alotuser</groupId>
    <artifactId>emqx-spring-boot-starter</artifactId>
    <version>1.0.1</version>
</dependency>
```

### 2. 基础配置

```yaml
# application.yml
spring:
  mqtt:
    server-uri: tcp://localhost:1883
    client-id: my-app-${random.uuid}
    username: admin
    password: password
    automatic-reconnect: true
```

### 3. 创建消息处理器

```java
@Service
public class SensorDataHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(SensorDataHandler.class);
    
    @MqttTopic("sensors/temperature")
    public void handleTemperature(String temperature) {
        logger.info("收到温度数据: {}°C", temperature);
        // 处理温度数据
    }
    
    @MqttTopic("sensors/humidity")
    public void handleHumidity(MqttMessageContext context) {
        logger.info("收到湿度数据，主题: {}, 值: {}", 
            context.getTopic(), context.getPayloadAsString());
    }
}
```

### 4. 发送消息

```java
@RestController
public class SensorController {
    
    @Autowired
    private MqttTemplate mqttTemplate;
    
    @PostMapping("/sensors/temperature")
    public String publishTemperature(@RequestParam double temperature) {
        mqttTemplate.publish("sensors/temperature", String.valueOf(temperature));
        return "温度数据发布成功";
    }
}
```

## 配置说明

### 基础配置

```yaml
emqx:
  mqtt:
    # 必需配置
    server-uri: tcp://mqtt-broker:1883
    client-id: my-application
    
    # 认证配置
    username: admin
    password: secret
    
    # 连接配置
    connection-timeout: 30
    keep-alive-interval: 60
    automatic-reconnect: true
    clean-session: false
```

### SSL/TLS 配置

```yaml
emqx:
  mqtt:
    ssl:
      enabled: true
      key-store: classpath:keystore.jks
      key-store-password: changeit
      trust-store: classpath:truststore.jks
      trust-store-password: changeit
      key-store-type: JKS
      trust-store-type: JKS
```

### 重试机制配置

```yaml
emqx:
  mqtt:
    retry:
      # 连接重试配置
      enable-connect-retry: true
      max-connect-attempts: 10
      connect-retry-interval: 5000
      max-connect-retry-interval: 60000
      connect-retry-multiplier: 2.0
      
      # 消息发布重试配置
      enable-publish-retry: true
      max-publish-attempts: 3
      publish-retry-interval: 1000
      max-publish-retry-interval: 5000
      publish-retry-multiplier: 1.5
      
      # 退避策略
      backoff-strategy: EXPONENTIAL  # FIXED, LINEAR, EXPONENTIAL
```

### 高级配置

```yaml
emqx:
  mqtt:
    # 监控配置
    monitor:
      enabled: true
      check-interval: 5000
      
    # 性能配置
    executor:
      core-pool-size: 5
      max-pool-size: 20
      queue-capacity: 100
```

## 核心组件

### MqttTemplate
消息发布的核心工具类，提供同步和异步消息发送能力。

```java
@Service
public class MessageService {
    
    @Autowired
    private MqttTemplate mqttTemplate;
    
    // 同步发布
    public void sendMessage(String topic, String payload) {
        mqttTemplate.publish(topic, payload, 1, false);
    }
    
    // 异步发布
    public CompletableFuture<Void> sendMessageAsync(String topic, String payload) {
        return mqttTemplate.publishAsync(topic, payload.getBytes(), 1, false);
    }
    
    // 手动订阅
    public void subscribeToAlerts() {
        mqttTemplate.subscribe("alerts/#", 2, (topic, message) -> {
            // 处理告警消息
            handleAlert(topic, new String(message.getPayload()));
        });
    }
}
```

### @MqttTopic 注解
方法级别注解，用于自动订阅 MQTT 主题。

**支持的参数类型：**
- `String` - 消息内容字符串
- `byte[]` - 原始字节数组
- `MqttMessage` - 原始 MQTT 消息对象
- `MqttMessageContext` - 增强的消息上下文

```java
@Component
public class MultiTypeMessageHandler {
    
    // 字节数组参数
    @MqttTopic("messages/binary")
    public void handleBinaryMessage(byte[] payload) {
        // 处理二进制消息
    }
    
    // 完整消息对象
    @MqttTopic("messages/full")
    public void handleFullMessage(MqttMessage message) {
        // 访问消息的所有属性
        int qos = message.getQos();
        boolean retained = message.isRetained();
    }
    
    // 增强上下文
    @MqttTopic("messages/context")
    public void handleContextMessage(MqttMessageContext context) {
        // 访问主题和消息
        String topic = context.getTopic();
        String payload = context.getPayloadAsString();
        int qos = context.getQos();
    }
    
    // 多参数组合
    @MqttTopic("messages/multi")
    public void handleMultiParam(String payload, String topic, MqttMessage message) {
        // 按需选择需要的参数
    }
}
```

### MqttMessageContext
消息上下文对象，提供便捷的消息访问方法。

```java
@MqttTopic("devices/+/status")
public void handleDeviceStatus(MqttMessageContext context) {
    // 提取设备ID
    String deviceId = extractDeviceId(context.getTopic());
    
    // 解析JSON消息
    DeviceStatus status = parseStatus(context.getPayloadAsString());
    
    // 记录消息属性
    logger.info("设备 {} 状态更新 (QoS: {}, 保留: {})", 
        deviceId, context.getQos(), context.isRetained());
    
    // 处理消息时间戳
    if (System.currentTimeMillis() - context.getTimestamp() > 5000) {
        logger.warn("收到延迟消息");
    }
}

private String extractDeviceId(String topic) {
    String[] parts = topic.split("/");
    return parts.length > 1 ? parts[1] : "unknown";
}
```

## 使用示例

### 1. IoT 设备管理

```java
@Service
@Slf4j
public class DeviceManagementService {
    
    @Autowired
    private MqttTemplate mqttTemplate;
    
    // 接收设备遥测数据
    @MqttTopic("devices/+/telemetry")
    public void handleTelemetry(MqttMessageContext context) {
        String deviceId = extractDeviceId(context.getTopic());
        TelemetryData data = parseTelemetry(context.getPayloadAsString());
        
        log.info("设备 {} 上报数据: {}", deviceId, data);
        
        // 保存到数据库
        saveTelemetryData(deviceId, data);
        
        // 检查异常值
        if (data.getTemperature() > 80) {
            sendAlert(deviceId, "温度过高: " + data.getTemperature());
        }
    }
    
    // 接收设备状态
    @MqttTopic("devices/+/status")
    public void handleDeviceStatus(String status, String topic) {
        String deviceId = extractDeviceId(topic);
        log.info("设备 {} 状态更新: {}", deviceId, status);
        
        // 更新设备状态
        updateDeviceStatus(deviceId, status);
    }
    
    // 向设备发送命令
    public void sendCommand(String deviceId, String command) {
        String topic = "devices/" + deviceId + "/command";
        mqttTemplate.publish(topic, command, 1, false);
        log.info("向设备 {} 发送命令: {}", deviceId, command);
    }
    
    // 广播配置更新
    public void broadcastConfig(String config) {
        mqttTemplate.publish("devices/+/config", config, 1, true);
        log.info("广播配置更新");
    }
    
    private String extractDeviceId(String topic) {
        // 从主题中提取设备ID
        return topic.split("/")[1];
    }
}
```

### 2. 实时数据处理

```java
@Component
@Slf4j
public class RealTimeDataProcessor {
    
    private final Map<String, DataWindow> dataWindows = new ConcurrentHashMap<>();
    
    // 处理传感器数据流
    @MqttTopic("sensors/+/data")
    public void processSensorData(MqttMessageContext context) {
        String sensorId = extractSensorId(context.getTopic());
        SensorData data = parseSensorData(context.getPayload());
        
        // 更新数据窗口
        DataWindow window = dataWindows.computeIfAbsent(sensorId,  k -> new DataWindow(100)); // 100个数据点的窗口
        window.addData(data);
        
        // 检查是否需要处理
        if (window.isReady()) {
            processDataWindow(sensorId, window);
            window.reset();
        }
    }
    
    // 处理聚合数据
    @MqttTopic("sensors/+/aggregate")
    public void processAggregateData(String payload, String topic) {
        AggregateData data = parseAggregateData(payload);
        String sensorId = extractSensorId(topic);
        
        log.info("传感器 {} 聚合数据: 平均值={}, 最大值={}", 
            sensorId, data.getAverage(), data.getMax());
        
        // 触发业务逻辑
        if (data.getAverage() > data.getThreshold()) {
            triggerAlert(sensorId, data);
        }
    }
    
    private void processDataWindow(String sensorId, DataWindow window) {
        // 计算统计信息
        double average = window.calculateAverage();
        double max = window.calculateMax();
        
        // 发布聚合数据
        AggregateData aggregate = new AggregateData(average, max, 75.0);
        String topic = "sensors/" + sensorId + "/aggregate";
        mqttTemplate.publish(topic, aggregate.toJson(), 1, false);
        
        log.debug("传感器 {} 数据窗口处理完成", sensorId);
    }
}
```

### 3. 消息路由和转换

```java
@Service
@Slf4j
public class MessageRouterService {
    
    @Autowired
    private MqttTemplate mqttTemplate;
    
    // 接收原始消息并路由
    @MqttTopic("raw/+/data")
    public void routeRawData(MqttMessageContext context) {
        String source = extractSource(context.getTopic());
        RawData rawData = parseRawData(context.getPayload());
        
        // 数据转换
        ProcessedData processed = transformData(rawData);
        
        // 根据数据类型路由到不同主题
        String outputTopic = determineOutputTopic(processed.getType());
        mqttTemplate.publish(outputTopic, processed.toJson(), 1, false);
        
        log.info("将 {} 数据从 {} 路由到 {}", 
            processed.getType(), source, outputTopic);
    }
    
    // 错误消息处理
    @MqttTopic("errors/+")
    public void handleErrorMessages(String errorMessage, String topic) {
        String component = extractComponent(topic);
        
        log.error("组件 {} 报告错误: {}", component, errorMessage);
        
        // 发送到错误聚合主题
        mqttTemplate.publish("errors/aggregate", 
            createErrorRecord(component, errorMessage), 1, false);
        
        // 如果严重错误，发送告警
        if (isCriticalError(errorMessage)) {
            mqttTemplate.publish("alerts/critical", 
                createCriticalAlert(component, errorMessage), 2, false);
        }
    }
    
    // 消息格式转换
    @MqttTopic("legacy/format/+")
    public void convertLegacyFormat(byte[] payload, String topic) {
        String deviceType = extractDeviceType(topic);
        
        // 转换旧格式到新格式
        String newFormat = convertToNewFormat(payload, deviceType);
        
        // 发布到新主题
        mqttTemplate.publish("modern/format/" + deviceType, newFormat, 1, false);
        
        log.debug("转换 {} 设备数据到新格式", deviceType);
    }
}
```

### 4. 系统监控和告警

```java
@Component
@Slf4j
public class SystemMonitorService {
    
    @Autowired
    private MqttTemplate mqttTemplate;
    
    // 监控系统健康状态
    @MqttTopic("system/+/health")
    public void monitorSystemHealth(MqttMessageContext context) {
        String systemId = extractSystemId(context.getTopic());
        HealthStatus health = parseHealthStatus(context.getPayloadAsString());
        
        log.info("系统 {} 健康状态: {}", systemId, health.getStatus());
        
        // 检查健康状态
        if (health.getStatus() == HealthStatus.Status.CRITICAL) {
            handleCriticalHealth(systemId, health);
        } else if (health.getStatus() == HealthStatus.Status.WARNING) {
            handleWarningHealth(systemId, health);
        }
        
        // 更新监控仪表板
        updateDashboard(systemId, health);
    }
    
    // 接收性能指标
    @MqttTopic("metrics/+/performance")
    public void handlePerformanceMetrics(String metricsJson, String topic) {
        String serviceName = extractServiceName(topic);
        PerformanceMetrics metrics = parseMetrics(metricsJson);
        
        // 分析性能指标
        analyzePerformance(serviceName, metrics);
        
        // 如果性能下降，发送优化建议
        if (metrics.getResponseTime() > metrics.getThreshold()) {
            sendOptimizationSuggestion(serviceName, metrics);
        }
    }
    
    // 处理资源使用情况
    @MqttTopic("resources/+/usage")
    public void handleResourceUsage(MqttMessage message, String topic) {
        String resourceId = extractResourceId(topic);
        ResourceUsage usage = parseResourceUsage(message.getPayload());
        
        log.debug("资源 {} 使用情况: CPU={}%, Memory={}%", 
            resourceId, usage.getCpuUsage(), usage.getMemoryUsage());
        
        // 检查资源瓶颈
        if (usage.getCpuUsage() > 90 || usage.getMemoryUsage() > 90) {
            triggerScaling(resourceId, usage);
        }
    }
    
    private void handleCriticalHealth(String systemId, HealthStatus health) {
        String alertMessage = String.format("系统 %s 处于严重状态: %s", systemId, health.getMessage());
        
        // 发送紧急告警
        mqttTemplate.publish("alerts/emergency", alertMessage, 2, true);
        
        // 通知运维团队
        mqttTemplate.publish("notifications/ops", alertMessage, 1, false);
        
        log.error("检测到系统严重状态: {}", systemId);
    }
}
```

### 5. 配置类示例

```java
@Configuration
@Slf4j
public class MqttConfiguration {
    
    @Bean
    public MqttConnectionListener mqttConnectionListener() {
        return new MqttConnectionListener();
    }
    
    /**
     * MQTT 连接事件监听器
     */
    @Component
    public static class MqttConnectionListener {
        
        @EventListener
        public void handleMqttConnected(MqttConnectedEvent event) {
            log.info("MQTT 连接已建立: {}", event.getServerURI());
            
            // 连接建立后的初始化操作
            initializeAfterConnection();
        }
        
        @EventListener
        public void handleMqttDisconnected(MqttDisconnectedEvent event) {
            log.warn("MQTT 连接断开: {}", event.getCause().getMessage());
            
            // 连接断开后的清理操作
            cleanupAfterDisconnection();
        }
        
        private void initializeAfterConnection() {
            // 连接建立后的初始化逻辑
            log.info("执行连接后初始化...");
        }
        
        private void cleanupAfterDisconnection() {
            // 连接断开后的清理逻辑
            log.info("执行断开连接后清理...");
        }
    }
}
```

## 高级功能

### 1. 自定义重试策略

```java
@Component
public class CustomRetryPolicy implements RetryPolicy {
    
    @Override
    public boolean canRetry(RetryContext context) {
        // 自定义重试逻辑
        if (context.getLastException() instanceof MqttException) {
            MqttException e = (MqttException) context.getLastException();
            return e.getReasonCode() != MqttException.REASON_CODE_CLIENT_EXCEPTION;
        }
        return context.getAttemptCount() < 5;
    }
    
    @Override
    public long getNextRetryInterval(RetryContext context) {
        // 自定义退避策略
        return Math.min(1000 * (long) Math.pow(2, context.getAttemptCount()), 30000);
    }
}
```

### 2. 消息拦截器

```java
@Component
public class MessageInterceptor implements MqttMessagePostProcessor {
    
    @Override
    public MqttMessage postProcessMessage(MqttMessage message, String topic) {
        // 添加消息时间戳
        String payloadWithTimestamp = addTimestamp(message.getPayload());
        message.setPayload(payloadWithTimestamp.getBytes());
        
        // 设置消息属性
        message.setQos(1);
        message.setRetained(false);
        
        return message;
    }
    
    private String addTimestamp(byte[] payload) {
        String original = new String(payload, StandardCharsets.UTF_8);
        JsonObject json = JsonParser.parseString(original).getAsJsonObject();
        json.addProperty("timestamp", System.currentTimeMillis());
        return json.toString();
    }
}
```

## 故障排除

### 常见问题

1. **连接失败**
   ```yaml
   # 检查网络和认证配置
   spring:
     mqtt:
       server-uri: tcp://correct-host:1883
       username: correct-username
       password: correct-password
   ```

2. **重连后收不到消息**
   - 确保 `clean-session: false`
   - 检查订阅管理器是否正确工作

3. **SSL 连接问题**
   ```bash
   # 检查证书路径和密码
   keytool -list -v -keystore keystore.jks
   ```

### 日志调试

```yaml
logging:
  level:
    com.example.mqtt: DEBUG
    org.eclipse.paho: WARN
```

## 最佳实践

### 1. 主题设计
- 使用分层主题结构：`domain/device-type/device-id/data-type`
- 避免使用 `#` 通配符订阅过多主题
- 使用有意义的主题名称

### 2. 消息设计
- 使用 JSON 格式便于扩展
- 包含时间戳和版本信息
- 控制消息大小，避免大消息

### 3. QoS 选择
- QoS 0: 性能要求高，允许消息丢失
- QoS 1: 大多数应用场景
- QoS 2: 关键业务，不允许重复和丢失

### 4. 错误处理
```java
@MqttTopic("sensors/+/data")
public void handleSensorData(MqttMessageContext context) {
    try {
        // 业务处理逻辑
        processSensorData(context);
    } catch (Exception e) {
        log.error("处理传感器数据失败: {}", context.getTopic(), e);
        
        // 发送错误消息
        mqttTemplate.publish("errors/sensor-processing", 
            createErrorReport(context, e), 1, false);
    }
}
```

这个开发文档提供了完整的使用指南和最佳实践，帮助开发者快速上手并高效使用 MQTT Spring Boot Starter。
