package de.uni_due.s3.jack3.beans.stagetypes.helpers;

import java.io.Serializable;

import de.uni_due.s3.jack3.entities.enums.EFillInSubmissionFieldType;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.SubmissionField;

public class FillInSubmissionViewField implements Serializable{

	private static final long serialVersionUID = 6060702355568186587L;

	private String taskDescription;
	
	private EFillInSubmissionFieldType fieldType;
	
	private SubmissionField stagesubmissionField;
	
	public FillInSubmissionViewField(SubmissionField stagesubmissionField) {
		this.stagesubmissionField = stagesubmissionField;
		fieldType = stagesubmissionField.getFieldType();
	}
	
	
	public FillInSubmissionViewField(String taskDescription) {
		this.taskDescription = taskDescription;
		fieldType = EFillInSubmissionFieldType.TYPE_TASK_DESCRIPTION;
	}

	public String getTaskDescription() {
		return taskDescription;
	}

	public EFillInSubmissionFieldType getFieldType() {
		return fieldType;
	}
	
	public SubmissionField getSubmissionField() {
		return stagesubmissionField;
	}
}
