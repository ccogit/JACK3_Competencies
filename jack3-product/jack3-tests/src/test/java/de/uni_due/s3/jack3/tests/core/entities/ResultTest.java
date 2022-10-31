package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCSubmission;
import de.uni_due.s3.jack3.entities.tenant.FeedbackMessage;
import de.uni_due.s3.jack3.entities.tenant.Result;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

/**
 * Test class for {@link Result}, based on a MC-submission.
 *
 * @author lukas.glaser
 *
 */
@NeedsExercise
class ResultTest extends AbstractContentTest {

	private StageSubmission stagesubmission;

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();

		exercise.addStage(new MCStage());
		exercise = baseService.merge(exercise);

		stagesubmission = new MCSubmission();
		stagesubmission.setStageId(exercise.getStagesAsList().get(0).getId());
		baseService.persist(stagesubmission);

		Result result = new Result(stagesubmission);
		baseService.persist(result);

		stagesubmission.addResult(result);
		stagesubmission = baseService.merge(stagesubmission);
	}

	private Result getResult() {
		return stagesubmission.getResults().iterator().next();
	}

	@Test
	void getSubmission() {
		assertEquals(stagesubmission, getResult().getSubmission());
	}

	@Test
	void changePoints() {
		getResult().setPoints(75);
		baseService.merge(getResult());

		assertEquals(75, getResult().getPoints());
	}

	@Test
	void changePublicComment() {
		getResult().setPublicComment("This is a public comment.");
		baseService.merge(getResult());

		assertEquals("This is a public comment.", getResult().getPublicComment());
	}

	@Test
	void changeInternalComment() {
		getResult().setInternalComment("This is an internal comment.");
		baseService.merge(getResult());

		assertEquals("This is an internal comment.", getResult().getInternalComment());
	}

	@Test
	public void addFeedbackMessage() {
		getResult().addFeedbackMessage(new FeedbackMessage());
		baseService.merge(getResult());

		assertEquals(1, getResult().getFeedbackMessages().size());
	}
}
