package de.uni_due.s3.jack3.utils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class StringGenerator {

	public static class Builder {

		private final Set<Character> includes;

		private final Set<Character> excludes;

		private Random random;

		private int minLength;

		private int maxLength;

		private int groupSize;

		private char separator;

		private Builder() {
			this.includes = new HashSet<>();
			this.excludes = new HashSet<>();
		}

		public Builder include(final char c) {
			includes.add(c);
			return this;
		}

		public Builder includeRange(final char startInclusive,final char endInclusive) {
			if (startInclusive > endInclusive) {
				throw new IllegalArgumentException("startInclusive > endInclusive.");
			}
			for (char c = startInclusive; c < endInclusive; c++) {
				include(c);
			}
			return this;
		}

		public Builder exclude(final char c) {
			excludes.add(c);
			return this;
		}

		public Builder excludeAmbiguityCandidates() {
			excludes.addAll(Set.of('0','O','Q','I','1','j','i','l'));
			return this;
		}

		public Builder useSecureRandom() {
			this.random = new SecureRandom();
			return this;
		}

		public Builder setLength(final int minLength,final int maxLength) {
			if (minLength < 1) {
				throw new IllegalArgumentException("minLength must be positive.");
			}
			if (minLength > maxLength) {
				throw new IllegalArgumentException("minLength > maxLength");
			}
			this.minLength = minLength;
			this.maxLength = maxLength;

			return this;
		}

		public Builder setLength(final int length) {
			return setLength(length,length);
		}

		public Builder setGroupSize(final int groupSize) {
			if (groupSize < 0) {
				throw new IllegalArgumentException("groupSize must be positive or zero.");
			}
			this.groupSize = groupSize;
			return this;
		}

		public Builder setGroupSeparator(final char separator) {
			this.separator = separator;
			return this;
		}

		public final StringGenerator build() {
			return new StringGenerator(this);
		}
	}

	public static Builder forPasswords() {
		return new Builder()
			.includeRange('A','Z')
			.includeRange('a','z')
			.includeRange('0','9')
			.setLength(10,14)
			.excludeAmbiguityCandidates()
			.useSecureRandom();
	}

	public static Builder forPseudonyms() {
		return new Builder()
			.includeRange('A','Z')
			.includeRange('a','z')
			.setLength(9)
			.setGroupSize(3)
			.setGroupSeparator('-');
	}

	public static Builder forCoursePasswords() {
		return new Builder()
			.includeRange('a', 'z')
			.includeRange('0', '9')
			.excludeAmbiguityCandidates()
			.setLength(8);
	}

	private final char[] chars;

	private int minLength;

	private int maxLength;

	private int groupSize;

	private char separator;

	private Random random;

	public StringGenerator(final Builder builder) {
		List<Character> characterList = new ArrayList<>(builder.includes);
		characterList.removeAll(builder.excludes);
		characterList.remove(Character.valueOf(builder.separator));

		if (characterList.isEmpty()) {
			throw new IllegalStateException("Set of available characters is empty.");
		}

		this.chars = new char[characterList.size()];
		for (int i = 0; i < characterList.size(); i++) {
			chars[i] = characterList.get(i);
		}
		this.minLength = builder.minLength;
		this.maxLength = builder.maxLength;
		this.groupSize = builder.groupSize;
		this.separator = builder.separator;
		this.random = builder.random;
	}

	public final String generate() {
		final Random random = obtainRandom();

		int length = minLength + random.nextInt(maxLength - minLength + 1);
		if (groupSize > 0) {
			length += (length - 1) / groupSize;
		}

		final char[] buf = new char[length];
		for (int i = 0; i < buf.length; i++) {
			if (groupSize > 0 && (i + 1) % (groupSize + 1) == 0) {
				buf[i] = separator;
			} else {
				buf[i] = chars[random.nextInt(chars.length)];
			}
		}

		return String.valueOf(buf);
	}

	private final Random obtainRandom() {
		if (this.random != null) {
			return random;
		}
		return ThreadLocalRandom.current();
	}
}
