package de.uni_due.s3.jack3.entities.stagetypes.r;

/**
 * This Enum has the same job as the &lt;rule type="... in the Jack2 XML files. It determines if the corresponding rule
 * has to present or absent to grant the referenced points.
 *
 * @author Benjamin Otto
 *
 */

public enum ETestcaseRuleMode {
	//TODO internationalization
	ABSENCE("Absence"), // To get the declared Points of this rule, the corresponding rule must be absent
	PRESENCE("Presence"); // To get the declared Points of this rule, the corresponding rule must be present

	private String mode;
	ETestcaseRuleMode(String mode) {
		this.mode = mode;
	}

	@Override
	public String toString() {
		return mode;
	}

}
