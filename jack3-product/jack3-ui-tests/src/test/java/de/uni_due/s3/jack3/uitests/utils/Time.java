package de.uni_due.s3.jack3.uitests.utils;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.opentest4j.AssertionFailedError;

public final class Time {

	// All timeouts in seconds
	public static final int TIMEOUT_DEFAULT = 4;
	public static final int TIMEOUT_MIN = 2;
	public static final int TIMEOUT_WAIT = 10;

	/**
	 * Wait until an element is present on the DOM of the current page.
	 * 
	 * @param by
	 *            Element locator
	 * @see ExpectedConditions#presenceOfElementLocated(By)
	 */
	public static void waitPresent(By by) {
		wait(ExpectedConditions.presenceOfElementLocated(by), null);
	}

	/**
	 * Wait until an element is visible on the current page.
	 * 
	 * @param by
	 *            Element locator
	 * @see ExpectedConditions#visibilityOfElementLocated(By)
	 */
	public static void waitVisible(By by) {
		wait(ExpectedConditions.visibilityOfElementLocated(by), null);
	}

	/**
	 * Wait until an element is visible on the current page.
	 * 
	 * @param by
	 *            Element
	 * @see ExpectedConditions#visibilityOf(WebElement)
	 */
	public static void waitVisible(WebElement webElement) {
		wait(ExpectedConditions.visibilityOf(webElement), null);
	}

	/**
	 * Wait until an element is clickable.
	 * 
	 * @param by
	 *            Element locator
	 * @see ExpectedConditions#elementToBeClickable(By)
	 */
	public static void waitClickable(By by) {
		wait(ExpectedConditions.elementToBeClickable(by), null);
	}

	/**
	 * Wait until an element is clickable.
	 * 
	 * @param by
	 *            Element
	 * @see ExpectedConditions#elementToBeClickable(WebElement)
	 */
	public static void waitClickable(WebElement webElement) {
		wait(ExpectedConditions.elementToBeClickable(webElement), null);
	}

	/**
	 * @see #waitPresent(By)
	 */
	public static void waitNotPresent(By by) {
		wait(ExpectedConditions.not(ExpectedConditions.presenceOfElementLocated(by)), null);
	}

	/**
	 * @see #waitVisible(By)
	 */
	public static void waitNotVisible(By by) {
		wait(ExpectedConditions.not(ExpectedConditions.visibilityOfElementLocated(by)), null);
	}

	/**
	 * @see #waitVisible(WebElement)
	 */
	public static void waitNotVisible(WebElement webElement) {
		wait(ExpectedConditions.not(ExpectedConditions.visibilityOf(webElement)), null);
	}

	/**
	 * @see #waitClickable(By)
	 */
	public static void waitNotClickable(By by) {
		wait(ExpectedConditions.not(ExpectedConditions.elementToBeClickable(by)), null);
	}

	/**
	 * @see #waitClickable(WebElement)
	 */
	public static void waitNotClickable(WebElement webElement) {
		wait(ExpectedConditions.not(ExpectedConditions.elementToBeClickable(webElement)), null);
	}

	/**
	 * Wait until an element is present on the DOM of the current page.
	 * @param by
	 *            Element locator
	 * @param message
	 *            Message for failure
	 * 
	 * @see ExpectedConditions#presenceOfElementLocated(By)
	 */
	public static void waitPresent(By by, String message) {
		wait(ExpectedConditions.presenceOfElementLocated(by), message);
	}

	/**
	 * Wait until an element is visible on the current page.
	 * @param by
	 *            Element locator
	 * @param message
	 *            Message for failure
	 * 
	 * @see ExpectedConditions#visibilityOfElementLocated(By)
	 */
	public static void waitVisible(By by, String message) {
		wait(ExpectedConditions.visibilityOfElementLocated(by), message);
	}

	/**
	 * Wait until an element is visible on the current page.
	 * @param message
	 *            Message for failure
	 * @param by
	 *            Element
	 * 
	 * @see ExpectedConditions#visibilityOf(WebElement)
	 */
	public static void waitVisible(WebElement webElement, String message) {
		wait(ExpectedConditions.visibilityOf(webElement), message);
	}

	/**
	 * Wait until an element is clickable.
	 * @param by
	 *            Element locator
	 * @param message
	 *            Message for failure
	 * 
	 * @see ExpectedConditions#elementToBeClickable(By)
	 */
	public static void waitClickable(By by, String message) {
		wait(ExpectedConditions.elementToBeClickable(by), message);
	}

	/**
	 * Wait until an element is clickable.
	 * @param message
	 *            Message for failure
	 * @param by
	 *            Element
	 * 
	 * @see ExpectedConditions#elementToBeClickable(WebElement)
	 */
	public static void waitClickable(WebElement webElement, String message) {
		wait(ExpectedConditions.elementToBeClickable(webElement), message);
	}

	/**
	 * @see #waitPresent(By)
	 */
	public static void waitNotPresent(By by, String message) {
		wait(ExpectedConditions.not(ExpectedConditions.presenceOfElementLocated(by)), message);
	}

	/**
	 * @see #waitVisible(By)
	 */
	public static void waitNotVisible(By by, String message) {
		wait(ExpectedConditions.not(ExpectedConditions.visibilityOfElementLocated(by)), message);
	}

	/**
	 * @see #waitVisible(WebElement)
	 */
	public static void waitNotVisible(WebElement webElement, String message) {
		wait(ExpectedConditions.not(ExpectedConditions.visibilityOf(webElement)), message);
	}

	/**
	 * @see #waitClickable(By)
	 */
	public static void waitNotClickable(By by, String message) {
		wait(ExpectedConditions.not(ExpectedConditions.elementToBeClickable(by)), message);
	}

	/**
	 * @see #waitClickable(WebElement)
	 */
	public static void waitNotClickable(WebElement webElement, String message) {
		wait(ExpectedConditions.not(ExpectedConditions.elementToBeClickable(webElement)), message);
	}

	/**
	 * @see WebDriverWait#until(java.util.function.Function)
	 */
	public static void wait(ExpectedCondition<?> condition, String message) {
		wait(condition, TIMEOUT_WAIT, message);
	}

	/**
	 * @see WebDriverWait#until(java.util.function.Function)
	 */
	public static void wait(ExpectedCondition<?> condition, int timeout, String message) {
		WebDriverWait wait = new WebDriverWait(Driver.get(), timeout);
		if (message != null) {
			try {
				wait.until(condition);
			} catch (TimeoutException e) {
				throw new AssertionFailedError(message, e);
			}
		} else {
			wait.until(condition);
		}
	}
	
	public static void waitUntilActionBarIsNotActive() {
		Time.wait(ExpectedConditions.attributeToBe(By.id("activity-bar"), "class", ""), "The action bar didn't stop to be active");
	}

	/**
	 * Configures the default implicit timeout while waiting for presence/visibility/... of a web element.
	 */
	public static void configureTimeout() {
		Driver.get().manage().timeouts().implicitlyWait(TIMEOUT_DEFAULT, TimeUnit.SECONDS);
	}

	/**
	 * Runs an action with the minimal timeout. After the action was performed, the default timeouts will be set.
	 */
	public static void runWithMinTimeout(Runnable action) {
		Driver.get().manage().timeouts().implicitlyWait(TIMEOUT_MIN, TimeUnit.SECONDS);
		action.run();
		Driver.get().manage().timeouts().implicitlyWait(TIMEOUT_DEFAULT, TimeUnit.SECONDS);
	}

	/**
	 * Runs an action without timeout. After the action was performed, the default timeouts will be set.
	 */
	public static void runWithNoTimeout(Runnable action) {
		Driver.get().manage().timeouts().implicitlyWait(TIMEOUT_MIN, TimeUnit.SECONDS);
		action.run();
		Driver.get().manage().timeouts().implicitlyWait(TIMEOUT_DEFAULT, TimeUnit.SECONDS);
	}

}
