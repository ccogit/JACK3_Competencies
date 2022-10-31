package de.uni_due.s3.jack3.entities.stagetypes.mc;

import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;
import de.uni_due.s3.jack3.utils.DeepCopyHelper;

/**
 * Represents a specific feedback for an MCStage including an expression, feedback text and result points.
 */
@Audited
@Entity
public class MCFeedback extends AbstractEntity implements DeepCopyable<MCFeedback> {

	private static final long serialVersionUID = -9031868325846426029L;

	public MCFeedback() {
		super();
	}

	public MCFeedback(EvaluatorExpression expression) {
		super();
		this.expression = Objects.requireNonNull(expression);
	}

	/** Expression that specifies when this feedback is shown (depending on user input) */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, optional = false, orphanRemoval = true)
	private EvaluatorExpression expression = new EvaluatorExpression();

	/** Condition that specifies when the feedback is shown (depending on variables) */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, optional = false, orphanRemoval = true)
	private EvaluatorExpression condition = new EvaluatorExpression();

	@Min(value = -100)
	@Max(value = 100)
	@Column(nullable = false)
	private int result = 0;

	@Column
	@Type(type = "text")
	private String feedbackText;

	public int getResult() {
		return result;
	}

	public void setResult(int result) {
		// Must be between 0 and 100
		if ((result < -100) || (result > 100)) {
			throw new IllegalArgumentException(
					"Invalid feedback points: " + result + ". Feedback points must be between -100 and 100.");
		}

		this.result = result;
	}

	public String getFeedbackText() {
		return feedbackText;
	}

	public void setFeedbackText(String feedbackText) {
		this.feedbackText = feedbackText;
	}

	public EvaluatorExpression getExpression() {
		return expression;
	}

	public EvaluatorExpression getCondition() {
		return condition;
	}

	@Override
	public MCFeedback deepCopy() {
		MCFeedback entityDeepCopy = new MCFeedback();

		entityDeepCopy.feedbackText = feedbackText;
		entityDeepCopy.result = result;
		entityDeepCopy.expression = DeepCopyHelper.deepCopyOrNull(expression);
		entityDeepCopy.condition = DeepCopyHelper.deepCopyOrNull(condition);

		return entityDeepCopy;
	}

}
