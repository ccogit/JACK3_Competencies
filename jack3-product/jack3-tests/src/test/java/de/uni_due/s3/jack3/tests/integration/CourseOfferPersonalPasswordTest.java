package de.uni_due.s3.jack3.tests.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.tests.utils.AbstractBusinessTest;

/**
 * Tests for course offer access mode "Personal password": Students must enter a password to enter the course. The
 * password is generated for each student or can be imported via a CSV file. A list of students with passwords can be
 * exported into a CSV file. Names with umlauts are also tested.
 *
 * @author lukas.glaser
 *
 */
class CourseOfferPersonalPasswordTest extends AbstractBusinessTest {

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private CourseBusiness courseBusiness;

	private Course course;
	private CourseOffer offer;

	private static final String PWD_PATTERN = "(a|b|c|d|e|f|g|h|i|j|k|m|n|o|p|q|r|s|t|u|v|w|x|y|z|2|3|4|5|6|7|8|9)*";

	/**
	 * Prepare tests: Create a course offer
	 * @throws ActionNotAllowedException
	 */
	@BeforeEach
	void prepareTest() throws ActionNotAllowedException {

		User user = getAdmin("Admin");

		ContentFolder contentFolder = user.getPersonalFolder();
		PresentationFolder presentationFolder = folderBusiness.getPresentationRoot();

		course = courseBusiness.createCourse("Course", user, contentFolder);
		offer = courseBusiness.createCourseOffer("Course Offer", course, presentationFolder, user);
	}

	/**
	 * Create multiple users of the given names.
	 */
	private List<User> createUsers(String... names) {
		List<User> users = new ArrayList<>(names.length);
		for (String name : names) {
			users.add(getStudent(name));
		}
		return users;
	}

	/**
	 * Check if a password complies with the rules.
	 */
	private boolean checkPasswordRules(String password) {
		return password.length() == 8
				&& password.matches(PWD_PATTERN);
	}

	/**
	 * Check if a CSV file (as a InputStream) of usernames and personal passwords matches a map (User -> String) of user
	 * and password.
	 */
	private void assertCorrectCsv(Map<User, String> map, InputStream inputStream) throws IOException {
		Map<String, String> expectedResult = new HashMap<>();
		Map<String, String> actualResult = new HashMap<>();

		try (InputStreamReader in = new InputStreamReader(inputStream);
				BufferedReader reader = new BufferedReader(in)) {

			for (Map.Entry<User, String> entry : map.entrySet()) {
				expectedResult.put(entry.getKey().getLoginName(), entry.getValue());
			}

			reader.lines().forEach(line -> {
				if (!line.contains("sep=")) {
					String[] splittedLine = line.split(";");
					if (splittedLine.length == 2) {
						actualResult.put(splittedLine[0], splittedLine[1]);
					}
				}

			});
		}

		assertEquals(expectedResult, actualResult);
	}

	/**
	 * Tests adding users directly to the course offer.
	 */
	@Test
	void addUsersDirectly() throws IOException {
		// Add some usernames
		List<User> users = createUsers("Anna", "Paul", "Joachim", "Ägidius", "Cäcilia", "Sören", "Jürgen", "Roßwieta");

		for (User user : users) {
			courseBusiness.addPersonalPasswordEntryToCourseOffer(offer, user);
		}

		// Check if all users were added with a personal password
		assertTrue(offer.getPersonalPasswords().keySet().containsAll(users));

		// Check if all personal passwords are compliant with the rules
		for (Map.Entry<User, String> entry : offer.getPersonalPasswords().entrySet()) {
			assertTrue(checkPasswordRules(entry.getValue()), "Password '" + entry.getValue() + "' for user "
					+ entry.getKey().getLoginName() + " is not compliant with the rules.");
		}

		// Check if CSV export is correct
		assertCorrectCsv(offer.getPersonalPasswords(), courseBusiness.downloadPersonalPasswordsFile(offer));
	}

	/*-
	 * CSV test - Expected results:
	 *
	 * 0) Anna - "password"
	 * 1) Paul - password already stored - "secret" (from CSV) should overwrite previous password
	 * 2) Joachim - generated password
	 * 3) Ägidius - generated password - not changed
	 * 4) Cäcilia - "secret"
	 * 5) Sören - "pass01l"
	 * 6) Jürgen - generated password
	 * 7) Roßwieta - "foobar"
	 * 8) Mareike - ignored, user is not in database
	 */
	@ParameterizedTest
	@ValueSource(strings = {
			"testdata/courseOfferPersonalPasswordTest/excel.csv", // Microsoft Excel format
			"testdata/courseOfferPersonalPasswordTest/libreoffice.csv", // LibreOffice Calc format
			"testdata/courseOfferPersonalPasswordTest/other.csv" // semicolon-separated with separator-character
	})
	void uploadFileAndAssertTestusers(final String filename) throws IOException {
		// Add usernames
		List<User> users = createUsers("Anna", "Paul", "Joachim", "Ägidius", "Cäcilia", "Sören", "Jürgen", "Roßwieta");

		// Add default passwords for Paul/Ägidius to check if original passwords are overwritten
		courseBusiness.addPersonalPasswordEntryToCourseOffer(offer, users.get(1));
		courseBusiness.addPersonalPasswordEntryToCourseOffer(offer, users.get(3));
		String aegidiusPreviousPassword = offer.getPersonalPasswords().get(users.get(3));

		ClassLoader classLoader = this.getClass().getClassLoader();
		InputStream in = classLoader.getResourceAsStream(filename);

		courseBusiness.uploadPersonalPasswordsFile(offer, in);

		Map<User, String> actualResult = offer.getPersonalPasswords();

		assertEquals(8, actualResult.size());

		// Check password results
		assertEquals("password", actualResult.get(users.get(0)));
		assertEquals("secret", actualResult.get(users.get(1)));
		assertTrue(checkPasswordRules(actualResult.get(users.get(2))));
		assertEquals(aegidiusPreviousPassword, actualResult.get(users.get(3)));
		assertEquals("secret", actualResult.get(users.get(4)));
		assertEquals("pass01l", actualResult.get(users.get(5)));
		assertTrue(checkPasswordRules(actualResult.get(users.get(6))));
		assertEquals("foobar", actualResult.get(users.get(7)));

		// Check if CSV export is correct
		assertCorrectCsv(actualResult, courseBusiness.downloadPersonalPasswordsFile(offer));
	}

	// See "SubmissionActionsTest" and "SubmissionPermissionTest" for tests from the perspective of students

}
