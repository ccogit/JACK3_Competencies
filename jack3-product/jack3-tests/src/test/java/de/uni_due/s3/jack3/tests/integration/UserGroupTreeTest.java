package de.uni_due.s3.jack3.tests.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;
import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

/**
 * Extra class for testing a group "tree" with direct and indirect member users/groups.
 */
class UserGroupTreeTest extends AbstractBasicTest {

	private int countFields = 3;
	private UserGroup root = new UserGroup("Root Group", "Description");
	private UserGroup[] groups = new UserGroup[countFields];
	private User[] users = new User[countFields];

	/*-
	 * The following hierarchy is used:
	 *
	 *              r o o t
	 *             /   |   \
	 *            G0   G1   U0
	 *          /   \
	 *         U1   G2
	 *                \
	 *                U2
	 */

	/**
	 * Create the hierarchy
	 */
	@BeforeEach
	@Override
	protected void doBeforeTest() {

		// create and persist entities
		userGroupService.persistUserGroup(root);

		for (int i = 0; i < countFields; i++) {

			groups[i] = new UserGroup("G" + i, "Description " + i);
			userGroupService.persistUserGroup(groups[i]);

			users[i] = new User("User" + i, "Pseudonym" + i, TestDataFactory.getEmptyPassword(), "", false, false);
			userService.persistUser(users[i]);
		}

		// insert users/groups in hierarchy
		root.addMemberGroup(groups[0]);
		root.addMemberGroup(groups[1]);
		root.addMemberUser(users[0]);

		groups[0].addMemberUser(users[1]);
		groups[0].addMemberGroup(groups[2]);

		groups[2].addMemberUser(users[2]);

		// merge groups
		groups[2] = userGroupService.mergeUserGroup(groups[2]);
		groups[0] = userGroupService.mergeUserGroup(groups[0]);
		groups[1] = userGroupService.mergeUserGroup(groups[1]);

		root = userGroupService.mergeUserGroup(root);

		// join-fetch all user groups so getters for memberGroups and memberUsers work
		// the list is sorted by name so items could be directly add to array
		List<UserGroup> allGroups = userGroupService.getAllUserGroups();
		for (int i = 0; i < countFields; i++) {
			groups[i] = allGroups.get(i);
		}
		// root is the last element in allGroups
		root = allGroups.get(3);
	}

	/**
	 * Creates a Set from an array
	 */
	private Set<Object> getSetFromArray(Object... array) {
		return new HashSet<>(Arrays.asList(array));
	}

	/**
	 * Test direct member groups
	 */
	@Test
	void containsGroupDirectly() {

		// root <-> G0
		assertTrue(root.containsGroupAsDirectMember(groups[0]));
		assertFalse(groups[0].containsGroupAsDirectMember(root));

		// root <-> G1
		assertTrue(root.containsGroupAsDirectMember(groups[1]));
		assertFalse(groups[1].containsGroupAsDirectMember(root));

		// root <-> G2
		assertFalse(root.containsGroupAsDirectMember(groups[2]));
		assertFalse(groups[2].containsGroupAsDirectMember(root));

		// G0 <-> G2
		assertTrue(groups[0].containsGroupAsDirectMember(groups[2]));
		assertFalse(groups[2].containsGroupAsDirectMember(groups[0]));

		// G1 <-> G2
		assertFalse(groups[1].containsGroupAsDirectMember(groups[2]));
		assertFalse(groups[2].containsGroupAsDirectMember(groups[1]));
	}

	/**
	 * Test inherited member groups
	 */
	@Test
	void containsGroupIndirectly() {

		// root <-> G0
		assertTrue(root.containsGroupAsAnyMember(groups[0]));
		assertFalse(groups[0].containsGroupAsAnyMember(root));

		// root <-> G1
		assertTrue(root.containsGroupAsAnyMember(groups[1]));
		assertFalse(groups[1].containsGroupAsAnyMember(root));

		// root <-> G2
		assertTrue(root.containsGroupAsAnyMember(groups[2]));
		assertFalse(groups[2].containsGroupAsAnyMember(root));

		// G0 <-> G2
		assertTrue(groups[0].containsGroupAsAnyMember(groups[2]));
		assertFalse(groups[2].containsGroupAsAnyMember(groups[0]));

		// G1 <-> G2
		assertFalse(groups[1].containsGroupAsAnyMember(groups[2]));
		assertFalse(groups[2].containsGroupAsAnyMember(groups[1]));
	}

	/**
	 * Test direct member users
	 */
	@Test
	void containsUserDirectly() {

		// root -> U0
		assertTrue(root.containsUserAsDirectMember(users[0]));

		// root -> U1
		assertFalse(root.containsUserAsDirectMember(users[1]));

		// root -> U2
		assertFalse(root.containsUserAsDirectMember(users[2]));

		// G0 -> U1
		assertTrue(groups[0].containsUserAsDirectMember(users[1]));

		// G0 -> U2
		assertFalse(groups[0].containsUserAsDirectMember(users[2]));

		// G2 -> U0
		assertFalse(groups[2].containsUserAsDirectMember(users[0]));

		// G2 -> U2
		assertTrue(groups[2].containsUserAsDirectMember(users[2]));

		// G1 -> U2
		assertFalse(groups[1].containsUserAsDirectMember(users[2]));
	}

	/**
	 * Test inherited member users
	 */
	@Test
	void containsUserIndirectly() {

		// root -> U0
		assertTrue(root.containsUserAsAnyMember(users[0]));

		// root -> U1
		assertTrue(root.containsUserAsAnyMember(users[1]));

		// root -> U2
		assertTrue(root.containsUserAsAnyMember(users[2]));

		// G0 -> U1
		assertTrue(groups[0].containsUserAsAnyMember(users[1]));

		// G0 -> U2
		assertTrue(groups[0].containsUserAsAnyMember(users[2]));

		// G2 -> U0
		assertFalse(groups[2].containsUserAsAnyMember(users[0]));

		// G2 -> U2
		assertTrue(groups[2].containsUserAsAnyMember(users[2]));

		// G1 -> U2
		assertFalse(groups[1].containsUserAsAnyMember(users[2]));
	}

	@Test
	void getMemberGroups() {
		assertEquals(getSetFromArray(groups[0], groups[1]), root.getMemberGroups());
		assertEquals(getSetFromArray(groups[2]), groups[0].getMemberGroups());
		assertTrue(groups[1].getMemberGroups().isEmpty());
		assertTrue(groups[2].getMemberGroups().isEmpty());

	}

	@Test
	void getMemberUsers() {
		assertEquals(getSetFromArray(users[0]), root.getMemberUsers());
		assertEquals(getSetFromArray(users[1]), groups[0].getMemberUsers());
		assertTrue(groups[1].getMemberUsers().isEmpty());
		assertEquals(getSetFromArray(users[2]), groups[2].getMemberUsers());
	}
}
