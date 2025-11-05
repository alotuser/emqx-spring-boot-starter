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

	
	
	public MqttTemplate(SubscriptionManager subscriptionManager) {
		this.clientFactory = subscriptionManager.getClientFactory();
		this.properties = clientFactory.getProperties();
		this.publishRetryPolicy = new MqttRetryPolicy(properties.getRetry(), RetryPolicyType.PUBLISH);
	}
	public MqttTemplate(MqttClientFactory clientFactory) {
		this.clientFactory = clientFactory;
		this.properties = clientFactory.getProperties();
		this.publishRetryPolicy = new MqttRetryPolicy(properties.getRetry(), RetryPolicyType.PUBLISH);
	}
	
	public MqttTemplate(MqttClientFactory clientFactory, MqttProperties properties) {
		this.clientFactory = clientFactory;
		this.properties = properties;
		this.publishRetryPolicy = new MqttRetryPolicy(properties.getRetry(), RetryPolicyType.PUBLISH);
	}

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

	// 异步发布
	public CompletableFuture<Void> publishAsync(String topic, byte[] payload, int qos, boolean retained) {
		return CompletableFuture.runAsync(() -> publish(topic, payload, qos, retained));
	}

	public void publish(String topic, String payload, int qos, boolean retained) {
		publish(topic, payload.getBytes(StandardCharsets.UTF_8), qos, retained);
	}

	public void publish(String topic, String payload) {
		publish(topic, payload, 1, false);
	}

	public void publish(String topic, byte[] payload) {
		publish(topic, payload, 1, false);
	}

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

	// 发布上下文类
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
