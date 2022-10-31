package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.tenant.ExerciseResource;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageResource;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

/**
 * Tests for stage resources, using a MultipleChoice stage
 */
@NeedsExercise
class StageResourceTest extends AbstractContentTest {

	private StageResource getStageResource() {
		return exercise.getStagesAsList().get(0).getStageResources().get(0);
	}

	private Stage getStage() {
		return exercise.getStagesAsList().get(0);
	}

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();
		Stage stage = new MCStage();
		exercise.addStage(stage);

		ExerciseResource exResource = new ExerciseResource("Resource", "Content".getBytes(), user, "", false);
		exercise.addExerciseResource(exResource);
		StageResource stageResource = new StageResource(exResource);
		stage.addStageResource(stageResource);
		exercise = baseService.merge(exercise);
	}

	@Test
	void changeDescription() {
		getStageResource().setDescription("Hello World!");
		exercise = baseService.merge(exercise);
		assertEquals("Hello World!", getStageResource().getDescription());
	}

	@Test
	void removeStageResource() {
		getStage().removeStageResource(getStageResource());
		exercise = baseService.merge(exercise);
		assertThrows(IndexOutOfBoundsException.class, () -> {
			getStageResource();
		});
	}

	/**
	 * This test checks the deepCopy of a comment.
	 */
	@Test
	void deepCopy() {
		StageResource stageResource = getStageResource();
		StageResource deepCopyOfStageResource = new StageResource();

		stageResource.setDescription("Deep-Copy Test");
		deepCopyOfStageResource = stageResource.deepCopy();

		assertNotEquals(stageResource, deepCopyOfStageResource, "The resource is the origin itself.");
		assertEquals(stageResource.getExerciseResource().getFilename(),
				deepCopyOfStageResource.getExerciseResource().getFilename(),
				"The filename of the resources are different.");
		assertEquals(stageResource.getExerciseResource().getDescription(),
				deepCopyOfStageResource.getExerciseResource().getDescription(),
				"The exercise description of the resources are different.");
		assertEquals(stageResource.getExerciseResource().getSize(),
				deepCopyOfStageResource.getExerciseResource().getSize(),
				"The exercise size of the resources are different.");
		assertEquals(stageResource.getExerciseResource().getMediaType(),
				deepCopyOfStageResource.getExerciseResource().getMediaType(),
				"The exercise media type of the resources are different.");
		assertEquals(stageResource.getExerciseResource().getMimeType(),
				deepCopyOfStageResource.getExerciseResource().getMimeType(),
				"The exercise mime type of the resources are different.");
		assertEquals(stageResource.getDescription(), deepCopyOfStageResource.getDescription(),
				"The description of the resources are different.");
	}

}
