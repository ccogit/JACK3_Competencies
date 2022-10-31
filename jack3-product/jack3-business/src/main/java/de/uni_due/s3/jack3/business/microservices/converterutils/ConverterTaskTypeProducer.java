package de.uni_due.s3.jack3.business.microservices.converterutils;

import de.uni_due.s3.evaluator_api.converter.request.ConverterTaskType;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.Placeholder;

public class ConverterTaskTypeProducer {

	public static ConverterTaskType byPlaceholder(Placeholder placeholder) {
		return placeholder.latexFlag ? ConverterTaskType.LATEX : ConverterTaskType.STRING;
	}

}
