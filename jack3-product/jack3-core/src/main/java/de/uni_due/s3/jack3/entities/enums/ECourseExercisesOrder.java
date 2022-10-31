package de.uni_due.s3.jack3.entities.enums;

import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;

/**
 * Options for sorting exercises in a course.
 */
public enum ECourseExercisesOrder {

	// NOTE That if you add new types here, you maybe have to update "isExerciseOrderSupported" in sub classes of "AbstractExerciseProvider"

	/**
	 * Order is based on the name of the exercise (A-Z)
	 * 
	 * @see AbstractExercise#name
	 */
	ALPHABETIC_ASCENDING,

	/**
	 * Order is based on the name of the exercise (Z-A)
	 * 
	 * @see AbstractExercise#name
	 */
	ALPHABETIC_DESCENDING,

	/**
	 * Order is based on difficulty (from easy to difficult)
	 * 
	 * @see AbstractExercise#difficulty
	 */
	DIFFICULTY_ASCENDING,

	/**
	 * Order is based on difficulty (from difficult to easy)
	 * 
	 * @see AbstractExercise#difficulty
	 */
	DIFFICULTY_DESCENDING,

	/**
	 * Order is based on the score (only available in a {@link FixedListExerciseProvider}).
	 * 
	 * @see CourseEntry#points
	 */
	POINTS_ASCENDING,

	/**
	 * Order is based on the score (only available in a {@link FixedListExerciseProvider}).
	 * 
	 * @see CourseEntry#points
	 */
	POINTS_DESCENDING,

	/**
	 * Order is based on the number of submissions for an exercise. Exercises with no / few submissions come first. This
	 * order type is calculated dynamically each time a student calls the course.
	 */
	NUMBER_OF_SUBMISSIONS,

	/**
	 * Order is set manually by the creator of the course.
	 */
	MANUAL;
}
