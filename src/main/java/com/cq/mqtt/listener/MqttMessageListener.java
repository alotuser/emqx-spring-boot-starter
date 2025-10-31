package com.cq.mqtt.listener;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public interface MqttMessageListener {
	void handleMessage(String topic, MqttMessage message);
}
