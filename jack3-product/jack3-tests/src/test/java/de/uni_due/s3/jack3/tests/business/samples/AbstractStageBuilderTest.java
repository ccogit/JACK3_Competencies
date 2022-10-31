package de.uni_due.s3.jack3.tests.business.samples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.builders.AbstractStageBuilder;
import de.uni_due.s3.jack3.builders.ExerciseBuilder;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;

public abstract class AbstractStageBuilderTest<T extends Stage, R extends AbstractStageBuilder<T, R>>
		extends AbstractBasicTest {

    protected AbstractExercise exercise;

	protected R stageBuilder;

	/**
	 * Persists User, Folder and Exercise. Before calling this Method the Exercise has to be created
	 */
	protected void persistUserFolderAndExercise() {
		userService.persistUser(user);
		baseService.persist(folder);

		folder.addChildExercise(exercise);
		baseService.persist(exercise);

		folder = folderService.mergeContentFolder(folder);
		folder = folderService.getContentFolderWithLazyData(folder);
	}

	protected abstract R getNewBuilder();

	protected abstract R addAnotherBuilder(ExerciseBuilder exerciseBuilder);

	@Override
	@BeforeEach
	protected void doBeforeTest() {
		super.doBeforeTest();
		stageBuilder = getNewBuilder();
	}

	@Test
	final void defaultSettingsTest() {
		exercise = stageBuilder.and()//
				.create();//

		persistUserFolderAndExercise();

		assertEquals(0, exercise.getStartStage().getHints().size());
		assertFalse(exercise.getStartStage().getAllowSkip());
		assertEquals(1, exercise.getStartStage().getWeight());
	}

	@Test
	final void titleTest() {
		exercise = stageBuilder.withTitle("myTestTitle2000")//
				.and()//
				.create();//

		persistUserFolderAndExercise();

		assertEquals("myTestTitle2000", exercise.getStartStage().getExternalName());

	}

	@Test
	final void descriptionTest() {
		exercise = stageBuilder.withDescription("In this exercise you have to give the right answer.")//
				.and()//
				.create();//

		persistUserFolderAndExercise();

		assertEquals("In this exercise you have to give the right answer.",
				exercise.getStartStage().getTaskDescription());
	}

	@Test
	final void hintsTest() {
		stageBuilder = stageBuilder.withHint("This is a useful Hint!");//
		exercise = addAnotherBuilder(stageBuilder.and()).withHint("My first Hint!")//
				.withHint("My Second Hint!")//
				.withHint("My last Hint.")//
				.and()//
				.create();

		persistUserFolderAndExercise();

		assertEquals(1, exercise.getStartStage().getHints().size());
		assertEquals("This is a useful Hint!", exercise.getStartStage().getHints().get(0).getText());

		assertEquals(3, exercise.getStagesAsList().get(1).getHints().size());
		assertEquals("My first Hint!", exercise.getStagesAsList().get(1).getHints().get(0).getText());
		assertEquals("My Second Hint!", exercise.getStagesAsList().get(1).getHints().get(1).getText());
		assertEquals("My last Hint.", exercise.getStagesAsList().get(1).getHints().get(2).getText());
	}

	@Test
	final void allowSkipTest() {
		stageBuilder = stageBuilder.allowSkip();//
		exercise = addAnotherBuilder(stageBuilder.and()).allowSkip("You Skipped this stage")//
				.and()//
				.create();//

		persistUserFolderAndExercise();

		assertTrue(exercise.getStartStage().getAllowSkip());

		assertTrue(exercise.getStagesAsList().get(1).getAllowSkip());
		assertEquals("You Skipped this stage", exercise.getStagesAsList().get(1).getSkipMessage());
	}

	@Test
	final void WeightTest() {
		exercise = stageBuilder.withWeight(3)//
				.and()//
				.create();//

		persistUserFolderAndExercise();

		assertEquals(3, exercise.getStartStage().getWeight());
	}
}
