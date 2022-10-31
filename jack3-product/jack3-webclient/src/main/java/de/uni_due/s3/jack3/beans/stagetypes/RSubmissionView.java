package de.uni_due.s3.jack3.beans.stagetypes;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps;
import de.uni_due.s3.jack3.entities.enums.ESubmissionLogEntryType;
import de.uni_due.s3.jack3.entities.stagetypes.r.RStage;
import de.uni_due.s3.jack3.entities.stagetypes.r.RSubmission;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.SubmissionLogEntry;
import de.uni_due.s3.jack3.utils.JackStringUtils;

/**
 * This is the view that is referenced as 'exercisePlayerView.getStageSubmissionViewBean(stageSubmission)' e.g. in the
 * 'stages/r/input.xhtml'
 */
public class RSubmissionView extends AbstractSubmissionView implements Serializable {

	private static final long serialVersionUID = 3680005750625082750L;

	private RSubmission rSubmission;
	private RStage rStage;

	@Override
	public Stage getStage() {
		return rStage;
	}

	private String initialCode;

	public String getTaskDescription() {
		EvaluatorMaps evaluatorMaps = new EvaluatorMaps();
		evaluatorMaps.setExerciseVariableMap(rSubmission.getVariableValues());
		return converter.replaceVariablesByVariableName(rStage.getTaskDescription(), evaluatorMaps);
	}

	@Override
	public void setStageSubmission(Submission submission, StageSubmission stageSubmission, Stage stage) {
		if (!(stageSubmission instanceof RSubmission)) {
			throw new IllegalArgumentException("RSubmissionView must be used with instances of RSubmission");
		}
		if (!(stage instanceof RStage)) {
			throw new IllegalArgumentException("RSubmissionView must be used with instances of RStage");
		}
		rSubmission = (RSubmission) stageSubmission;
		rStage = (RStage) stage;

		if (rSubmission.getStudentInput() != null) {
			initialCode = rSubmission.getStudentInput();
		} else {
			String initialCodez = rStage.getInitialCode();
			if (JackStringUtils.isNotBlank(initialCodez)) {
				initialCode = replaceVariables(stageSubmission, initialCodez);
			}
		}
	}

	private String replaceVariables(StageSubmission stageSubmission, String initialCode) {
		EvaluatorMaps evaluatorMaps = new EvaluatorMaps();
		evaluatorMaps.setExerciseVariableMap(stageSubmission.getVariableValues());

		return converter.replaceVariablesByVariableName(initialCode, evaluatorMaps);
	}

	/**
	 * This gets called within the JavaScript in "stages/r/input.xhtml" that sets up the ace-editor!
	 */
	public boolean editorIsReadonly() {
		List<SubmissionLogEntry> submissionLog = getLogEntries();

		return (submissionLog.size() != 1) || !ESubmissionLogEntryType.ENTER.equals(submissionLog.get(0).getType());
	}

	/**
	 * This gets called through an onclick javascript event "handleRStudentSubmission()" on the submit-button
	 * in the exercisePlayer.xhtml!
	 */
	public void updateStudentRSubmissionInSubmission() {
		Map<String, String[]> requestParameterValuesMap = FacesContext.getCurrentInstance() //
				.getExternalContext() //
				.getRequestParameterValuesMap(); //

		String studentRSubmission = requestParameterValuesMap.get("studentRSubmission")[0];
		initialCode = studentRSubmission;
		rSubmission.setStudentInput(studentRSubmission);
	}

	public String getInitialCode() {
		return initialCode;
	}

	public void setInitialCode(String initialCode) {
		this.initialCode = initialCode;
	}
}
