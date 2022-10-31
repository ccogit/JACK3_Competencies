package de.uni_due.s3.jack3.enums;

import de.uni_due.s3.jack3.entities.tenant.Submission;

/**
 * Represents various states that a {@link Submission} object has regarding the life cycle when submitting an exercise.
 * 
 * @author lukas.glaser
 */
public enum ESubmissionStatus {

	/** The exercise has not yet been submitted, there is no submission available yet. */
	NOT_STARTED,

	/** The exercise has been started, but no action (SUBMIT, SKIP, HINT) has been executed */
	STARTED_UNPROCESSED,

	/** The exercise was started and actions were performed, but the submission is not completed. */
	PARTLY_PROCESSED,

	/** The submission is completed. */
	COMPLETED;
}
