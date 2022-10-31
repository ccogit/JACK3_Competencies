package de.uni_due.s3.jack3.tests.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.IdentityProfileField;
import de.uni_due.s3.jack3.entities.tenant.ProfileField;
import de.uni_due.s3.jack3.services.ProfileFieldService;
import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;

class ProfileFieldServiceTest extends AbstractBasicTest {

	@Inject
	private ProfileFieldService profileFieldService;

	/**
	 * Save a new profile field and check if it could be found in the database.
	 */
	@Test
	void saveField() {
		ProfileField field = new IdentityProfileField("identityAttribute");
		profileFieldService.persistProfileField(field);

		assertTrue(profileFieldService.getIdentityField("identityAttribute").isPresent());
		assertFalse(profileFieldService.getIdentityField("notExistingAttributeName").isPresent());

		assertEquals(1, profileFieldService.getAllFields().size());
	}

	/**
	 * Save two new profile fields and check if both could be found in the database.
	 */
	@Test
	void saveFields() {
		ProfileField field1 = new IdentityProfileField("identityAttributeOne");
		profileFieldService.persistProfileField(field1);
		ProfileField field2 = new IdentityProfileField("identityAttributeTwo");
		profileFieldService.persistProfileField(field2);

		assertTrue(profileFieldService.getIdentityField("identityAttributeOne").isPresent());
		assertTrue(profileFieldService.getIdentityField("identityAttributeTwo").isPresent());
		assertFalse(profileFieldService.getIdentityField("notExistingAttributeName").isPresent());

		assertEquals(2, profileFieldService.getAllFields().size());
	}

	/**
	 * Get not-existing profile fields
	 */
	@Test
	void getNotExistingField() {
		assertFalse(profileFieldService.getIdentityField("notExistingAttributeName").isPresent());
		assertTrue(profileFieldService.getAllFields().isEmpty());
	}

	/**
	 * Create not-existing profile fields and check if a new was created
	 */
	@Test
	void createNotExistingField() {
		profileFieldService.getOrCreateIdentityField("identityAttribute");

		assertTrue(profileFieldService.getIdentityField("identityAttribute").isPresent());
		assertFalse(profileFieldService.getIdentityField("notExistingAttributeName").isPresent());

		assertEquals(1, profileFieldService.getAllFields().size());
	}

	/**
	 * Check that "getOrCreate" does not create a new profile field if there is already a profile field with this name
	 */
	@Test
	void getExistingField() {
		ProfileField field1 = new IdentityProfileField("identityAttribute");
		profileFieldService.persistProfileField(field1);

		field1 = profileFieldService.getIdentityField("identityAttribute").get();
		ProfileField field2 = profileFieldService.getOrCreateIdentityField("identityAttribute");

		assertEquals(1, profileFieldService.getAllFields().size());
		assertEquals(field1, field2);

		assertFalse(profileFieldService.getIdentityField("notExistingAttributeName").isPresent());
	}

}
