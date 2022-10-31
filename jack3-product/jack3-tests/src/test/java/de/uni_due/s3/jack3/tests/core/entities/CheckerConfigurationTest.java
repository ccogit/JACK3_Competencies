package de.uni_due.s3.jack3.tests.core.entities;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.CheckerConfiguration;
import de.uni_due.s3.jack3.tests.utils.AbstractTest;
import de.uni_due.s3.jack3.utils.EntityReflectionHelper;

/**
 * Test class for {@linkplain CheckerConfiguration}.
 *
 * @author marc.kasper
 *
 */
class CheckerConfigurationTest extends AbstractTest {

	/*
	 * This test checks the deep copy of a checker configuration.
	 * The configuration includes two config values and all booleans are true.
	 */
	@Test
	void deepCopyOfCheckerConfiguration() {
		CheckerConfiguration originCheckerConfiguration;
		CheckerConfiguration deepCopyOfCheckerConfiguration = null;

		originCheckerConfiguration = new CheckerConfiguration();

		originCheckerConfiguration.setName("test for deep copy checker" + " configuration");

		try {
			// set fields per Reflection
			EntityReflectionHelper.setPerReflection(originCheckerConfiguration, "resultLabel",
					"result label for test " + "deep copy of checker " + "configuration");
			EntityReflectionHelper.setPerReflection(originCheckerConfiguration, "isActive", true);
			EntityReflectionHelper.setPerReflection(originCheckerConfiguration, "hasVisibleResult", true);
			EntityReflectionHelper.setPerReflection(originCheckerConfiguration, "hasVisibleFeedback", true);

			deepCopyOfCheckerConfiguration = originCheckerConfiguration.deepCopy();

			// define getter fields for access per reflection 
			Field resultLabelField = deepCopyOfCheckerConfiguration.getClass().getDeclaredField("resultLabel");
			Field isActiveField = deepCopyOfCheckerConfiguration.getClass().getDeclaredField("isActive");
			Field hasVisibleResultField = deepCopyOfCheckerConfiguration.getClass()
					.getDeclaredField("hasVisibleResult");
			Field hasVisibleFeedbackField = deepCopyOfCheckerConfiguration.getClass()
					.getDeclaredField("hasVisibleFeedback");
			resultLabelField.setAccessible(true);
			isActiveField.setAccessible(true);
			hasVisibleResultField.setAccessible(true);
			hasVisibleFeedbackField.setAccessible(true);

			assertNotEquals(originCheckerConfiguration, deepCopyOfCheckerConfiguration,
					"The deep copy is the origin checker configuration itself.");
			assertEquals("test for deep copy checker configuration", deepCopyOfCheckerConfiguration.getName(),
					"The instance name of checker configuration is different.");
			assertEquals("result label for test deep copy of checker configuration",
					resultLabelField.get(deepCopyOfCheckerConfiguration),
					"The ID of checker configuration is different.");
			assertTrue(isActiveField.getBoolean(deepCopyOfCheckerConfiguration),
					"The checker configuration is not active.");
			assertTrue(hasVisibleResultField.getBoolean(deepCopyOfCheckerConfiguration),
					"The checker configuration has not a visible result field.");
			assertTrue(hasVisibleFeedbackField.getBoolean(deepCopyOfCheckerConfiguration),
					"The checker configuration has not a visible feedback field.");
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			throw new AssertionError("Field could not be found!");
		}
	}

	/*
	 * This test checks the deep copy of a checker configuration.
	 * The configuration has no config values and all booleans are false.
	 */
	@Test
	void deepCopyOfEmptyCheckerConfiguration() {
		CheckerConfiguration originCheckerConfiguration;
		CheckerConfiguration deepCopyOfCheckerConfiguration = null;

		originCheckerConfiguration = new CheckerConfiguration();

		originCheckerConfiguration.setName("test for deep copy " + "of empty checker" + " configuration");

		try {
			// set fields per Reflection
			EntityReflectionHelper.setPerReflection(originCheckerConfiguration, "resultLabel",
					"result label for test " + "deep copy of checker " + "configuration");
			EntityReflectionHelper.setPerReflection(originCheckerConfiguration, "isActive", false);
			EntityReflectionHelper.setPerReflection(originCheckerConfiguration, "hasVisibleResult", false);
			EntityReflectionHelper.setPerReflection(originCheckerConfiguration, "hasVisibleFeedback", false);

			deepCopyOfCheckerConfiguration = originCheckerConfiguration.deepCopy();

			// define getter fields for access per reflection 
			Field resultLabelField = deepCopyOfCheckerConfiguration.getClass().getDeclaredField("resultLabel");
			Field isActiveField = deepCopyOfCheckerConfiguration.getClass().getDeclaredField("isActive");
			Field hasVisibleResultField = deepCopyOfCheckerConfiguration.getClass()
					.getDeclaredField("hasVisibleResult");
			Field hasVisibleFeedbackField = deepCopyOfCheckerConfiguration.getClass()
					.getDeclaredField("hasVisibleFeedback");
			resultLabelField.setAccessible(true);
			isActiveField.setAccessible(true);
			hasVisibleResultField.setAccessible(true);
			hasVisibleFeedbackField.setAccessible(true);

			assertNotEquals(originCheckerConfiguration, deepCopyOfCheckerConfiguration,
					"The deep copy is the origin checker configuration itself.");
			assertEquals("test for deep copy of empty checker configuration", deepCopyOfCheckerConfiguration.getName(),
					"The instance name of checker configuration is different.");
			assertEquals("result label for test deep copy of checker configuration",
					resultLabelField.get(deepCopyOfCheckerConfiguration),
					"The ID of checker configuration is different.");
			assertFalse(isActiveField.getBoolean(deepCopyOfCheckerConfiguration),
					"The checker configuration is not active.");
			assertFalse(hasVisibleResultField.getBoolean(deepCopyOfCheckerConfiguration),
					"The checker configuration has not a visible result field.");
			assertFalse(hasVisibleFeedbackField.getBoolean(deepCopyOfCheckerConfiguration),
					"The checker configuration has not a visible feedback field.");
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			throw new AssertionError("Field could not be found!");
		}
	}
}