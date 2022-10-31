package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;
import de.uni_due.s3.jack3.entities.tenant.VariableUpdate;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

/**
 * Test class for {@linkplain VariableUpdate} based on a sample MC-stage.
 *
 * @author lukas.glaser
 *
 */
@NeedsExercise
class VariableUpdateTest extends AbstractContentTest {

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();
		exercise.addVariable(new VariableDeclaration("var1"));

		MCStage stage = new MCStage();
		stage.addVariableUpdateBeforeCheck(new VariableUpdate());
		stage.addVariableUpdateAfterCheck(new VariableUpdate());
		stage.addVariableUpdateOnNormalExit(new VariableUpdate());
		stage.addVariableUpdateOnRepeat(new VariableUpdate());
		stage.addVariableUpdateOnSkip(new VariableUpdate());
		exercise.addStage(stage);

		exercise = baseService.merge(exercise);
	}


	@Test
	void changeUpdateCodeBeforeCheck() {
		EvaluatorExpression expression = new EvaluatorExpression();
		expression.setCode("1+1");
		exercise.getStagesAsList().get(0).getVariableUpdatesBeforeCheck().get(0).setUpdateCode(expression);

		exercise = baseService.merge(exercise);
		assertEquals("1+1",
				exercise.getStagesAsList().get(0).getVariableUpdatesBeforeCheck().get(0).getUpdateCode().getCode());
	}

	@Test
	void getVariableReferenceBeforeCheck() {
		exercise.getStagesAsList().get(0).getVariableUpdatesBeforeCheck().get(0).setVariableReference(exercise.getVariableDeclarations().get(0));
		exercise = baseService.merge(exercise);

		assertEquals("var1", exercise.getStagesAsList().get(0).getVariableUpdatesBeforeCheck().get(0)
				.getVariableReference().getName());
	}

	@Test
	void changeUpdateCodeAfterCheck() {
		EvaluatorExpression expression = new EvaluatorExpression();
		expression.setCode("1+1");
		exercise.getStagesAsList().get(0).getVariableUpdatesAfterCheck().get(0).setUpdateCode(expression);

		exercise = baseService.merge(exercise);
		assertEquals("1+1",
				exercise.getStagesAsList().get(0).getVariableUpdatesAfterCheck().get(0).getUpdateCode().getCode());
	}

	@Test
	void getVariableReferenceAfterCheck() {
		exercise.getStagesAsList().get(0).getVariableUpdatesAfterCheck().get(0)
		.setVariableReference(exercise.getVariableDeclarations().get(0));
		exercise = baseService.merge(exercise);

		assertEquals("var1", exercise.getStagesAsList().get(0).getVariableUpdatesAfterCheck().get(0)
				.getVariableReference().getName());
	}

	@Test
	void changeUpdateCodeOnNormalExit() {
		EvaluatorExpression expression = new EvaluatorExpression();
		expression.setCode("1+1");
		exercise.getStagesAsList().get(0).getVariableUpdatesOnNormalExit().get(0).setUpdateCode(expression);

		exercise = baseService.merge(exercise);
		assertEquals("1+1",
				exercise.getStagesAsList().get(0).getVariableUpdatesOnNormalExit().get(0).getUpdateCode().getCode());
	}

	@Test
	void getVariableReferenceOnNormalExit() {
		exercise.getStagesAsList().get(0).getVariableUpdatesOnNormalExit().get(0)
		.setVariableReference(exercise.getVariableDeclarations().get(0));
		exercise = baseService.merge(exercise);

		assertEquals("var1", exercise.getStagesAsList().get(0).getVariableUpdatesOnNormalExit().get(0)
				.getVariableReference().getName());
	}

	@Test
	void changeUpdateCodeOnRepeat() {
		EvaluatorExpression expression = new EvaluatorExpression();
		expression.setCode("1+1");
		exercise.getStagesAsList().get(0).getVariableUpdatesOnRepeat().get(0).setUpdateCode(expression);

		exercise = baseService.merge(exercise);
		assertEquals("1+1",
				exercise.getStagesAsList().get(0).getVariableUpdatesOnRepeat().get(0).getUpdateCode().getCode());
	}

	@Test
	void getVariableReferenceOnRepeat() {
		exercise.getStagesAsList().get(0).getVariableUpdatesOnRepeat().get(0)
		.setVariableReference(exercise.getVariableDeclarations().get(0));
		exercise = baseService.merge(exercise);

		assertEquals("var1",
				exercise.getStagesAsList().get(0).getVariableUpdatesOnRepeat().get(0).getVariableReference().getName());
	}

	@Test
	void changeUpdateCodeOnSkip() {
		EvaluatorExpression expression = new EvaluatorExpression();
		expression.setCode("1+1");
		exercise.getStagesAsList().get(0).getVariableUpdatesOnSkip().get(0).setUpdateCode(expression);

		exercise = baseService.merge(exercise);
		assertEquals("1+1",
				exercise.getStagesAsList().get(0).getVariableUpdatesOnSkip().get(0).getUpdateCode().getCode());
	}

	@Test
	void getVariableReferenceOnSkip() {
		exercise.getStagesAsList().get(0).getVariableUpdatesOnSkip().get(0)
		.setVariableReference(exercise.getVariableDeclarations().get(0));
		exercise = baseService.merge(exercise);

		assertEquals("var1",
				exercise.getStagesAsList().get(0).getVariableUpdatesOnSkip().get(0).getVariableReference().getName());
	}

	/**
	 * 
	 * This test checks that a reference of variable update can not be null.
	 */
	@Test
	void setVariableReferenceToNull() {
		VariableUpdate varUpdate = new VariableUpdate();

		assertThrows(IllegalArgumentException.class, () -> {
			varUpdate.setVariableReference(null);
		});
	}

	/*
	 * This test checks the deep copy of variable update.
	 */
	@Test
	void deepCopyOfVariableUpdate() {
		VariableUpdate originVariableUpdate;
		VariableUpdate deepCopyOfVariableUpdate;
		VariableDeclaration varDeclaration;

		varDeclaration = new VariableDeclaration("Deep copy test of " + "variable update.");
		varDeclaration.setInitializationCode(new EvaluatorExpression("1=2"));
		originVariableUpdate = new VariableUpdate(varDeclaration);
		originVariableUpdate.setUpdateCode(new EvaluatorExpression("1==2"));

		deepCopyOfVariableUpdate = originVariableUpdate.deepCopy();

		assertNotEquals(originVariableUpdate, deepCopyOfVariableUpdate,
				"The deep copy is the origin variable update itself.");
		assertEquals("Deep copy test of variable update.", deepCopyOfVariableUpdate.getVariableReference().getName(),
				"The name of referenced variable declaration is different.");
		assertEquals("1=2", deepCopyOfVariableUpdate.getVariableReference().getInitializationCode().getCode(),
				"The initialization code of referenced variable declaration is different.");
		assertEquals("1==2", deepCopyOfVariableUpdate.getUpdateCode().getCode(),
				"The update code of variable update is different");
	}

}
