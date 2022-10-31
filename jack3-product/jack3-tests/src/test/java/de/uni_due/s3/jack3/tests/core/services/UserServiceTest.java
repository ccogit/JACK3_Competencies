package de.uni_due.s3.jack3.tests.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;

import javax.ejb.EJBTransactionRolledbackException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

class UserServiceTest extends AbstractBasicTest {

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();
		persistUser();
	}

	/**
	 * Get 1 user
	 */
	@Test
	void getOneUser() {
		// there should be one user
		assertEquals(1, userService.countUser());
		assertTrue(userService.getUserByName("User").isPresent());
		assertFalse(userService.getUserByName("First").isPresent());
	}

	/**
	 * get all users
	 */
	@Test
	void getAllUsers() {
		List<User> allUsers = userService.getAllUsers();
		assertEquals(1, allUsers.size());
		assertEquals(userService.countUser(), allUsers.size());
		assertEquals(user, allUsers.get(0));

		//add 2 more users
		userService.persistUser(TestDataFactory.getUser("User2"));
		userService.persistUser(TestDataFactory.getUser("User3"));

		allUsers = userService.getAllUsers();
		assertEquals(3, allUsers.size());
		assertEquals(userService.countUser(), allUsers.size());
	}

	/**
	 * Get user by ID
	 */
	@Test
	void getUserById() {
		User user = TestDataFactory.getUser("User2");
		userService.persistUser(user);

		assertEquals(user, userService.getUserById(user.getId())
												.orElseThrow(AssertionError::new));
		assertNotEquals(user, userService.getUserById(super.user.getId())
												.orElseThrow(AssertionError::new));
		assertEquals(super.user, userService.getUserById(super.user.getId())
													.orElseThrow(AssertionError::new));
		assertNotEquals(super.user, userService.getUserById(user.getId())
														.orElseThrow(AssertionError::new));
	}

	/**
	 * check if a user with a given pseudonym exists
	 */
	@Test
	void hasPseudonym() {
		assertTrue(userService.hasPseudonym(user.getPseudonym()));
		assertFalse(userService.hasPseudonym("notExisitingPseudonym"));
	}

	/**
	 * Add 3 users with different rights (Admin, Lecturer, Student)
	 */
	@Test
	void addMultipleUsers() {
		// adding lecturer and student with different rights
		User lecturer = TestDataFactory.getUser("Lecturer");
		User student = TestDataFactory.getUser("Student", false, false);

		userService.persistUser(lecturer);
		userService.persistUser(student);

		// there should be 3 users at all: First User, Lecturer, Student
		assertFalse(userService.hasNoUser());
		assertEquals(3, userService.countUser());

		// there should be 2 users with edit rights: First User, Lecturer
		Collection<User> usersWithEditRights = userService.getAllUsersWithEditRights();
		assertEquals(2, usersWithEditRights.size());
		assertTrue(usersWithEditRights.contains(lecturer));
		assertFalse(usersWithEditRights.contains(student));

		// there should be 1 user without edit rights: Student
		Collection<User> usersWithoutEditRights = userService.getAllUsersWithoutEditRights();
		assertEquals(1, usersWithoutEditRights.size());
		assertTrue(usersWithoutEditRights.contains(student));

		assertTrue(userService.getUserByName("User").isPresent());
		assertTrue(userService.getUserByName("Lecturer").isPresent());
		assertTrue(userService.getUserByName("Student").isPresent());
	}

	/**
	 * Add the same user twice (->Rollback)
	 */
	@Test
	void createDuplicateUsers() {
		final User user = TestDataFactory.getUser("User");
		final long numberOfUsers = userService.countUser();

		// adding the user twice should roll back
		assertThrows(EJBTransactionRolledbackException.class, () -> {
			userService.persistUser(user);
		});

		//the number of users shouldn't have changed
		assertEquals(numberOfUsers, userService.countUser());
	}

	/**
	 * Get not available user
	 */
	@Test
	void getNotAvailableUser() {
		assertFalse(userService.getUserByName("Student 50").isPresent());
		assertFalse(userService.getUserById(-1).isPresent());
	}

	/**
	 * Create content folder as personal user and check if user has it
	 */
	@Test
	void changePersonalFolder() {

		User user = userService	.getUserByName("User")
								.orElseThrow(AssertionError::new);

		// user should not have a personal folder
		assertNull(user.getPersonalFolder());

		persistFolder();

		user.setPersonalFolder(folder);
		user = userService.mergeUser(user);

		// user should have one folder
		assertEquals(folder, user.getPersonalFolder());
		assertEquals(user, userService.getUserOwningThisFolder(folder)
												.orElseThrow(AssertionError::new));
	}

	/**
	 * Change edit rights of a user, after revoke edit rights, the query "UserWithEditRights" should not contain it
	 */
	@Test
	void changeUserEditRights() {
		User user = userService	.getUserByName("User")
								.orElseThrow(AssertionError::new);

		assertTrue(userService.getAllUsersWithEditRights().contains(user));

		// change edit rights
		user.setHasEditRights(false);
		user = userService.mergeUser(user);

		assertFalse(userService.getAllUsersWithEditRights().contains(user));

	}
}
