package cn.alotus.mqtt.core;

import java.nio.charset.StandardCharsets;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * MQTT 消息上下文，封装了接收到的 MQTT 消息及其相关信息
 * 
 * @author alotuser
 * @since 2025/5/10
 */
public class MqttMessageContext {

	private final String topic;
	private final MqttMessage message;
	private final long timestamp;

	public MqttMessageContext(String topic, MqttMessage message) {
		this.topic = topic;
		this.message = message;
		this.timestamp = System.currentTimeMillis();
	}

	// getters
	public String getTopic() {
		return topic;
	}

	public MqttMessage getMessage() {
		return message;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public byte[] getPayload() {
		return message.getPayload();
	}

	public String getPayloadAsString() {
		return new String(message.getPayload(), StandardCharsets.UTF_8);
	}

	public int getQos() {
		return message.getQos();
	}

	public boolean isRetained() {
		return message.isRetained();
	}

	public boolean isDuplicate() {
		return message.isDuplicate();
	}
}
