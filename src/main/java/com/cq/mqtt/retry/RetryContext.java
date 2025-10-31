package com.cq.mqtt.retry;

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
