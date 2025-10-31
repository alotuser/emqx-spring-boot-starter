package com.cq.mqtt.retry;

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
