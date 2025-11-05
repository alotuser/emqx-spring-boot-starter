package com.cq.mqtt.core;

import org.eclipse.paho.client.mqttv3.MqttClient;

import com.cq.mqtt.config.MqttProperties;

/**
 * MQTT 客户端工厂接口，定义了获取 MQTT 客户端实例和连接状态的方法
 * 
 * @author alotuser
 * @since 2025/5/10
 */
public interface MqttClientFactory {
	MqttClient getClient();

	boolean isConnected();
	
	MqttProperties getProperties(); 
}
