package de.uni_due.s3.jack3.business.stagetypes;

import static de.uni_due.s3.jack3.business.microservices.placeholderutils.PlaceholderConstants.MC;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import de.uni_due.s3.jack3.business.ExercisePlayerBusiness;
import de.uni_due.s3.jack3.business.microservices.CalculatorBusiness;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.CalculatorException;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.InternalErrorEvaluatorException;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.PlaceholderPatternProducer;
import de.uni_due.s3.jack3.business.microservices.variableutils.VariableValueFactory;
import de.uni_due.s3.jack3.entities.enums.EMCRuleType;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCAnswer;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCFeedback;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCSubmission;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.Result;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.jack3.services.ResultService;
import de.uni_due.s3.jack3.services.StageSubmissionService;
import de.uni_due.s3.jack3.utils.JackStringUtils;

@ApplicationScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class MCStageBusiness extends AbstractStageBusiness implements Serializable {

	private static final long serialVersionUID = 2604045610877638847L;

	@Inject
	private ResultService resultService;

	@Inject
	private ExercisePlayerBusiness exercisePlayerBusiness;

	@Inject
	private StageSubmissionService stageSubmissionService;

	@Inject
	private CalculatorBusiness evaluatorBusiness;

	@Override
	public StageSubmission startGrading(Submission submission, Stage stage, StageSubmission stagesubmission,
			EvaluatorMaps evaluatorMaps) {
		assureCorrectClassUsage(MCSubmission.class, stagesubmission);

		MCSubmission mcSubmission = (MCSubmission) stagesubmission;
		final long mcStageId = mcSubmission.getStageId();
		final MCStage mcStage = (MCStage) stage;

		// Evaluate feedback rules
		final EvaluatorExpression correctAnswerExpression = getCorrectExpression(mcStage);

		// 3. Compare user input with the correct expression
		boolean correct = false;
		try {
			correct = matchInputs(correctAnswerExpression, evaluatorMaps);
			mcSubmission = computeMetaVariables(mcStage, mcSubmission, evaluatorMaps);
		} catch (CalculatorException | InternalErrorEvaluatorException e) {
			getLogger().warn(
					"Error evaluating correct answer expression for stage id " + mcStageId + " and stagesubmission id "
							+ mcSubmission.getId() + ". Expression was \"" + correctAnswerExpression.getCode() + "\".",
					e);
			return exercisePlayerBusiness.addFailureToSubmission(submission, stagesubmission,
					"Cannot evaluate correct answer expression " + correctAnswerExpression.getCode() + " in stage "
							+ stage.getInternalName() + ": " + e.getMessage(),
					e.getCause());
		}

		// Report result
		// 1. Create result object
		Result result = new Result(mcSubmission);

		// 2. Add additional data to result object
		if (correct) {
			result.setPoints(100);
			result.setPublicComment(mcStage.getCorrectAnswerFeedback());
		} else {
			// Check if there is matching extra feedback
			List<MCFeedback> matchingExtraFeedback = Collections.emptyList();
			try {
				matchingExtraFeedback = getMatchingExtraFeedback(mcStage.getExtraFeedbacks(), evaluatorMaps);
			} catch (CalculatorException | InternalErrorEvaluatorException e) {
				getLogger().warn("Error evaluating extra feedback for stage id " + mcStage.getId()
						+ " and stagesubmission id " + mcSubmission.getId(), e);
				return exercisePlayerBusiness.addFailureToSubmission(submission, stagesubmission,
						"Cannot evaluate extra feedback expressions in stage " + stage.getInternalName() + ": "
								+ e.getMessage(),
						e.getCause());
			}

			if (matchingExtraFeedback.isEmpty()) {
				// If there is no extra feedback we use the defaults from the stage.
				result.setPoints(mcStage.getDefaultResult());
				result.setPublicComment(mcStage.getDefaultFeedback());
			} else {
				// Otherwise we add up the points and the feedback text from all matching feedback.
				int points = mcStage.getDefaultResult();
				final StringBuilder publicComment = new StringBuilder();
				for (final MCFeedback feedback : matchingExtraFeedback) {
					publicComment.append("<p>").append(feedback.getFeedbackText()).append("</p>");
					points += feedback.getResult();
				}

				// We clamp the resulting points between 0 and 100.
				result.setPoints(Math.max(0, Math.min(points, 100)));
				result.setPublicComment(publicComment.toString());
			}
		}

		// 3. Store additional data
		resultService.persistResult(result);

		// 4. Add result to stagesubmission
		return exercisePlayerBusiness.addResultToSubmission(submission, mcSubmission, stage, result);
	}

	@Override
	public StageSubmission prepareSubmission(Submission submission, Stage stage, StageSubmission stagesubmission) {
		assureCorrectClassUsage(MCSubmission.class, stagesubmission);

		final MCSubmission mcSubmission = (MCSubmission) stagesubmission;
		final MCStage mcStage = (MCStage) stage;

		List<Integer> optionsOrder = new LinkedList<>();
		for (int i = 0; i < mcStage.getAnswerOptions().size(); i++) {
			optionsOrder.add(i);
		}
		if (mcStage.isRandomize()) {
			Collections.shuffle(optionsOrder);
		}
		mcSubmission.clearOptionsOder();
		mcSubmission.addOptionsOrder(optionsOrder);

		return mcSubmission;
	}

	@Override
	public boolean evaluateTransition(Submission submission, Stage stage, StageSubmission stagesubmission,
			StageTransition transition, EvaluatorMaps evaluatorMaps) throws InternalErrorEvaluatorException {
		assureCorrectClassUsage(MCSubmission.class, stagesubmission);
		return evaluatorBusiness.evaluateStageTransitionExpression(transition, evaluatorMaps);
	}

	@Override
	public StageSubmission updateStatus(StageSubmission stagesubmission, Submission submission) {
		if (!stagesubmission.getResults().isEmpty()) {
			stagesubmission.setHasPendingChecks(false);

			if (stagesubmission.getResults().size() > 1) {
				stagesubmission.setHasInternalErrors(true);
			} else {
				stagesubmission.setHasInternalErrors(false);
				stagesubmission.setPoints(stagesubmission.getResults().iterator().next().getPoints());
			}
		} else {
			stagesubmission.setHasPendingChecks(true);
			stagesubmission.setHasInternalErrors(false);
			stagesubmission.setPoints(0);
		}
		return stageSubmissionService.mergeStageSubmission(stagesubmission);
	}

	@Override
	public Map<String, VariableValue> getInputVariables(StageSubmission stagesubmission) {
		assureCorrectClassUsage(MCSubmission.class, stagesubmission);

		Map<String, VariableValue> inputs = new HashMap<>();
		String tickedPattern = ((MCSubmission) stagesubmission).getTickedPattern();

		if (tickedPattern != null) {
			final char[] tickedOptions = tickedPattern.toCharArray();

			for (int i = 0; i < tickedOptions.length; i++) {
				boolean ticked = tickedOptions[i] == '1';
				VariableValue tickValue = VariableValueFactory.createVariableValueForOpenMathBoolean(ticked);
				inputs.put(MC + i, tickValue);
			}
		}

		return inputs;
	}

	/**
	 * Return all extra-feedback elements that match with the user's input.
	 *
	 * @return All matching extra-feedback elements.
	 * @throws InternalErrorEvaluatorException
	 *             If the evaluator failed to evaluate one of the feedbacks' conditions.
	 */
	private List<MCFeedback> getMatchingExtraFeedback(final List<MCFeedback> extraFeedbacks,
			final EvaluatorMaps evaluatorMaps) throws CalculatorException, InternalErrorEvaluatorException {

		List<MCFeedback> feedbackList = new ArrayList<>();
		for (MCFeedback feedback : extraFeedbacks) {

			// Check if the answer pattern matches with the user input
			if (matchInputs(feedback.getExpression(), evaluatorMaps)) {

				// If so, check the additional condition (if specified)
				final EvaluatorExpression condition = feedback.getCondition();
				boolean additionalConditionMatches =
						// There is no additional condition
						(condition == null) || JackStringUtils.isBlank(condition.getCode()) ||
						// There is a condition and it does match
								evaluatorBusiness.calculateToBoolean(condition, evaluatorMaps);

				if (additionalConditionMatches) {
					feedbackList.add(feedback);
				}
			}
		}
		return feedbackList;
	}

	/**
	 * Check if a user input matches the specified expression
	 *
	 * @throws CalculatorException
	 * @throws InternalErrorEvaluatorException
	 */
	private boolean matchInputs(EvaluatorExpression master, EvaluatorMaps evaluatorMaps)
			throws CalculatorException, InternalErrorEvaluatorException {

		// Empty expression means that the stage has no answer option or no expression was given
		if (JackStringUtils.isBlank(master.getCode())) {
			return true;
		}

		return evaluatorBusiness.calculateToBoolean(master, evaluatorMaps);
	}

	private EvaluatorExpression getCorrectExpression(MCStage stage) {
		StringJoiner stringJoiner = new StringJoiner("&&");

		int i = 0;
		for (MCAnswer answer : stage.getAnswerOptions()) {
			switch (answer.getRule()) {
			case CORRECT:
				stringJoiner.add(PlaceholderPatternProducer.forMcInputVariable(i));
				break;
			case VARIABLE:
				stringJoiner.add(PlaceholderPatternProducer.forMcInputVariable(i) + "=="
						+ PlaceholderPatternProducer.forExerciseVariable(answer.getVariableName()));
				break;
			case WRONG:
				stringJoiner.add("!" + PlaceholderPatternProducer.forMcInputVariable(i));
				break;
			default:
				break;
			}
			i++;
		}

		EvaluatorExpression expression = new EvaluatorExpression();
		expression.setCode(stringJoiner.toString());
		return expression;
	}

	private MCSubmission computeMetaVariables(MCStage stage, MCSubmission stagesubmission, EvaluatorMaps evaluatorMaps)
			throws CalculatorException, InternalErrorEvaluatorException {
		int correctTicks = 0;
		int incorrectTicks = 0;

		String tickedPattern = stagesubmission.getTickedPattern();
		List<MCAnswer> answerOptions = stage.getAnswerOptions();

		if (tickedPattern != null) {
			final char[] tickedOptions = tickedPattern.toCharArray();

			for (int i = 0; i < tickedOptions.length; i++) {
				if (tickedOptions[i] == '1') {
					MCAnswer answer = answerOptions.get(i);
					EMCRuleType rule = answer.getRule();
					if (rule == EMCRuleType.CORRECT) {
						correctTicks++;
					} else if (rule == EMCRuleType.VARIABLE) {
						EvaluatorExpression expression = new EvaluatorExpression();
						expression.setCode(PlaceholderPatternProducer.forExerciseVariable(answer.getVariableName()));
						if (evaluatorBusiness.calculateToBoolean(expression, evaluatorMaps)) {
							correctTicks++;
						} else {
							incorrectTicks++;
						}
					} else if (rule == EMCRuleType.WRONG) {
						incorrectTicks++;
					}
				}
			}
		}

		stagesubmission.setCorrectTicks(correctTicks);
		stagesubmission.setIncorrectTicks(incorrectTicks);
		return (MCSubmission) stageSubmissionService.mergeStageSubmission(stagesubmission);
	}

	@Override
	public Map<String, VariableValue> getMetaVariables(StageSubmission stagesubmission) {
		assureCorrectClassUsage(MCSubmission.class, stagesubmission);
		MCSubmission mcSubmission = (MCSubmission) stagesubmission;

		EvaluatorMaps evaluatorMaps = new EvaluatorMaps();

		if (mcSubmission.getTickedPattern() != null) {
			// We only add these meta variables in case there has been an actual answer
			evaluatorMaps.addMetaVariable("mcStageCorrectTicks", mcSubmission.getCorrectTicks());
			evaluatorMaps.addMetaVariable("mcStageIncorrectTicks", mcSubmission.getIncorrectTicks());
			evaluatorMaps.addMetaVariable("mcStageTotalTicks",
					mcSubmission.getCorrectTicks() + mcSubmission.getIncorrectTicks());
			evaluatorMaps.addMetaVariable("mcStageNumberOfAnswerOptions", mcSubmission.getTickedPattern().length());
		}

		return evaluatorMaps.getMetaVariableMap();
	}
}
