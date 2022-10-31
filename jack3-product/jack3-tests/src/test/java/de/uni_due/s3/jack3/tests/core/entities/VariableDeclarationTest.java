package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

/**
 * Test class for {@linkplain VariableDeclaration} based on a sample MC-stage.
 *
 * @author lukas.glaser
 *
 */
@NeedsExercise
class VariableDeclarationTest extends AbstractContentTest {

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();
		exercise.addVariable(new VariableDeclaration("var"));
		exercise = baseService.merge(exercise);
	}

	private VariableDeclaration getVariableDeclaration() {
		return exercise.getVariableDeclarations().get(0);
	}

	@Test
	void changeName() {
		assertEquals("var", getVariableDeclaration().getName());
		getVariableDeclaration().setName("variable");
		exercise = baseService.merge(exercise);
		assertEquals("variable", getVariableDeclaration().getName());
	}

	@Test
	void changeInitializationCode() {
		assertNotNull(getVariableDeclaration().getInitializationCode());

		EvaluatorExpression expression = new EvaluatorExpression();
		expression.setCode("1+1");
		getVariableDeclaration().setInitializationCode(expression);
		exercise = baseService.merge(exercise);

		assertEquals("1+1", getVariableDeclaration().getInitializationCode().getCode());
	}

	/**
	 * 
	 * This test checks that a name of variable declaration can not be null.
	 */
	@Test
	void setVariableDeclarationNameToNull() {
		VariableDeclaration varDec = new VariableDeclaration();

		assertThrows(NullPointerException.class, () -> {
			varDec.setName(null);
		});

	}

	/**
	 * This test checks that a name of variable declaration can not be empty.
	 */
	@Test
	void setEmptyVariableDeclarationName() {
		VariableDeclaration varDec = new VariableDeclaration();

		assertThrows(IllegalArgumentException.class, () -> {
			varDec.setName("");
		});
	}

	/*
	 * This test checks the deep copy of variable declaration.
	 */
	@Test
	void deepCopyOfVariableDeclaration() {
		VariableDeclaration originVariableDeclaration;
		VariableDeclaration deepCopyOfVariableDeclaration;

		originVariableDeclaration = new VariableDeclaration("Deep copy test of variable " + "declaration.");
		originVariableDeclaration.setInitializationCode(new EvaluatorExpression("1+1"));

		deepCopyOfVariableDeclaration = originVariableDeclaration.deepCopy();

		assertNotEquals(originVariableDeclaration, deepCopyOfVariableDeclaration,
				"The deep copy is the origin variable declaration itself.");
		assertEquals("Deep copy test of variable declaration.", deepCopyOfVariableDeclaration.getName(),
				"The name of variable declaration is different.");
		assertEquals("1+1", deepCopyOfVariableDeclaration.getInitializationCode().getCode(),
				"The initialization code of variable declaration is different.");
	}

}
