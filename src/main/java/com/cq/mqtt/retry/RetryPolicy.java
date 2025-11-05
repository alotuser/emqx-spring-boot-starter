package com.cq.mqtt.retry;

/**
 * 重试策略接口，定义了重试的规则和行为
 * 
 * @author alotuser
 * @since 2025/5/10
 */
public interface RetryPolicy {
    
    /**
     * 是否可以重试
     */
    boolean canRetry(RetryContext context);
    
    /**
     * 获取下次重试的延迟时间
     */
    long getNextRetryInterval(RetryContext context);
    
    /**
     * 重试前的操作
     */
    default void beforeRetry(RetryContext context) {
        // 默认实现
    }
}
