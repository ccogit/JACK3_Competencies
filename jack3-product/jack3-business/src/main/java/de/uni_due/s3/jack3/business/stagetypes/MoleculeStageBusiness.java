package de.uni_due.s3.jack3.business.stagetypes;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import de.uni_due.s3.jack3.business.ExercisePlayerBusiness;
import de.uni_due.s3.jack3.business.microservices.CalculatorBusiness;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.CalculatorException;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.InternalErrorEvaluatorException;
import de.uni_due.s3.jack3.business.microservices.variableutils.VariableValueFactory;
import de.uni_due.s3.jack3.entities.stagetypes.molecule.MoleculeRule;
import de.uni_due.s3.jack3.entities.stagetypes.molecule.MoleculeStage;
import de.uni_due.s3.jack3.entities.stagetypes.molecule.MoleculeSubmission;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.FeedbackMessage;
import de.uni_due.s3.jack3.entities.tenant.Result;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.jack3.services.ResultService;
import de.uni_due.s3.jack3.services.StageSubmissionService;

@ApplicationScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class MoleculeStageBusiness extends AbstractStageBusiness {

	private static final Type arrayListType = new TypeToken<ArrayList<String>>() {
	}.getType();

	@Inject
	private ResultService resultService;

	@Inject
	private StageSubmissionService stageSubmissionService;

	@Inject
	private ExercisePlayerBusiness exercisePlayerBusiness;

	@Inject
	private CalculatorBusiness evaluatorBusiness;

	@Override
	public StageSubmission prepareSubmission(Submission submission, Stage stage, StageSubmission stagesubmission) {
		// Nothing to prepare
		return stagesubmission;
	}

	@Override
	public StageSubmission startGrading(Submission submission, Stage stage, StageSubmission stagesubmission,
			EvaluatorMaps evaluatorMaps) {
		assureCorrectClassUsage(MoleculeSubmission.class, stagesubmission);
		assureCorrectClassUsage(MoleculeStage.class, stage);

		MoleculeSubmission moleculeSubmission = (MoleculeSubmission) stagesubmission;
		MoleculeStage moleculeStage = (MoleculeStage) stage;

		// Abort early if stage is not well-defined
		if (moleculeStage.getExpectedInchiString() == null) {
			stagesubmission.setHasInternalErrors(true);
			return stageSubmissionService.mergeStageSubmission(stagesubmission);
		}

		// Check if inchi string from student input equals expected string
		List<String> expected = generateInChIList(moleculeStage.getExpectedInchiString());
		List<String> actual = generateInChIList(moleculeSubmission.getInchiString());

		boolean correct = compareInChiLists(expected, actual);
		int points = 0;
		List<String> feedback = new LinkedList<>();

		// if correct answer not matched check if other feedback matches else user get default feedback
		if (!correct) {
			boolean userGetsDefaultFeedback = true;

			// Iterate over rules to generate feedback and points
			for (MoleculeRule rule : moleculeStage.getFeedbackRulesAsList()) {

				EvaluatorExpression ruleExpressionEvaluatorNotation = rule.getValidationExpression();
				try {
					// An empty Evaluator Expression is interpreted as False and will be skipped
					if (!ruleExpressionEvaluatorNotation.isEmpty()
							&& evaluatorBusiness.calculateToBoolean(ruleExpressionEvaluatorNotation, evaluatorMaps)) {
						userGetsDefaultFeedback = false;
						points += rule.getPoints();
						feedback.add(rule.getFeedbackText());

						if (rule.isTerminal()) {
							break;
						}
					}
				} catch (InternalErrorEvaluatorException e) {
					return getStageSubmissionForEvaluationException(e, e.getMessage(), moleculeSubmission, submission,
							ruleExpressionEvaluatorNotation, moleculeStage);
				}
			}

			if (userGetsDefaultFeedback) {
				feedback.add(moleculeStage.getErrorFeedback());
				points = 0;
			}
		} else {
			points = 100;
			feedback.add(moleculeStage.getCorrectFeedback());
		}

		// Score must be between 0 and 100!
		points = Math.min(100, Math.max(0, points));

		// Report result
		// 1. Create result object
		Result result = new Result(moleculeSubmission);

		// 2. Add additional data to result object
		result.setPoints(points);

		if (feedback.size() == 1) {
			result.setPublicComment(feedback.get(0));
		} else {
			for (String feedbackLine : feedback) {
				result.addFeedbackMessage(new FeedbackMessage(feedbackLine));
			}
		}

		// 3. Store additional data
		resultService.persistResult(result);

		// 4. Add result to stagesubmission
		return exercisePlayerBusiness.addResultToSubmission(submission, moleculeSubmission, stage, result);
	}

	private StageSubmission getStageSubmissionForEvaluationException(Exception e, String enMessage,
			MoleculeSubmission moleculeSubmission, Submission submission,
			EvaluatorExpression ruleExpressionEvaluatorNotation, MoleculeStage moleculeStage) {
		getLogger().warnf(e,
				"Cannot evaluate validation expression %s in stage with id %s. Submission result may thus be incorrect!",
				ruleExpressionEvaluatorNotation, moleculeSubmission.getStageId());

		return exercisePlayerBusiness.addFailureToSubmission(submission, moleculeSubmission,
				"Cannot evaluate validation expression " + ruleExpressionEvaluatorNotation.getCode() + " in stage "
						+ moleculeStage.getInternalName() + ": " + enMessage,
						e.getCause());
	}

	private boolean compareInChiLists(List<String> expected, List<String> actual) {
		return expected.size() == actual.size() && actual.containsAll(expected) && expected.containsAll(actual);
	}

	private List<String> generateInChIList(String inchis) {
		if (inchis == null || inchis.isEmpty()) {
			return List.of();
		}
		if (!inchis.startsWith("[") && !inchis.endsWith("]")) {
			return List.of(inchis);
		}
		try {
			return new Gson().fromJson(inchis, arrayListType);
		} catch (JsonParseException e) {
			return List.of();
		}
	}

	public void removeFeedbackFromStage(int feedbackRuleOrderIndex, MoleculeStage stage) {
		stage.removeFeedbackRule(feedbackRuleOrderIndex);
		List<MoleculeRule> rules = stage.getFeedbackRulesAsList();
		setNewRuleOrderOnStage(stage, rules);
	}

	public void reorderFeedbackRules(MoleculeStage stage, int fromIndex, int toIndex) {
		List<MoleculeRule> rules = stage.getFeedbackRulesAsList();
		final MoleculeRule ruleToReorder = rules.remove(fromIndex);
		rules.add(toIndex, ruleToReorder);
		setNewRuleOrderOnStage(stage, rules);
	}

	private void setNewRuleOrderOnStage(MoleculeStage stage, List<MoleculeRule> rules) {
		int i = 0;
		for (MoleculeRule rule : rules) {
			rule.setOrderIndex(i);
			i++;
		}
		Collections.sort(rules);
		stage.addFeedbackRules(rules);
	}

	@Override
	public StageSubmission updateStatus(StageSubmission stagesubmission, Submission submission) {
		if (!stagesubmission.getResults().isEmpty()) {
			// Case 1: Handle existing result
			stagesubmission.setHasPendingChecks(false);

			if (stagesubmission.getResults().size() > 1) {
				stagesubmission.setHasInternalErrors(true);
			} else {
				stagesubmission.setHasInternalErrors(false);
				stagesubmission.setPoints(stagesubmission.getResults().iterator().next().getPoints());
			}
		} else {
			if (stagesubmission.hasInternalErrors()) {
				// Case 2: Handle internal error
				stagesubmission.setHasPendingChecks(false);
			} else {
				// Case 3: Handle pending check
				stagesubmission.setHasPendingChecks(true);
				stagesubmission.setHasInternalErrors(false);
			}
			stagesubmission.setPoints(0);
		}
		return stageSubmissionService.mergeStageSubmission(stagesubmission);
	}

	@Override
	public boolean evaluateTransition(Submission submission, Stage stage, StageSubmission stagesubmission,
			StageTransition transition, EvaluatorMaps evaluatorMaps)
					throws InternalErrorEvaluatorException, CalculatorException {
		assureCorrectClassUsage(MoleculeSubmission.class, stagesubmission);

		return evaluatorBusiness.evaluateStageTransitionExpression(transition, evaluatorMaps);
	}

	@Override
	public Map<String, VariableValue> getInputVariables(StageSubmission stagesubmisson) {
		assureCorrectClassUsage(MoleculeSubmission.class, stagesubmisson);
		MoleculeSubmission moleculeSubmission = (MoleculeSubmission) stagesubmisson;

		Map<String, VariableValue> inputs = new HashMap<>();

		if (moleculeSubmission.getInchiString() != null) {
			inputs.put("inchi",
					VariableValueFactory.createVariableValueForOpenMathStringList(
							generateInChIList(moleculeSubmission.getInchiString())));
		}

		return inputs;
	}

	@Override
	public Map<String, VariableValue> getMetaVariables(StageSubmission stagesubmission) {
		return new HashMap<>();
	}

}
