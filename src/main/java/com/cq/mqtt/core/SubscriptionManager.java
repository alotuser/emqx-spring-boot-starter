package com.cq.mqtt.core;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 订阅管理器，负责管理 MQTT 主题的订阅信息 包括注册订阅、取消订阅和重新订阅等功能
 * 
 * @author alotuser
 * @since 2025/5/10
 */
@Component
public class SubscriptionManager {

	private static final Logger logger = LoggerFactory.getLogger(SubscriptionManager.class);

	private final MqttClientFactory clientFactory;
	private final Map<String, SubscriptionInfo> subscriptionMap = new ConcurrentHashMap<>();
	private volatile boolean reconnecting = false;

	public SubscriptionManager(MqttClientFactory clientFactory) {
		this.clientFactory = clientFactory;
	}

	/**
	 * 注册订阅
	 */
	public void registerSubscription(String topic, int qos, IMqttMessageListener listener) {
		String key = generateKey(topic, qos);
		subscriptionMap.put(key, new SubscriptionInfo(topic, qos, listener));

		// 如果当前已连接，立即订阅
		if (clientFactory.isConnected() && !reconnecting) {
			subscribeImmediately(topic, qos, listener);
		}
		
	}

	/**
	 * 立即订阅
	 */
	private void subscribeImmediately(String topic, int qos, IMqttMessageListener listener) {
		try {
			clientFactory.getClient().subscribe(topic, qos, listener);
			logger.debug("Subscribed to topic: {} with QoS: {}", topic, qos);
		} catch (MqttException e) {
			logger.error("Failed to subscribe to topic: {}", topic, e);
		}
	}

	/**
	 * 重新订阅所有主题
	 */
	public void resubscribeAll() {
		if (subscriptionMap.isEmpty()) {
			return;
		}

		reconnecting = true;
		try {
			logger.info("Resubscribing to {} topics after reconnection", subscriptionMap.size());

			for (SubscriptionInfo info : subscriptionMap.values()) {
				try {
					clientFactory.getClient().subscribe(info.getTopic(), info.getQos(), info.getListener());
					logger.debug("Resubscribed to topic: {} with QoS: {}", info.getTopic(), info.getQos());

					// 添加小延迟，避免一次性大量订阅导致问题
					Thread.sleep(10);
				} catch (Exception e) {
					logger.error("Failed to resubscribe to topic: {}", info.getTopic(), e);
				}
			}

			logger.info("Resubscribe completed successfully");
		} catch (Exception e) {
			logger.error("Error during resubscribe", e);
		} finally {
			reconnecting = false;
		}
	}

	/**
	 * 取消订阅
	 */
	public void unsubscribe(String topic, int qos) {
		String key = generateKey(topic, qos);
		subscriptionMap.remove(key);

		if (clientFactory.isConnected()) {
			try {
				clientFactory.getClient().unsubscribe(topic);
			} catch (MqttException e) {
				logger.error("Failed to unsubscribe from topic: {}", topic, e);
			}
		}
	}

	/**
	 * 获取所有订阅信息
	 */
	public Collection<SubscriptionInfo> getAllSubscriptions() {
		return subscriptionMap.values();
	}

	/**
	 * 生成订阅键
	 */
	private String generateKey(String topic, int qos) {
		return topic + "|" + qos;
	}

	/**
	 * 订阅信息类
	 */
	public static class SubscriptionInfo {
		private final String topic;
		private final int qos;
		private final IMqttMessageListener listener;

		public SubscriptionInfo(String topic, int qos, IMqttMessageListener listener) {
			this.topic = topic;
			this.qos = qos;
			this.listener = listener;
		}

		public String getTopic() {
			return topic;
		}

		public int getQos() {
			return qos;
		}

		public IMqttMessageListener getListener() {
			return listener;
		}
	}

	public MqttClientFactory getClientFactory() {
		return clientFactory;
	}
}
