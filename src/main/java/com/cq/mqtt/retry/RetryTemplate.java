package com.cq.mqtt.retry;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 重试模板类，提供执行带重试操作的功能
 * 
 * @author alotuser
 * @since 2025/5/10
 */
public class RetryTemplate {

	private static final Logger logger = LoggerFactory.getLogger(RetryTemplate.class);

	/**
	 * 执行带重试的操作
	 */
	public static <T> T execute(RetryPolicy retryPolicy, RetryCallback<T> retryCallback) {
		int attemptCount = 0;
		long firstAttemptTime = System.currentTimeMillis();
		Exception lastException = null;

		while (true) {
			attemptCount++;
			try {
				return retryCallback.doWithRetry();

			} catch (Exception e) {
				lastException = e;

				RetryContext context = new RetryContext(attemptCount, firstAttemptTime, e, retryCallback.getContextData());

				if (!retryPolicy.canRetry(context)) {
					logger.error("Operation failed after {} attempts", attemptCount, e);
					throw new RetryExhaustedException("Retry exhausted after " + attemptCount + " attempts", e);
				}

				long waitTime = retryPolicy.getNextRetryInterval(context);
				retryPolicy.beforeRetry(context);

				try {
					Thread.sleep(waitTime);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new RetryExhaustedException("Retry interrupted", ie);
				}
			}
		}
	}

	/**
	 * 异步执行带重试的操作
	 */
	public static <T> CompletableFuture<T> executeAsync(RetryPolicy retryPolicy, RetryCallback<T> retryCallback) {
		return CompletableFuture.supplyAsync(() -> execute(retryPolicy, retryCallback));
	}
}
