package com.cq.mqtt.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cq.mqtt.config.MqttProperties;
import com.cq.mqtt.retry.RetryCallback;
import com.cq.mqtt.retry.RetryContext;
import com.cq.mqtt.retry.RetryExhaustedException;
import com.cq.mqtt.retry.RetryPolicy;
import com.cq.mqtt.retry.RetryTemplate;

/**
 * 默认的 MQTT 客户端工厂实现，负责创建和管理 MQTT 客户端实例 包括连接管理、重连机制和订阅管理
 * 
 * @author alotuser
 * @since 2025/5/10
 */
public class DefaultMqttClientFactory implements MqttClientFactory {

	private static final Logger logger = LoggerFactory.getLogger(DefaultMqttClientFactory.class);

	private final MqttProperties properties;
	private final MqttConnectOptions connectOptions;
	private final RetryPolicy connectRetryPolicy;
	private final SubscriptionManager subscriptionManager;

	private MqttClient mqttClient;
	private volatile boolean connected = false;
	private ScheduledExecutorService reconnectExecutor;
	private final Object connectionLock = new Object();

	public DefaultMqttClientFactory(MqttProperties properties, MqttConnectOptions connectOptions, SubscriptionManager subscriptionManager) {
		this.properties = properties;
		this.connectOptions = connectOptions;
		this.subscriptionManager = subscriptionManager;
		this.connectRetryPolicy = new MqttRetryPolicy(properties.getRetry(), "connect");
		this.reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "mqtt-reconnect-thread");
			t.setDaemon(true);
			return t;
		});

		initializeClient();
	}

	private void initializeClient() {
		synchronized (connectionLock) {
			try {
				String clientId = properties.getClientId();
				if (clientId == null || clientId.trim().isEmpty()) {
					clientId = "mqtt-client-" + System.currentTimeMillis();
				}

				mqttClient = new MqttClient(properties.getServerUri(), clientId);

				// 设置回调，处理连接状态变化
				mqttClient.setCallback(new MqttCallbackExtended() {
					@Override
					public void connectComplete(boolean reconnect, String serverURI) {
						synchronized (connectionLock) {
							connected = true;
							logger.info("MQTT connection {} established to {}", reconnect ? "reconnected" : "connected", serverURI);

							// 如果是重连，重新订阅所有主题
							if (reconnect) {
								logger.info("Reconnection detected, resubscribing to topics...");
								resubscribeAfterReconnect();
							}
						}
					}

					@Override
					public void connectionLost(Throwable cause) {
						synchronized (connectionLock) {
							connected = false;
							logger.warn("MQTT connection lost", cause);
							scheduleReconnect();
						}
					}

					@Override
					public void messageArrived(String topic, MqttMessage message) {
						// 消息分发由专门的监听器处理
						logger.trace("Message arrived on topic: {}", topic);
					}

					@Override
					public void deliveryComplete(IMqttDeliveryToken token) {
						// 消息发送完成
						logger.trace("Message delivery complete");
					}
				});

				connectWithRetry();

			} catch (MqttException e) {
				logger.error("Failed to initialize MQTT client", e);
				scheduleReconnect();
			}
		}
	}

	/**
	 * 重连后重新订阅
	 */
	private void resubscribeAfterReconnect() {
		// 延迟一段时间再重新订阅，确保连接稳定
		reconnectExecutor.schedule(() -> {
			try {
				logger.info("Starting to resubscribe topics after reconnection...");
				subscriptionManager.resubscribeAll();
				logger.info("Topic resubscription completed after reconnection");
			} catch (Exception e) {
				logger.error("Error during topic resubscription after reconnection", e);
			}
		}, 2, TimeUnit.SECONDS); // 延迟2秒确保连接稳定
	}

	private void connectWithRetry() {
		try {
			RetryTemplate.execute(connectRetryPolicy, new RetryCallback<Boolean>() {
				@Override
				public Boolean doWithRetry() throws Exception {
					logger.info("Attempting to connect to MQTT broker...");
					synchronized (connectionLock) {
						mqttClient.connect(connectOptions);
						connected = true;
					}
					return true;
				}

				@Override
				public Object getContextData() {
					return properties.getServerUri();
				}
			});

		} catch (RetryExhaustedException e) {
			logger.error("Failed to connect to MQTT broker after all retry attempts", e);
			scheduleReconnect();
		}
	}

	private void scheduleReconnect() {
		if (!properties.getRetry().isEnableConnectRetry()) {
			return;
		}

		// 使用退避策略计算重连间隔
		RetryContext context = new RetryContext(1, System.currentTimeMillis(), null, properties.getServerUri());
		long delay = connectRetryPolicy.getNextRetryInterval(context);

		logger.info("Scheduling reconnect in {} ms", delay);
		reconnectExecutor.schedule(this::connectWithRetry, delay, TimeUnit.MILLISECONDS);
	}

	@Override
	public MqttClient getClient() {
		return mqttClient;
	}

	@Override
	public boolean isConnected() {
		return connected && mqttClient != null && mqttClient.isConnected();
	}

	@PreDestroy
	public void destroy() {
		logger.info("Shutting down MQTT client factory...");

		if (reconnectExecutor != null) {
			reconnectExecutor.shutdown();
			try {
				if (!reconnectExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
					reconnectExecutor.shutdownNow();
				}
			} catch (InterruptedException e) {
				reconnectExecutor.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}

		if (mqttClient != null) {
			synchronized (connectionLock) {
				if (mqttClient.isConnected()) {
					try {
						mqttClient.disconnect();
						logger.info("MQTT client disconnected");
					} catch (MqttException e) {
						logger.error("Error disconnecting MQTT client", e);
					}
				}
				try {
					mqttClient.close();
					logger.info("MQTT client closed");
				} catch (MqttException e) {
					logger.error("Error closing MQTT client", e);
				}
			}
		}

		logger.info("MQTT client factory shutdown completed");
	}

	@Override
	public MqttProperties getProperties() {
		return properties;
	}
}
