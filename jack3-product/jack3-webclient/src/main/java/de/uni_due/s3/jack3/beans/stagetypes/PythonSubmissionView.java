package de.uni_due.s3.jack3.beans.stagetypes;

import java.util.Map;

import javax.faces.context.FacesContext;

import de.uni_due.s3.jack3.entities.stagetypes.python.PythonStage;
import de.uni_due.s3.jack3.entities.stagetypes.python.PythonSubmission;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.Submission;

public class PythonSubmissionView extends AbstractSubmissionView {

	private static final long serialVersionUID = -1921246843821022155L;

	private PythonStage stage;
	private PythonSubmission stageSubmission;

	private String taskDescription;

	private String initialCode;

	/**
	 *
	 * @return Task description with all variables replaced
	 */
	public String getTaskDescription() {
		return taskDescription;
	}

	public String getSubmissionCode() {
		return stageSubmission.getPythonCode();
	}

	public void setSubmissionCode(String pythonCode) {
		stageSubmission.setPythonCode(pythonCode);
	}

	/**
	 * This gets called through an onclick javascript event "handlePythonStudentSubmission()" on the submit-button in
	 * the exercisePlayer.xhtml!
	 */
	public void updateStudentPythonSubmissionInSubmission() {
		Map<String, String[]> requestParameterValuesMap = FacesContext.getCurrentInstance().getExternalContext()
				.getRequestParameterValuesMap();

		String studentPythonSubmission = requestParameterValuesMap.get("studentPythonSubmission")[0];
		initialCode = studentPythonSubmission;
		stageSubmission.setPythonCode(studentPythonSubmission);
	}

	public String getInitialCode() {
		return initialCode;
	}

	public void setInitialCode(String initialCode) {
		this.initialCode = initialCode;
	}

	@Override
	public void setStageSubmission(Submission submission, StageSubmission stageSubmission, Stage stage) {
		if (!(stage instanceof PythonStage)) {
			throw new IllegalArgumentException("PythonSubmissionView must be used with instances of PythonStage");
		}
		if (!(stageSubmission instanceof PythonSubmission)) {
			throw new IllegalArgumentException("PythonSubmissionView must be used with instances of PythonSubmission");
		}

		this.stage = (PythonStage) stage;
		this.stageSubmission = (PythonSubmission) stageSubmission;
		taskDescription = exercisePlayerBusiness.resolvePlaceholders(stage.getTaskDescription(), submission,
				stageSubmission, stage, true);
		if (this.stageSubmission.getPythonCode() != null) {
			initialCode = this.stageSubmission.getPythonCode();
		}
	}

	@Override
	public Stage getStage() {
		return stage;
	}
}
