package de.uni_due.s3.jack3.entities.tenant;

import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;

import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.stagetypes.r.AbstractTestCase;


@Audited
@Entity
public class RStageJob extends Job {

	private static final long serialVersionUID = 7659235399056467876L;

	@OneToOne(cascade = { CascadeType.MERGE }, fetch = FetchType.EAGER)
	private AbstractTestCase testCase;

	public RStageJob() {
		// Only for Hibernate
	}

	public RStageJob(Submission submission, StageSubmission stageSubmission, AbstractTestCase testCase, String kafkaTopic) {
		super(submission, stageSubmission, "RStage", kafkaTopic);
		this.testCase = Objects.requireNonNull(testCase);
	}

	public AbstractTestCase getTestCase() {
		return testCase;
	}

	public void setTestCase(AbstractTestCase testCase) {
		this.testCase = testCase;
	}

}
