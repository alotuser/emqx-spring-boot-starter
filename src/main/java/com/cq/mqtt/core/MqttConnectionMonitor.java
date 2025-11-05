package com.cq.mqtt.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * MQTT 连接监控器，定期检查连接状态并在连接恢复时重新订阅所有主题
 * 
 * @author alotuser
 * @since 2025/5/10
 */
@Component
public class MqttConnectionMonitor {

	private static final Logger logger = LoggerFactory.getLogger(MqttConnectionMonitor.class);

	private final MqttClientFactory clientFactory;
	private final SubscriptionManager subscriptionManager;
	private final ScheduledExecutorService monitorExecutor;
	private volatile boolean lastConnectionState = false;

	public MqttConnectionMonitor(MqttClientFactory clientFactory, SubscriptionManager subscriptionManager) {
		this.clientFactory = clientFactory;
		this.subscriptionManager = subscriptionManager;
		this.monitorExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "mqtt-connection-monitor");
			t.setDaemon(true);
			return t;
		});

		startMonitoring();
	}

	/**
	 * 开始监控连接状态
	 */
	private void startMonitoring() {
		monitorExecutor.scheduleAtFixedRate(() -> {
			try {
				boolean currentState = clientFactory.isConnected();

				// 检测连接状态变化：从断开到连接
				if (currentState && !lastConnectionState) {
					logger.info("Connection state changed from disconnected to connected");
					// 连接恢复后，确保重新订阅
					subscriptionManager.resubscribeAll();
				}

				lastConnectionState = currentState;

			} catch (Exception e) {
				logger.error("Error in connection monitoring", e);
			}
		}, 10, 5, TimeUnit.SECONDS); // 10秒后开始，每5秒检查一次
	}

	@PreDestroy
	public void destroy() {
		if (monitorExecutor != null) {
			monitorExecutor.shutdown();
			try {
				if (!monitorExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
					monitorExecutor.shutdownNow();
				}
			} catch (InterruptedException e) {
				monitorExecutor.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
	}
}
