package de.uni_due.s3.jack3.business.microservices.converterutils;

import de.uni_due.s3.evaluator_api.converter.properties.ConverterFormatProperties;
import de.uni_due.s3.evaluator_api.converter.request.ConverterTask;
import de.uni_due.s3.evaluator_api.converter.request.ConverterTaskType;
import de.uni_due.s3.evaluator_api.properties.EvaluatorContext;
import de.uni_due.s3.evaluator_api.properties.EvaluatorProperties;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps.VariableNotDefinedException;
import de.uni_due.s3.jack3.business.microservices.evaluatorutils.EvaluatorPropertiesProducer;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.Placeholder;

public class ConverterTaskProducer {

	public static ConverterTask byPlaceholder(Placeholder placeholder, EvaluatorContext context)
			throws VariableNotDefinedException {
		EvaluatorProperties properties = EvaluatorPropertiesProducer.byPlaceholder(placeholder, context);
		ConverterTaskType taskType = ConverterTaskTypeProducer.byPlaceholder(placeholder);
		ConverterFormatProperties formatProperties = ConverterFormatPropertiesProducer.byPlaceholder(placeholder);
		return new ConverterTask(properties, taskType, formatProperties);
	}

}
