package de.uni_due.s3.jack3.tests.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCSubmission;
import de.uni_due.s3.jack3.entities.tenant.Result;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.services.ResultService;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

@NeedsExercise
class ResultServiceTest extends AbstractContentTest {

	@Inject
	private ResultService resultService;

	private Stage stage;
	private StageSubmission stagesubmission;

	/**
	 * Create stage and stage submission
	 */
	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();

		// create stage
		exercise.addStage(new MCStage());
		exercise = baseService.merge(exercise);
		stage = exercise.getStagesAsList().get(0);

		// create submission
		stagesubmission = new MCSubmission();
		stagesubmission.setStageId(stage.getId());
		baseService.persist(stagesubmission);
	}

	/**
	 * Persist new result
	 */
	@Test
	void persistResult() {
		Result result = new Result(stagesubmission);
		resultService.persistResult(result);

		// get result service
		var queryResponse = baseService.findAll(Result.class);
		assertEquals(1, queryResponse.size());
		assertEquals(result, queryResponse.get(0));
	}

}
