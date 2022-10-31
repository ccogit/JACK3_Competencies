package de.uni_due.s3.jack3.business.microservices;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import de.uni_due.s3.evaluator_api.converter.properties.ConverterFormatProperties;
import de.uni_due.s3.evaluator_api.converter.request.ConverterRequest;
import de.uni_due.s3.evaluator_api.converter.request.ConverterTask;
import de.uni_due.s3.evaluator_api.converter.request.ConverterTaskType;
import de.uni_due.s3.evaluator_api.converter.response.ConverterResponse;
import de.uni_due.s3.evaluator_api.converter.response.ConverterResult;
import de.uni_due.s3.evaluator_api.converter.response.ConverterResultType;
import de.uni_due.s3.evaluator_api.properties.EvaluatorContext;
import de.uni_due.s3.evaluator_api.properties.EvaluatorContextVariable;
import de.uni_due.s3.evaluator_api.properties.EvaluatorProperties;
import de.uni_due.s3.evaluator_api.properties.EvaluatorVariableType;
import de.uni_due.s3.jack3.business.AbstractBusiness;
import de.uni_due.s3.jack3.business.microservices.converterutils.ConverterException;
import de.uni_due.s3.jack3.business.microservices.converterutils.ConverterFormatPropertiesProducer;
import de.uni_due.s3.jack3.business.microservices.converterutils.ConverterRequestProducer;
import de.uni_due.s3.jack3.business.microservices.converterutils.ConverterResponseHandler;
import de.uni_due.s3.jack3.business.microservices.converterutils.InternalErrorConverterException;
import de.uni_due.s3.jack3.business.microservices.evaluatorutils.EvaluatorPropertiesProducer;
import de.uni_due.s3.jack3.business.microservices.openobjectutils.OpenObjectConverter;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.Placeholder;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.PlaceholderFinder;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.PlaceholderReplacement;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.PlaceholderReplacer;
import de.uni_due.s3.jack3.business.microservices.variableutils.VariableValueFactory;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.openobject.OpenObject;

@ApplicationScoped
public class ConverterBusiness extends AbstractBusiness {

	@Inject
	private EvaluatorBusiness eurekaBusiness;

	private static ConverterTask createTaskFor(OpenObject openObject, ConverterTaskType type) {
		EvaluatorProperties props = EvaluatorPropertiesProducer.byOpenObject("name", EvaluatorVariableType.VAR,
				openObject);
		ConverterFormatProperties formatProps = ConverterFormatPropertiesProducer.NONE;
		return new ConverterTask(props, type, formatProps);
	}

	private static ConverterRequest createRequestForSingleTask(VariableValue variableValue, ConverterTaskType type) {
		OpenObject openObject = OpenObjectConverter.fromVariableValue(variableValue);
		ConverterRequest request = new ConverterRequest();
		request.context = new EvaluatorContext();
		request.context.addVariable(new EvaluatorContextVariable(
				EvaluatorPropertiesProducer.byOpenObject("name", EvaluatorVariableType.VAR, openObject), openObject));
		ConverterTask task = createTaskFor(openObject, type);
		request.addTask(task);
		return request;
	}

	private static ConverterResult getSingleResultOf(ConverterResponse response) throws ConverterException {
		if (!response.getResults().isEmpty()) {
			return response.getResults().get(0);
		}
		throw new ConverterException("There is no ConverterResult!");
	}

	private static String getStringContentOf(ConverterResult result) throws ConverterException {
		if (ConverterResultType.STRING.equals(result.type) || ConverterResultType.LATEX.equals(result.type)) {
			return result.content.stringResult;
		}
		throw new ConverterException("Single converter result type is wrong!");
	}

	private static List<OpenObject> getOpenObjectListContentOf(ConverterResult result) throws ConverterException {
		if (ConverterResultType.OPEN_OBJECT_LIST.equals(result.type)) {
			return result.content.getListResult();
		}
		throw new ConverterException("Single converter result type is wrong!");
	}

	private static List<VariableValue> getListContentOf(ConverterResult result) throws ConverterException {
		return getOpenObjectListContentOf(result).stream().map(VariableValueFactory::createVariableValue)
				.collect(Collectors.toList());
	}

	public String convertToString(VariableValue variableValue)
			throws InternalErrorConverterException, ConverterException {
		ConverterRequest request = createRequestForSingleTask(variableValue, ConverterTaskType.STRING);
		ConverterResponse response = doConverterRequest(request);
		ConverterResult result = getSingleResultOf(response);
		return getStringContentOf(result);
	}

	public String convertToLaTeX(VariableValue variableValue)
			throws InternalErrorConverterException, ConverterException {
		ConverterRequest request = createRequestForSingleTask(variableValue, ConverterTaskType.LATEX);
		ConverterResponse response = doConverterRequest(request);
		ConverterResult result = getSingleResultOf(response);
		return getStringContentOf(result);
	}

	public List<VariableValue> convertToList(VariableValue variableValue)
			throws InternalErrorConverterException, ConverterException {
		ConverterRequest request = createRequestForSingleTask(variableValue, ConverterTaskType.LATEX);
		ConverterResponse response = doConverterRequest(request);
		ConverterResult result = getSingleResultOf(response);
		return getListContentOf(result);
	}

	/**
	 * Replaces all occurrences of variable references in the given text, if values for those are available in the
	 * EvaluatorMaps object. Variable references to names that are not available in the object are not replaced.
	 * Replaced values are formatted as plain strings or LaTeX depending on the type of reference.
	 *
	 * @param text
	 *            The text in which variable references should be replaced
	 * @param maps
	 *            A EvaluatorMaps object containing variable values.
	 * @return A new string in which all possible variable references are replaced.
	 */
	public String replaceVariablesByVariableName(String text, EvaluatorMaps maps) {
		if (null == text) {
			return text;
		}
		try {
			return replaceVariablesInText(text, maps);
		} catch (ConverterException | InternalErrorConverterException e) {
			// REVIEW: vorher war das hier ein ", e" was jedesmal zu einem riesigem Stacktrace führt, wenn in einem
			// Feedback eine nicht vorhandene Variable ersetzt werden soll (der Lehrende kriegt das dann aber auch nicht
			// mit). Besser wäre es aus meiner Sicht, wenn wir hier ein error-submissionlogentry für den Lehrenden
			// erzeugen wie es zB auch beim Betreten einer Aufgabe mit falschem Evaluator Befehl für die
			// Variablenbelegung passiert. Dazu muss hier aber immer
			// die Submission übergeben werden. Bis zur Klärung durch " + e" ersetzt.
			getLogger().error("Could not Convert all Variables due to: " + e.getMessage());
			return text;
		}
	}

	private String replaceVariablesInText(String text, EvaluatorMaps maps)
			throws InternalErrorConverterException, ConverterException {
		List<Placeholder> placeholders = PlaceholderFinder.findPlaceholderForText(text);
		ConverterRequest request = ConverterRequestProducer.ofPlaceholdersAndMaps(placeholders, maps);
		ConverterResponse response = doConverterRequest(request);
		List<PlaceholderReplacement> replacements = ConverterResponseHandler.getAllPlaceholderReplacements(placeholders,
				response);
		return PlaceholderReplacer.replaceTextBy(text, replacements);
	}

	private ConverterResponse doConverterRequest(ConverterRequest request) throws InternalErrorConverterException {
		try {
			return doIfNeeded(request);
		} catch (Exception e) {
			throw new InternalErrorConverterException(e.getMessage(), e.getClass().getSimpleName());
		}
	}

	private ConverterResponse doIfNeeded(ConverterRequest request) {
		if (request.getTasks().isEmpty()) {
			return new ConverterResponse();
		}
		return eurekaBusiness.convert(request);
	}

}
