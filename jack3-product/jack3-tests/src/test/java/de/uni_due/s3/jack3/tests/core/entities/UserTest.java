package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Locale;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.BcryptBusiness;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.Password;
import de.uni_due.s3.jack3.entities.tenant.ProfileField;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserExerciseFilter;
import de.uni_due.s3.jack3.services.ProfileFieldService;
import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

class UserTest extends AbstractBasicTest {

	// User from database with actual values
	private User userFromDB;

	@Inject
	private BcryptBusiness bcryptBusiness;

	@Inject
	private ProfileFieldService profileFieldService;

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		persistUser();
		userFromDB = userService.getUserByName("User")
								.orElseThrow(AssertionError::new);
	}

	/**
	 * Tests if creating a new user with illegal arguments is not possible
	 */
	@Test
	void createUserWithNoName() {
		final var pwd = TestDataFactory.getEmptyPassword();
		assertThrows(IllegalArgumentException.class, () -> {
			new User("", "K1", pwd, "email", false, false);
		});
	}

	/**
	 * Test if compare by name works
	 */
	@Test
	void compareUsers() {
		assertEquals(0, user.compareTo(userFromDB));
		assertTrue(TestDataFactory.getUser("Anton").compareTo(userFromDB) < 0);
		assertTrue(TestDataFactory.getUser("Xaver").compareTo(userFromDB) > 0);
	}

	@Test
	void testMail() {
		assertEquals(user.getEmail(), userFromDB.getEmail());

		// change mail
		userFromDB.setEmail("newmail@foobar.com");
		userFromDB = userService.mergeUser(userFromDB);

		assertEquals("newmail@foobar.com", userFromDB.getEmail());
	}

	/**
	 * Get and set exercise filters
	 */
	@Test
	@Disabled("No cascading and lazy-fetching is implemented yet.")
	void testExerciseFilters() {
		assertTrue(userFromDB.getExerciseFilters().isEmpty());

		// add exercise filter
		HashMap<CourseOffer, UserExerciseFilter> filters = new HashMap<>();
		filters.put(new CourseOffer(), new UserExerciseFilter());
		userFromDB.addExerciseFilters(filters);
		userFromDB = userService.mergeUser(userFromDB);

		assertEquals(1, userFromDB.getExerciseFilters().size());
	}

	@Test
	void testLanguage() {
		assertNull(userFromDB.getLanguage());

		// change language
		userFromDB.setLanguage("de-de");
		userFromDB = userService.mergeUser(userFromDB);

		assertEquals(Locale.GERMANY, userFromDB.getLanguage());
	}

	@Test
	void testLastLogin() {
		assertNull(userFromDB.getLastLogin());

		// change last login
		userFromDB.setLastLogin(TestDataFactory.getDateTime());
		userFromDB = userService.mergeUser(userFromDB);

		assertEquals(TestDataFactory.getDateTime(), userFromDB.getLastLogin());
	}

	@Test
	void testPassword() {
		assertEquals(user.getPassword(), userFromDB.getPassword());

		// change password to a new hash
		Password newPasword = bcryptBusiness.createPassword("evenmoresecure");
		userFromDB.setPassword(newPasword);
		userFromDB = userService.mergeUser(userFromDB);

		assertEquals(newPasword, userFromDB.getPassword());
	}

	/**
	 * Get and set ProfileFields
	 */
	@Test
	void testProfileData() {
		assertTrue(userFromDB.getProfileData().isEmpty());

		ProfileField field = profileFieldService.getOrCreateIdentityField("key");

		// add profile data
		HashMap<ProfileField, String> fields = new HashMap<>();
		fields.put(field, "value");
		userFromDB.addProfileData(fields);
		userFromDB = userService.mergeUser(userFromDB);

		assertEquals(1, userFromDB.getProfileData().size());
		assertEquals("value", userFromDB.getProfileData().get(field));
	}

	@Test
	void testName() {
		assertEquals(user.getPseudonym(), userFromDB.getPseudonym());
		assertEquals(user.getLoginName(), userFromDB.getLoginName());
		assertEquals("user", userFromDB.getLoginName());
	}

	@Test
	void testAdminRights() {
		assertTrue(userFromDB.isHasAdminRights());

		// change rights
		userFromDB.setHasAdminRights(false);
		userFromDB = userService.mergeUser(userFromDB);

		assertFalse(userFromDB.isHasAdminRights());
	}

	@Test
	void testEditRights() {
		assertTrue(userFromDB.isHasEditRights());

		// change rights
		userFromDB.setHasEditRights(false);
		userFromDB = userService.mergeUser(userFromDB);

		assertFalse(userFromDB.isHasEditRights());
	}

}
