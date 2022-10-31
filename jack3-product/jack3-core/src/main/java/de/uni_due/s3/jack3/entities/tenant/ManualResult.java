package de.uni_due.s3.jack3.entities.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.AbstractEntity;

/**
 * Represents the result of a manual check for a stage submission.
 */
@Audited
@Entity
public class ManualResult extends AbstractEntity {

	private static final long serialVersionUID = -2357203857474222154L;

	public ManualResult() {
		super();
	}

	public ManualResult(final User createdBy) {
		super();
		this.createdBy = createdBy;
	}

	/**
	 * An internal comment that is only visible for lecturers.
	 */
	@Type(type = "text")
	@Column
	private String internalComment;

	/**
	 * A public comment that is visible for students viewing the submission, if feedback is enabled.
	 */
	@Type(type = "text")
	@Column
	private String publicComment;

	/**
	 * The score that the submission earned.
	 */
	@Min(0)
	@Max(100)
	@Column
	private int points;

	/**
	 * If the automatic feedback should still be shown. Otherwise, only the manual one is shown.
	 */
	@Column
	private boolean showAutomaticResult;

	/**
	 * The user who created the manual feedback
	 */
	@ManyToOne(optional = false)
	private User createdBy;

	public String getInternalComment() {
		return internalComment;
	}

	public void setInternalComment(String internalComment) {
		this.internalComment = internalComment;
	}

	public String getPublicComment() {
		return publicComment;
	}

	public void setPublicComment(String publicComment) {
		this.publicComment = publicComment;
	}

	public int getPoints() {
		return points;
	}

	public void setPoints(int points) {
		if (points < 0 || points > 100) {
			throw new IllegalArgumentException("Points must be between 0 and 100, but was " + points);
		}

		this.points = points;
	}

	public boolean isShowAutomaticResult() {
		return showAutomaticResult;
	}

	public void setShowAutomaticResult(boolean showAutomaticResult) {
		this.showAutomaticResult = showAutomaticResult;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

}
