package com.cq.mqtt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
/**
 * MQTT 配置属性类
 * @author alotuser
 * @since 2025/5/10
 */
@ConfigurationProperties(prefix = "emqx.mqtt")
public class MqttProperties {

	private String serverUri = "tcp://localhost:1883";
	private String clientId;
	private String username;
	private String password;
	private int connectionTimeout = 30;
	private int keepAliveInterval = 60;
	private boolean automaticReconnect = true;
	private boolean cleanSession = true;

	// SSL 配置
	private Ssl ssl = new Ssl();

	public static class Ssl {
		private boolean enabled = false;
		private String keyStore;
		private String keyStorePassword;
		private String trustStore;
		private String trustStorePassword;
		private String keyStoreType = "JKS";
		private String trustStoreType = "JKS";

		// getters and setters
		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getKeyStore() {
			return keyStore;
		}

		public void setKeyStore(String keyStore) {
			this.keyStore = keyStore;
		}

		public String getKeyStorePassword() {
			return keyStorePassword;
		}

		public void setKeyStorePassword(String keyStorePassword) {
			this.keyStorePassword = keyStorePassword;
		}

		public String getTrustStore() {
			return trustStore;
		}

		public void setTrustStore(String trustStore) {
			this.trustStore = trustStore;
		}

		public String getTrustStorePassword() {
			return trustStorePassword;
		}

		public void setTrustStorePassword(String trustStorePassword) {
			this.trustStorePassword = trustStorePassword;
		}

		public String getKeyStoreType() {
			return keyStoreType;
		}

		public void setKeyStoreType(String keyStoreType) {
			this.keyStoreType = keyStoreType;
		}

		public String getTrustStoreType() {
			return trustStoreType;
		}

		public void setTrustStoreType(String trustStoreType) {
			this.trustStoreType = trustStoreType;
		}
	}

	// 重试配置
	private Retry retry = new Retry();

	public static class Retry {
		// 连接重试配置
		private boolean enableConnectRetry = true;
		private int maxConnectAttempts = 5;
		private long connectRetryInterval = 5000; // ms
		private long maxConnectRetryInterval = 30000; // ms
		private double connectRetryMultiplier = 1.5;

		// 消息发送重试配置
		private boolean enablePublishRetry = true;
		private int maxPublishAttempts = 3;
		private long publishRetryInterval = 1000; // ms
		private long maxPublishRetryInterval = 5000; // ms
		private double publishRetryMultiplier = 1.2;

		// 退避策略
		private BackoffStrategy backoffStrategy = BackoffStrategy.EXPONENTIAL;

		public enum BackoffStrategy {
			FIXED, // 固定间隔
			LINEAR, // 线性增长
			EXPONENTIAL // 指数退避
		}

		// getters and setters
		public boolean isEnableConnectRetry() {
			return enableConnectRetry;
		}

		public void setEnableConnectRetry(boolean enableConnectRetry) {
			this.enableConnectRetry = enableConnectRetry;
		}

		public int getMaxConnectAttempts() {
			return maxConnectAttempts;
		}

		public void setMaxConnectAttempts(int maxConnectAttempts) {
			this.maxConnectAttempts = maxConnectAttempts;
		}

		public long getConnectRetryInterval() {
			return connectRetryInterval;
		}

		public void setConnectRetryInterval(long connectRetryInterval) {
			this.connectRetryInterval = connectRetryInterval;
		}

		public long getMaxConnectRetryInterval() {
			return maxConnectRetryInterval;
		}

		public void setMaxConnectRetryInterval(long maxConnectRetryInterval) {
			this.maxConnectRetryInterval = maxConnectRetryInterval;
		}

		public double getConnectRetryMultiplier() {
			return connectRetryMultiplier;
		}

		public void setConnectRetryMultiplier(double connectRetryMultiplier) {
			this.connectRetryMultiplier = connectRetryMultiplier;
		}

		public boolean isEnablePublishRetry() {
			return enablePublishRetry;
		}

		public void setEnablePublishRetry(boolean enablePublishRetry) {
			this.enablePublishRetry = enablePublishRetry;
		}

		public int getMaxPublishAttempts() {
			return maxPublishAttempts;
		}

		public void setMaxPublishAttempts(int maxPublishAttempts) {
			this.maxPublishAttempts = maxPublishAttempts;
		}

		public long getPublishRetryInterval() {
			return publishRetryInterval;
		}

		public void setPublishRetryInterval(long publishRetryInterval) {
			this.publishRetryInterval = publishRetryInterval;
		}

		public long getMaxPublishRetryInterval() {
			return maxPublishRetryInterval;
		}

		public void setMaxPublishRetryInterval(long maxPublishRetryInterval) {
			this.maxPublishRetryInterval = maxPublishRetryInterval;
		}

		public double getPublishRetryMultiplier() {
			return publishRetryMultiplier;
		}

		public void setPublishRetryMultiplier(double publishRetryMultiplier) {
			this.publishRetryMultiplier = publishRetryMultiplier;
		}

		public BackoffStrategy getBackoffStrategy() {
			return backoffStrategy;
		}

		public void setBackoffStrategy(BackoffStrategy backoffStrategy) {
			this.backoffStrategy = backoffStrategy;
		}
	}

	public Retry getRetry() {
		return retry;
	}

	public void setRetry(Retry retry) {
		this.retry = retry;
	}

	// getters and setters
	public String getServerUri() {
		return serverUri;
	}

	public void setServerUri(String serverUri) {
		this.serverUri = serverUri;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public int getKeepAliveInterval() {
		return keepAliveInterval;
	}

	public void setKeepAliveInterval(int keepAliveInterval) {
		this.keepAliveInterval = keepAliveInterval;
	}

	public boolean isAutomaticReconnect() {
		return automaticReconnect;
	}

	public void setAutomaticReconnect(boolean automaticReconnect) {
		this.automaticReconnect = automaticReconnect;
	}

	public boolean isCleanSession() {
		return cleanSession;
	}

	public void setCleanSession(boolean cleanSession) {
		this.cleanSession = cleanSession;
	}

	public Ssl getSsl() {
		return ssl;
	}

	public void setSsl(Ssl ssl) {
		this.ssl = ssl;
	}
}
