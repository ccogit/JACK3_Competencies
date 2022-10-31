package de.uni_due.s3.jack3.builders;

import de.uni_due.s3.jack3.entities.enums.EMCRuleType;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCFeedback;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;

/**
 * Builder for MC stages
 * 
 * @author lukas.glaser
 */
public class MCStageBuilder extends AbstractStageBuilder<MCStage, MCStageBuilder> {

	/**
	 * Creates a new instance of this builder without an exercise.
	 */
	public MCStageBuilder() {
		super();
	}

	/**
	 * Creates a new instance of this builder with an exercise.
	 */
	public MCStageBuilder(ExerciseBuilder exerciseBuilder) {
		super(exerciseBuilder);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected MCStage getNewStage() {
		return new MCStage();
	}

	/**
	 * Forces the user to select only one answer (default: allow the user to select many answers).
	 */
	public MCStageBuilder selectOne() {
		stage.setSingleChoice(true);
		return this;
	}

	/**
	 * Enables randomizing the order of answer options.
	 */
	public MCStageBuilder withRandomizedAnswerOrder() {
		stage.setRandomize(true);
		return this;
	}

	/**
	 * Sets feedback text for correct answer.
	 * 
	 * @param feedback
	 *            The text that is shown if the user's submission is correct
	 */
	public MCStageBuilder withCorrectFeedback(String feedback) {
		stage.setCorrectAnswerFeedback(feedback);
		return this;
	}

	/**
	 * Sets default feedback text and points.
	 * 
	 * @param feedback
	 *            The text that is shown if the user's submission is not correct and there is no specified
	 *            extra-feedback that matches it
	 * @param points
	 *            How much points the default feedback gains
	 */
	public MCStageBuilder withDefaultFeedback(String feedback, int points) {
		stage.setDefaultFeedback(feedback);
		stage.setDefaultResult(points);
		return this;
	}

	/**
	 * Adds an extra feedback to the stage.
	 * 
	 * @param feedback
	 *            The text that is shown if the user's submission matches the expression.
	 * @param expression
	 *            The expression that checks the user input
	 * @param points
	 *            How much points the feedback gains
	 */
	public MCStageBuilder withExtraFeedback(String feedback, String expression, int points) {
		stage.addFeedbackOption(new EvaluatorExpression(expression));
		MCFeedback mcFeedback = stage.getExtraFeedbacks().get(stage.getExtraFeedbacks().size() - 1);
		mcFeedback.setFeedbackText(feedback);
		mcFeedback.setResult(points);
		return this;
	}

	/**
	 * Adds an answer option.
	 * 
	 * @param text
	 *            The text of this answer option
	 * @param isCorrect
	 *            If this answer option is correct or not
	 */
	public MCStageBuilder withAnswerOption(String text, boolean isCorrect) {
		stage.addAnswerOption(text);
		EMCRuleType rule = isCorrect ? EMCRuleType.CORRECT : EMCRuleType.WRONG;
		stage.getAnswerOptions().get(stage.getAnswerOptions().size() - 1).setRule(rule);
		return this;
	}

}
