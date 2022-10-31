package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.Config;
import de.uni_due.s3.jack3.services.ConfigurationService;
import de.uni_due.s3.jack3.services.DevelopmentService;
import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;

class ConfigTest extends AbstractBasicTest {

	@Inject
	private ConfigurationService configurationService;

	@Override
	@BeforeEach
	protected void doBeforeTest() {
		// No First Time Setup (otherwise the start configuration would be loaded)
		// We must delete database before test because FirstTimeSetup is automatically called before starting the test.
		devService.deleteTenantDatabase(DevelopmentService.EDatabaseType.H2);
	}

	/**
	 * Testing the non-default constructor
	 */
	@Test
	void testConstructor() {
		Config configKeyValuePair = new Config("TestKey", "myValue");
		assertTrue(configurationService.saveConfig(configKeyValuePair));

		assertEquals(1, baseService.countAll(Config.class));
		assertEquals("myValue", configurationService.getConfigByKey("TestKey").get().getValue());

	}

	/**
	 * Testing the Key attribute
	 */
	@Test
	void testKey() {
		// Creating ConfigKeyValuePair
		Config configKeyValuePair = new Config();
		configKeyValuePair.setKey("MasterKey");
		configurationService.saveConfig(configKeyValuePair);
		assertEquals(1, baseService.countAll(Config.class));
		assertEquals("MasterKey", configurationService.getConfigByKey("MasterKey").get().getKey());

		// Creating 2 more ConfigKeyValuesPairs having the same Key
		Config configKeyValuePair2 = new Config();
		configKeyValuePair2.setKey("AlsoMasterKey");
		assertTrue(configurationService.saveConfig(configKeyValuePair2));
		Config configKeyValuePair3 = new Config();
		configKeyValuePair3.setKey("AlsoMasterKey");
		assertFalse(configurationService.saveConfig(configKeyValuePair3));

		assertEquals(2, baseService.countAll(Config.class));

	}

	/**
	 * Testing the Value attribute
	 */
	@Test
	void testValue() {
		Config configKeyValuePair = new Config();
		configKeyValuePair.setValue("coolValue");
		configKeyValuePair.setKey("coolKey");
		configurationService.saveConfig(configKeyValuePair);

		assertEquals(1, baseService.countAll(Config.class));
		assertTrue(configurationService.getConfigByKey("coolKey").isPresent());
		assertEquals("coolValue", configurationService.getConfigByKey("coolKey").get().getValue());

	}

}
