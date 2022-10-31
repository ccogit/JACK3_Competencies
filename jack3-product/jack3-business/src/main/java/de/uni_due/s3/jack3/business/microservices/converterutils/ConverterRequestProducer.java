package de.uni_due.s3.jack3.business.microservices.converterutils;

import java.util.ArrayList;
import java.util.List;

import de.uni_due.s3.evaluator_api.converter.request.ConverterRequest;
import de.uni_due.s3.evaluator_api.properties.EvaluatorContext;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps.VariableNotDefinedException;
import de.uni_due.s3.jack3.business.microservices.evaluatorutils.EvaluatorContextProducer;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.Placeholder;

public class ConverterRequestProducer {

	private final ConverterRequest request = new ConverterRequest();
	private final List<ConverterExceptionMessage> errors = new ArrayList<>();

	private ConverterRequestProducer(EvaluatorContext context) {
		this.request.context = context;
	}

	public static ConverterRequest ofPlaceholdersAndMaps(List<Placeholder> placeholders, EvaluatorMaps maps)
			throws ConverterException {
		EvaluatorContext context = EvaluatorContextProducer.byEvaluatorMaps(maps);
		return new ConverterRequestProducer(context).createConverterRequest(placeholders);
	}

	private ConverterRequest createConverterRequest(List<Placeholder> placeholders) throws ConverterException {
		placeholders.forEach(this::convertToConverterTask);
		if (!errors.isEmpty())
			throw new ConverterException(errors);
		else
			return request;
	}

	private void convertToConverterTask(Placeholder p) {
		try {
			request.addTask(ConverterTaskProducer.byPlaceholder(p, request.context));
		} catch (VariableNotDefinedException e) {
			errors.add(new ConverterExceptionMessage(p, "Not Defined!"));
		}
	}

}
