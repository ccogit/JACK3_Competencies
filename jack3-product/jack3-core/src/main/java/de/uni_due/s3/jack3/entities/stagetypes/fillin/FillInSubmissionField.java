package de.uni_due.s3.jack3.entities.stagetypes.fillin;

import javax.persistence.Column;
import javax.persistence.Entity;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.enums.EFillInEditorType;
import de.uni_due.s3.jack3.entities.enums.EFillInSubmissionFieldType;

@Audited
@Entity
public class FillInSubmissionField extends SubmissionField {

	private static final long serialVersionUID = 4214577875075909167L;

	@Column
	@Type(type = "text")
	private String userInput;
	
	@Enumerated(EnumType.STRING)
	private EFillInEditorType editorType;
	
	@Column
	private int size;

	public int getSize() {
		return size;
	}

	public FillInSubmissionField() {
	}

	public FillInSubmissionField(String fieldName, EFillInSubmissionFieldType fieldType,EFillInEditorType editorType, int size) {
		super(fieldName,fieldType);
		this.editorType = editorType;
		this.size = size;
	}

	public String getUserInput() {
		return userInput;
	}

	public void setUserInput(String userInput) {
		this.userInput = userInput;
	}

	public EFillInEditorType getEditorType() {
		return editorType;
	}
}
