package de.uni_due.s3.jack3.tests.core.stagetypes.mc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInSubmission;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCSubmission;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.tests.utils.AbstractStageSubmissionTest;

public class MCSubmissionTest extends AbstractStageSubmissionTest<MCStage, MCSubmission> { // NOSONAR Javadoc reference

	@Override
	protected MCStage getNewStage() {
		return new MCStage();
	}

	@Override
	protected MCSubmission getNewSubmission() {
		return new MCSubmission();
	}

	@Test
	void changeTickedPattern() {
		assertNull(stagesubmission.getTickedPattern());

		stagesubmission.setTickedPattern("0100");
		assertEquals("0100", stagesubmission.getTickedPattern());
	}

	@Test
	void testOptionsOrder() {
		assertTrue(stagesubmission.getOptionsOrder().isEmpty());

		stagesubmission.addOptionsOrder(Arrays.asList(0, 2, 3, 1));
		assertEquals(Arrays.asList(0, 2, 3, 1), stagesubmission.getOptionsOrder());
	}

	@Test
	void testExceptionInCopyFromStageSubmission() {
		StageSubmission wrongStageSubmission = new FillInSubmission();
		wrongStageSubmission.setStageId(exercise.getStagesAsList().get(0).getId());

		assertThrows(IllegalArgumentException.class, () -> {
			stagesubmission.copyFromStageSubmission(wrongStageSubmission);
		});
	}

	@Test
	void testCopyFromStageSubmission() {
		MCSubmission other = getNewSubmission();
		other.setStageId(exercise.getStagesAsList().get(0).getId());
		other.setTickedPattern("0001");
		other.addOptionsOrder(Arrays.asList(3, 1, 2, 0));

		stagesubmission.copyFromStageSubmission(other);
		assertEquals(other.getTickedPattern(), stagesubmission.getTickedPattern());
		assertEquals(other.getOptionsOrder(), stagesubmission.getOptionsOrder());
	}
}
