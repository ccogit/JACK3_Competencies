package de.uni_due.s3.jack3.entities.enums;

import de.uni_due.s3.jack3.exceptions.DeepCloningException;

/**
 * Diffentiates the possible Causes of a {@link DeepCloningException}
 */
public enum EDeepCopyExceptionErrorCode {

	/** Only courses with fixed-list-providers can be deep-copied. */
	ONLY_FIXEDLIST_EXERCISEPROVIDER_ALLOWED,

	/** Only courses that consist solely of frozen exercises can be deep-copied. */
	ONLY_FROZEN_EXERCISES_IN_FROZENCOURSES_ALLOWED

}
