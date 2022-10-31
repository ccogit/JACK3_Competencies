package de.uni_due.s3.jack3.entities.stagetypes.fillin;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.enums.EFillInSubmissionFieldType;

@Audited
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class SubmissionField extends AbstractEntity implements Comparable<SubmissionField>{

	private static final long serialVersionUID = -4468042314837684957L;
	
	@Column
	@Type(type = "text")
	protected String fieldName;

	@Enumerated(EnumType.STRING)
	protected EFillInSubmissionFieldType fieldType;

	@Column
	protected int orderIndex;

	public SubmissionField(){}

	protected SubmissionField(String fieldName, EFillInSubmissionFieldType fieldType) {
		this.fieldName = fieldName;
		this.fieldType = fieldType;
	}

	public EFillInSubmissionFieldType getFieldType() {
		return fieldType;
	}

	public int getOrderIndex() {
		return orderIndex;
	}

	public void setOrderIndex(int orderIndex) {
		this.orderIndex = orderIndex;
	}

	@Override
	public int compareTo(SubmissionField o) {
		return Integer.valueOf(orderIndex).compareTo(Integer.valueOf(o.getOrderIndex()));
	}

	public String getFieldName() {
		return fieldName;
	}
	
	public abstract void setUserInput(String userInput);
	
	public abstract String getUserInput();
}
