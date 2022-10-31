package de.uni_due.s3.jack3.tests.business;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.ConfigurationBusiness;
import de.uni_due.s3.jack3.entities.tenant.Config;
import de.uni_due.s3.jack3.tests.utils.AbstractBusinessTest;

class ConfigurationBusinessTest extends AbstractBusinessTest {

	@Inject
	private ConfigurationBusiness configurationBusiness;

	private Config config1;
	private Config config2;

	@BeforeEach
	void prepareTest() {
		config1 = new Config("testkey1", "[\"testvalue1\"]");
		config2 = new Config("testkey2", "[\"testvalue2\"]");
		configurationBusiness.saveConfig(config1);
		configurationBusiness.saveConfig(config2);
	}

	@Test
	void getAllConfigs() {
		List<Config> configList = configurationBusiness.getAllConfigs();
		assertTrue(configList.contains(config1) && configList.contains(config2));
	}

	@Test
	void deleteConfigValuePair() {
		configurationBusiness.deleteConfigKeyValuePair(config1);

		assertFalse(configurationBusiness.getAllConfigs().contains(config1));
		assertTrue(configurationBusiness.getAllConfigs().contains(config2));
	}

	@Test
	void getSingleConfig() {
		assertEquals("testkey1", configurationBusiness.getSingleConfig("testkey1").get().getKey());
		assertEquals("[\"testvalue1\"]", configurationBusiness.getSingleConfig("testkey1").get().getValue());
	}

	@Test
	void getSingleValue() {
		assertFalse(configurationBusiness.getSingleValue("non-existing-key").isPresent());
		assertEquals("testvalue1", configurationBusiness.getSingleValue("testkey1").get());
	}

	@Test
	void getValueList() {
		configurationBusiness.saveConfig(new Config("list-value", "[\"value1\", \"value2\"]"));

		assertTrue(configurationBusiness.getValueList("non-existing-key").isEmpty());
		assertEquals(2, configurationBusiness.getValueList("list-value").size());
	}

	@Test
	void booleanOf() {
		configurationBusiness.saveConfig(new Config("config_false", "[\"false\"]"));
		configurationBusiness.saveConfig(new Config("config_true", "[\"true\"]"));

		assertFalse(configurationBusiness.booleanOf("config_false"));
		assertTrue(configurationBusiness.booleanOf("config_true"));
		assertFalse(configurationBusiness.booleanOf("non-existing-key"));
	}

	@Test
	void serializeStringListToJson() {
		List<String> stringList = Arrays.asList("foo", "bar");

		assertEquals("[\"foo\",\"bar\"]", ConfigurationBusiness.serializeStringListToJson(stringList));
	}

}
