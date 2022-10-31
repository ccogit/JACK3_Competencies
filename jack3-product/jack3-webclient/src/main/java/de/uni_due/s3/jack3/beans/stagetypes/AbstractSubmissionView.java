package de.uni_due.s3.jack3.beans.stagetypes;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import de.uni_due.s3.jack3.beans.AbstractView;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.ExercisePlayerBusiness;
import de.uni_due.s3.jack3.business.ResourceBusiness;
import de.uni_due.s3.jack3.business.microservices.CalculatorBusiness;
import de.uni_due.s3.jack3.business.microservices.ConverterBusiness;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.SubmissionLogEntry;


public abstract class AbstractSubmissionView extends AbstractView implements Serializable {

	private static final long serialVersionUID = 3024389660489178980L;

	private boolean isVisible = true;

	private List<String> hints = new LinkedList<>();

	private List<SubmissionLogEntry> logEntries = new LinkedList<>();

	@Inject
	protected CalculatorBusiness evaluator;

	@Inject
	protected ConverterBusiness converter;

	@Inject
	protected ExerciseBusiness exerciseBusiness;

	@Inject
	protected ExercisePlayerBusiness exercisePlayerBusiness;

	@Inject
	protected ResourceBusiness resourceBusiness;

	/**
	 * Initializes the View with a {@link Submission}, a {@link StageSubmission} and the {@link Stage} that belongs to
	 * the submission. Implementations can e.g. save the parameters in fields, replace variables in the task description
	 * or initialize UI fields.
	 *
	 * It is required that the passed stage is saved due to the {@link #getStage()} method.
	 */
	public abstract void setStageSubmission(Submission submission, StageSubmission stageSubmission, Stage stage);

	/**
	 * Implementations of this method return the stage of the submission.
	 */
	public abstract Stage getStage();

	public void addHint(String hintText) {
		hints.add(hintText);
	}

	public List<String> getHints() {
		return hints;
	}

	public void addSubmissionLogEntry(SubmissionLogEntry entry) {
		logEntries.add(entry);
	}

	public List<SubmissionLogEntry> getLogEntries() {
		return logEntries;
	}

	public boolean isVisible() {
		return isVisible;
	}

	public void setVisible(boolean isVisible) {
		this.isVisible = isVisible;
	}
}
