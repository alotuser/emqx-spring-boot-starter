package com.cq.mqtt.retry;

/**
 * 重试回调接口，定义了需要重试的操作
 * 
 * @author alotuser
 * @since 2025/5/10
 */
@FunctionalInterface
public interface RetryCallback<T> {

	/**
	 * 执行需要重试的操作
	 */
	T doWithRetry() throws Exception;

	/**
	 * 获取上下文数据
	 */
	default Object getContextData() {
		return null;
	}
}
