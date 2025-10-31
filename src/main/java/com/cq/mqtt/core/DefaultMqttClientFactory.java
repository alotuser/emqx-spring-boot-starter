package com.cq.mqtt.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cq.mqtt.config.MqttProperties;
import com.cq.mqtt.retry.RetryCallback;
import com.cq.mqtt.retry.RetryContext;
import com.cq.mqtt.retry.RetryExhaustedException;
import com.cq.mqtt.retry.RetryPolicy;
import com.cq.mqtt.retry.RetryTemplate;

//DefaultMqttClientFactory.java
public class DefaultMqttClientFactory implements MqttClientFactory {

	private static final Logger logger = LoggerFactory.getLogger(DefaultMqttClientFactory.class);

	private final MqttProperties properties;
    private final MqttConnectOptions connectOptions;
    private final RetryPolicy connectRetryPolicy;
    private MqttClient mqttClient;
    private volatile boolean connected = false;
    private ScheduledExecutorService reconnectExecutor;
    
    public DefaultMqttClientFactory(MqttProperties properties, MqttConnectOptions connectOptions) {
        this.properties = properties;
        this.connectOptions = connectOptions;
        this.connectRetryPolicy = new MqttRetryPolicy(properties.getRetry(), "connect");
        this.reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mqtt-reconnect-thread");
            t.setDaemon(true);
            return t;
        });
        
        initializeClient();
    }
    
    private void initializeClient() {
        try {
            String clientId = properties.getClientId();
            if (clientId == null || clientId.trim().isEmpty()) {
                clientId = "mqtt-client-" + System.currentTimeMillis();
            }
            
            mqttClient = new MqttClient(properties.getServerUri(), clientId);
            
            mqttClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    connected = true;
                    logger.info("MQTT connection {} established to {}", 
                        reconnect ? "reconnected" : "connected", serverURI);
                }
                
                @Override
                public void connectionLost(Throwable cause) {
                    connected = false;
                    logger.warn("MQTT connection lost", cause);
                    scheduleReconnect();
                }
                
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    // 消息分发由专门的处理器处理
                }
                
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // 消息发送完成
                }
            });
            
            connectWithRetry();
            
        } catch (MqttException e) {
            logger.error("Failed to initialize MQTT client", e);
            scheduleReconnect();
        }
    }
    
    private void connectWithRetry() {
        try {
            RetryTemplate.execute(connectRetryPolicy, new RetryCallback<Boolean>() {
                @Override
                public Boolean doWithRetry() throws Exception {
                    logger.info("Attempting to connect to MQTT broker...");
                    mqttClient.connect(connectOptions);
                    return true;
                }
                
                @Override
                public Object getContextData() {
                    return properties.getServerUri();
                }
            });
            
        } catch (RetryExhaustedException e) {
            logger.error("Failed to connect to MQTT broker after all retry attempts", e);
            scheduleReconnect();
        }
    }
    
    private void scheduleReconnect() {
        if (!properties.getRetry().isEnableConnectRetry()) {
            return;
        }
        
        // 使用退避策略计算重连间隔
        RetryContext context = new RetryContext(1, System.currentTimeMillis(), 
            null, properties.getServerUri());
        long delay = connectRetryPolicy.getNextRetryInterval(context);
        
        logger.info("Scheduling reconnect in {} ms", delay);
        reconnectExecutor.schedule(this::connectWithRetry, delay, TimeUnit.MILLISECONDS);
    }

	private void connect() {
		try {
			mqttClient.connect(connectOptions);
			connected = true;
		} catch (MqttException e) {
			logger.error("Failed to connect to MQTT broker", e);
			// 连接失败会由自动重连机制处理
		}
	}

	@Override
	public MqttClient getClient() {
		return mqttClient;
	}

	@Override
	public boolean isConnected() {
		return connected && mqttClient != null && mqttClient.isConnected();
	}

	 @PreDestroy
	    public void destroy() {
	        if (reconnectExecutor != null) {
	            reconnectExecutor.shutdown();
	            try {
	                if (!reconnectExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
	                    reconnectExecutor.shutdownNow();
	                }
	            } catch (InterruptedException e) {
	                reconnectExecutor.shutdownNow();
	                Thread.currentThread().interrupt();
	            }
	        }
	        
	        if (mqttClient != null && mqttClient.isConnected()) {
	            try {
	                mqttClient.disconnect();
	                mqttClient.close();
	            } catch (MqttException e) {
	                logger.error("Error disconnecting MQTT client", e);
	            }
	        }
	    }

	 @Override
	 public MqttProperties getProperties() {
		return properties;
	 }
}
