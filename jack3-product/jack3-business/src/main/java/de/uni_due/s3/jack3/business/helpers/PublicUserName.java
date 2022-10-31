package de.uni_due.s3.jack3.business.helpers;

import java.io.Serializable;
import java.util.Objects;

import de.uni_due.s3.jack3.utils.JackStringUtils;

/**
 * This small data class is used as a return-value for getting someone's public username. Use {@link #of(String)} or
 * {@link #ofPseudonym(String)} to create an object.
 * 
 * @author lukas.glaser
 */
public class PublicUserName implements Serializable, Comparable<PublicUserName> {

	private static final long serialVersionUID = -1131416759622611275L;

	private final String name;
	private final boolean pseudonym;

	private PublicUserName(String name, boolean pseudonym) {
		this.name = name;
		this.pseudonym = pseudonym;
	}

	public String getName() {
		return name;
	}

	public boolean isPseudonym() {
		return pseudonym;
	}

	@Override
	public String toString() {
		// This makes it possible to insert the object as a value in the UI where a String is expected
		return name;
	}

	@Override
	public int compareTo(PublicUserName o) {
		// User names are case insensitive
		return name.compareToIgnoreCase(o.name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof PublicUserName)) {
			return false;
		}
		PublicUserName other = (PublicUserName) obj;
		return JackStringUtils.equalsIgnoreCase(name, other.name) && pseudonym == other.pseudonym;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, pseudonym);
	}

	/**
	 * Constructs a new clear user name (not pseudonymized).
	 */
	public static PublicUserName of(String clearName) {
		return new PublicUserName(clearName, false);
	}

	/**
	 * Constructs a new pseudonymized user name.
	 */
	public static PublicUserName ofPseudonym(String pseudonym) {
		return new PublicUserName(pseudonym, true);
	}

}
