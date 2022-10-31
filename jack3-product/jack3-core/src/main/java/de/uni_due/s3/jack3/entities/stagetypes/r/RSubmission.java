package de.uni_due.s3.jack3.entities.stagetypes.r;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.SubmissionResource;

@NamedQuery(
		name = RSubmission.RSUBMISSION_WITH_LAZY_DATA_BY_ID, //
		query = "SELECT r FROM RSubmission r " //
				+ "LEFT JOIN FETCH r.testCaseTupleResults " //
				+ "WHERE r.id=:id")
@Audited
@Entity
public class RSubmission extends StageSubmission {

	private static final long serialVersionUID = -6339536931637788701L;

	public static final String RSUBMISSION_WITH_LAZY_DATA_BY_ID = "RSUBMISSION_WITH_LAZY_DATA_BY_ID";

	public RSubmission() {
	}

	@OneToMany(
			fetch = FetchType.EAGER,
			cascade = { CascadeType.ALL })
	private Set<TestCaseTupleResult> testCaseTupleResults = new HashSet<>();

	@ToString
	@Column
	@Lob
	private String studentInput;

	public String getStudentInput() {
		return studentInput;
	}

	public void setStudentInput(String studentInput) {
		this.studentInput = studentInput;
	}

	@ManyToMany
	private List<SubmissionResource> submissionResources;

	public RSubmission(List<SubmissionResource> submissionResources) {
		this.submissionResources = submissionResources;
	}

	public List<SubmissionResource> getSubmissionResources() {
		return submissionResources;
	}

	public void setSubmissionResources(List<SubmissionResource> submissionResources) {
		this.submissionResources = submissionResources;
	}

	// Sets the map with null-values for result
	public void initializeTestCaseTupleResult(TestCaseTuple testCasetuple) {
		testCaseTupleResults.add(new TestCaseTupleResult(testCasetuple));
	}

	/**
	 * This gets called when the user starts a already submitted stage as new. So we do not copy the test results here.
	 */
	@Override
	public void copyFromStageSubmission(StageSubmission otherStageSubmission) {
		RSubmission otherRSubmission = (RSubmission) otherStageSubmission;
		studentInput = otherRSubmission.getStudentInput();
	}

	public Set<TestCaseTupleResult> getTestCaseTupleResults() {
		return testCaseTupleResults;
	}

	public TestCaseTupleResult getTestCaseTupleResultContaining(AbstractTestCase testcase) {
		return testCaseTupleResults.stream() //
				.filter(testCaseTupleResult -> testCaseTupleResult.getTestCaseResultsMap().containsKey(testcase)) //
				.findFirst() // 
				.orElseThrow();
	}

}
