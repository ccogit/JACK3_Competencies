package de.uni_due.s3.jack3.tests.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCSubmission;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.services.StageSubmissionService;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

/**
 * Tests submission service based on MCSubmission
 */
@NeedsExercise
class StageSubmissionServiceTest extends AbstractContentTest {

	@Inject
	private StageSubmissionService stageSubmissionService;

	/**
	 * Get Stage submission with lazy data
	 */
	@Test
	void getStageSubmissionWithLazyData() {

		// create stage
		exercise.addStage(new MCStage());
		exercise = baseService.merge(exercise);
		Stage stage = exercise	.getStagesAsList()
								.get(0);

		// create submission
		StageSubmission stagesubmission = new MCSubmission();
		stagesubmission.setStageId(stage.getId());
		stageSubmissionService.persistStageSubmission(stagesubmission);
		stageSubmissionService.mergeStageSubmission(stagesubmission);

		// get submission
		StageSubmission stagesubmissionFromDB = stageSubmissionService	.getStageSubmissionWithLazyData(
				stagesubmission.getId())
																	.orElseThrow(AssertionError::new);

		assertEquals(stagesubmission, stagesubmissionFromDB);
		assertTrue(stagesubmissionFromDB.getResults().isEmpty());
	}
}
