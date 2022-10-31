package de.uni_due.s3.jack3.entities.stagetypes.mc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.annotations.Type;
import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageHint;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;

@Audited
@Entity
@AttributeOverride(name = "id", column = @Column(name = "id"))
@XStreamAlias("MCStage")
public class MCStage extends Stage {

	private static final long serialVersionUID = -2839980863563437043L;

	@OneToMany(mappedBy="mcstage", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@OrderColumn(name="answer_options_order")
	@AuditMappedBy(mappedBy = "mcstage", positionMappedBy = "answer_options_order") // fixes #363
	private List<MCAnswer> answerOptions = new ArrayList<>();

	@Column
	private boolean randomize; // randomize answer options?

	@Column
	private boolean singleChoice; // single choice answers?

	@Column
	@Type(type = "text")
	private String correctAnswerFeedback;

	@Column
	@Type(type = "text")
	private String defaultFeedback;

	@Column
	private int defaultResult;

	/** Extra feedbacks shown to the user depending on the input */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@OrderColumn
	private List<MCFeedback> extraFeedbacks = new ArrayList<>();

	/*
	 * @return unmodifiableList of answerOptions
	 */
	public List<MCAnswer> getAnswerOptions() {
		return Collections.unmodifiableList(answerOptions);
	}

	public List<MCAnswer> getAnswerOptionsForReoder() {
		List<MCAnswer> answersForReoder = new ArrayList<>(answerOptions.size());
		answersForReoder.addAll(answerOptions);
		return answersForReoder;
	}

	public String getCorrectAnswerFeedback() {
		return correctAnswerFeedback;
	}

	public String getDefaultFeedback() {
		return defaultFeedback;
	}

	public List<MCFeedback> getExtraFeedbacks() {
		return extraFeedbacks;
	}

	public boolean isRandomize() {
		return randomize;
	}

	public boolean isSingleChoice() {
		return singleChoice;
	}

	public void setCorrectAnswerFeedback(String correctAnswerFeedback) {
		this.correctAnswerFeedback = correctAnswerFeedback;
	}

	public void setDefaultFeedback(String defaultFeedback) {
		this.defaultFeedback = defaultFeedback;
	}

	public int getDefaultResult() {
		return defaultResult;
	}

	public void setDefaultResult(int defaultResult) {
		// Must be between 0 and 100
		if ((defaultResult < 0) || (defaultResult > 100)) {
			throw new IllegalArgumentException(
					"Invalid default result: " + defaultResult + ". Result must be between 0 and 100.");
		}

		this.defaultResult = defaultResult;
	}

	public void setRandomize(boolean randomize) {
		this.randomize = randomize;
	}

	public void setSingleChoice(boolean singleChoice) {
		this.singleChoice = singleChoice;
	}

	public void moveAnswerOption(int fromIndex, int toIndex) {
		if ((0 > fromIndex) || (fromIndex >= answerOptions.size())) {
			throw new IndexOutOfBoundsException("fromIndex: " + fromIndex);
		}

		if ((0 > toIndex) || (toIndex >= answerOptions.size())) {
			throw new IndexOutOfBoundsException("toIndex: " + toIndex);
		}

		answerOptions.add(toIndex, answerOptions.remove(fromIndex));

		// Reorder expressions for extra feedbacks and transitions
		extraFeedbacks.forEach(extraFeedback -> moveExpression(extraFeedback.getExpression(), fromIndex, toIndex));
		stageTransitions.forEach(transition -> moveExpression(transition.getStageExpression(), fromIndex, toIndex));
	}

	public void removeAnswerOption(MCAnswer option) {
		final int index = answerOptions.indexOf(option);

		answerOptions.remove(option);
		extraFeedbacks.forEach(extraFeedback -> removePartFromExpression(extraFeedback.getExpression(), index));
		stageTransitions.forEach(transition -> removePartFromExpression(transition.getStageExpression(), index));
	}

	public void addAnswerOption(String text) {
		MCAnswer mcAnswer = new MCAnswer(text);
		mcAnswer.setMCStage(this);
		answerOptions.add(mcAnswer);

		// Add option to existing feedbacks and transitions
		extraFeedbacks.forEach(feedback -> addAnswerOptionToExpression(feedback.getExpression()));
		stageTransitions.forEach(transition -> addAnswerOptionToExpression(transition.getStageExpression()));
	}

	public void removeFeedbackOption(MCFeedback feedback) {
		extraFeedbacks.remove(feedback);
	}

	/**
	 * Adds a new feedback option with the given expression and a feedback text. The feedback is always added, even if
	 * there already is a duplicate feedback.
	 */
	public void addFeedbackOption(EvaluatorExpression expression) {
		extraFeedbacks.add(new MCFeedback(expression));
	}

	public void addStageTransition(EvaluatorExpression expression) {
		// Uniqueness is not checked because there could be transitions with the same stageExpression but different
		// conditionExpressions
		final StageTransition transition = new StageTransition();
		transition.setStageExpression(expression);
		stageTransitions.add(transition);
	}

	/**
	 * Removes a part at the given index from an expression and replaces position numbers
	 */
	private void removePartFromExpression(final EvaluatorExpression expression, int index) {
		String[] expressions = ArrayUtils.remove(expression.getCode().split("&&"), index);
		for (int i = 0; i < expressions.length; i++) {
			expressions[i] = expressions[i].replaceFirst("\\[input=mcindex_(\\d+)\\]", "[input=mcindex_" + i + "]");
		}
		expression.setCode(getConjunction(expressions));
	}

	/**
	 * Reorders an expression
	 */
	private void moveExpression(final EvaluatorExpression expression, int from, int to) {
		final String[] expressions = expression.getCode().split("&&");
		// Swap expressions and replace position numbers
		final String fromExpression = expressions[from];
		expressions[from] = expressions[to].replace("input=mcindex_" + to, "input=mcindex_" + from);
		expressions[to] = fromExpression.replace("input=mcindex_" + from, "input=mcindex_" + to);
		expression.setCode(getConjunction(expressions));
	}

	/**
	 * Adds a new answer option to an expression. The logical value should not changed.
	 */
	private void addAnswerOptionToExpression(final EvaluatorExpression expression) {
		if (expression.getCode().isEmpty()) {
			expression.setCode("true()");
		} else {
			expression.setCode(expression.getCode() + "&&true()");
		}
	}

	/**
	 * Converts expressions to a conjunction (...{@literal &&}...{@literal &&}...)
	 */
	private String getConjunction(String[] expressions) {
		StringJoiner stringJoiner = new StringJoiner("&&");
		for (String expression : expressions) {
			stringJoiner.add(expression);
		}
		return stringJoiner.toString();
	}

	@Override
	public MCStage deepCopy() {
		MCStage mcStageClone = new MCStage();
		mcStageClone.deepCopyStageVars(this);

		for (MCAnswer answer : answerOptions) {
			MCAnswer deepCopyAnswer = answer.deepCopy();
			deepCopyAnswer.setMCStage(mcStageClone);
			mcStageClone.answerOptions.add(deepCopyAnswer);
		}

		mcStageClone.randomize = randomize;
		mcStageClone.singleChoice = singleChoice;
		mcStageClone.correctAnswerFeedback = correctAnswerFeedback;
		mcStageClone.defaultFeedback = defaultFeedback;
		mcStageClone.defaultResult = defaultResult;

		for (MCFeedback mcFeedback : extraFeedbacks) {
			mcStageClone.extraFeedbacks.add(mcFeedback.deepCopy());
		}

		// Fix stage-references in StageHint. We can do this here, since all stageHints in this stage reference
		// this stage here anyway.
		for (StageHint hint : mcStageClone.getHints()) {
			hint.setStage(mcStageClone);
		}

		return mcStageClone;
	}

	@Override
	public boolean mustWaitForPendingJobs() {
		return true;
	}
}
