package de.uni_due.s3.jack3.business.microservices.calculatorutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.evaluator_api.calculator.request.CalculatorTask;
import de.uni_due.s3.evaluator_api.calculator.request.CalculatorTaskType;
import de.uni_due.s3.evaluator_api.properties.EvaluatorDomainType;
import de.uni_due.s3.evaluator_api.properties.EvaluatorVariableType;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression.EDomain;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;
import de.uni_due.s3.jack3.entities.tenant.VariableUpdate;

public class CalculatorTaskProducerTest {

	@Test
	public void testVariableDeclaration() {
		VariableDeclaration d = new VariableDeclaration("name5");
		d.setInitializationCode(new EvaluatorExpression("H"));
		d.getInitializationCode().setDomain(EDomain.CHEM);
		CalculatorTask task = CalculatorTaskProducer.byVariableDeclaration(d);

		assertEquals("H", task.expression);
		assertEquals(CalculatorTaskType.EVALUATE, task.type);
		assertEquals("name5", task.properties.name);
		assertEquals(EvaluatorVariableType.VAR, task.properties.variableType);
		assertEquals(EvaluatorDomainType.CHEMISTRY, task.properties.domainType);
	}

	@Test
	public void testVariableUpdate() {
		VariableDeclaration d = new VariableDeclaration("name1");
		VariableUpdate u = new VariableUpdate();
		u.setUpdateCode(new EvaluatorExpression("x+1"));
		u.setVariableReference(d);

		CalculatorTask task = CalculatorTaskProducer.byVariableUpdate(u);

		assertEquals("x+1", task.expression);
		assertEquals(CalculatorTaskType.EVALUATE, task.type);
		assertEquals("name1", task.properties.name);
		assertEquals(EvaluatorVariableType.VAR, task.properties.variableType);
		assertEquals(EvaluatorDomainType.MATHEMATICS, task.properties.domainType);
	}

	@Test
	public void testSingleBoolean() {
		CalculatorTask task = CalculatorTaskProducer.singleBooleanTaskByEvaluatorExpression("singleBool",
				new EvaluatorExpression("x*x"));

		assertEquals("x*x", task.expression);
		assertEquals(CalculatorTaskType.BOOLEAN, task.type);
		assertEquals("singleBool", task.properties.name);
		assertEquals(EvaluatorVariableType.NONE, task.properties.variableType);
		assertEquals(EvaluatorDomainType.MATHEMATICS, task.properties.domainType);
	}

	@Test
	public void testSingleEvaluate() {
		CalculatorTask task = CalculatorTaskProducer.singleEvaluationTaskByEvaluatorExpression("singleBool",
				new EvaluatorExpression("x*x"));

		assertEquals("x*x", task.expression);
		assertEquals(CalculatorTaskType.EVALUATE, task.type);
		assertEquals("singleBool", task.properties.name);
		assertEquals(EvaluatorVariableType.VAR, task.properties.variableType);
		assertEquals(EvaluatorDomainType.MATHEMATICS, task.properties.domainType);
	}

	@Test
	public void testCommentsEvaluate() {
		CalculatorTask task = CalculatorTaskProducer.singleEvaluationTaskByEvaluatorExpression("name1",
				new EvaluatorExpression("1+1\n // Comment 1 2 3 \n  +4+5 \n // 3+4"));

		assertEquals("1+1\n  +4+5 ", task.expression);
		assertEquals(CalculatorTaskType.EVALUATE, task.type);
		assertEquals("name1", task.properties.name);
		assertEquals(EvaluatorVariableType.VAR, task.properties.variableType);
		assertEquals(EvaluatorDomainType.MATHEMATICS, task.properties.domainType);
	}
}
