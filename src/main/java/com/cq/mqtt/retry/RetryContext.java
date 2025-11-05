package com.cq.mqtt.retry;

/**
 * 重试上下文，封装了重试操作的相关信息
 * 
 * @author alotuser
 * @since 2025/5/10
 */
public class RetryContext {
	private final int attemptCount;
	private final long firstAttemptTime;
	private final Exception lastException;
	private final Object contextData;

	public RetryContext(int attemptCount, long firstAttemptTime, Exception lastException, Object contextData) {
		this.attemptCount = attemptCount;
		this.firstAttemptTime = firstAttemptTime;
		this.lastException = lastException;
		this.contextData = contextData;
	}

	// getters
	public int getAttemptCount() {
		return attemptCount;
	}

	public long getFirstAttemptTime() {
		return firstAttemptTime;
	}

	public Exception getLastException() {
		return lastException;
	}

	public Object getContextData() {
		return contextData;
	}
}
