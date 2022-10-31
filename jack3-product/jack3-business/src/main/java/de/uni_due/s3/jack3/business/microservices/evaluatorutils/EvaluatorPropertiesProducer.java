package de.uni_due.s3.jack3.business.microservices.evaluatorutils;

import de.uni_due.s3.evaluator_api.properties.EvaluatorContext;
import de.uni_due.s3.evaluator_api.properties.EvaluatorDomainType;
import de.uni_due.s3.evaluator_api.properties.EvaluatorProperties;
import de.uni_due.s3.evaluator_api.properties.EvaluatorVariableType;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps.VariableNotDefinedException;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.Placeholder;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.openobject.OpenObject;

public class EvaluatorPropertiesProducer {

	public static EvaluatorProperties byPlaceholder(Placeholder placeholder, EvaluatorContext context)
			throws VariableNotDefinedException {
		EvaluatorVariableType variableType = EvaluatorVariableTypeProducer.byPlaceholder(placeholder);
		return byInput(placeholder.name, variableType, context);
	}

	public static EvaluatorProperties byInput(String name, EvaluatorVariableType type, EvaluatorContext context)
			throws VariableNotDefinedException {
		EvaluatorDomainType domainType = EvaluatorDomainTypeProducer.getFromContextByNameAndType(name, type, context);
		return new EvaluatorProperties(name, type, domainType);
	}

	public static EvaluatorProperties byOpenObject(String name, EvaluatorVariableType variableType, OpenObject value) {
		EvaluatorDomainType domainType = EvaluatorDomainTypeProducer.byOpenObject(value);
		return new EvaluatorProperties(name, variableType, domainType);
	}

	public static EvaluatorProperties forEvaluatorExpression(String name, EvaluatorVariableType variableType,
			EvaluatorExpression expression) {
		EvaluatorDomainType domainType = EvaluatorDomainTypeProducer.byEvaluatorExpression(expression);
		return new EvaluatorProperties(name, variableType, domainType);
	}

}
