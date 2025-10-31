package com.cq.mqtt.retry;

public class RetryExhaustedException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RetryExhaustedException(String message) {
		super(message);
	}

	public RetryExhaustedException(String message, Throwable cause) {
		super(message, cause);
	}
}
