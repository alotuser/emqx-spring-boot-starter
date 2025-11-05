package cn.alotus.mqtt.core;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.alotus.mqtt.config.MqttProperties;
import cn.alotus.mqtt.retry.RetryCallback;
import cn.alotus.mqtt.retry.RetryExhaustedException;
import cn.alotus.mqtt.retry.RetryPolicy;
import cn.alotus.mqtt.retry.RetryPolicyType;
import cn.alotus.mqtt.retry.RetryTemplate;

/**
 * MQTT 模板类，封装了 MQTT 发布和订阅的核心操作，提供简化的接口供应用程序使用
 * 
 * @author alotuser
 * @since 2025/5/10
 */
public class MqttTemplate {

	private static final Logger logger = LoggerFactory.getLogger(MqttTemplate.class);

	private final MqttClientFactory clientFactory;
	private final RetryPolicy publishRetryPolicy;
	private final MqttProperties properties;

	
	/**
	 * 使用订阅管理器初始化 MqttTemplate
	 * 
	 * @param subscriptionManager 订阅管理器
	 */
	public MqttTemplate(SubscriptionManager subscriptionManager) {
		this.clientFactory = subscriptionManager.getClientFactory();
		this.properties = clientFactory.getProperties();
		this.publishRetryPolicy = new MqttRetryPolicy(properties.getRetry(), RetryPolicyType.PUBLISH);
	}

	/**
	 * 使用客户端工厂初始化 MqttTemplate
	 * 
	 * @param clientFactory MQTT 客户端工厂
	 */
	public MqttTemplate(MqttClientFactory clientFactory) {
		this.clientFactory = clientFactory;
		this.properties = clientFactory.getProperties();
		this.publishRetryPolicy = new MqttRetryPolicy(properties.getRetry(), RetryPolicyType.PUBLISH);
	}

	/**
	 * 使用客户端工厂和配置属性初始化 MqttTemplate
	 * 
	 * @param clientFactory MQTT 客户端工厂
	 * @param properties    MQTT 配置属性
	 */
	public MqttTemplate(MqttClientFactory clientFactory, MqttProperties properties) {
		this.clientFactory = clientFactory;
		this.properties = properties;
		this.publishRetryPolicy = new MqttRetryPolicy(properties.getRetry(), RetryPolicyType.PUBLISH);
	}

	/**
	 * 发布消息到指定主题，支持重试机制
	 * 
	 * @param topic    主题
	 * @param payload  消息负载
	 * @param qos      服务质量等级
	 * @param retained 是否保留消息
	 */
	public void publish(String topic, byte[] payload, int qos, boolean retained) {
		PublishContext context = new PublishContext(topic, payload, qos, retained);

		try {
			RetryTemplate.execute(publishRetryPolicy, new RetryCallback<Boolean>() {
				@Override
				public Boolean doWithRetry() throws Exception {
					return doPublish(context);
				}

				@Override
				public Object getContextData() {
					return context;
				}
			});

		} catch (RetryExhaustedException e) {
			logger.error("Failed to publish message after all retry attempts. Topic: {}", topic, e);
			throw new RuntimeException("MQTT publish failed after retries", e);
		}
	}

	/**
	 * 执行实际的消息发布操作
	 * 
	 * @param context 发布上下文
	 * @return 发布是否成功
	 * @throws MqttException 如果发布过程中发生错误
	 */
	private boolean doPublish(PublishContext context) throws MqttException {
		if (!clientFactory.isConnected()) {
			throw new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
		}

		try {
			MqttMessage message = new MqttMessage(context.getPayload());
			message.setQos(context.getQos());
			message.setRetained(context.isRetained());

			clientFactory.getClient().publish(context.getTopic(), message);
			logger.debug("Message published successfully to topic: {}", context.getTopic());
			return true;

		} catch (MqttException e) {
			logger.warn("Failed to publish message to topic: {}, attempt will be retried", context.getTopic(), e);
			throw e;
		}
	}

	/**
	 * 异步发布消息到指定主题
	 * 
	 * @param topic    主题
	 * @param payload  消息负载
	 * @param qos      服务质量等级
	 * @param retained 是否保留消息
	 * @return CompletableFuture 表示异步发布操作的结果
	 */
	public CompletableFuture<Void> publishAsync(String topic, byte[] payload, int qos, boolean retained) {
		return CompletableFuture.runAsync(() -> publish(topic, payload, qos, retained));
	}

	/**
	 * 重载的发布方法，支持字符串类型的消息负载
	 * 
	 * @param topic    主题
	 * @param payload  字符串消息负载
	 * @param qos      服务质量等级
	 * @param retained 是否保留消息
	 */
	public void publish(String topic, String payload, int qos, boolean retained) {
		publish(topic, payload.getBytes(StandardCharsets.UTF_8), qos, retained);
	}

	/**
	 * 重载的发布方法，使用默认的 QoS 和不保留消息
	 * 
	 * @param topic   主题
	 * @param payload 字符串消息负载
	 */
	public void publish(String topic, String payload) {
		publish(topic, payload, 1, false);
	}

	/**
	 * 重载的发布方法，使用默认的 QoS 和不保留消息
	 * 
	 * @param topic   主题
	 * @param payload 消息负载
	 */
	public void publish(String topic, byte[] payload) {
		publish(topic, payload, 1, false);
	}

	/**
	 * 订阅指定主题，使用消息监听器处理接收到的消息
	 * 
	 * @param topic           主题
	 * @param qos             服务质量等级
	 * @param messageListener 消息监听器
	 */
	public void subscribe(String topic, int qos, IMqttMessageListener messageListener) {
		if (!clientFactory.isConnected()) {
			throw new IllegalStateException("MQTT client is not connected");
		}

		try {
			clientFactory.getClient().subscribe(topic, qos, messageListener);
			logger.info("Subscribed to topic: {} with QoS: {}", topic, qos);
		} catch (MqttException e) {
			logger.error("Failed to subscribe to topic: {}", topic, e);
			throw new RuntimeException("MQTT subscribe failed", e);
		}
	}

	/**
	 * 取消订阅指定主题
	 * 
	 * @param topic 主题
	 */
	public void unsubscribe(String topic) {
		if (!clientFactory.isConnected()) {
			return;
		}

		try {
			clientFactory.getClient().unsubscribe(topic);
			logger.info("Unsubscribed from topic: {}", topic);
		} catch (MqttException e) {
			logger.error("Failed to unsubscribe from topic: {}", topic, e);
		}
	}

	/**
	 * 内部类，封装发布消息的上下文信息
	 */
	private static class PublishContext {
		private final String topic;
		private final byte[] payload;
		private final int qos;
		private final boolean retained;

		public PublishContext(String topic, byte[] payload, int qos, boolean retained) {
			this.topic = topic;
			this.payload = payload;
			this.qos = qos;
			this.retained = retained;
		}

		public String getTopic() {
			return topic;
		}

		public byte[] getPayload() {
			return payload;
		}

		public int getQos() {
			return qos;
		}

		public boolean isRetained() {
			return retained;
		}
	}
}
