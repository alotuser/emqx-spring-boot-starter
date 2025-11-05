package com.cq.mqtt.retry;

/**
 * 操作类型枚举，定义了支持的操作类型
 * 
 * @author alotuser
 * @since 2025/5/10
 */
public enum RetryPolicyType {

	CONNECT("connect"), PUBLISH("publish");

	private final String name;

	RetryPolicyType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public boolean equalsName(String otherName) {
		return name.equalsIgnoreCase(otherName);
	}

	public boolean isConnect() {
		return this == CONNECT;
	}

	public boolean isPublish() {
		return this == PUBLISH;
	}
	
	/**
	 * 根据名称获取枚举值
	 */
	public static RetryPolicyType fromName(String name) {
		for (RetryPolicyType type : values()) {
			if (type.name.equalsIgnoreCase(name)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown operation type: " + name);
	}

	/**
	 * 检查名称是否有效
	 */
	public static boolean isValid(String name) {
		for (RetryPolicyType type : values()) {
			if (type.name.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return name;
	}
}
