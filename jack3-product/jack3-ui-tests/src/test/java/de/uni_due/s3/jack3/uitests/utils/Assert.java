package de.uni_due.s3.jack3.uitests.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.opentest4j.AssertionFailedError;

/**
 * Provides Selenium-specific assertions
 */
public final class Assert {

	/**
	 * Assert that an element is present in the DOM of the current page.
	 * 
	 * @param by
	 *            Element locator
	 * @throws AssertionFailedError
	 *             If element is not present after a short waiting period.
	 */
	public static void assertPresent(By by) {
		assertPresent(by, null);
	}

	/**
	 * Assert that an element is clickable.
	 * @param by
	 *            Element locator
	 * @param message
	 *            Message for failure
	 * @throws AssertionFailedError
	 *             If element is not clickable after a short waiting period.
	 */
	public static void assertPresent(By by, String message) {
		WebDriverWait wait = new WebDriverWait(Driver.get(), Time.TIMEOUT_MIN);
		try {
			wait.until(ExpectedConditions.presenceOfElementLocated(by));
		} catch (TimeoutException e) {
			fail(message);
		}
	}

	/**
	 * Assert that an element is not present in the DOM of the current page.
	 * 
	 * @param by
	 *            Element locator
	 * @throws AssertionFailedError
	 *             If element is still present after a short waiting period.
	 */
	public static void assertNotPresent(By by) {
		assertNotPresent(by, null);
	}

	/**
	 * Assert that an element is not present in the DOM of the current page.
	 * @param by
	 *            Element locator
	 * @param message
	 *            Message for failure
	 * @throws AssertionFailedError
	 *             If element is still present after a short waiting period.
	 */
	public static void assertNotPresent(By by, String message) {
		Time.runWithMinTimeout(
				() -> Misc.expectException(message, AssertionFailedError.class, () -> assertPresent(by)));
	}

	/**
	 * Assert that an element is visible on the current page.
	 * 
	 * @param by
	 *            Element locator
	 * @throws AssertionFailedError
	 *             If element is not visible after a short waiting period.
	 */
	public static void assertVisible(By by) {
		assertVisible(by, null);
	}

	/**
	 * Assert that an element is clickable.
	 * @param by
	 *            Element locator
	 * @param message
	 *            Message for failure
	 * @throws AssertionFailedError
	 *             If element is not clickable after a short waiting period.
	 */
	public static void assertVisible(By by, String message) {
		WebDriverWait wait = new WebDriverWait(Driver.get(), Time.TIMEOUT_MIN);
		try {
			wait.until(ExpectedConditions.visibilityOfElementLocated(by));
		} catch (TimeoutException e) {
			fail(message);
		}
	}

	/**
	 * Assert that an element is not visible on the current page.
	 * 
	 * @param message
	 *            Message for failure
	 * @param by
	 *            Element locator
	 * @throws AssertionFailedError
	 *             If element is still visible after a short waiting period.
	 */
	public static void assertNotVisible(By by) {
		assertNotVisible(by, null);
	}

	/**
	 * Assert that an element is not visible on the current page.
	 * @param by
	 *            Element locator
	 * @param message
	 *            Message for failure
	 * 
	 * @throws AssertionFailedError
	 *             If element is still visible after a short waiting period.
	 */
	public static void assertNotVisible(By by, String message) {
		Time.runWithMinTimeout(
				() -> Misc.expectException(message, AssertionFailedError.class, () -> assertVisible(by)));
	}

	/**
	 * Assert that an element is clickable.
	 * 
	 * @param by
	 *            Element locator
	 * @throws AssertionFailedError
	 *             If element is not clickable after a short waiting period.
	 */
	public static void assertClickable(By by) {
		assertClickable(by, null);
	}

	/**
	 * Assert that an element is clickable.
	 * @param by
	 *            Element locator
	 * @param message
	 *            Message for failure
	 * 
	 * @throws AssertionFailedError
	 *             If element is not clickable after a short waiting period.
	 */
	public static void assertClickable(By by, String message) {
		WebDriverWait wait = new WebDriverWait(Driver.get(), Time.TIMEOUT_MIN);
		try {
			wait.until(ExpectedConditions.elementToBeClickable(by));
		} catch (TimeoutException e) {
			fail(message);
		}
	}

	/**
	 * Assert that an element is not clickable.
	 * 
	 * @param by
	 *            Element locator
	 * @throws AssertionFailedError
	 *             If element is still clickable after a short waiting period.
	 */
	public static void assertNotClickable(By by) {
		assertNotClickable(by, null);
	}

	/**
	 * Assert that an element is not clickable.
	 * @param by
	 *            Element locator
	 * @param message
	 *            Message for failure
	 * 
	 * @throws AssertionFailedError
	 *             If element is still clickable after a short waiting period.
	 */
	public static void assertNotClickable(By by, String message) {
		Time.runWithMinTimeout(
				() -> Misc.expectException(message, AssertionFailedError.class, () -> assertClickable(by)));
	}

	public static <T extends Comparable<T>> void assertUnorderedListEquals(List<T> expected, List<T> actual) {

		final List<T> expectedNew = new ArrayList<>(expected);
		final List<T> actualNew = new ArrayList<>(actual);

		Collections.sort(expectedNew);
		Collections.sort(actualNew);

		assertEquals(expectedNew, actualNew);
	}

}
