package de.uni_due.s3.jack3.business.microservices.evaluatorutils;

import java.util.NoSuchElementException;
import java.util.Optional;

import de.uni_due.s3.evaluator_api.properties.EvaluatorContext;
import de.uni_due.s3.evaluator_api.properties.EvaluatorContextVariable;
import de.uni_due.s3.evaluator_api.properties.EvaluatorDomainType;
import de.uni_due.s3.evaluator_api.properties.EvaluatorVariableType;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps.VariableNotDefinedException;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression.EDomain;
import de.uni_due.s3.openobject.OpenObject;

public class EvaluatorDomainTypeProducer {

	public static EvaluatorDomainType byOpenObject(OpenObject value) {
		return value.getOCOBJ() != null ? EvaluatorDomainType.CHEMISTRY : EvaluatorDomainType.MATHEMATICS;
	}

	public static EvaluatorDomainType byEvaluatorExpression(EvaluatorExpression expression) {
		return expression.getDomain() == EDomain.CHEM ? EvaluatorDomainType.CHEMISTRY : EvaluatorDomainType.MATHEMATICS;
	}

	public static EvaluatorDomainType getFromContextByNameAndType(String name, EvaluatorVariableType type,
			EvaluatorContext context) throws VariableNotDefinedException {
		try {
			return findVariableInContextByNameAndType(name, type, context).orElseThrow().properties.domainType;
		} catch (NoSuchElementException e) {
			throw new VariableNotDefinedException(name, type.toString());
		}
	}

	private static Optional<EvaluatorContextVariable> findVariableInContextByNameAndType(String name,
			EvaluatorVariableType type, EvaluatorContext context) {
		return context.getVariables().stream().filter((v) -> isEqualEvaluatorProperties(name, type, v)).findFirst();
	}

	private static boolean isEqualEvaluatorProperties(String name, EvaluatorVariableType type,
			EvaluatorContextVariable variable) {
		return type.equals(variable.properties.variableType) && name.equals(variable.properties.name);
	}

}
