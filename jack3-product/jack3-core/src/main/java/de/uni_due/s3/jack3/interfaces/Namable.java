package de.uni_due.s3.jack3.interfaces;

import java.util.Comparator;

import javax.annotation.Nonnull;

/**
 * This interface denotes entities that have a name. The name must always be present, but does not have to be unique.
 */
public interface Namable {

	public static final Comparator<Namable> NAME_COMPARATOR = Comparator.comparing(Namable::getName);

	/**
	 * @return A user-defined {@link String} that does not contain any internal information.
	 */
	@Nonnull
	String getName();

}
