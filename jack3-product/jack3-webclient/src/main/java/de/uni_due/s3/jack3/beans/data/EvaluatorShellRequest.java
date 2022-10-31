package de.uni_due.s3.jack3.beans.data;

import java.io.Serializable;

import de.uni_due.s3.jack3.beans.EvaluatorShellView;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;

/**
 * Helper class that stores user input for {@link EvaluatorShellView}.
 */
public class EvaluatorShellRequest implements Serializable {

	public enum ERepresentation {
		LATEX, STRING
	}

	private static final long serialVersionUID = -2192864759629406997L;

	private final EvaluatorExpression expression = new EvaluatorExpression();
	private EvaluatorMaps maps = new EvaluatorMaps();
	private String result;
	private String responseTime;
	private ERepresentation representation = ERepresentation.LATEX;

	public EvaluatorExpression getExpression() {
		return expression;
	}

	public EvaluatorMaps getMaps() {
		return maps;
	}

	public void setMaps(EvaluatorMaps maps) {
		this.maps = maps;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getResponseTime() {
		return responseTime;
	}

	public void setResponseTime(String responseTime) {
		this.responseTime = responseTime;
	}

	public ERepresentation getRepresentation() {
		return representation;
	}

	public void setRepresentation(ERepresentation representation) {
		this.representation = representation;
	}

}
