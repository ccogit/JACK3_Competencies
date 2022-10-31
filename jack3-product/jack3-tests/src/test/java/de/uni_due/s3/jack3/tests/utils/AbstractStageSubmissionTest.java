package de.uni_due.s3.jack3.tests.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.ManualResult;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.core.stagetypes.mc.MCSubmissionTest;

/**
 * Super class for testing stage submissions. T and U specify the type of stage and submission, the methods for creating
 * an empty stage and a submission should be overridden by the test class. See {@linkplain MCSubmissionTest} as an
 * example. This class also tests {@linkplain StageSubmission}.
 * <p>
 * Do not use this class only for inserting a submission to a test. Use {@linkplain AbstractContentTest} and insert the
 * submission manually.
 *
 * @author lukas.glaser
 * @see AbstractStageTest
 */
@NeedsExercise
public abstract class AbstractStageSubmissionTest<T extends Stage, U extends StageSubmission>
		extends AbstractContentTest {

	protected U stagesubmission;

	/**
	 * Implementations should return a new blank stage, e.g.:
	 * 
	 * <pre>
	 * return new FillInStage();
	 * </pre>
	 */
	protected abstract T getNewStage();

	/**
	 * Implementations should return a new blank stage submission, e.g.:
	 * 
	 * <pre>
	 * return new FillInSubmission();
	 * </pre>
	 */
	protected abstract U getNewSubmission();

	/**
	 * Prepare testing the stage: Add a stage to the exercise and add a submission for the stage
	 */
	@BeforeEach
	@Override
	protected final void doBeforeTest() {
		super.doBeforeTest();

		exercise.addStage(getNewStage());
		exercise = baseService.merge(exercise);

		stagesubmission = getNewSubmission();
		stagesubmission.setStageId(exercise.getStagesAsList().get(0).getId());
	}

	@Test
	final void changePoints() {
		assertEquals(0, stagesubmission.getPoints());

		stagesubmission.setPoints(50);

		assertEquals(50, stagesubmission.getPoints());
	}

	@Test
	final void changeInternalErrors() {
		assertFalse(stagesubmission.hasInternalErrors());

		stagesubmission.setHasInternalErrors(true);

		assertTrue(stagesubmission.hasInternalErrors());
	}

	@Test
	final void changeHasPendingChecks() {
		assertFalse(stagesubmission.hasPendingChecks());

		stagesubmission.setHasPendingChecks(true);

		assertTrue(stagesubmission.hasPendingChecks());
	}

	@Test
	final void changeManualResult() {
		assertFalse(stagesubmission.isHasManualResult());
		assertFalse(stagesubmission.getManualResult().isPresent());

		stagesubmission.setManualResult(new ManualResult(user));

		assertTrue(stagesubmission.isHasManualResult());
		assertTrue(stagesubmission.getManualResult().isPresent());
	}

}
