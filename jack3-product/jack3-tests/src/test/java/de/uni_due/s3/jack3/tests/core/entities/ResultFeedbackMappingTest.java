package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.ResultFeedbackMapping;
import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;

/**
 * Test class for {@linkplain ResultFeedbackMapping}.
 * 
 * @author Marc.Kasper
 *
 */
class ResultFeedbackMappingTest extends AbstractBasicTest {

	/**
	 * This test checks the deepCopy of a result feedback mapping.
	 */
	@Test
	void deepCopyTestOfResultFeedbackMapping() {

		ResultFeedbackMapping originResultFeedbackMapping;
		ResultFeedbackMapping deepCopyOfResultFeedbackMapping;

		originResultFeedbackMapping = new ResultFeedbackMapping("2+3", "Deep copy test of result " + "feedback mapping",
				"Test the feedback mapping.");

		deepCopyOfResultFeedbackMapping = originResultFeedbackMapping.deepCopy();

		assertNotEquals(originResultFeedbackMapping, deepCopyOfResultFeedbackMapping,
				"The deepcopy is the origin result feedback mapping itself.");
		assertEquals("Deep copy test of result feedback mapping", deepCopyOfResultFeedbackMapping.getTitle(),
				"The title of result feedback mappings are different.");
		assertEquals("Test the feedback mapping.", deepCopyOfResultFeedbackMapping.getText(),
				"The text of result feedback mappings are different.");
		assertEquals("2+3", deepCopyOfResultFeedbackMapping.getExpression(),
				"The expression of result feedback mappings are different.");
	}
}