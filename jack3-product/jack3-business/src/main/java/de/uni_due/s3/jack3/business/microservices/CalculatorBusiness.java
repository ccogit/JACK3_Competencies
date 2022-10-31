package de.uni_due.s3.jack3.business.microservices;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import de.uni_due.s3.evaluator_api.calculator.request.CalculatorRequest;
import de.uni_due.s3.evaluator_api.calculator.response.CalculatorResponse;
import de.uni_due.s3.jack3.business.AbstractBusiness;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.CalculatorRequestProducer;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.CalculatorResponseBooleanizeHandler;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.CalculatorResponseEvaluationHandler;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.InternalErrorEvaluatorException;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;
import de.uni_due.s3.jack3.entities.tenant.VariableUpdate;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;

@ApplicationScoped
public class CalculatorBusiness extends AbstractBusiness {

	@Inject
	private EvaluatorBusiness eurekaBusiness;

	public Map<String, VariableValue> initVariables(List<VariableDeclaration> variableDeclarations)
			throws InternalErrorEvaluatorException {
		return calculateToEvaluationMap(
				CalculatorRequestProducer.byVariableDeclarations(variableDeclarations, new EvaluatorMaps()));
	}

	public Map<String, VariableValue> variableUpdate(List<VariableUpdate> variableUpdates, EvaluatorMaps maps)
			throws InternalErrorEvaluatorException {
		return calculateToEvaluationMap(CalculatorRequestProducer.byVariableUpdates(variableUpdates, maps));
	}

	public boolean evaluateStageTransitionExpression(StageTransition transition, EvaluatorMaps maps)
			throws InternalErrorEvaluatorException {
		return booleanizeTransitionExpression(transition.getStageExpression(), maps);
	}

	public boolean evaluateStageTransitionCondition(StageTransition transition, EvaluatorMaps maps)
			throws InternalErrorEvaluatorException {
		return booleanizeTransitionExpression(transition.getConditionExpression(), maps);
	}

	private boolean booleanizeTransitionExpression(EvaluatorExpression expression, EvaluatorMaps maps)
			throws InternalErrorEvaluatorException {
		if (expression.isEmpty()) {
			return true; // empty conditions are considered true, since a transition without condition is always active
		}
		return calculateToBoolean(expression, maps);
	}

	public boolean calculateToBoolean(EvaluatorExpression expression, EvaluatorMaps maps)
			throws InternalErrorEvaluatorException {
		String name = "single";
		CalculatorRequest request = CalculatorRequestProducer.createBooleanRequest(name, expression, maps);
		return calculateToBooleanMap(request).get(name);
	}

	private Map<String, Boolean> calculateToBooleanMap(CalculatorRequest request)
			throws InternalErrorEvaluatorException {
		CalculatorResponse response = calculate(request);
		return CalculatorResponseBooleanizeHandler.convertToBooleanizedMap(response);
	}

	public VariableValue calculateToVariableValue(EvaluatorExpression expression, EvaluatorMaps maps)
			throws InternalErrorEvaluatorException {
		String name = "single";
		CalculatorRequest request = CalculatorRequestProducer.createEvaluationRequest(name, expression, maps);
		return calculateToEvaluationMap(request).get(name);
	}

	private Map<String, VariableValue> calculateToEvaluationMap(CalculatorRequest request)
			throws InternalErrorEvaluatorException {
		CalculatorResponse response = calculate(request);
		return CalculatorResponseEvaluationHandler.convertToEvaluatedMap(response);
	}

	private CalculatorResponse calculate(CalculatorRequest request) {
		if (request.getTasks().isEmpty()) {
			return new CalculatorResponse();
		}
		return eurekaBusiness.calculate(request);
	}
}
