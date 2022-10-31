package de.uni_due.s3.jack3.tests.business.samples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.builders.ExerciseBuilder;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression.EDomain;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;
import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;

class ExerciseBuilderTest extends AbstractBasicTest {

    private AbstractExercise exercise;

	/**
	 * Persists User, Folder and Exercise. Before calling this Method the Exercise has to be created
	 */
	private void persistUserFolderAndExercise() {
		userService.persistUser(user);
		baseService.persist(folder);

		folder.addChildExercise(exercise);
		baseService.persist(exercise);

		folder = folderService.mergeContentFolder(folder);
		folder = folderService.getContentFolderWithLazyData(folder);
	}

	@Test
	void constructorTest() {

		exercise = new ExerciseBuilder("nameOfTheExercise") //
				.create(); //

		persistUserFolderAndExercise();

		assertEquals("nameOfTheExercise", exercise.getName());
	}

	@Test
	void variableDeclarationTest() {
		exercise = new ExerciseBuilder("Jack") //
				.withVariableDeclaration("number", "randomIntegerBetween(1,4)") //
				.withVariableDeclaration("anotherNumber", "constpi()") //
				.withVariableDeclaration("lastNumber", "randomIntegerBetween(0,10)", EDomain.MATH) //
				.withRandomVariableDeclaration("random", 3, 4) //
				.create();

		persistUserFolderAndExercise();

		final List<VariableDeclaration> variableDeclarations = exercise.getVariableDeclarations();
		assertEquals(4, variableDeclarations.size());

		assertEquals("number", variableDeclarations.get(0).getName());
		assertEquals("randomIntegerBetween(1,4)",
				variableDeclarations.get(0).getInitializationCode().getCode());

		assertEquals("anotherNumber", variableDeclarations.get(1).getName());
		assertEquals("constpi()",
				variableDeclarations.get(1).getInitializationCode().getCode());

		assertEquals("lastNumber", variableDeclarations.get(2).getName());
		assertEquals("randomIntegerBetween(0,10)",
				variableDeclarations.get(2).getInitializationCode().getCode());
		assertEquals(EDomain.MATH, variableDeclarations.get(2).getInitializationCode().getDomain());

		assertEquals("random", variableDeclarations.get(3).getName());
		assertEquals("randomIntegerBetween(3, 4)",
				variableDeclarations.get(3).getInitializationCode().getCode());
	}

	@Test
	void descriptionTest() {
		exercise = new ExerciseBuilder("Jack")
				.withPublicDescription("Welcome to this Exercise :)")
				.create();

		persistUserFolderAndExercise();

		assertEquals("Welcome to this Exercise :)", exercise.getPublicDescription());
	}

	@Test
	void difficultyTest() {
		exercise = new ExerciseBuilder("Jack") //
				.withDifficulty(5) //
				.create(); //

		persistUserFolderAndExercise();

		assertEquals(5, exercise.getDifficulty());
	}

	@Test
	void withMCStageTest() {
		exercise = new ExerciseBuilder("Jack") //
				.withMCStage() //
				.and() //
				.withMCStage() //
				.and() //
				.create(); //

		persistUserFolderAndExercise();

		assertEquals(2, exercise.getStages().size());
		for (Stage stage : exercise.getStages()) {
			assertTrue(stage instanceof MCStage);
		}
	}

	@Test
	void withFillInStagTest() {
		exercise = new ExerciseBuilder("Jack").withFillInStage()
																			.and()
																			.withFillInStage()
																			.and()
																			.create();

		persistUserFolderAndExercise();

		assertEquals(2, exercise.getStages().size());
		for (Stage stage : exercise.getStages()) {
			assertTrue(stage instanceof FillInStage);
		}
	}

	@Test
	void StagesInCorrectOrderTest() {
		exercise = new ExerciseBuilder("Jack").withFillInStage()
																			.withTitle("Stage1")
																			.and()
																			.withFillInStage()
																			.withTitle("Stage2")
																			.and()
																			.withMCStage()
																			.withTitle("Stage3")
																			.and()
																			.create();

		persistUserFolderAndExercise();

		assertEquals(3, exercise.getStages().size());

		assertEquals("Stage1", exercise.getStartStage().getExternalName());
		assertEquals(0, exercise.getStartStage().getOrderIndex());

		assertEquals("Stage2", exercise.getStartStage().getDefaultTransition().getTarget().getExternalName());
		assertEquals(1, exercise.getStartStage().getDefaultTransition().getTarget().getOrderIndex());

		assertEquals("Stage3", exercise.getStartStage().getDefaultTransition().getTarget().getDefaultTransition()
				.getTarget().getExternalName());
		assertEquals(2, exercise.getStartStage().getDefaultTransition().getTarget().getDefaultTransition()
				.getTarget().getOrderIndex());

		assertNull(exercise.getStartStage().getDefaultTransition().getTarget().getDefaultTransition().getTarget()
				.getDefaultTransition().getTarget());
	}

}
