package de.uni_due.s3.jack3.beans;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import de.uni_due.s3.jack3.beans.data.EvaluatorShellRequest;
import de.uni_due.s3.jack3.business.microservices.CalculatorBusiness;
import de.uni_due.s3.jack3.business.microservices.ConverterBusiness;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.InternalErrorEvaluatorException;
import de.uni_due.s3.jack3.business.microservices.converterutils.InternalErrorConverterException;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.jack3.utils.JackStringUtils;
import de.uni_due.s3.jack3.utils.StopWatch;

/**
 * The Evaluator Shell is used for testing evaluator expressions. Users enter an expression and a
 * {@link EvaluatorExpression.EDomain} and the view shows the evaluator response.
 */
@Named
@ViewScoped
public class EvaluatorShellView extends AbstractView implements Serializable {

	private static final long serialVersionUID = -4338488547649086807L;

	@Inject
	private CalculatorBusiness evaluatorBusiness;

	@Inject
	private ConverterBusiness converterBusiness;

	/**
	 * Saves the states of different evaluator consoles. For showing the shell in {@link ExercisePlayerView} multiple
	 * times, we use the {@link StageSubmission} ID. In other cases where the shell is directly added to the view, we
	 * can use any {@link String}.
	 */
	private final Map<String, EvaluatorShellRequest> savedInputs = new HashMap<>();

	/**
	 * Returns the {@link EvaluatorShellRequest} that was stored by the key. This method ensures that a request is
	 * stored behind the key. If the key was not stored previously, a new {@link EvaluatorShellRequest} will be created.
	 */
	public EvaluatorShellRequest getRequest(String key) {
		if (savedInputs.containsKey(key)) {
			return savedInputs.get(key);
		} else {
			savedInputs.put(key, new EvaluatorShellRequest());
			return savedInputs.get(key);
		}
	}

	/**
	 * Evaluates the {@link EvaluatorShellRequest} that was stored by the key. It saves the result
	 */
	public void evaluate(String key) {
		final StopWatch stopWatch = new StopWatch().start();
		final EvaluatorShellRequest request = getRequest(key);
		removeInvalidRequestFragments(request);
		try {
			final VariableValue result = evaluatorBusiness.calculateToVariableValue(request.getExpression(), request.getMaps());
			switch (request.getRepresentation()) {
			case STRING:
				request.setResult(converterBusiness.convertToString(result));
				break;
			case LATEX:
			default:
				request.setResult("$" + converterBusiness.convertToLaTeX(result) + "$");
				break;
			}
		} catch (InternalErrorEvaluatorException | InternalErrorConverterException e) {
			// Show a message to the user if an error has occurred.
			String result = e.getMessage();
			// Shorten the task name
			if (JackStringUtils.isNotBlank(result)) {
				if (result.contains(":"))
					result = result.substring(result.indexOf(':') + 2);
				result = result.replace("\n", "<br/>");
			} else {
				result = e.getClass().getSimpleName();
			}
			request.setResult(result);
		} catch (Exception e) {
			request.setResult(e.getClass().getSimpleName()
					+ (JackStringUtils.isBlank(e.getMessage()) ? "." : ": " + e.getMessage()));
		}
		stopWatch.stop();
		request.setResponseTime(stopWatch.getElapsedMilliseconds());
	}

	/**
	 * Removes all invalid (= value is null) exercise, input and meta variables from a given
	 * {@link EvaluatorShellRequest}.
	 */
	private void removeInvalidRequestFragments(EvaluatorShellRequest request) {
		request.getMaps().getExerciseVariableMap().values().removeIf(varValue -> varValue.getContent() == null);
		request.getMaps().getInputVariableMap().values().removeIf(varValue -> varValue.getContent() == null);
		request.getMaps().getMetaVariableMap().values().removeIf(varValue -> varValue.getContent() == null);
	}

	/**
	 * @return All available {@link EvaluatorShellRequest.ERepresentation}
	 */
	public EvaluatorShellRequest.ERepresentation[] getRepresentations() {
		return EvaluatorShellRequest.ERepresentation.values();
	}

}
