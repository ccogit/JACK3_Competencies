package de.uni_due.s3.jack3.beans.data;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import de.uni_due.s3.jack3.entities.enums.ECourseScoring;
import de.uni_due.s3.jack3.entities.tenant.Submission;

public class CoursePlayerSubmissionCache implements Serializable {

	private static final long serialVersionUID = 4100462899224375060L;

	private Submission bestSubmission;
	private Submission lastSubmission;
	private List<Submission> oldSubmissions;
	private int countAllSubmissions;

	/**
	 * 
	 * @param submissions Submissions for this exercise ordered by ID DESC
	 * @param exerciseAbsentInCourse If the course contains this exercise
	 */
	public CoursePlayerSubmissionCache(List<Submission> submissions, boolean exerciseAbsentInCourse) {
		if (submissions.isEmpty()) {
			// No submissions at all
			bestSubmission = null;
			lastSubmission = null;
			oldSubmissions = Collections.emptyList();
			countAllSubmissions = 0;

		} else if (exerciseAbsentInCourse) {
			// Submissions, but course does not contain this exercise anymore
			// -> we don't need best / last submissions
			bestSubmission = null;
			lastSubmission = null;
			oldSubmissions = submissions;
			countAllSubmissions = submissions.size();

		} else {
			// Regular submission list (not empty)
			bestSubmission = submissions.stream().max(Comparator.comparing(Submission::getResultPoints)).get(); // NOSONAR
			lastSubmission = submissions.remove(0);
			oldSubmissions = submissions;
			countAllSubmissions = submissions.size() + 1;
		}
	}

	public Optional<Submission> getSubmission(ECourseScoring scoring) {
		return scoring == ECourseScoring.LAST ? getLastSubmission() : getBestSubmission();
	}

	public Optional<Submission> getLastSubmission() {
		return Optional.ofNullable(lastSubmission);
	}

	public void setLastSubmission(Submission lastSubmission) {
		this.lastSubmission = lastSubmission;
	}

	public Optional<Submission> getBestSubmission() {
		return Optional.ofNullable(bestSubmission);
	}

	public void setBestSubmission(Submission bestSubmission) {
		this.bestSubmission = bestSubmission;
	}

	public List<Submission> getOldSubmissions() {
		return oldSubmissions;
	}

	public int countAllSubmissions() {
		return countAllSubmissions;
	}

}
