package de.uni_due.s3.jack3.tests.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.ejb.EJBException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;
import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

class UserGroupServiceTest extends AbstractBasicTest {

	private User lecturer1;
	private User lecturer2;
	private User student1;
	private User student2;

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();
		persistUser();

		lecturer1 = getLecturer("LecturerOne");
		lecturer2 = getLecturer("LecturerTwo");
		student1 = getStudent("StudentOne");
		student2 = getStudent("StudentTwo");
	}

	/**
	 * Tests empty database
	 */
	@Test
	void getEmptyGroupList() {

		assertTrue(userGroupService.getAllUserGroups().isEmpty());
	}

	/**
	 * Get empty user group
	 */
	@Test
	void getEmptyUserGroups() {

		userGroupService.persistUserGroup(TestDataFactory.getUserGroup("Lecturers"));
		userGroupService.persistUserGroup(TestDataFactory.getUserGroup("Students"));

		// there should be 2 user groups (lecturers, students)
		assertEquals(2, userGroupService.getAllUserGroups().size());
		assertTrue(userGroupService.getUserGroupByName("Lecturers").isPresent());
		assertTrue(userGroupService.getUserGroupByName("Students").isPresent());

		// with no people
		assertTrue(userGroupService.getUserGroupsForUser(lecturer1).isEmpty());
		assertTrue(userGroupService.getUserGroupsForUser(lecturer2).isEmpty());
		assertTrue(userGroupService.getUserGroupsForUser(student1).isEmpty());
		assertTrue(userGroupService.getUserGroupsForUser(student2).isEmpty());
	}

	/**
	 * Get user groups with people
	 */
	@Test
	void getUserGroupsWithUsers() {

		UserGroup lecturers = TestDataFactory.getUserGroup("Lecturers", lecturer1, lecturer2);
		UserGroup students = TestDataFactory.getUserGroup("Students", student1, student2);

		userGroupService.persistUserGroup(lecturers);
		userGroupService.persistUserGroup(students);

		// there should be 2 user groups (lecturers, students)
		assertEquals(2, userGroupService.getAllUserGroups().size());
		assertTrue(userGroupService.getUserGroupByName("Lecturers").isPresent());
		assertTrue(userGroupService.getUserGroupByName("Students").isPresent());

		// with each 2 users
		assertTrue(userGroupService.getUserGroupsForUser(user).isEmpty());
		assertEquals(1, userGroupService.getUserGroupsForUser(lecturer1).size());
		assertEquals(1, userGroupService.getUserGroupsForUser(lecturer2).size());
		assertEquals(1, userGroupService.getUserGroupsForUser(student1).size());
		assertEquals(1, userGroupService.getUserGroupsForUser(student2).size());
	}

	/**
	 * Create duplicate groups
	 */
	@Test
	void createDuplicateGroups() {

		userGroupService.persistUserGroup(TestDataFactory.getUserGroup("Lecturers", lecturer1, lecturer2));
		userGroupService.persistUserGroup(TestDataFactory.getUserGroup("Lecturers", lecturer1, lecturer2));
		assertEquals(Long.valueOf(2), (Long) hqlService.getSingleResult("select count(ug) from UserGroup ug").get(0));
	}

	/**
	 * Get not available group
	 */
	@Test
	void getNotAvailableGroup() {

		assertFalse(userGroupService.getUserGroupByName("Lecturers").isPresent());
		assertFalse(userGroupService.getUserGroupByName("Students").isPresent());
		assertTrue(userGroupService.getUserGroupsForUser(user).isEmpty());

		// get group with members throws exception
		UserGroup notAvailableGroup = TestDataFactory.getUserGroup("Not available User Group");

		EJBException thrown = assertThrows(EJBException.class, () -> {
			userGroupService.getUserGroupWithMemberUsers(notAvailableGroup);
		});
		assertTrue(thrown.getCause() instanceof IllegalStateException);
	}

	/**
	 * Remove group from UserGroupService
	 */
	// TODO Does not test the "UserGroupService" as there is no method for deleting.
	@Test
	void removeGroupWithUsers() {
		userGroup.addMemberUser(user);
		userGroupService.persistUserGroup(userGroup);

		// group should be deleted
		baseService.deleteEntity(userGroup);
		assertTrue(userGroupService.getAllUserGroups().isEmpty());
	}

	/**
	 * Remove group from UserService
	 */
	@Test
	void removeUserGroup() {

		userGroup.addMemberUser(user);
		userGroupService.persistUserGroup(userGroup);

		// group should be deleted
		userGroupService.removeUserGroup(userGroup);
		assertTrue(userGroupService.getAllUserGroups().isEmpty());
	}

	/**
	 * Tests adding user groups to another
	 */
	@Test
	void addGroupToGroup() {

		UserGroup lecturers = TestDataFactory.getUserGroup("Lecturers", lecturer1, lecturer2);
		UserGroup students = TestDataFactory.getUserGroup("Students", student1, student2);

		userGroupService.persistUserGroup(lecturers);
		userGroupService.persistUserGroup(students);

		// employee-group created from lecture-group and admin added
		UserGroup employees = TestDataFactory.getUserGroup("Employees");
		employees.addMemberGroup(lecturers);
		employees.addMemberUser(user);

		userGroupService.persistUserGroup(employees);

		// there should be 3 user groups (lecturers, students, employees)
		assertEquals(3, userGroupService.getAllUserGroups().size());
		assertTrue(userGroupService.getUserGroupByName("Lecturers").isPresent());
		assertTrue(userGroupService.getUserGroupByName("Students").isPresent());
		assertTrue(userGroupService.getUserGroupByName("Employees").isPresent());

		// students should be only in 1 list (students), lecturers in 2 (lecturers and employees) and admin in 1 list
		// (employee)
		assertEquals(1, userGroupService.getUserGroupsForUser(user).size());
		assertTrue(userGroupService.getUserGroupsForUser(user).contains(employees));

		assertEquals(2, userGroupService.getUserGroupsForUser(lecturer1).size());
		assertTrue(userGroupService.getUserGroupsForUser(lecturer1).contains(lecturers));
		assertTrue(userGroupService.getUserGroupsForUser(lecturer1).contains(employees));

		assertEquals(2, userGroupService.getUserGroupsForUser(lecturer2).size());
		assertTrue(userGroupService.getUserGroupsForUser(lecturer2).contains(lecturers));
		assertTrue(userGroupService.getUserGroupsForUser(lecturer2).contains(employees));

		assertEquals(1, userGroupService.getUserGroupsForUser(student1).size());
		assertTrue(userGroupService.getUserGroupsForUser(student1).contains(students));
		assertEquals(1, userGroupService.getUserGroupsForUser(student2).size());
		assertTrue(userGroupService.getUserGroupsForUser(student2).contains(students));
	}

	/**
	 * Tests multiple groups for one user
	 */
	@Test
	void addMultipleGroupsForOneUser() {

		UserGroup lecturers = TestDataFactory.getUserGroup("Lecturers", lecturer1, lecturer2);
		UserGroup students = TestDataFactory.getUserGroup("Students", student1, student2);

		userGroupService.persistUserGroup(lecturers);
		userGroupService.persistUserGroup(students);

		// student 2 should be in 1 group
		assertEquals(1, userGroupService.getUserGroupsForUser(student2).size());

		// adding student2 to lecturers group
		lecturers.addMemberUser(student2);
		lecturers = userGroupService.mergeUserGroup(lecturers);

		// there should be 2 user groups (lecturers, students)
		assertEquals(2, userGroupService.getAllUserGroups().size());
		assertTrue(userGroupService.getUserGroupByName("Lecturers").isPresent());
		assertTrue(userGroupService.getUserGroupByName("Students").isPresent());

		// only student2 should be in 2 lists
		assertEquals(0, userGroupService.getUserGroupsForUser(user).size());
		assertEquals(1, userGroupService.getUserGroupsForUser(lecturer1).size());
		assertEquals(1, userGroupService.getUserGroupsForUser(lecturer2).size());
		assertEquals(1, userGroupService.getUserGroupsForUser(student1).size());
		assertEquals(2, userGroupService.getUserGroupsForUser(student2).size());
	}

	/**
	 * Get group with member users
	 */
	@Test
	void getGroupWithUsers() {
		UserGroup students = TestDataFactory.getUserGroup("Students", student1, student2);
		userGroupService.persistUserGroup(students);

		students = userGroupService.getUserGroupWithMemberUsers(students);
		assertTrue(students.containsUserAsDirectMember(student1));
		assertTrue(students.containsUserAsDirectMember(student2));
	}

	/**
	 * Get group with member groups
	 */
	@Test
	void getGroupWithGroups() {
		UserGroup students = TestDataFactory.getUserGroup("Students", student1, student2);
		UserGroup lecturers = TestDataFactory.getUserGroup("Lecturers", lecturer1, lecturer2);
		userGroupService.persistUserGroup(students);
		userGroupService.persistUserGroup(lecturers);

		UserGroup allUsers = TestDataFactory.getUserGroup("All users");
		allUsers.addMemberGroup(students);
		allUsers.addMemberGroup(lecturers);
		userGroupService.persistUserGroup(allUsers);

		allUsers = userGroupService.getUserGroupWithMemberGroups(allUsers);
		assertTrue(allUsers.containsGroupAsDirectMember(students));
		assertTrue(allUsers.containsGroupAsDirectMember(lecturers));
	}

	/**
	 * Test if a group with a changed name could be found
	 */
	@Test
	void getUserGroupProperties() {
		UserGroup group = TestDataFactory.getUserGroup("User Group");
		userGroupService.persistUserGroup(group);

		assertTrue(userGroupService.getUserGroupByName("User Group").isPresent());
		assertFalse(userGroupService.getUserGroupByName("New name").isPresent());

		group.setName("New name");
		group = userGroupService.mergeUserGroup(group);

		assertFalse(userGroupService.getUserGroupByName("User Group").isPresent());
		assertTrue(userGroupService.getUserGroupByName("New name").isPresent());
	}

}
