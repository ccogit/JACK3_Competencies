package de.uni_due.s3.jack3.tests.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.uni_due.s3.jack3.entities.AbstractEntity;

public final class Assert {

	private Assert() {
	}

	public static <T extends AbstractEntity> void assertEqualsEntityListUnordered(List<T> expected, List<T> actual) {

		final List<T> expectedNew = new ArrayList<>(expected);
		final List<T> actualNew = new ArrayList<>(actual);

		Collections.sort(expectedNew, Comparator.comparing(AbstractEntity::getId));
		Collections.sort(actualNew, Comparator.comparing(AbstractEntity::getId));

		assertEquals(expectedNew, actualNew);
	}

	public static <T extends Comparable<T>> void assertEqualsListUnordered(List<T> expected, List<T> actual) {

		final List<T> expectedNew = new ArrayList<>(expected);
		final List<T> actualNew = new ArrayList<>(actual);

		Collections.sort(expectedNew);
		Collections.sort(actualNew);

		assertEquals(expectedNew, actualNew);
	}

}
