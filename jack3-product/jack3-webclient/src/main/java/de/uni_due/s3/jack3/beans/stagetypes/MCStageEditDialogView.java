package de.uni_due.s3.jack3.beans.stagetypes;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.primefaces.event.ReorderEvent;

import de.uni_due.s3.jack3.beans.stagetypes.helpers.MCStageInputOption;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.PlaceholderConstants;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.PlaceholderPatternProducer;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCAnswer;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCFeedback;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;

public class MCStageEditDialogView extends AbstractStageEditDialogView implements Serializable {

	private static final long serialVersionUID = -7217398132832916689L;

	private MCStage stage;

	private List<MCStageInputOption> newOptions = new ArrayList<>();

	public List<MCStageInputOption> updateNewOptions() {
		newOptions.clear();
		stage.getAnswerOptions().forEach(answer -> newOptions.add(new MCStageInputOption(answer)));
		return newOptions;
	}

	public void onAnswerReorder(ReorderEvent event) {
		final int from = event.getFromIndex();
		final int to = event.getToIndex();

		stage.moveAnswerOption(from, to);
	}

	public void addNewAnswerOption() {
		final String baseName = parentView.getLocalizedMessage("global.answer");
		final int index = stage.getAnswerOptions().size() + 1;
		final String label = MessageFormat.format(baseName,index);
		stage.addAnswerOption(label);
	}

	public void removeAnswerOption(MCAnswer answer) {
		stage.removeAnswerOption(answer);
	}

	public void addNewFeedbackOption() {
		stage.addFeedbackOption(getExpressionFromOptions(newOptions));
	}

	public void removeFeedbackOption(MCFeedback feedback) {
		stage.removeFeedbackOption(feedback);
	}

	public void addNewTargetOption() {
		stage.addStageTransition(getExpressionFromOptions(newOptions));
	}

	public void removeStageTransition(StageTransition transition) {
		stage.removeStageTransition(transition);
	}

	public List<String> getRulesList(MCFeedback feedback) {
		return getRulesFromExpression(feedback.getExpression());
	}

	public List<String> getRulesList(StageTransition transition) {
		return getRulesFromExpression(transition.getStageExpression());
	}

	public List<MCStageInputOption> getNewOptions() {
		return newOptions;
	}

	@Override
	public void setStage(Stage stage) {
		if (!(stage instanceof MCStage)) {
			throw new IllegalArgumentException("MCStageEditDialogView must be used with instances of MCStage");
		}

		this.stage = (MCStage) stage;
	}

	@Override
	public Stage getStage() {
		return stage;
	}

	private EvaluatorExpression getExpressionFromOptions(List<MCStageInputOption> inputOptions) {
		StringJoiner stringJoiner = new StringJoiner("&&");

		int i = 0;
		for (MCStageInputOption inputOption : inputOptions) {
			switch (inputOption.getRule()) {
			case "CORRECT":
				stringJoiner.add(PlaceholderPatternProducer.forMcInputVariable(i));
				break;
			case "WRONG":
				stringJoiner.add("!" + PlaceholderPatternProducer.forMcInputVariable(i));
				break;
			case "NO_MATTER":
				stringJoiner.add("true()");
				break;
			default:
				stringJoiner.add(PlaceholderPatternProducer.forMcInputVariable(i) + "=="
						+ PlaceholderPatternProducer.forExerciseVariable(inputOption.getRule()));
				break;

			}
			i++;
		}

		EvaluatorExpression expression = new EvaluatorExpression();
		expression.setCode(stringJoiner.toString());
		return expression;
	}

	private List<String> getRulesFromExpression(EvaluatorExpression expression) {
		List<String> rules = new ArrayList<>();
		for (String part : expression.getCode().split("&&")) {
			if (part.equals("true()")) {
				rules.add("NO_MATTER");
			} else if (part.startsWith("![" + PlaceholderConstants.INPUT_IDENTIFIER + "=" + PlaceholderConstants.MC)) {
				rules.add("WRONG");
			} else if (part.contains("]==[")) {
				rules.add(part.substring(part.indexOf("]==[var=") + 8, part.lastIndexOf(']')));
			} else {
				rules.add("CORRECT");
			}
		}
		return rules;

	}

}
