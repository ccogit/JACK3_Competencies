package de.uni_due.s3.jack3.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

public class JackStringUtils {

	private JackStringUtils() {
		throw new AssertionError("This class shouldn't be instantiated.");
	}

	/**
	 * Returns {@code true} if the passed String is {@code null}, empty or contains only whitespace characters.
	 */
	public static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	/**
	 * Returns {@code true} if the passed String contains non-whitespace characters.
	 */
	public static boolean isNotBlank(String s) {
		return s != null && !s.isBlank();
	}

	/**
	 * If the passed string is not blank, it is trimmed by leading and trailing whitespace characters. Otherwise,
	 * {@code null} is returned.
	 * 
	 * @see #isBlank(String)
	 */
	@CheckForNull
	public static String stripOrNull(String s) {
		return isBlank(s) ? null : s.strip();
	}

	/**
	 * If the passed string is not blank, it is trimmed by leading and trailing whitespace characters. Otherwise, an
	 * empty String is returned.
	 * 
	 * @see #isBlank(String)
	 */
	@Nonnull
	public static String stripOrEmpty(String s) {
		return isBlank(s) ? "" : s.strip();
	}

	/**
	 * Splits the passed string based on line breaks. Each line is trimmed by leading and trailing whitespace
	 * characters.
	 */
	@Nonnull
	public static List<String> splitAndStripLines(String s) {
		return Arrays.stream(s.split("\r?\n"))
				.filter(JackStringUtils::isNotBlank)
				.map(String::strip)
				.collect(Collectors.toList());
	}

	/**
	 * Compares two Strings ignoring case consideration.
	 */
	public static boolean equalsIgnoreCase(final String s1, final String s2) {
		if (s1 == null || s2 == null) {
			return s1 == s2; // NOSONAR
		} else {
			return s1.equalsIgnoreCase(s2);
		}
	}

}
