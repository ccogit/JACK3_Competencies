package de.uni_due.s3.jack3.interfaces;

import java.util.Optional;

import javax.annotation.Nonnull;

import de.uni_due.s3.jack3.entities.tenant.CourseOffer;

/**
 * An entity class that represents a submission by a student or a testing submission should implement this interface.
 */
public interface TestableSubmission {
	
	/**
	 * If the submission was created during a manual test by a lecturer.
	 */
	boolean isTestSubmission();

	/**
	 * If the submission is a regular submission, this method returns the linked course offer. For test submissions, an
	 * empty Optional is returned.
	 */
	@Nonnull
	Optional<CourseOffer> getCourseOffer();

}
