package de.uni_due.s3.jack3.uitests;

import static de.uni_due.s3.jack3.uitests.utils.Misc.login;
import static de.uni_due.s3.jack3.uitests.utils.Misc.navigate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Predicate;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.opentest4j.AssertionFailedError;

import de.uni_due.s3.jack3.business.BcryptBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.JackUrl;
import de.uni_due.s3.jack3.uitests.utils.Time;

class ChangeUserdataTest extends AbstractSeleniumTest {
	
	private final String username = "superuser";
	private final String oldPassword = "oldPassword";
	private final String newPassword = "newPassword";
	private final String oldMail = "superuser@foobar.com";
	private final String newMail = "superuser@example.com";

	@Inject
	private UserBusiness userBusiness;
	
	@Inject
	private BcryptBusiness bcryptBusiness;

	@Override
	protected void initializeTest() {
		super.initializeTest();
		userBusiness.createUser(username, oldPassword, oldMail, true, true);
	}
	
	@Test
	@Order(1)
	@RunAsClient
	void changeEmail() { // NOSONAR no assertions here
		login(username, oldPassword);
		navigate(JackUrl.MY_ACCOUNT);
		
		// Change email adress
		driver.findElement(By.id("myAccountMail:newEmail")).clear();
		driver.findElement(By.id("myAccountMail:newEmail")).sendKeys(newMail);
		driver.findElement(By.id("myAccountMail:cbUpdateMailSettingsSave")).click();
		Time.waitVisible(By.id("globalGrowl_container"));
	}

	@Test
	@Order(2)
	void checkEmail() {
		User user = userBusiness.getUserByName(username).orElseThrow(AssertionFailedError::new);
		assertNotEquals(oldMail, user.getEmail(), "Email was not changed");
		assertEquals(newMail, user.getEmail(), "New email is not correct");
	}

	@Test
	@Order(3)
	@RunAsClient
	void changePassword() { // NOSONAR no assertions here
		login(username, oldPassword);
		navigate(JackUrl.MY_ACCOUNT);

		// Change password
		driver.findElement(By.id("myAccountChangePassword:currentPassword")).sendKeys(oldPassword);
		driver.findElement(By.id("myAccountChangePassword:newPassword")).sendKeys(newPassword);
		driver.findElement(By.id("myAccountChangePassword:confirmPassword")).sendKeys(newPassword);
		driver.findElement(By.id("myAccountChangePassword:cbMyAccountChangePasswordOk")).click();
		Time.waitVisible(By.id("globalGrowl_container"));
	}

	@Test
	@Order(4)
	void checkPassword() {
		User user = userBusiness.getUserByName(username).orElseThrow(AssertionFailedError::new);
		Predicate<String> match = hash -> bcryptBusiness.matches(hash, user.getPassword());
		assertFalse(match.test(oldPassword), "Password was not changed");
		assertTrue(match.test(newPassword), "New password is not correct");
	}

}
