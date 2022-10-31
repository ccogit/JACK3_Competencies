package de.uni_due.s3.jack3.uitests;

import static de.uni_due.s3.jack3.uitests.utils.Assert.assertNotVisible;
import static de.uni_due.s3.jack3.uitests.utils.Assert.assertVisible;
import static de.uni_due.s3.jack3.uitests.utils.Misc.navigate;
import static de.uni_due.s3.jack3.uitests.utils.Misc.openUserSubmenu;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitVisible;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import de.uni_due.s3.jack3.business.BcryptBusiness;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.UserService;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.Click;
import de.uni_due.s3.jack3.uitests.utils.I18nHelper;
import de.uni_due.s3.jack3.uitests.utils.JackUrl;

class SetupTest extends AbstractSeleniumTest {

	@Inject
	private UserService userService;

	@Inject
	private BcryptBusiness bcryptBusiness;

	/**
	 * Setup the initial user with username "admin" and password "secret"
	 */
	@Test
	@Order(1)
	@RunAsClient
	void setup() { // NOSONAR no assertions here
		navigate("public/setup.xhtml");

		// Send user data
		driver.findElement(By.id("SetupChangePassword:setupUsername")).sendKeys("admin");
		driver.findElement(By.id("SetupChangePassword:newPassword")).sendKeys("secret");
		driver.findElement(By.id("SetupChangePassword:confirmPassword")).sendKeys("secret");
		driver.findElement(By.id("SetupChangePassword:setupEmail")).sendKeys("foo@bar.com");

		// Click button to finish setup and wait for login page
		Click.clickWithRedirect(By.id("SetupChangePassword:cbPerformSetup"));
	}

	/**
	 * Database test of initial user
	 */
	@Test
	@Order(2)
	void verifyCreatedUser() {
		Optional<User> user = userService.getUserByName("admin");
		assertTrue(user.isPresent(), "No user with name 'admin' was found.");
		assertTrue(bcryptBusiness.matches("secret", user.get().getPassword()),
				"User with name 'admin' was found, but passwords don't match.");
	}

	/**
	 * Incorrect login with wrong password
	 */
	@Test
	@Order(3)
	@RunAsClient
	void incorrectLogin() {
		navigate(JackUrl.AVAILABLE_COURSES);

		final By msgLocator = By.id("globalGrowl_container");

		// Send login data
		driver.findElement(By.id("loginForm:loginUsername")).sendKeys("admin");
		driver.findElement(By.id("loginForm:loginPasswordField")).sendKeys("incorrect");

		// Message should be not shown at the beginning
		assertNotVisible(msgLocator);

		// Click login button with incorrect password --> Error message is showing up
		driver.findElement(By.id("loginForm:loginButton")).click();

		assertVisible(msgLocator, "Error message was not shown.");
		WebElement msg = driver.findElement(msgLocator);
		assertTrue(msg.getText().contains(I18nHelper.LOGIN_FAILED), "Expected message: 'Anmeldung fehlgeschlagen'.");
	}

	/**
	 * Correct login & Logout
	 */
	@Test
	@Order(4)
	@RunAsClient
	void loginLogout() { // NOSONAR no assertions here
		navigate(JackUrl.AVAILABLE_COURSES);

		// Send login data
		driver.findElement(By.id("loginForm:loginUsername")).sendKeys("admin");
		driver.findElement(By.id("loginForm:loginPasswordField")).sendKeys("secret");

		// Click login button with correct password --> redirection
		WebElement loginButton = driver.findElement(By.id("loginForm:loginButton"));
		Click.clickWithRedirect(loginButton);
		waitVisible(By.id("menubar:menubarAccount"), "Username should be shown in menubar after login.");

		// Click logout button
		openUserSubmenu();
		WebElement logoutButton = driver.findElement(By.id("menubar:buttonMenubarLogout"));
		Click.clickWithRedirect(logoutButton);
		waitVisible(By.id("loginForm:loginButton"), "Login button should be shown after logout");
	}
}
