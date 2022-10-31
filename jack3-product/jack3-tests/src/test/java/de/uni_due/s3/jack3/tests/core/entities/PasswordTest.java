package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.Password;

/**
 * Tests for illegal password parameters
 */
class PasswordTest {

	@Test
	void createPassword() { // NOSONAR This only checks if no Exception occurs
		new Password(5, new byte[16], new byte[23]);
	}

	// Illegal round number 1
	@Test
	void createIllegalPassword1() {
		assertThrows(IllegalArgumentException.class, () -> {
			new Password(3, new byte[16], new byte[23]);
		});
	}

	// Illegal round number 2
	@Test
	void createIllegalPassword2() {
		assertThrows(IllegalArgumentException.class, () -> {
			new Password(32, new byte[16], new byte[23]);
		});
	}

	// Illegal salt size 1
	@Test
	void createIllegalPassword3() {
		assertThrows(IllegalArgumentException.class, () -> {
			new Password(5, new byte[15], new byte[23]);
		});
	}

	// Illegal salt size 2
	@Test
	void createIllegalPassword4() {
		assertThrows(IllegalArgumentException.class, () -> {
			new Password(5, new byte[17], new byte[23]);
		});
	}

	// Illegal hash size 1
	@Test
	void createIllegalPassword5() {
		assertThrows(IllegalArgumentException.class, () -> {
			new Password(5, new byte[16], new byte[22]);
		});
	}

	// Illegal hash size 2
	@Test
	void createIllegalPassword6() {
		assertThrows(IllegalArgumentException.class, () -> {
			new Password(5, new byte[16], new byte[24]);
		});
	}

}
