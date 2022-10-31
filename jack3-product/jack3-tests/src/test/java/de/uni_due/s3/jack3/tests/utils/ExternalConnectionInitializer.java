package de.uni_due.s3.jack3.tests.utils;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.jboss.logging.Logger;

import de.uni_due.s3.jack3.business.ConfigurationBusiness;
import de.uni_due.s3.jack3.entities.tenant.Config;

/**
 * Helper class for initializing Eureka with default URLs.
 */
final class ExternalConnectionInitializer {

	private ExternalConnectionInitializer() {
	}

	private static final String EUREKA_URL_KEY = "EurekaServerURLs";
	private static final String EUREKA_URL = "http://10.168.68.74:8761/eureka";

	private static final String KAFKA_URL_KEY = "KafkaBootstrapServer";
	private static final String KAFKA_URL = "10.168.68.145:9092";
	private static final String KAFKA_GROUP = "jack3-tests";
	private static final String KAFKA_GROUP_KEY = "KafkaGroupId";
	private static final String KAFKA_RESULTTOPIC_KEY = "KafkaResultTopic";
	private static final String KAFKA_DEFAULT_RETENTION_MS = String.valueOf(MINUTES.toMillis(10));

	private static final Logger LOGGER = Logger.getLogger(ExternalConnectionInitializer.class);

	private static ConfigurationBusiness configBusiness;

	private static boolean eurekaIsInitialized = false;
	private static boolean kafkaIsInitialized = false;

	/**
	 * Sets the Eureka configuration to default values and updates the cache.
	 */
	public static boolean initializeEureka() {
		resolveConfigBusiness();
		if (!eurekaIsInitialized) {
			configBusiness.saveConfig(new Config(EUREKA_URL_KEY, "[\"" + EUREKA_URL + "\"]"));
			eurekaIsInitialized = true;
			return true;
		}
		return false;
	}

	/**
	 * Sets the kafka configuration to default values and updates the cache. The group and result topic are randomly
	 * generated to prevent that two tests running in parallel interfere with each other.
	 */
	public static boolean initializeKafka() {
		resolveConfigBusiness();
		if (!kafkaIsInitialized) {
			final String kafkaKey = RandomStringUtils.randomAlphanumeric(16).toLowerCase();
			final String topicKey = "test-" + kafkaKey + "-results";
			LOGGER.infof("Using key %s for kafka configuration.", kafkaKey);

			configBusiness.saveConfig(new Config(KAFKA_URL_KEY, "[\"" + KAFKA_URL + "\"]"));
			configBusiness.saveConfig(new Config(KAFKA_GROUP_KEY, "[\"" + KAFKA_GROUP + "\"]"));
			configBusiness.saveConfig(new Config(KAFKA_RESULTTOPIC_KEY, "[\"" + topicKey + "\"]"));

			// We create the topic manually because we want to configure the topic retention time so that
			// after some time the log (=message) will be deleted because this is only test data. (default: 10 minutes)
			Properties adminConfig = new Properties();
			adminConfig.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, configBusiness.getValueList(KAFKA_URL_KEY));
			try (final AdminClient client = AdminClient.create(adminConfig)) {
				// Use default number of partitions and replication factor
				NewTopic topic = new NewTopic(topicKey, Optional.empty(), Optional.empty());
				Map<String, String> topicConfig = new HashMap<>(1);
				topicConfig.put(TopicConfig.RETENTION_MS_CONFIG, KAFKA_DEFAULT_RETENTION_MS);
				topic.configs(topicConfig);
				client.createTopics(Arrays.asList(topic));
				LOGGER.infof("Created topic %s for receiving results.", topicKey);
			} catch (Exception e) {
				LOGGER.errorf(e, "Setting retention for topic %s was not possible.", topicKey);
			}

			kafkaIsInitialized = true;
			return true;
		}
		return false;
	}

	private static void resolveConfigBusiness() {
		if (configBusiness != null) {
			return;
		}

		final Instance<ConfigurationBusiness> configurationBusiness = CDI.current().select(ConfigurationBusiness.class);
		if (configurationBusiness.isUnsatisfied()) {
			throw new Error("Could not resolve configuration business!");
		}
		configBusiness = configurationBusiness.get();
	}

}
