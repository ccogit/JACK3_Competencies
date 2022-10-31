package de.uni_due.s3.jack3.business.microservices.evaluatorutils;

import de.uni_due.s3.evaluator_api.properties.EvaluatorContext;
import de.uni_due.s3.evaluator_api.properties.EvaluatorContextVariable;
import de.uni_due.s3.evaluator_api.properties.EvaluatorDomainType;
import de.uni_due.s3.evaluator_api.properties.EvaluatorProperties;
import de.uni_due.s3.evaluator_api.properties.EvaluatorVariableType;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps;
import de.uni_due.s3.jack3.business.microservices.openobjectutils.OpenObjectConverter;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.openobject.OpenObject;

public class EvaluatorContextProducer {

	private final EvaluatorContext context = new EvaluatorContext();

	public static EvaluatorContext byEvaluatorMaps(EvaluatorMaps maps) {
		return new EvaluatorContextProducer().createByEvaluatorMaps(maps);
	}

	private EvaluatorContext createByEvaluatorMaps(EvaluatorMaps maps) {
		addAllExerciseVariables(maps);
		addAllInputVariables(maps);
		addAllMetaVariables(maps);
		addAllCheckVariables(maps);
		return context;
	}

	private void addAllExerciseVariables(EvaluatorMaps maps) {
		maps.getExerciseVariableMap().forEach(this::addExerciseVariable);
	}

	private void addExerciseVariable(String name, VariableValue value) {
		addEvaluatorContextVariable(name, value, EvaluatorVariableType.VAR);
	}

	private void addAllInputVariables(EvaluatorMaps maps) {
		maps.getInputVariableMap().forEach(this::addInputVariable);
	}

	private void addInputVariable(String name, VariableValue value) {
		addEvaluatorContextVariable(name, value, EvaluatorVariableType.INPUT);
	}

	private void addAllMetaVariables(EvaluatorMaps maps) {
		maps.getMetaVariableMap().forEach(this::addMetaVariable);
	}

	private void addMetaVariable(String name, VariableValue value) {
		addEvaluatorContextVariable(name, value, EvaluatorVariableType.META);
	}

	private void addAllCheckVariables(EvaluatorMaps maps) {
		maps.getCheckVariableMap().forEach(this::addCheckVariable);
	}

	private void addCheckVariable(String name, VariableValue value) {
		addEvaluatorContextVariable(name, value, EvaluatorVariableType.CHECK);
	}

	private void addEvaluatorContextVariable(String name, VariableValue value, EvaluatorVariableType variableType) {
		OpenObject oo = OpenObjectConverter.fromVariableValue(value);
		EvaluatorDomainType domainType = EvaluatorDomainTypeProducer.byOpenObject(oo);
		EvaluatorProperties properties = new EvaluatorProperties(name, variableType, domainType);
		EvaluatorContextVariable variable = new EvaluatorContextVariable(properties, oo);
		context.addVariable(variable);
	}

}
