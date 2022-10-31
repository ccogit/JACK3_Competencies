package de.uni_due.s3.jack3.uitests.utils;

import static de.uni_due.s3.jack3.uitests.utils.Click.click;
import static de.uni_due.s3.jack3.uitests.utils.Click.clickWithRedirect;
import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.graphene.request.RequestGuardException;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.opentest4j.AssertionFailedError;

public final class Misc {

	public static final By LOGIN_BUTTON = By.id("loginForm:loginButton");
	public static final By LOGOUT_BUTTON = By.id("menubar:buttonMenubarLogout");

	// Change this to allow more/less login tries
	public static final int MAX_LOGIN_TRIES = 8;

	/**
	 * <p>
	 * Checks that a specific action throws an exception. A test case is only successful if an exception was thrown and
	 * the thrown exception class is the <u>same</u> class as the expected one. Type hierarchy is not checked. The test
	 * case fails if no exception was thrown. Any {@link Throwable} can be expected.
	 * <p>
	 *
	 * <p>
	 * Example:
	 * </p>
	 *
	 * <pre>
	 * expectException(IllegalArgumentException.class, () -> rule.setPoints(-1));
	 * </pre>
	 *
	 * @param action
	 *            Action that should throw something as a runnable (feel free to use Lambdas).
	 * @param expected
	 *            The class of the expected Exception.
	 */
	public static void expectException(Class<? extends Throwable> expected, Runnable action) {
		expectException(null, expected, action);
	}

	/**
	 * <p>
	 * Checks that a specific action throws an exception. A test case is only successful if an exception was thrown and
	 * the thrown exception class is the <u>same</u> class as the expected one. Type hierarchy is not checked. The test
	 * case fails if no exception was thrown. Any {@link Throwable} can be expected.
	 * <p>
	 *
	 * <p>
	 * Example:
	 * </p>
	 *
	 * <pre>
	 * expectException(IllegalArgumentException.class, () -> rule.setPoints(-1));
	 * </pre>
	 *
	 * @param message
	 *            Message for failure if no exception occurred
	 * @param action
	 *            Action that should throw something as a runnable (feel free to use Lambdas).
	 * @param expected
	 *            The class of the expected Exception.
	 */
	public static void expectException(String message, Class<? extends Throwable> expected, Runnable action) {
		try {
			action.run();
		} catch (Throwable e) {
			if (expected.equals(e.getClass())) {
				return;
			} else {
				fail(String.format("Exception expected: <%s>, but was: <%s>", expected.getName(),
						e.getClass().getName()));
			}
		}
		fail(message);
	}

	/**
	 * Performs a login.
	 *
	 * @param username
	 *            Login name of the user
	 * @param password
	 *            Plain password of the user
	 */
	public static void login(String username, String password) {
		navigate(JackUrl.AVAILABLE_COURSES);
		File[] screenshots = new File[MAX_LOGIN_TRIES];

		// Evaluate if a user (and the correct user) is already logged in
		if (isUserLoggedIn()) {

			// Extract the current user from the welcome message
			String loggedInUsername = find("menubar:menubarAccount").getText();
			if (loggedInUsername.equalsIgnoreCase(username)) {
				// The correct user is already logged in
				return;
			}

			// We must logout the current user to login the correctuser
			logout();
		}

		// We try the Login several times.
		for (int tryNumber = 0; tryNumber < MAX_LOGIN_TRIES; tryNumber++) {

			// We are at the login page and can enter the credentials
			find("loginForm:loginUsername").clear();
			find("loginForm:loginUsername").sendKeys(username);
			find("loginForm:loginPasswordField").clear();
			find("loginForm:loginPasswordField").sendKeys(password);

			screenshots[tryNumber] = makeScreenshotWithoutSaving();
			try {
				clickWithRedirect(LOGIN_BUTTON);
			} catch (RequestGuardException e) {
				// We just ignore the Exception because we try it again (until the max number of tries is reached)
				System.out.println("[Login Error] Try " + tryNumber + " - RequestGuardException: " + e.getMessage());
			}

			// We wait until username appears in the menubar
			try {
				Time.waitVisible(By.id("menubar:menubarAccount"));
				// Login successful
				return;
			} catch (AssertionFailedError e) {
				// We just ignore the error because we try it again (until the max number of tries is reached)
				System.out.println("[Login Error] Try " + tryNumber + " - AssertionFailedError: " + e.getMessage());
			} catch (TimeoutException e) {
				// We just ignore the error because we try it again (until the max number of tries is reached)
				System.out.println("[Login Error] Try " + tryNumber + " - TimeoutException: " + e.getMessage());
			}
		}

		System.out.println("[Login Error] Max number of tries reached.");

		// We are still not logged in, so let the test fail
		for (int i = 0; i < screenshots.length; i++) {
			saveScreenshot(screenshots[i], "login-failure-try-" + (i + 1));
		}

		String screenshotError = makeScreenshot("login-failure-max-tries-reached");
		throw new Error("Login was not successful. Max number of tries reached. " + screenshotError);
	}

	/**
	 * Performs a logout.
	 */
	public static void logout() {
		// This forces the browser to load a page => it prevents logout failing due to shown dialogs
		navigate(JackUrl.AVAILABLE_COURSES);

		if (!isUserLoggedIn()) {
			// Already logged out
			return;
		}

		try {
			openUserSubmenu();
			clickWithRedirect(LOGOUT_BUTTON);
		} catch (Exception e) {
			String screenshot = makeScreenshot("logout-failure");
			throw new Error("Logout was not successful. " + screenshot, e);
		}

		navigate(JackUrl.AVAILABLE_COURSES);
	}

	public static void openUserSubmenu() {
		final WebElement submenuButton = find("menubar:main-menubar")
				.findElement(By.tagName("ul"))
				.findElement(By.id("menubar:menubarAccount"));
		click(submenuButton);
	}

	public static void openAdministrationSubmenu() {
		WebElement submenuButton = find("menubar:main-menubar")
				.findElement(By.tagName("ul"));
		try {
			submenuButton.findElement(By.id("menubar:menubarAdministration")).click();
		} catch (NoSuchElementException e) {
			throw new AssertionFailedError("The Administration submenu was not found.", e);
		}
	}

	/**
	 * @return TRUE if a user is logged in, FALSE if a user is logged out, Exception if neither a logout button nor a
	 *         login button was found.
	 */
	private static boolean isUserLoggedIn() {
		try {
			// Evaluate if the user is already logged in. This is the case when the logout button is shown.
			find(LOGOUT_BUTTON);
			return true;
		} catch (NoSuchElementException loginNotFoundException) {
			try {
				// Evaluate if the user is logged out. This is the case when the login button is shown.
				find(LOGIN_BUTTON);
				return false;
			} catch (NoSuchElementException logoutNotFoundException) {
				String screenshot = makeScreenshot("check-no-button");
				throw new Error("Neither a logout button nor a login button was found. " + screenshot);
			}
		}
	}

	/**
	 * Wait until the complete Page has been loaded
	 */
	public static void waitUntilPageHasLoaded() {
		WebDriverWait wait = new WebDriverWait(Driver.get(), 15);
		wait.until(driver1 -> ((JavascriptExecutor) driver1).executeScript("return document.readyState")
				.equals("complete"));
	}

	/**
	 * Sometimes a UI element is outside the visible (virtual) window frame. This happens especially in headless Firefox. This method executes JavaScript to scroll to the corresponding element.
	 */
	public static void scrollElementIntoView(WebElement element) {
		((JavascriptExecutor) Driver.get()).executeScript("arguments[0].scrollIntoView(true);", element);
	}

	/**
	 * Waits until the developer presses ENTER on the console.
	 */
	public static void waitDebug() {
		try {
			System.out.println("Debug Breakpoint, press ENTER to continue > ");
			System.in.read();
		} catch (IOException e) {
			System.err.println("Error in waitDebug()!");
			e.printStackTrace();
		}
	}

	/**
	 * Navigate to the specified web page.
	 *
	 * @param url
	 *            URL of the web page <strong>without(!)</strong> the domain and port, e.g. "public/login.xhtml"
	 */
	public static void navigate(String url) {
		Time.configureTimeout();
		Driver.get().get(Driver.getUrl().toExternalForm() + url);
		waitUntilPageHasLoaded();
	}

	/**
	 * Reload the current Page
	 */
	public static void reloadPage() {
		Driver.get().navigate().refresh();
		waitUntilPageHasLoaded();
	}

	/**
	 * Makes a screenshot. The file will be copied to <code>target/screenshot...</code>
	 *
	 * @return filename if screenshot was successful
	 */
	public static String makeScreenshot() {
		try {
			File screenshot = ((TakesScreenshot) Driver.get()).getScreenshotAs(OutputType.FILE);
			String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS"));
			String filename = String.format("./target/screenshot_%s.jpg", time);
			FileUtils.copyFile(screenshot, new File(filename));
			return "Screenshot: " + filename;
		} catch (Exception e) {
			return "Screenshot n.a.";
		}
	}

	/**
	 * Makes a screenshot. The file will be copied to <code>target/screenshot...</code>
	 *
	 * @param filename
	 *            file name without file extension
	 * @return filename if screenshot was successful
	 */
	public static String makeScreenshot(String filename) {
		try {
			File screenshot = ((TakesScreenshot) Driver.get()).getScreenshotAs(OutputType.FILE);
			String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS"));
			filename = String.format("./target/screenshot_%s_%s.png", time, filename);
			FileUtils.copyFile(screenshot, new File(filename));
			return "Screenshot: " + filename;
		} catch (Exception e) {
			return "Screenshot n.a.";
		}
	}

	/**
	 * Makes a screenshot but it will not be copied to <code>target/screenshot...</code>
	 *
	 * @return the screenshot
	 */
	public static File makeScreenshotWithoutSaving() {
		return ((TakesScreenshot) Driver.get()).getScreenshotAs(OutputType.FILE);
	}

	/**
	 * Saves a screenshot to <code>target/screenshot...</code>
	 *
	 * @param screenshot
	 *            the screenshot that will be saved
	 * @param filename
	 *            file name without file extension
	 * @return filename if saving the screenshot was successful
	 */
	public static String saveScreenshot(File screenshot, String filename) {
		try {
			String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS"));
			filename = String.format("./target/screenshot_%s_%s.jpg", time, filename);
			FileUtils.copyFile(screenshot, new File(filename));
			return "Screenshot: " + filename;
		} catch (Exception e) {
			return "Screenshot n.a.";
		}
	}

	public static String formatDate(LocalDate date) {
		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMANY);
		return date.format(formatter);
	}

	public static String formatDateTime(LocalDateTime date) {
		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d. MMMM yyyy, HH\\:mm\\:ss", Locale.GERMANY);
		return date.format(formatter);
	}

	/**
	 * Skips the current test if an exception occurred during execution.
	 * @param r
	 *            Operation to execute
	 * @param message
	 *            Additional message
	 */
	// assumeNoException is not part of the JUnit framework anymore
	public static void assumeNoException(Runnable r, String message) {
		boolean exceptionThrown = false;
		try {
			r.run();
		} catch (Exception e) {
			exceptionThrown = true;
		}
		assumeFalse(exceptionThrown);
	}

	/**
	 * Skips the current test if no user is logged in
	 */
	public static void assumeLogin() {
		assumeNoException(() -> find(LOGOUT_BUTTON), "No user was logged in.");
	}

	/**
	 * Assert that a user was logged in
	 */
	public static void assertLogin() {
		try {
			find(LOGOUT_BUTTON);
		} catch (Exception e) {
			throw new AssertionFailedError("No user was logged in.");
		}
	}

	// TODO add "assumeLogin(String expectedUsername)"

}
