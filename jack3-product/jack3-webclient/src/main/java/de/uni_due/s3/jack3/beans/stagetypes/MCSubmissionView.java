package de.uni_due.s3.jack3.beans.stagetypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.faces.model.SelectItem;

import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCSubmission;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.Submission;

public class MCSubmissionView extends AbstractSubmissionView implements Serializable {

	private static final long serialVersionUID = -8169538438130778792L;

	private MCStage stage;
	private MCSubmission stageSubmission;

	@Override
	public Stage getStage() {
		return stage;
	}

	private String taskDescription;

	/**
	 *
	 * @return Task description with all variables replaced
	 */
	public String getTaskDescription() {
		return taskDescription;
	}

	private List<SelectItem> answerOptions;

	/**
	 *
	 * @return List of all available answer options with all variables replaced
	 */
	public List<SelectItem> getAnswerOptions() {
		return answerOptions;
	}

	private void createMCSubmissionViewFields(Submission submission, MCSubmission stageSubmission, MCStage stage) {
		taskDescription = exercisePlayerBusiness.resolvePlaceholders(stage.getTaskDescription(), submission,
				stageSubmission,
				stage, true);

		answerOptions = new LinkedList<>();
		for (final int i : stageSubmission.getOptionsOrder()) {
			String answerOptionText = stage.getAnswerOptions().get(i).getText();
			answerOptionText = exercisePlayerBusiness.resolvePlaceholders(answerOptionText, submission,
					stageSubmission,
					stage, true);
			SelectItem answerItem = new SelectItem(i, answerOptionText);
			answerItem.setEscape(false);
			answerOptions.add(answerItem);
		}
	}

	/**
	 * Method to be called from MCSubmissionInput.xhtml to store user's selection for the multiple choice variant of
	 * MCStage. Also called internally from setTickedAnswer(String).
	 *
	 * @param tickedAnswerOptions
	 *            List of selected answer options
	 */
	public void setTickedAnswers(List<Integer> tickedAnswerOptions) {
		final StringBuilder sb = new StringBuilder();

		for (int i = 0; i < answerOptions.size(); i++) {
			if (tickedAnswerOptions.contains(i)) {
				sb.append("1");
			} else {
				sb.append("0");
			}
		}

		stageSubmission.setTickedPattern(sb.toString());
	}

	/**
	 * Method to be called from MCSubmissionInput.xhtml to retrieve user's selection for the multiple choice variant of
	 * MCStage.
	 *
	 * @return List of selected answer options with all variables replaced
	 */
	public List<String> getTickedAnswers() {
		final String tickedPattern = stageSubmission.getTickedPattern();

		// The stage has not been answered yet, so there is no ticked pattern to
		// process. It is save to return null, as JSF is able to handle this.
		if (tickedPattern == null) {
			return null;
		}

		final List<String> tickedAnswerOptions = new ArrayList<>();
		for (int i = 0; i < tickedPattern.length(); i++) {
			if (tickedPattern.charAt(i) == '1') {
				tickedAnswerOptions.add(Integer.toString(i));
			}
		}

		return tickedAnswerOptions;
	}

	/**
	 * Method to be called from MCSubmissionInput.xhtml to store user's selection for the single choice variant of
	 * MCStage. Internally calls {@link #setTickedAnswers(List)}.
	 *
	 * @param tickedAnswerOption
	 *            Selected answer option
	 */
	public void setTickedAnswer(Integer tickedAnswerOption) {
		final List<Integer> tickedAnswers = new LinkedList<>();
		tickedAnswers.add(tickedAnswerOption);
		setTickedAnswers(tickedAnswers);
	}

	/**
	 * Method to be called from MCSubmissionInput.xhtml to retrieve user's selection for the single choice variant of
	 * MCStage.
	 *
	 * @return Single selected answer option with all variables replaced
	 */
	public Integer getTickedAnswer() {
		final List<String> tickedAnswers = getTickedAnswers();

		if ((tickedAnswers == null) || tickedAnswers.isEmpty()) {
			return null;
		}

		if (tickedAnswers.size() > 1) {
			throw new IllegalStateException("There is more than one ticked answer.");
		}

		return Integer.valueOf(tickedAnswers.get(0));
	}

	@Override
	public void setStageSubmission(Submission submission, StageSubmission stageSubmission, Stage stage) {
		if (!(stageSubmission instanceof MCSubmission)) {
			throw new IllegalArgumentException("MCSubmissionView must be used with instances of MCSubmission");
		}
		if (!(stage instanceof MCStage)) {
			throw new IllegalArgumentException("MCSubmissionView must be used with instances of MCStage");
		}

		this.stageSubmission = (MCSubmission) stageSubmission;
		this.stage = (MCStage) stage;
		createMCSubmissionViewFields(submission, (MCSubmission) stageSubmission, (MCStage) stage);
	}
}
