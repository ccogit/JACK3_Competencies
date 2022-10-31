package de.uni_due.s3.jack3.entities.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit-test for {@link EFillInEditorType}
 *
 */
class EFillInEditorTypeTest {

	@Test
	void testNormalField() {
		assertEquals("", EFillInEditorType.NONE.getTypeLabel());
	}

	@Test
	void testTextField() {
		assertEquals("Text", EFillInEditorType.TEXT.getTypeLabel());
	}

	@Test
	void testNumberField() {
		assertEquals("Number", EFillInEditorType.NUMBER.getTypeLabel());
	}

	@Test
	void testFormularEditorField() {
		assertEquals("", EFillInEditorType.NONE.getTypeLabel());
	}

}
