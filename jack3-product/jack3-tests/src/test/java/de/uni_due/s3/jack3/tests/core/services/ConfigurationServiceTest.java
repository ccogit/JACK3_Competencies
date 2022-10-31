package de.uni_due.s3.jack3.tests.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.Config;
import de.uni_due.s3.jack3.services.ConfigurationService;
import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;

class ConfigurationServiceTest extends AbstractBasicTest {

	@Inject
	private ConfigurationService configService;

	@Test
	void testGetAllConfigs() {
		long size = baseService.countAll(Config.class);

		Config config = new Config();
		configService.saveConfig(config);

		assertEquals(size + 1, baseService.countAll(Config.class));
		baseService.findAll(Config.class).contains(config);

	}

	@Test
	void testSaveConfig() {
		Config config = new Config("key", "value");

		assertFalse(queryResultList("SELECT c FROM Config c", Config.class).contains(config));
		int size = queryResultList("SELECT c FROM Config c", Config.class).size();

		assertTrue(configService.saveConfig(config));

		assertTrue(queryResultList("SELECT c FROM Config c", Config.class).contains(config));
		assertEquals(size + 1, queryResultList("SELECT c FROM Config c", Config.class).size());

		Config configWithSameKey = new Config("key", "eulav");

		assertFalse(configService.saveConfig(configWithSameKey));
	}

	@Test
	void testConfigKeyValuePairWithKeyExists() {

		Config config = new Config("secret", "v");
		assertFalse(configService.configKeyValuePairWithKeyExists(config));

		configService.saveConfig(config);

		assertTrue(configService.configKeyValuePairWithKeyExists(config));
	}

	@Test
	void testGetConfigByKey() {
		Config config = new Config("k", "val");
		assertFalse(configService.getConfigByKey(config.getKey()).isPresent());

		configService.saveConfig(config);

		assertEquals(config, configService.getConfigByKey(config.getKey()).get());
	}

	@Test
	void testDeleteConfigKeyValuePair() {
		Config config = new Config("deleteMe", "toDelete");
		configService.saveConfig(config);

		assertTrue(configService.configKeyValuePairWithKeyExists(config));

		configService.deleteConfigKeyValuePair(config);

		assertFalse(configService.configKeyValuePairWithKeyExists(config));
	}

	@Test
	void testMerge() {

		Config config = new Config("kkey", "wrongValue");
		configService.saveConfig(config);

		config.setValue("correctValue");
		assertNotEquals(config.getValue(), configService.getConfigByKey(config.getKey()).get().getValue());

		baseService.merge(config);
		assertEquals(config.getValue(), configService.getConfigByKey(config.getKey()).get().getValue());

	}

}
