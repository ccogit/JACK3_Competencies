package de.uni_due.s3.jack3.entities.enums;

/**
 * Enumeration of the different options for when students can see the submission review.
 */
public enum ECourseOfferReviewMode {

	/** Immediately after the fist submission */
	ALWAYS,

	/** After completion of the course */
	AFTER_EXIT,

	/** After course offer deadline */
	AFTER_END,

	/** Lecturer sets submission review visible after manually checking all results */
	AFTER_REVIEW, // TODO not implemented yet

	/** Submission review is not visible at all */
	NEVER;
}
