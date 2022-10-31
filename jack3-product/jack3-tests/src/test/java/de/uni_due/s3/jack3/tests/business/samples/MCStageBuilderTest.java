package de.uni_due.s3.jack3.tests.business.samples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.builders.ExerciseBuilder;
import de.uni_due.s3.jack3.builders.MCStageBuilder;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;

class MCStageBuilderTest extends AbstractStageBuilderTest<MCStage, MCStageBuilder> {

	@Override
	protected MCStageBuilder getNewBuilder() {
		return new ExerciseBuilder("testName").withMCStage();
	}

	@Override
	protected MCStageBuilder addAnotherBuilder(ExerciseBuilder exerciseBuilder) {
		return exerciseBuilder.withMCStage();
	}

	@Test
	void selectOnlyOneAnswerTest() {
		exercise = stageBuilder.selectOne()//
				.and()//
				.create();//

		persistUserFolderAndExercise();
		assertTrue(((MCStage) exercise.getStartStage()).isSingleChoice());
	}

	@Test
	void randomizedAnswerOrderTest() {
		exercise = stageBuilder.withRandomizedAnswerOrder()//
				.and()//
				.create();//

		persistUserFolderAndExercise();
		assertTrue(((MCStage) exercise.getStartStage()).isRandomize());
	}

	@Test
	void correctFeedbackTest() {
		exercise = stageBuilder.withCorrectFeedback("Jeah that was correct :)")//
				.and()//
				.create();//

		persistUserFolderAndExercise();

		assertEquals("Jeah that was correct :)",
				((MCStage) exercise.getStartStage()).getCorrectAnswerFeedback());
	}

	@Test
	void withDefaultFeedbackTest() {
		exercise = stageBuilder.withDefaultFeedback("Try it again!", 10)//
				.and()//
				.create();//

		persistUserFolderAndExercise();

		assertEquals("Try it again!", ((MCStage) exercise.getStartStage()).getDefaultFeedback());
		assertEquals(10, ((MCStage) exercise.getStartStage()).getDefaultResult());
	}

	@Test
	void answerOptionTest() {
		exercise = stageBuilder.withAnswerOption("Answer A", true)//
				.withAnswerOption("Answer B", false)//
				.withAnswerOption("Answer C", false)//
				.withAnswerOption("Answer D", false)//
				.and()//
				.create();//

		persistUserFolderAndExercise();

		assertEquals(4, ((MCStage) exercise.getStartStage()).getAnswerOptions().size());
		assertEquals("Answer A", ((MCStage) exercise.getStartStage()).getAnswerOptions().get(0).getText());
		assertEquals("Answer B", ((MCStage) exercise.getStartStage()).getAnswerOptions().get(1).getText());
		assertEquals("Answer C", ((MCStage) exercise.getStartStage()).getAnswerOptions().get(2).getText());
		assertEquals("Answer D", ((MCStage) exercise.getStartStage()).getAnswerOptions().get(3).getText());
	}

}
