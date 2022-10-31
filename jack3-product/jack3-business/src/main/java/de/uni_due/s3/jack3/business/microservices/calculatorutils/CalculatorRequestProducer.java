package de.uni_due.s3.jack3.business.microservices.calculatorutils;

import java.util.List;

import de.uni_due.s3.evaluator_api.calculator.request.CalculatorRequest;
import de.uni_due.s3.evaluator_api.properties.EvaluatorContext;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps;
import de.uni_due.s3.jack3.business.microservices.evaluatorutils.EvaluatorContextProducer;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;
import de.uni_due.s3.jack3.entities.tenant.VariableUpdate;

public class CalculatorRequestProducer {

	private final CalculatorRequest request = new CalculatorRequest();

	private CalculatorRequestProducer(EvaluatorContext context) {
		this.request.context = context;
	}

	public static CalculatorRequest byVariableDeclarations(List<VariableDeclaration> declarations, EvaluatorMaps maps) {
		return new CalculatorRequestProducer(EvaluatorContextProducer.byEvaluatorMaps(maps))
				.createCalculatorRequestByDeclarations(declarations);
	}

	private CalculatorRequest createCalculatorRequestByDeclarations(List<VariableDeclaration> declarations) {
		declarations.stream().map(CalculatorTaskProducer::byVariableDeclaration).forEach(request::addTask);
		return request;
	}

	public static CalculatorRequest byVariableUpdates(List<VariableUpdate> updates, EvaluatorMaps maps) {
		return new CalculatorRequestProducer(EvaluatorContextProducer.byEvaluatorMaps(maps))
				.createCalculatorRequestByUpdates(updates);
	}

	private CalculatorRequest createCalculatorRequestByUpdates(List<VariableUpdate> updates) {
		updates.stream().map(CalculatorTaskProducer::byVariableUpdate).forEach(request::addTask);
		return request;
	}

	public static CalculatorRequest createBooleanRequest(String name, EvaluatorExpression expression,
			EvaluatorMaps maps) {
		return new CalculatorRequestProducer(EvaluatorContextProducer.byEvaluatorMaps(maps))
				.createCalculatorBooleanRequestBy(name, expression);
	}

	private CalculatorRequest createCalculatorBooleanRequestBy(String name, EvaluatorExpression expression) {
		request.addTask(CalculatorTaskProducer.singleBooleanTaskByEvaluatorExpression(name, expression));
		return request;
	}

	public static CalculatorRequest createEvaluationRequest(String name, EvaluatorExpression expression,
			EvaluatorMaps maps) {
		return new CalculatorRequestProducer(EvaluatorContextProducer.byEvaluatorMaps(maps))
				.createCalculatorEvaluationRequestBy(name, expression);
	}

	private CalculatorRequest createCalculatorEvaluationRequestBy(String name, EvaluatorExpression expression) {
		request.addTask(CalculatorTaskProducer.singleEvaluationTaskByEvaluatorExpression(name, expression));
		return request;
	}
}
