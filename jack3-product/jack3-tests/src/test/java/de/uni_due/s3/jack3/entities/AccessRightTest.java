package de.uni_due.s3.jack3.entities;

import static de.uni_due.s3.jack3.entities.AccessRight.EXTENDED_READ;
import static de.uni_due.s3.jack3.entities.AccessRight.GRADE;
import static de.uni_due.s3.jack3.entities.AccessRight.MANAGE;
import static de.uni_due.s3.jack3.entities.AccessRight.READ;
import static de.uni_due.s3.jack3.entities.AccessRight.WRITE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

class AccessRightTest {

	private void assertEquals(int expectedFlags, AccessRight actual) {
		final int actualFlags = actual.getBitFlag();
		if (expectedFlags != actualFlags) {
			fail(String.format("expected: <%s> but was: <%s>", AccessRight.getFromBitFlag(expectedFlags), actual));
		}
	}

	private void assertEquals(AccessRight expected, AccessRight actual) {
		final int expectedFlags = expected.getBitFlag();
		final int actualFlags = actual.getBitFlag();
		if (expectedFlags != actualFlags) {
			fail(String.format("expected: <%s> but was: <%s>", expected, actual));
		}
	}

	@Test
	void testFlagsNone() {

		final AccessRight none = AccessRight.getFromFlags();
		assertTrue(none.isNone());
		assertFalse(none.isRead());
		assertFalse(none.isExtendedRead());
		assertFalse(none.isWrite());
		assertFalse(none.isGrade());
		assertFalse(none.isManage());
		assertEquals(0, none);
	}

	@Test
	void testFlagsRead() {

		final AccessRight read = AccessRight.getFromFlags(READ);
		assertFalse(read.isNone());
		assertTrue(read.isRead());
		assertFalse(read.isExtendedRead());
		assertFalse(read.isWrite());
		assertFalse(read.isGrade());
		assertFalse(read.isManage());
		assertEquals(0b00001, read);
	}

	@Test
	void testFlagsExtendedRead() {

		final AccessRight extRead1 = AccessRight.getFromFlags(EXTENDED_READ);
		assertFalse(extRead1.isNone());
		assertTrue(extRead1.isRead());
		assertTrue(extRead1.isExtendedRead());
		assertFalse(extRead1.isWrite());
		assertFalse(extRead1.isGrade());
		assertFalse(extRead1.isManage());
		assertEquals(0b00011, extRead1);

		final AccessRight extRead2 = AccessRight.getFromFlags(READ, EXTENDED_READ);
		assertFalse(extRead2.isNone());
		assertTrue(extRead2.isRead());
		assertTrue(extRead2.isExtendedRead());
		assertFalse(extRead2.isWrite());
		assertFalse(extRead2.isGrade());
		assertFalse(extRead2.isManage());
		assertEquals(0b00011, extRead2);
	}

	@Test
	void testFlagsWrite() {

		final AccessRight write1 = AccessRight.getFromFlags(WRITE);
		assertFalse(write1.isNone());
		assertTrue(write1.isRead());
		assertFalse(write1.isExtendedRead());
		assertTrue(write1.isWrite());
		assertFalse(write1.isGrade());
		assertFalse(write1.isManage());
		assertEquals(0b00101, write1);

		final AccessRight write2 = AccessRight.getFromFlags(READ, WRITE);
		assertFalse(write2.isNone());
		assertTrue(write2.isRead());
		assertFalse(write2.isExtendedRead());
		assertTrue(write2.isWrite());
		assertFalse(write2.isGrade());
		assertFalse(write2.isManage());
		assertEquals(0b00101, write2);
	}

	@Test
	void testFlagsGrade() {

		final AccessRight grade1 = AccessRight.getFromFlags(GRADE);
		assertFalse(grade1.isNone());
		assertTrue(grade1.isRead());
		assertFalse(grade1.isExtendedRead());
		assertFalse(grade1.isWrite());
		assertTrue(grade1.isGrade());
		assertEquals(0b01001, grade1);

		final AccessRight grade2 = AccessRight.getFromFlags(READ, GRADE);
		assertFalse(grade2.isNone());
		assertTrue(grade2.isRead());
		assertFalse(grade2.isExtendedRead());
		assertFalse(grade2.isWrite());
		assertTrue(grade2.isGrade());
		assertEquals(0b01001, grade2);
	}

	@Test
	void testFlagsManage() {

		final AccessRight manage1 = AccessRight.getFromFlags(MANAGE);
		assertFalse(manage1.isNone());
		assertTrue(manage1.isRead());
		assertTrue(manage1.isExtendedRead());
		assertTrue(manage1.isWrite());
		assertTrue(manage1.isGrade());
		assertTrue(manage1.isManage());
		assertEquals(0b11111, manage1);

		final AccessRight manage2 = AccessRight.getFromFlags(MANAGE, EXTENDED_READ, WRITE, GRADE);
		assertFalse(manage2.isNone());
		assertTrue(manage2.isRead());
		assertTrue(manage2.isExtendedRead());
		assertTrue(manage2.isWrite());
		assertTrue(manage2.isGrade());
		assertTrue(manage2.isManage());
		assertEquals(0b11111, manage2);
	}

	@Test
	void testFlagsFull() {

		final AccessRight full1 = AccessRight.getFromFlags(EXTENDED_READ, WRITE, GRADE, MANAGE);
		assertFalse(full1.isNone());
		assertTrue(full1.isRead());
		assertTrue(full1.isExtendedRead());
		assertTrue(full1.isWrite());
		assertTrue(full1.isGrade());
		assertTrue(full1.isManage());
		assertEquals(0b11111, full1);

		assertEquals(0b11111, AccessRight.getFromFlags(EXTENDED_READ, WRITE, MANAGE));
		assertEquals(0b11111, AccessRight.getFromFlags(WRITE, MANAGE));
		assertEquals(0b11111, AccessRight.getFromFlags(EXTENDED_READ, MANAGE));
		assertEquals(0b11111, AccessRight.getFromFlags(READ, MANAGE));
		assertEquals(0b11111, AccessRight.getFromFlags(READ, GRADE, MANAGE));
	}

	@Test
	void testNone() {
		final AccessRight none = AccessRight.getNone();
		assertTrue(none.isNone());
		assertFalse(none.isRead());
		assertFalse(none.isExtendedRead());
		assertFalse(none.isWrite());
		assertFalse(none.isManage());
		assertEquals(0, none);
		assertEquals(AccessRight.getFromFlags(), none);
	}

	@Test
	void testFull() {
		final AccessRight full = AccessRight.getFull();
		assertFalse(full.isNone());
		assertTrue(full.isRead());
		assertTrue(full.isExtendedRead());
		assertTrue(full.isWrite());
		assertTrue(full.isManage());
		assertEquals(0b11111, full);
		assertEquals(AccessRight.getFromFlags(READ, EXTENDED_READ, WRITE, GRADE, MANAGE), full);
	}

	@Test
	void addToNone() {
		final AccessRight right = AccessRight.getNone();
		assertEquals(0b00000, right.add(AccessRight.getNone()));
		assertEquals(0b11111, right.add(AccessRight.getFull()));
		assertEquals(0b00001, right.add(READ));
		assertEquals(0b00011, right.add(EXTENDED_READ));
		assertEquals(0b00101, right.add(WRITE));
		assertEquals(0b01001, right.add(GRADE));
		assertEquals(0b00111, right.add(AccessRight.getFromFlags(WRITE, EXTENDED_READ)));
	}

	@Test
	void addToRead() {
		final AccessRight right = AccessRight.getFromFlags(READ);
		assertEquals(0b00001, right.add(AccessRight.getNone()));
		assertEquals(0b11111, right.add(AccessRight.getFull()));
		assertEquals(0b00001, right.add(READ));
		assertEquals(0b00011, right.add(EXTENDED_READ));
		assertEquals(0b00101, right.add(WRITE));
		assertEquals(0b01001, right.add(GRADE));
		assertEquals(0b00111, right.add(AccessRight.getFromFlags(WRITE, EXTENDED_READ)));
	}

	@Test
	void addToExtendedRead() {
		final AccessRight right = AccessRight.getFromFlags(EXTENDED_READ);
		assertEquals(0b00011, right.add(AccessRight.getNone()));
		assertEquals(0b11111, right.add(AccessRight.getFull()));
		assertEquals(0b00011, right.add(READ));
		assertEquals(0b00011, right.add(EXTENDED_READ));
		assertEquals(0b00111, right.add(WRITE));
		assertEquals(0b01011, right.add(GRADE));
		assertEquals(0b00111, right.add(AccessRight.getFromFlags(WRITE, EXTENDED_READ)));
	}

	@Test
	void addToWrite() {
		final AccessRight right = AccessRight.getFromFlags(WRITE);
		assertEquals(0b00101, right.add(AccessRight.getNone()));
		assertEquals(0b11111, right.add(AccessRight.getFull()));
		assertEquals(0b00101, right.add(READ));
		assertEquals(0b00111, right.add(EXTENDED_READ));
		assertEquals(0b00101, right.add(WRITE));
		assertEquals(0b01101, right.add(GRADE));
		assertEquals(0b00111, right.add(AccessRight.getFromFlags(WRITE, EXTENDED_READ)));
	}

	@Test
	void addToGrade() {
		final AccessRight right = AccessRight.getFromFlags(GRADE);
		assertEquals(0b01001, right.add(AccessRight.getNone()));
		assertEquals(0b11111, right.add(AccessRight.getFull()));
		assertEquals(0b01001, right.add(READ));
		assertEquals(0b01011, right.add(EXTENDED_READ));
		assertEquals(0b01101, right.add(WRITE));
		assertEquals(0b01001, right.add(GRADE));
		assertEquals(0b01111, right.add(AccessRight.getFromFlags(WRITE, EXTENDED_READ)));
	}

	@Test
	void addToManage() {
		final AccessRight right = AccessRight.getFromFlags(MANAGE);
		assertEquals(0b11111, right.add(AccessRight.getNone()));
		assertEquals(0b11111, right.add(AccessRight.getFull()));
		assertEquals(0b11111, right.add(READ));
		assertEquals(0b11111, right.add(EXTENDED_READ));
		assertEquals(0b11111, right.add(WRITE));
		assertEquals(0b11111, right.add(GRADE));
		assertEquals(0b11111, right.add(AccessRight.getFromFlags(WRITE, EXTENDED_READ)));
	}

	@Test
	void addToFull() {
		final AccessRight right = AccessRight.getFull();
		assertEquals(0b11111, right.add(AccessRight.getNone()));
		assertEquals(0b11111, right.add(AccessRight.getFull()));
		assertEquals(0b11111, right.add(READ));
		assertEquals(0b11111, right.add(EXTENDED_READ));
		assertEquals(0b11111, right.add(WRITE));
		assertEquals(0b11111, right.add(GRADE));
		assertEquals(0b11111, right.add(AccessRight.getFromFlags(WRITE, EXTENDED_READ)));
	}

}
