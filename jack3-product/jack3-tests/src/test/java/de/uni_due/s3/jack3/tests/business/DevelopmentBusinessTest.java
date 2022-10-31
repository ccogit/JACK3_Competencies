package de.uni_due.s3.jack3.tests.business;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.DevelopmentBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.tests.utils.AbstractBusinessTest;

/**
 * Tests for DevelopmentBusiness
 *
 * @author leander.harlos
 *
 */
class DevelopmentBusinessTest extends AbstractBusinessTest {

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private DevelopmentBusiness developmentBusiness;

	@Test
	void testLookupLecturerUpperLecturer() {
		userBusiness.createUser("Lecturer", "secret", "foo@bar.com", false, true);
		assertTrue(developmentBusiness.lookupLecturer().isPresent());
	}

	@Test
	void testLookupLecturerLowerLecturer() {

		userBusiness.createUser("lecturer", "secret", "foo@bar.com", false, true);
		assertTrue(developmentBusiness.lookupLecturer().isPresent());
	}

	@Test
	void testLookupLecturerNoUsers() {

		assertFalse(developmentBusiness.lookupLecturer().isPresent());
	}
}
