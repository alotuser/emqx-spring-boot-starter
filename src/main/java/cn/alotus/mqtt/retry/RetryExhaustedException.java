package cn.alotus.mqtt.retry;

/**
 * 重试耗尽异常，当所有重试尝试均失败时抛出此异常
 * 
 * @author alotuser
 * @since 2025/5/10
 */
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
