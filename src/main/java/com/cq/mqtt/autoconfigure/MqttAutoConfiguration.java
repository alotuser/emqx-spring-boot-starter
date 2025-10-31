package com.cq.mqtt.autoconfigure;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.cq.mqtt.config.MqttProperties;
import com.cq.mqtt.core.DefaultMqttClientFactory;
import com.cq.mqtt.core.MqttClientFactory;
import com.cq.mqtt.core.MqttTemplate;
import com.cq.mqtt.listener.MqttMessageListenerProcessor;

@Configuration
@EnableConfigurationProperties(MqttProperties.class)
@ConditionalOnClass(MqttClient.class)
public class MqttAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(MqttAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	public MqttConnectOptions mqttConnectOptions(MqttProperties properties) {
		MqttConnectOptions options = new MqttConnectOptions();
		options.setServerURIs(new String[] { properties.getServerUri() });
		options.setUserName(properties.getUsername());
		options.setPassword(properties.getPassword() != null ? properties.getPassword().toCharArray() : null);
		options.setConnectionTimeout(properties.getConnectionTimeout());
		options.setKeepAliveInterval(properties.getKeepAliveInterval());
		options.setAutomaticReconnect(properties.isAutomaticReconnect());
		options.setCleanSession(properties.isCleanSession());

		// 配置 SSL
		if (properties.getSsl().isEnabled()) {
			configureSsl(options, properties.getSsl());
		}

		return options;
	}

	private void configureSsl(MqttConnectOptions options, MqttProperties.Ssl sslConfig) {
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");

			KeyManager[] keyManagers = null;
			TrustManager[] trustManagers = null;

			// 配置 TrustStore
			if (StringUtils.hasText(sslConfig.getTrustStore())) {
				TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				KeyStore trustStore = KeyStore.getInstance(sslConfig.getTrustStoreType());
				try (InputStream is = new FileInputStream(sslConfig.getTrustStore())) {
					trustStore.load(is, sslConfig.getTrustStorePassword() != null ? sslConfig.getTrustStorePassword().toCharArray() : null);
				}
				trustManagerFactory.init(trustStore);
				trustManagers = trustManagerFactory.getTrustManagers();
			}

			// 配置 KeyStore
			if (StringUtils.hasText(sslConfig.getKeyStore())) {
				KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				KeyStore keyStore = KeyStore.getInstance(sslConfig.getKeyStoreType());
				try (InputStream is = new FileInputStream(sslConfig.getKeyStore())) {
					keyStore.load(is, sslConfig.getKeyStorePassword() != null ? sslConfig.getKeyStorePassword().toCharArray() : null);
				}
				keyManagerFactory.init(keyStore, sslConfig.getKeyStorePassword() != null ? sslConfig.getKeyStorePassword().toCharArray() : null);
				keyManagers = keyManagerFactory.getKeyManagers();
			}

			sslContext.init(keyManagers, trustManagers, new SecureRandom());
			options.setSocketFactory(sslContext.getSocketFactory());

		} catch (Exception e) {
			logger.error("Failed to configure SSL for MQTT", e);
			throw new RuntimeException("MQTT SSL configuration failed", e);
		}
	}

	@Bean
	@ConditionalOnMissingBean
	public MqttClientFactory mqttClientFactory(MqttProperties properties, MqttConnectOptions connectOptions) {
		return new DefaultMqttClientFactory(properties, connectOptions);
	}

	@Bean
	public MqttTemplate mqttTemplate(MqttClientFactory clientFactory, MqttProperties properties) {
		return new MqttTemplate(clientFactory, properties);
	}

	@Bean
	public MqttMessageListenerProcessor mqttMessageListenerProcessor(MqttClientFactory clientFactory) {
		return new MqttMessageListenerProcessor(clientFactory);
	}
}
