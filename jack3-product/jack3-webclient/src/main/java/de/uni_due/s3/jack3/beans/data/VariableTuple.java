package de.uni_due.s3.jack3.beans.data;

import java.io.Serializable;
import java.util.Objects;

import de.uni_due.s3.jack3.entities.tenant.VariableValue;

/**
 * Class for merging the necessary Data from VariableValue and VariableDeclaration for displaying in
 * exercisePlayer.xhtml
 */
public class VariableTuple implements Serializable {

	/**
	 * The different variable types ("normal" variable, input variable, meta variable)
	 */
	public enum EVariableType {
		VAR, INPUT, META;

		public String getDisplayName() {
			return name().toLowerCase();
		}
	}

	private static final long serialVersionUID = -5615348482633577503L;
	/** Key of the variable */
	private final String name;
	/** Value of the variable */
	private final VariableValue value;
	/** Type of the variable */
	private final EVariableType type;

	public VariableTuple(String name, VariableValue variableValue, EVariableType varType) {
		this.name = name;
		this.value = variableValue;
		this.type = varType;
	}

	public String getName() {
		return name;
	}

	public VariableValue getValue() {
		return value;
	}

	public EVariableType getType() {
		return type;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}

		VariableTuple other = (VariableTuple) obj;
		return this.type == other.type && this.name.equals(other.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, type);
	}
}
