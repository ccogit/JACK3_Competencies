package de.uni_due.s3.jack3.entities.enums;

import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.Enrollment;

/**
 * Different enrollment status a user can have.
 * 
 * @see Enrollment
 * @author lukas.glaser
 */
public enum EEnrollmentStatus {

	/**
	 * The user is registered in to the {@link CourseOffer}. Only in this state he/she can start {@link CourseRecord}s
	 * and submit exercises. This state can be reached if
	 * <ul>
	 * <li>the user enrolls himself or herself</li>
	 * <li>the user is enrolled automatically if he/she moves up from the waiting list</li>
	 * <li>the user is manually enrolled by a lecturer</li>
	 * </ul>
	 */
	ENROLLED,

	/**
	 * The user is disenrolled from the {@link CourseOffer}. This means that he was previously enrolled or was on the
	 * waiting list and disenrolled him/herself or was manually disenrolled by a teacher.
	 */
	DISENROLLED,

	/**
	 * The user has placed him/herself on the waiting list and has the opportunity to move up to the registration
	 * deadline.
	 */
	ON_WAITINGLIST;

}
