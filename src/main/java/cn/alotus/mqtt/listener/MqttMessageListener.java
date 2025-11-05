package cn.alotus.mqtt.listener;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * MQTT 消息监听器接口，定义了处理接收到的 MQTT 消息的方法
 * 
 * @author alotuser
 * @since 2025/5/10
 */
public interface MqttMessageListener {
	void handleMessage(String topic, MqttMessage message);
}
