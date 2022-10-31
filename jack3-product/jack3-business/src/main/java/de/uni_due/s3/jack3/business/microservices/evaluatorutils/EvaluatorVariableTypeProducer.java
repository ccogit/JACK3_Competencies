package de.uni_due.s3.jack3.business.microservices.evaluatorutils;

import de.uni_due.s3.evaluator_api.properties.EvaluatorVariableType;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.Placeholder;

public class EvaluatorVariableTypeProducer {

	public static EvaluatorVariableType byPlaceholder(Placeholder placeholder) {
		return byInputString(placeholder.typeIdentifier);
	}

	private static EvaluatorVariableType byInputString(String type) {
		if ("var".equals(type))
			return EvaluatorVariableType.VAR;
		else if ("input".equals(type))
			return EvaluatorVariableType.INPUT;
		else if ("meta".equals(type))
			return EvaluatorVariableType.META;
		else if ("check".equals(type))
			return EvaluatorVariableType.CHECK;
		else
			return EvaluatorVariableType.NONE;

	}
}
