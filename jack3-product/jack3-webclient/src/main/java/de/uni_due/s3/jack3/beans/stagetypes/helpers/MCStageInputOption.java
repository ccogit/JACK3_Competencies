package de.uni_due.s3.jack3.beans.stagetypes.helpers;

import java.io.Serializable;

import de.uni_due.s3.jack3.entities.enums.EMCRuleType;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCAnswer;

public class MCStageInputOption implements Serializable {

	private static final long serialVersionUID = -8640161095084482876L;

	public MCStageInputOption(MCAnswer answer) {
		this.text = answer.getText();
		if (answer.getRule() == EMCRuleType.VARIABLE) {
			this.rule = answer.getVariableName();
		} else {
			this.rule = answer.getRule().name();
		}
	}

	private final String text;

	/**
	 * The rule saves a variable name or "WRONG", "CORRECT", "NO_MATTER"
	 */
	private String rule = "";

	public String getText() {
		return text;
	}

	public String getRule() {
		return rule;
	}

	public void setRule(String rule) {
		try {
			// If "WRONG", "CORRECT" or "NO_MATTER", enum name is saved
			this.rule = EMCRuleType.valueOf(rule).name();
		} catch (IllegalArgumentException | NullPointerException e) {
			// Save variable name otherwise
			this.rule = rule;
		}
	}

}
