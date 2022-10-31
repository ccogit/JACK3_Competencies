package de.uni_due.s3.jack3.business.microservices.calculatorutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.evaluator_api.calculator.request.CalculatorRequest;
import de.uni_due.s3.evaluator_api.calculator.request.CalculatorTaskType;
import de.uni_due.s3.evaluator_api.properties.EvaluatorDomainType;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression.EDomain;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;

public class CalculatorRequestProducerTest {

	@Test
	public void testEmpty() {
		CalculatorRequest request = CalculatorRequestProducer.byVariableDeclarations(List.of(), new EvaluatorMaps());
		assertEquals(0, request.getTasks().size());
	}

	@Test
	public void test1() {
		VariableDeclaration d = new VariableDeclaration();
		d.setInitializationCode(new EvaluatorExpression("x+1"));
		d.setName("name3");

		CalculatorRequest request = CalculatorRequestProducer.byVariableDeclarations(List.of(d), new EvaluatorMaps());
		assertEquals(1, request.getTasks().size());
		assertEquals("x+1", request.getTasks().get(0).expression);
		assertEquals("name3", request.getTasks().get(0).properties.name);
		assertEquals(EvaluatorDomainType.MATHEMATICS, request.getTasks().get(0).properties.domainType);
		assertEquals(CalculatorTaskType.EVALUATE, request.getTasks().get(0).type);

	}

	@Test
	public void test2() {
		VariableDeclaration d = new VariableDeclaration();
		d.setInitializationCode(new EvaluatorExpression("x+1"));
		d.setName("name3");

		VariableDeclaration e = new VariableDeclaration();
		e.setInitializationCode(new EvaluatorExpression("x+x"));
		e.setName("name6");
		e.getInitializationCode().setDomain(EDomain.CHEM);

		CalculatorRequest request = CalculatorRequestProducer.byVariableDeclarations(List.of(d, e),
				new EvaluatorMaps());
		assertEquals(2, request.getTasks().size());
		assertEquals("x+1", request.getTasks().get(0).expression);
		assertEquals("name3", request.getTasks().get(0).properties.name);
		assertEquals(EvaluatorDomainType.MATHEMATICS, request.getTasks().get(0).properties.domainType);
		assertEquals(CalculatorTaskType.EVALUATE, request.getTasks().get(0).type);

		assertEquals("x+x", request.getTasks().get(1).expression);
		assertEquals("name6", request.getTasks().get(1).properties.name);
		assertEquals(EvaluatorDomainType.CHEMISTRY, request.getTasks().get(1).properties.domainType);
		assertEquals(CalculatorTaskType.EVALUATE, request.getTasks().get(1).type);

	}

}
