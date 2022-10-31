package de.uni_due.s3.jack3.beans.stagetypes;

import java.util.Map;

import javax.faces.context.FacesContext;

import de.uni_due.s3.jack3.entities.stagetypes.molecule.MoleculeStage;
import de.uni_due.s3.jack3.entities.stagetypes.molecule.MoleculeSubmission;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.Submission;

public class MoleculeSubmissionView extends AbstractSubmissionView {

	private static final long serialVersionUID = -212495039230452509L;

	private MoleculeStage stage;
	private MoleculeSubmission stageSubmission;

	private String taskDescription;

	/**
	 *
	 * @return Task description with all variables replaced
	 */
	public String getTaskDescription() {
		return taskDescription;
	}

	public String getSubmissionInchi() {
		return stageSubmission.getInchiString();
	}

	public String getSubmissionMolfile() {
		return stageSubmission.getMolfileString();
	}

	public String getSubmissionEditorContent() {
		return stageSubmission.getEditorContentString() != null ? stageSubmission.getEditorContentString() : "''";
	}

	public String getSubmissionEditorContentFormat() {
		return stageSubmission.getEditorContentString() != null ? "Kekule.IO.DataFormat.KEKULE_JSON"
				: "Kekule.IO.DataFormat.MOL";
	}

	/**
	 * This gets called through an onclick javascript event "handleStudentSubmission()" on the submit-button in
	 * the exercisePlayer.xhtml!
	 */
	public void updateStudentMoleculeSubmissionInSubmission() {
		Map<String, String[]> requestParameterValuesMap = FacesContext.getCurrentInstance().getExternalContext()
				.getRequestParameterValuesMap();

		String studentMoleculeSubmissionInchi = requestParameterValuesMap.get("studentMoleculeSubmissionInChIs")[0];
		stageSubmission.setInchiString(studentMoleculeSubmissionInchi);
		String studentMoleculeSubmissionMol = requestParameterValuesMap.get("studentMoleculeSubmissionMols")[0];
		stageSubmission.setMolfileString(studentMoleculeSubmissionMol);
		String studentMoleculeSubmissionEditorContent = requestParameterValuesMap
				.get("studentMoleculeSubmissionEditorContent")[0];
		stageSubmission.setEditorContentString(studentMoleculeSubmissionEditorContent);
	}

	@Override
	public void setStageSubmission(Submission submission, StageSubmission stageSubmission, Stage stage) {
		if (!(stage instanceof MoleculeStage)) {
			throw new IllegalArgumentException("MoleculeSubmissionView must be used with instances of MoleculeStage");
		}
		if (!(stageSubmission instanceof MoleculeSubmission)) {
			throw new IllegalArgumentException(
					"MoleculeSubmissionView must be used with instances of MoleculeSubmission");
		}

		this.stage = (MoleculeStage) stage;
		this.stageSubmission = (MoleculeSubmission) stageSubmission;
		taskDescription = exercisePlayerBusiness.resolvePlaceholders(stage.getTaskDescription(), submission,
				stageSubmission, stage, true);
	}

	@Override
	public Stage getStage() {
		return stage;
	}
}
