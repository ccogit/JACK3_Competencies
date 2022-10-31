package de.uni_due.s3.jack3.entities.tenant;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.enums.ESubmissionLogEntryType;
import de.uni_due.s3.jack3.interfaces.TimeComparable;

@Audited
@Entity
public class SubmissionLogEntry extends AbstractEntity implements TimeComparable {

	private static final long serialVersionUID = 3134806182000225341L;

	@ToString
	@Column
	@Type(type = "text")
	private String text;

	@ToString
	@Column
	private LocalDateTime timestamp;

	@ToString
	@Enumerated(EnumType.STRING)
	private ESubmissionLogEntryType type;

	@ToString
	@Column
	private long stageId;

	@ToString
	@ManyToOne
	private StageSubmission stagesubmission;

	@ToString
	@ManyToOne
	private Result result;

	public SubmissionLogEntry() {
		super();
	}

	public SubmissionLogEntry(ESubmissionLogEntryType type, long stageId) {
		this.stageId = stageId;
		timestamp = LocalDateTime.now();
		this.type = type;
	}

	public String getText() {
		return text;
	}

	@Override
	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public ESubmissionLogEntryType getType() {
		return type;
	}

	public long getStageId() {
		return stageId;
	}

	public StageSubmission getSubmission() {
		return stagesubmission;
	}

	public Result getResult() {
		return result;
	}

	public void setSubmission(StageSubmission stagesubmission) {
		this.stagesubmission = stagesubmission;
	}

	public void setResult(Result result) {
		this.result = result;
	}

	public void setText(String text) {
		this.text = text;
	}

}
