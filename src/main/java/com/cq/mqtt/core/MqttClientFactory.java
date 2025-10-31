package com.cq.mqtt.core;

import org.eclipse.paho.client.mqttv3.MqttClient;

import com.cq.mqtt.config.MqttProperties;

public interface MqttClientFactory {
	MqttClient getClient();

	boolean isConnected();
	
	MqttProperties getProperties(); 
}
