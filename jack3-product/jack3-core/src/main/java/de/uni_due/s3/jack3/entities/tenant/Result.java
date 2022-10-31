package de.uni_due.s3.jack3.entities.tenant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;

/**
 * Represents a result of a check from one StageSubmission. The result comments are always written in the same language
 * as the Exercise is.
 */
@Audited
@Entity
public class Result extends AbstractEntity {

	private static final long serialVersionUID = 2522572879993354896L;

	public Result() {
		super();
	}

	@ToString
	@Column(nullable = false)
	private int points;

	@Column
	@Type(type = "text")
	private String checkerLog;

	@Column
	@Type(type = "text")
	private String publicComment;

	@Column
	@Type(type = "text")
	// REVIEW lg - unbenutzt
	private String internalComment;

	@ManyToOne(optional = false)
	private StageSubmission stagesubmission;

	@ToString
	@OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
	private List<FeedbackMessage> feedbackMessages = new ArrayList<>();

	/**
	 * The checker configuration that spawned a job which returned this result. May be {@code null} if the corresponding
	 * stage does not use explicit checker configurations.
	 */
	@Column
	private CheckerConfiguration checkerConfiguration;

	/**
	 * Set to the name of the kafka topic if this result object was created from an asynchronous check; set to
	 * {@code null} otherwise.
	 */
	@ToString
	@Column
	@Type(type = "text")
	private String fromKafkaTopic;

	/**
	 * Set to {@code true} if this result object describes an internal error (e.g. by a failed asynchronous check); set
	 * to {@code false} otherwise.
	 */
	@Column(columnDefinition = "boolean default false")
	private boolean isErrorResult;

	public Result(StageSubmission stagesubmission) {
		this.stagesubmission = Objects.requireNonNull(stagesubmission, "You must specify a submission.");
	}

	public StageSubmission getSubmission() {
		return stagesubmission;
	}

	public void setSubmission(StageSubmission stagesubmission) {
		this.stagesubmission = stagesubmission;
	}

	/*
	 * @return unmodifiableList of feedback messages
	 */
	public List<FeedbackMessage> getFeedbackMessages() {
		return Collections.unmodifiableList(feedbackMessages);
	}

	public void addFeedbackMessage(FeedbackMessage feedbackMessage) {
		if (!feedbackMessages.contains(feedbackMessage)) {
			feedbackMessages.add(feedbackMessage);
		}
	}

	public int getPoints() {
		return points;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	public String getPublicComment() {
		return publicComment;
	}

	public void setPublicComment(String publicComment) {
		this.publicComment = publicComment;
	}

	public String getInternalComment() {
		return internalComment;
	}

	public void setInternalComment(String internalComment) {
		this.internalComment = internalComment;
	}

	public String getCheckerLog() {
		return checkerLog;
	}

	public void setCheckerLog(String checkerLog) {
		this.checkerLog = checkerLog;
	}

	/**
	 * Returns the name of the kafka topic if this result object was created from an asynchronous check; returns
	 * {@code null} otherwise.
	 */
	public String getFromKafkaTopic() {
		return fromKafkaTopic;
	}

	public void setFromKafkaTopic(String fromKafkaTopic) {
		this.fromKafkaTopic = fromKafkaTopic;
	}

	/**
	 * Returns {@code true} if this result object describes an internal error (e.g. by a failed asynchronous check);
	 * returns {@code false} otherwise.
	 */
	public boolean isErrorResult() {
		return isErrorResult;
	}

	public void setErrorResult(boolean isErrorResult) {
		this.isErrorResult = isErrorResult;
	}

	public CheckerConfiguration getCheckerConfiguration() {
		return checkerConfiguration;
	}

	public void setCheckerConfiguration(CheckerConfiguration checkerConfiguration) {
		this.checkerConfiguration = checkerConfiguration;
	}

}
