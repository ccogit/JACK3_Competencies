package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCSubmission;
import de.uni_due.s3.jack3.entities.tenant.ManualResult;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

/**
 * Test class for {@link ManualResult}, based on a MC-submission.
 *
 * @author lukas.glaser
 *
 */
@NeedsExercise
class ManualResultTest extends AbstractContentTest {

	private StageSubmission submission;
	private ManualResult manualResult;

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();

		exercise.addStage(new MCStage());
		exercise = baseService.merge(exercise);

		submission = new MCSubmission();
		manualResult = new ManualResult(user);
		submission.setStageId(exercise.getStagesAsList().get(0).getId());
		submission.setManualResult(manualResult);
		baseService.persist(submission);
	}

	private void mergeSubmission() {
		submission = baseService.merge(submission);
		manualResult = submission.getManualResult().orElse(null);
	}

	@Test
	void testInternalComment() {
		manualResult.setInternalComment("This is an internal comment.");
		mergeSubmission();
		assertEquals("This is an internal comment.", manualResult.getInternalComment());
	}

	@Test
	void testPublicComment() {
		manualResult.setPublicComment("This is a public comment.");
		mergeSubmission();
		assertEquals("This is a public comment.", manualResult.getPublicComment());
	}

	@Test
	void testPoints() {
		manualResult.setPoints(15);
		mergeSubmission();
		assertEquals(15, manualResult.getPoints());

		// Test illegal values
		assertThrows(IllegalArgumentException.class, () -> manualResult.setPoints(-1));
		assertThrows(IllegalArgumentException.class, () -> manualResult.setPoints(-100));
		assertThrows(IllegalArgumentException.class, () -> manualResult.setPoints(101));
		assertThrows(IllegalArgumentException.class, () -> manualResult.setPoints(500));

		// Test legal values
		assertDoesNotThrow(() -> manualResult.setPoints(0));
		assertDoesNotThrow(() -> manualResult.setPoints(50));
		assertDoesNotThrow(() -> manualResult.setPoints(100));
	}

	@Test
	void testShowAutomaticResult() {
		manualResult.setShowAutomaticResult(true);
		mergeSubmission();
		assertTrue(manualResult.isShowAutomaticResult());
	}

	@Test
	void testCreatedBy() {
		assertNotNull(manualResult.getCreatedBy());
		assertEquals(user, manualResult.getCreatedBy());
	}
}
