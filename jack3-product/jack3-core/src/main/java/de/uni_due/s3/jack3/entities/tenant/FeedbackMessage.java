package de.uni_due.s3.jack3.entities.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.enums.ECommentThumb;

/**
 * Representation of a single feedback message within a larger result
 */
@Audited
@Entity
public class FeedbackMessage extends AbstractEntity {

	private static final long serialVersionUID = -708754005539708795L;

	@Column
	@Type(type = "text")
	private String text;

	@Column
	@Type(type = "text")
	private String details;

	@Column
	private boolean isHidden;

	@Enumerated(EnumType.STRING)
	private ECommentThumb studentVote;

	public FeedbackMessage() {
		super();
	}

	public FeedbackMessage(String text) {
		super();

		this.text = text;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public boolean isHidden() {
		return isHidden;
	}

	public void setHidden(boolean isHidden) {
		this.isHidden = isHidden;
	}

	public ECommentThumb getStudentVote() {
		return studentVote;
	}

	public void setStudentVote(ECommentThumb studentVote) {
		this.studentVote = studentVote;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}

}
