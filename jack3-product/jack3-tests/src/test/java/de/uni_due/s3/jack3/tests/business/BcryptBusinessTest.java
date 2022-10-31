package de.uni_due.s3.jack3.tests.business;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.BcryptBusiness;
import de.uni_due.s3.jack3.entities.tenant.Password;
import de.uni_due.s3.jack3.tests.utils.AbstractTest;

/**
 * Tests for hashing passwords with BcryptBusiness
 *
 * @author lukas.glaser
 *
 */
class BcryptBusinessTest extends AbstractTest {

	@Inject
	private BcryptBusiness bcryptBusiness;

	/**
	 * Tests if a generated hash matches with the right password
	 */
	@Test
	void testMatching() {
		String plaintext = "Hello World!";
		Password password = bcryptBusiness.createPassword(plaintext);

		assertTrue(bcryptBusiness.matches(plaintext, password));
	}

	/**
	 * Tests if a generated hash not matches with a wrong password
	 */
	@Test
	void testNotMatching() {
		Password password = bcryptBusiness.createPassword("Hello World!");
		assertFalse(bcryptBusiness.matches("Hello Bug!", password));
	}

}
