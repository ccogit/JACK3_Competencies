package de.uni_due.s3.jack3.entities.enums;

import de.uni_due.s3.jack3.entities.tenant.ResultFeedbackMapping;

/**
 * Wether and which total result of a course record should be displayed after completion of a course.
 */
public enum ECourseResultDisplay {

	/** No results are shown */
	NONE,

	/** Total score is shown */
	POINTS,

	/**
	 * A feedback text is shown
	 * 
	 * @see ResultFeedbackMapping
	 */
	TEXT,

	/** Both feedback text and the score are shown */
	BOTH;

}
