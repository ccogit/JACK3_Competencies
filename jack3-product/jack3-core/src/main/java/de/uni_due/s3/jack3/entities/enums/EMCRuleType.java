package de.uni_due.s3.jack3.entities.enums;

import de.uni_due.s3.jack3.entities.stagetypes.mc.MCAnswer;

/**
 * Correctness of a mc answer option.
 * 
 * @see MCAnswer
 */
public enum EMCRuleType {
	/**
	 * The answer is always wrong.
	 */
	WRONG,

	/**
	 * The answer is always correct.
	 */
	CORRECT,

	/**
	 * It does not matter if the answer is ticked.
	 */
	NO_MATTER,

	/**
	 * Correctness is bound to a variable
	 */
	VARIABLE;
}
