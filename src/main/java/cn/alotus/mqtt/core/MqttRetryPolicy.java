package cn.alotus.mqtt.core;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.alotus.mqtt.config.MqttProperties;
import cn.alotus.mqtt.retry.RetryContext;
import cn.alotus.mqtt.retry.RetryPolicy;
import cn.alotus.mqtt.retry.RetryPolicyType;

/**
 * MQTT 重试策略实现，根据配置的重试策略和参数决定是否进行重试以及重试间隔时间
 * 
 * @author alotuser
 * @since 2025/5/10
 */
public class MqttRetryPolicy implements RetryPolicy {

	private static final Logger logger = LoggerFactory.getLogger(MqttRetryPolicy.class);

	private final MqttProperties.Retry retryConfig;
	private final RetryPolicyType operationType; // "connect" or "publish"

	public MqttRetryPolicy(MqttProperties.Retry retryConfig, RetryPolicyType operationType) {
		this.retryConfig = retryConfig;
		this.operationType = operationType;
	}

	@Override
	public boolean canRetry(RetryContext context) {
		if (!isRetryEnabled()) {
			return false;
		}

		int maxAttempts = getMaxAttempts();
		if (context.getAttemptCount() >= maxAttempts) {
			logger.warn("{} retry exceeded maximum attempts: {}", operationType, maxAttempts);
			return false;
		}

		// 检查异常类型，某些异常不应该重试
		if (!shouldRetryForException(context.getLastException())) {
			return false;
		}

		return true;
	}

	@Override
	public long getNextRetryInterval(RetryContext context) {
		int attempt = context.getAttemptCount();
		long baseInterval = getBaseInterval();
		long maxInterval = getMaxInterval();
		double multiplier = getMultiplier();

		switch (retryConfig.getBackoffStrategy()) {
		case FIXED:
			return baseInterval;

		case LINEAR:
			return Math.min(baseInterval * attempt, maxInterval);

		case EXPONENTIAL:
			long interval = (long) (baseInterval * Math.pow(multiplier, attempt - 1));
			return Math.min(interval, maxInterval);

		default:
			return baseInterval;
		}
	}

	@Override
	public void beforeRetry(RetryContext context) {
		logger.info("{} retry attempt {} after {} ms", operationType, context.getAttemptCount() + 1, getNextRetryInterval(context));
	}

	private boolean isRetryEnabled() {
		return operationType.isConnect() ? retryConfig.isEnableConnectRetry() : retryConfig.isEnablePublishRetry();
	}

	private int getMaxAttempts() {
		return operationType.isConnect() ? retryConfig.getMaxConnectAttempts() : retryConfig.getMaxPublishAttempts();
	}

	private long getBaseInterval() {
		return operationType.isConnect() ? retryConfig.getConnectRetryInterval() : retryConfig.getPublishRetryInterval();
	}

	private long getMaxInterval() {
		return operationType.isConnect() ? retryConfig.getMaxConnectRetryInterval() : retryConfig.getMaxPublishRetryInterval();
	}

	private double getMultiplier() {
		return operationType.isConnect() ? retryConfig.getConnectRetryMultiplier() : retryConfig.getPublishRetryMultiplier();
	}

	private boolean shouldRetryForException(Exception exception) {
		if (exception instanceof MqttException) {
			MqttException mqttException = (MqttException) exception;
			int reasonCode = mqttException.getReasonCode();

			// 以下错误码不应该重试
			switch (reasonCode) {
			case MqttException.REASON_CODE_CLIENT_EXCEPTION:
			case MqttException.REASON_CODE_INVALID_PROTOCOL_VERSION:
			case MqttException.REASON_CODE_INVALID_CLIENT_ID:
			case MqttException.REASON_CODE_FAILED_AUTHENTICATION:
			case MqttException.REASON_CODE_NOT_AUTHORIZED:
				return false;
			default:
				return true;
			}
		}
		return true;
	}
}
