package de.uni_due.s3.jack3.business.microservices.converterutils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.uni_due.s3.evaluator_api.converter.response.ConverterResponse;
import de.uni_due.s3.evaluator_api.converter.response.ConverterResult;
import de.uni_due.s3.evaluator_api.converter.response.ConverterResultType;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.Placeholder;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.PlaceholderReplacement;

public class ConverterResponseHandler {

	private ConverterResponse response;
	private List<ConverterExceptionMessage> errors = new ArrayList<>();
	private List<PlaceholderReplacement> replacements = new ArrayList<>();

	private ConverterResponseHandler(ConverterResponse response) {
		this.response = response;
	}

	public static List<PlaceholderReplacement> getAllPlaceholderReplacements(List<Placeholder> placeholders,
			ConverterResponse response) throws ConverterException {
		return new ConverterResponseHandler(response).fetchReplacementsFor(placeholders);
	}

	private List<PlaceholderReplacement> fetchReplacementsFor(List<Placeholder> placeholders)
			throws ConverterException {
		placeholders.forEach(this::handlePlaceholder);
		if (!errors.isEmpty())
			throw new ConverterException(errors);
		else
			return replacements;
	}

	private void handlePlaceholder(Placeholder placeholder) {
		findConverterResult(placeholder).ifPresentOrElse((r) -> addReplacementFor(placeholder, r),
				() -> addExceptionForNotFound(placeholder));
	}

	private void addReplacementFor(Placeholder placeholder, ConverterResult result) {
		if (result.type.equals(ConverterResultType.EXCEPTION)) {
			addExceptionFor(placeholder, result.content.exceptionMessage);
		}
		replacements.add(new PlaceholderReplacement(placeholder, result.content.stringResult));
	}

	private void addExceptionFor(Placeholder placeholder, String message) {
		errors.add(new ConverterExceptionMessage(placeholder, message));
	}

	private void addExceptionForNotFound(Placeholder placeholder) {
		addExceptionFor(placeholder, "Not Defined!");
	}

	private Optional<ConverterResult> findConverterResult(Placeholder placeholder) {
		return response.getResults().stream().filter(placeholder::matchesConverterResultProperties).findFirst();
	}

}
