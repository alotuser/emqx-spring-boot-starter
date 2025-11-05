package com.cq.mqtt.listener;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import com.cq.mqtt.annotation.MqttTopic;
import com.cq.mqtt.core.MqttMessageContext;
import com.cq.mqtt.core.SubscriptionManager;

/**
 * MQTT 消息监听器处理器，负责扫描 Spring 容器中的 Bean，查找使用 @MqttTopic 注解的方法， 并为这些方法注册 MQTT 主题订阅和消息监听器
 * 
 * @author alotuser
 * @since 2025/5/10
 */
@Component
public class MqttMessageListenerProcessor implements ApplicationContextAware, SmartInitializingSingleton {

	private static final Logger logger = LoggerFactory.getLogger(MqttMessageListenerProcessor.class);


	private final SubscriptionManager subscriptionManager;
	private ApplicationContext applicationContext;
	
	public MqttMessageListenerProcessor(SubscriptionManager subscriptionManager) {
	      this.subscriptionManager = subscriptionManager;
	}
	  


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterSingletonsInstantiated() {
		// 方法1: 获取所有 Bean 名称，然后逐个检查
		scanAllBeansForMqttTopics();

		// 方法2: 使用 Spring 的注解扫描（备选方案）
		// scanWithAnnotationFilter();
	}

	/**
	 * 方法2: 使用 Spring 的注解扫描器（备选方案）
	 */
	@SuppressWarnings("unused")
	private void scanWithAnnotationFilter() {
		// 获取所有包含 @MqttTopic 注解的 Bean
		Map<String, Object> beansWithMqttTopic = applicationContext.getBeansWithAnnotation(MqttTopic.class);

		// 但是 @MqttTopic 是方法级别的注解，所以我们需要另一种方式
		// 使用 ClassPathScanningCandidateComponentProvider 扫描所有类
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

		scanner.addIncludeFilter(new AnnotationTypeFilter(MqttTopic.class, false) {
			@Override
			public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
				return super.match(metadataReader, metadataReaderFactory);
			}
		});
	}

	 /**
     * 扫描所有 Spring 管理的 Bean
     */
    private void scanAllBeansForMqttTopics() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        int topicCount = 0;
        
        for (String beanName : beanNames) {
            Object bean = null;
            try {
                bean = applicationContext.getBean(beanName);
            } catch (Exception e) {
                logger.debug("Could not get bean: {}", beanName, e);
                continue;
            }
            
            if (isSpringInternalBean(beanName)) {
                continue;
            }
            
            topicCount += processBeanForMqttTopics(bean, beanName);
        }
        
        logger.info("Scanned {} beans, found {} MQTT topics", beanNames.length, topicCount);
    }
    
    /**
     * 处理单个 Bean 的 MQTT 主题注解
     */
    private int processBeanForMqttTopics(Object bean, String beanName) {
        Class<?> beanClass = getBeanClass(bean);
        if (beanClass == null) {
            return 0;
        }
        
        Method[] methods = getAllDeclaredMethods(beanClass);
        int topicCount = 0;
        
        for (Method method : methods) {
            if (method.isAnnotationPresent(MqttTopic.class)) {
                MqttTopic annotation = method.getAnnotation(MqttTopic.class);
                registerMqttTopicSubscription(annotation, bean, method, beanName);
                topicCount++;
            }
        }
        
        return topicCount;
    }
    
    /**
     * 注册 MQTT 主题订阅
     */
    private void registerMqttTopicSubscription(MqttTopic annotation, Object bean, 
                                             Method method, String beanName) {
        String topic = annotation.value();
        int qos = annotation.qos();
        
        // 验证方法参数
        validateMethodParameters(method, topic);
        
        // 创建消息监听器
        IMqttMessageListener listener = createMessageListener(bean, method, topic);
        
        // 使用订阅管理器注册订阅
        subscriptionManager.registerSubscription(topic, qos, listener);
        
        logger.info("Registered MQTT listener for topic: {} with QoS: {}, method: {}.{}", topic, qos, bean.getClass().getSimpleName(), method.getName());
    }
    
    /**
     * 创建消息监听器
     */
    private IMqttMessageListener createMessageListener(Object bean, Method method, String topic) {
        return (topicName, mqttMessage) -> {
            try {
                invokeMethodWithMessage(bean, method, topicName, mqttMessage);
            } catch (Exception e) {
                logger.error("Error handling MQTT message for topic: {}", topic, e);
                handleMessageProcessingError(e, topic, mqttMessage);
            }
        };
    }
    
    /**
     * 调用目标方法处理消息
     */
    private void invokeMethodWithMessage(Object bean, Method method, 
                                       String topicName, MqttMessage mqttMessage) 
        throws Exception {
        
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] args = new Object[parameterTypes.length];
        
        for (int i = 0; i < parameterTypes.length; i++) {
            args[i] = getArgumentValue(parameterTypes[i], topicName, mqttMessage);
        }
        
        method.setAccessible(true);
        method.invoke(bean, args);
    }
    
    /**
     * 根据参数类型获取参数值
     */
    private Object getArgumentValue(Class<?> paramType, String topic, MqttMessage message) {
        if (paramType == String.class) {
            return topic;
        } else if (paramType == byte[].class) {
            return message.getPayload();
        } else if (paramType == MqttMessage.class) {
            return message;
        } else if (paramType == MqttMessageContext.class) {
            return new MqttMessageContext(topic, message);
        }
        return null;
    }
	
	/**
	 * 获取 Bean 的真实类（处理代理类的情况）
	 */
	private Class<?> getBeanClass(Object bean) {
		Class<?> beanClass = bean.getClass();

		// 处理 Spring AOP 代理
		if (org.springframework.aop.support.AopUtils.isAopProxy(bean)) {
			beanClass = org.springframework.aop.support.AopUtils.getTargetClass(bean);
		}

		// 处理 CGLIB 代理
		if (beanClass.getName().contains("$$")) {
			Class<?> superClass = beanClass.getSuperclass();
			if (superClass != null && !superClass.getName().equals("java.lang.Object")) {
				beanClass = superClass;
			}
		}

		return beanClass;
	}

	/**
	 * 获取类中所有声明的方法（包括继承的方法）
	 */
	private Method[] getAllDeclaredMethods(Class<?> clazz) {
		List<Method> methods = new ArrayList<>();
		Class<?> currentClass = clazz;

		while (currentClass != null && currentClass != Object.class) {
			methods.addAll(Arrays.asList(currentClass.getDeclaredMethods()));
			currentClass = currentClass.getSuperclass();
		}

		return methods.toArray(new Method[0]);
	}

	/**
	 * 判断是否是 Spring 内部 Bean
	 */
	private boolean isSpringInternalBean(String beanName) {
		return beanName.startsWith("org.springframework.") || beanName.startsWith("spring.") || beanName.contains(".") && (beanName.endsWith(".MqttMessageListenerProcessor") || beanName.endsWith(".MqttAutoConfiguration") || beanName.endsWith(".MqttTemplate") || beanName.endsWith(".MqttClientFactory"));
	}

//	/**
//	 * 订阅主题
//	 */
//	private void subscribeToTopic(MqttTopic annotation, Object bean, Method method) {
//		String topic = annotation.value();
//		int qos = annotation.qos();
//
//		// 验证方法参数
//		validateMethodParameters(method, topic);
//
//		IMqttMessageListener listener = createMessageListener(bean, method, topic);
//
//		// 使用模板订阅
//		MqttTemplate template = new MqttTemplate(subscriptionManager);
//		try {
//			template.subscribe(topic, qos, listener);
//			logger.info("Registered MQTT listener for topic: {} with QoS: {}, method: {}.{}", topic, qos, bean.getClass().getSimpleName(), method.getName());
//		} catch (Exception e) {
//			logger.error("Failed to subscribe to topic: {}", topic, e);
//		}
//	}

	/**
	 * 验证方法参数是否合法
	 */
	private void validateMethodParameters(Method method, String topic) {
		Class<?>[] parameterTypes = method.getParameterTypes();

		for (Class<?> paramType : parameterTypes) {
			if (!isSupportedParameterType(paramType)) {
				throw new IllegalArgumentException(String.format("Unsupported parameter type %s in method %s for topic %s. " + "Supported types: String, byte[], MqttMessage", paramType.getSimpleName(), method.getName(), topic));
			}
		}

		if (parameterTypes.length > 3) {
			throw new IllegalArgumentException(String.format("Too many parameters in method %s for topic %s. " + "Maximum 3 parameters supported: payload, topic, message", method.getName(), topic));
		}
	}

	/**
	 * 检查参数类型是否支持
	 */
	private boolean isSupportedParameterType(Class<?> paramType) {
		return paramType == String.class || paramType == byte[].class || paramType == MqttMessage.class || paramType == MqttMessageContext.class; // 新增上下文类型
	}

	/**
	 * 处理消息处理过程中的错误
	 */
	private void handleMessageProcessingError(Exception error, String topic, MqttMessage message) {
		// 这里可以添加错误处理逻辑，比如重试、记录日志、发送到死信队列等
		logger.warn("Message processing failed for topic: {}, message: {}", topic, new String(message.getPayload(), StandardCharsets.UTF_8));

		// 可以根据错误类型采取不同的处理策略
		if (error instanceof RuntimeException) {
			// 对于运行时异常，可能需要进行特殊处理
		}
	}
}