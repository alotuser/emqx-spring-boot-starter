package com.cq.mqtt.listener;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.cq.mqtt.annotation.MqttTopic;
import com.cq.mqtt.core.MqttClientFactory;
import com.cq.mqtt.core.MqttTemplate;

@Component
public class MqttMessageListenerProcessor implements ApplicationContextAware, SmartInitializingSingleton {
	

	private static final Logger logger = LoggerFactory.getLogger(MqttMessageListenerProcessor.class);

	private final MqttClientFactory clientFactory;
	private ApplicationContext applicationContext;

	public MqttMessageListenerProcessor(MqttClientFactory clientFactory) {
		this.clientFactory = clientFactory;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterSingletonsInstantiated() {
		Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Component.class);

		for (Object bean : beans.values()) {
			Method[] methods = bean.getClass().getMethods();
			for (Method method : methods) {
				if (method.isAnnotationPresent(MqttTopic.class)) {
					MqttTopic annotation = method.getAnnotation(MqttTopic.class);
					subscribeToTopic(annotation, bean, method);
				}
			}
		}
	}

	private void subscribeToTopic(MqttTopic annotation, Object bean, Method method) {
		String topic = annotation.value();
		int qos = annotation.qos();

		IMqttMessageListener listener = (topicName, mqttMessage) -> {
			try {
				// 根据方法参数类型调用相应的方法
				Class<?>[] parameterTypes = method.getParameterTypes();
				Object[] args = new Object[parameterTypes.length];

				for (int i = 0; i < parameterTypes.length; i++) {
					if (parameterTypes[i] == String.class) {
						args[i] = new String(mqttMessage.getPayload(), StandardCharsets.UTF_8);
					} else if (parameterTypes[i] == byte[].class) {
						args[i] = mqttMessage.getPayload();
					} else if (parameterTypes[i] == MqttMessage.class) {
						args[i] = mqttMessage;
					} else {
						args[i] = null;
					}
				}

				method.invoke(bean, args);

			} catch (Exception e) {
				logger.error("Error handling MQTT message for topic: {}", topic, e);
			}
		};
		
		// 使用模板订阅
		MqttTemplate template = new MqttTemplate(clientFactory,clientFactory.getProperties());
		template.subscribe(topic, qos, listener);

		logger.info("Registered MQTT listener for topic: {} with QoS: {}", topic, qos);
	}
}
