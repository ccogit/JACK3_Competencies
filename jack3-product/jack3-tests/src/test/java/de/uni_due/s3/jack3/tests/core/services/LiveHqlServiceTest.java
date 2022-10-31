package de.uni_due.s3.jack3.tests.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import javax.ejb.EJBException;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

/**
 * Tests LiveHqlService queries based on UserService.
 */
class LiveHqlServiceTest extends AbstractBasicTest {

	/**
	 * Get empty results (get single result) -> NoResultException
	 */
	@Test
	void getEmptySingleResult() {
		EJBException thrown = assertThrows(EJBException.class, () -> {
			hqlService.getSingleResult("FROM User");
		});
		assertTrue(thrown.getCause() instanceof NoResultException);
	}

	/**
	 * Get empty results (get result list)
	 */
	@Test
	void getEmptyResultList() {
		assertTrue(hqlService.getResultList("FROM User").isEmpty());
	}

	/**
	 * Get 1 user (get single result)
	 */
	@Test
	void getUserBySingleResult() {
		persistUser();
		assertEquals(Arrays.asList(user), hqlService.getSingleResult("FROM User"));
	}

	/**
	 * Get 1 user (get result list)
	 */
	@Test
	void getUserByResultList() {
		persistUser();
		assertEquals(Arrays.asList(user), hqlService.getResultList("FROM User"));
	}

	/**
	 * Get user list (get single result) -> NonUniqueResultException
	 */
	@Test
	void getUsersBySingleResult() {
		User user1 = TestDataFactory.getUser("User1");
		userService.persistUser(user1);
		User user2 = TestDataFactory.getUser("User2");
		userService.persistUser(user2);

		EJBException thrown = assertThrows(EJBException.class, () -> {
			hqlService.getSingleResult("FROM User");
		});
		assertTrue(thrown.getCause() instanceof NonUniqueResultException);
	}

	/**
	 * Get user list (get user list)
	 */
	@SuppressWarnings("unchecked")
	@Test
	void getUsersByResultList() {
		User user1 = TestDataFactory.getUser("User1");
		userService.persistUser(user1);
		User user2 = TestDataFactory.getUser("User2");
		userService.persistUser(user2);

		List<User> getResultList = (List<User>) hqlService.getResultList("FROM User");
		assertEquals(2, getResultList.size());
		assertTrue(getResultList.containsAll(Arrays.asList(user1, user2)));
	}
}
