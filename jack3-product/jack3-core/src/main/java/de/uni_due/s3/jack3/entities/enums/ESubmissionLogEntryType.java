package de.uni_due.s3.jack3.entities.enums;

import de.uni_due.s3.jack3.entities.tenant.SubmissionLogEntry;

/**
 * The type of a submission log entry
 *
 * @see SubmissionLogEntry
 */
public enum ESubmissionLogEntryType {

	/** A student enters an exercise. */
	ENTER,

	/** A student receives a hint. */
	HINT,

	/** A student submits a submission. */
	SUBMIT,

	/** A student skips the current stage. */
	SKIP,

	/** A student leaves the stage / the exercise. */
	EXIT,

	/** A student erases all input for some stage and all subsequent ones. */
	ERASE,

	/** There is an incoming result. */
	CHECK,

	/** A variable gets updated in the course of the submission of a submission. */
	VAR_UPDATE,

	/** A student get's redirected to the very same stage. */
	REPEAT,

	/** The exercise ends. */
	END,

	/** An error occurs while processing the exercise. */
	FAIL;
}
