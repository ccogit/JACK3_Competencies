package de.uni_due.s3.jack3.business.helpers;

/**
 * Access that is granted to a user for a course offer.
 */
public enum ECourseOfferAccess {

	/**
	 * No access: Course offer is not visible.
	 */
	NONE,

	/**
	 * Lecturer access without edit rights.
	 */
	READ,
	
	/**
	 * Lecturer access with edit rights.
	 */
	EDIT,
	
	/**
	 * Student access: Course offer is visible from student's view.
	 */
	SEE_AS_STUDENT;
	
}
