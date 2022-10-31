package de.uni_due.s3.jack3.entities.stagetypes.r;

/**
 * 
 * This Enum controls if points in the corresponding testcase are added or subtracted from the total of all testcases
 * in the testcase-tuple. Points total can't go below 0 or above 100! 
 * 
 * @author Benjamin Otto
 *
 */

public enum ETestCasePointsMode {

	DEDUCTION("Deduction"), // The points in this testcase are subtracted from the total (min. 0).   
	GAIN("Gain"); // The points in this testcase are added to the total (max. 100).

	private String mode;
	ETestCasePointsMode(String mode) {
		this.mode = mode;
	}

	@Override
	public String toString() {
		return mode;
	}

}
