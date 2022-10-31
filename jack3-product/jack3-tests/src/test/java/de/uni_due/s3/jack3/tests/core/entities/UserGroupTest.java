package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;

class UserGroupTest extends AbstractBasicTest {

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();
		persistUserGroup();
		userGroup = userGroupService.getUserGroupByName("User Group")
									.orElseThrow(AssertionError::new);
	}

	@Test
	void testDescription() {
		assertEquals("Description", userGroup.getDescription());

		// change description
		userGroup.setDescription("New description");
		userGroup = userGroupService.mergeUserGroup(userGroup);

		assertEquals("New description", userGroup.getDescription());
	}

	@Test
	void testName() {
		assertEquals("User Group", userGroup.getName());

		// change description
		userGroup.setName("New name");
		userGroup = userGroupService.mergeUserGroup(userGroup);

		assertEquals("New name", userGroup.getName());
	}

}
