package de.uni_due.s3.jack3.entities.tenant;

import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;

import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;

/**
 * This class provides information about asynchronous checks.
 *
 * @author Benjamin Otto
 *
 */
@NamedQuery(name = Job.ALL_JOBS_FOR_SUBMISSION, //
query = "SELECT DISTINCT j FROM Job j " + //
		"WHERE j.submission = :submission")
@NamedQuery(
		name = Job.COUNT_PENDING_JOBS_FOR_STAGE_SUBMISSION, //
		query = "SELECT count(j) FROM Job j " + //
		"WHERE j.stageSubmission = :stageSubmission AND j.finishedAt = null")
@Audited
@Entity
public class Job extends AbstractEntity {

	private static final long serialVersionUID = -4655286145172838862L;

	public static final String ALL_JOBS_FOR_SUBMISSION = "Job.allJobsForSubmission";

	public static final String COUNT_PENDING_JOBS_FOR_STAGE_SUBMISSION = "Job.countPendingJobsForStageSubmission";

	@OneToOne(cascade = { CascadeType.MERGE }, fetch = FetchType.LAZY)
	private Submission submission;

	@OneToOne(cascade = { CascadeType.MERGE }, fetch = FetchType.LAZY)
	private StageSubmission stageSubmission;

	@ToString
	@Column
	private String stageTypeName;

	@ToString
	@Column
	private LocalDateTime startedAt;

	@ToString
	@Column
	private LocalDateTime finishedAt;

	// The backend that has processed the job. This can of course only be set after the Job has finished.
	@ToString
	@Column
	private String graderId;

	@ToString
	@Column
	private String kafkaTopic;

	@Column
	private String kafkaBootstrapServer;

	/**
	 * The checker configuration that spawned this job. May be {@code null} if the corresponding stage does not use
	 * explicit checker configurations.
	 */
	@Column
	private CheckerConfiguration checkerConfiguration;

	public Job() {
		// Only for Hibernate
	}

	public Job(Submission submission, StageSubmission stageSubmission, String stageTypeName, String kafkaTopic) {
		super();
		this.submission = Objects.requireNonNull(submission);
		this.stageSubmission = Objects.requireNonNull(stageSubmission);
		this.stageTypeName = Objects.requireNonNull(stageTypeName);
		this.kafkaTopic = Objects.requireNonNull(kafkaTopic);
	}

	public Submission getSubmission() {
		return submission;
	}

	public void setSubmission(Submission submission) {
		this.submission = submission;
	}

	public StageSubmission getStageSubmission() {
		return stageSubmission;
	}

	public void setStageSubmission(StageSubmission stageSubmission) {
		this.stageSubmission = stageSubmission;
	}

	public String getGraderId() {
		return graderId;
	}

	public void setGraderId(String graderId) {
		this.graderId = graderId;
	}

	public void setStarted() {
		startedAt = LocalDateTime.now();
	}

	public void setFinished() {
		finishedAt = LocalDateTime.now();
	}

	public LocalDateTime getStartedAt() {
		return startedAt;
	}

	public LocalDateTime getFinishedAt() {
		return finishedAt;
	}

	public String getKafkaTopic() {
		return kafkaTopic;
	}

	public void setKafkaTopic(String kafkaTopic) {
		this.kafkaTopic = kafkaTopic;
	}

	public String getKafkaBootstrapServer() {
		return kafkaBootstrapServer;
	}

	public void setKafkaBootstrapServer(String kafkaBootstrapServer) {
		this.kafkaBootstrapServer = kafkaBootstrapServer;
	}

	public String getStageTypeName() {
		return stageTypeName;
	}

	public void setStageTypeName(String stageTypeName) {
		this.stageTypeName = stageTypeName;
	}

	public CheckerConfiguration getCheckerConfiguration() {
		return checkerConfiguration;
	}

	public void setCheckerConfiguration(CheckerConfiguration checkerConfiguration) {
		this.checkerConfiguration = checkerConfiguration;
	}

}
