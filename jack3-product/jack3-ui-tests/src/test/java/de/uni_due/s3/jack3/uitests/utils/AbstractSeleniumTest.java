package de.uni_due.s3.jack3.uitests.utils;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import de.uni_due.s3.jack3.business.ConfigurationBusiness;
import de.uni_due.s3.jack3.business.FirstTimeSetupBusiness;
import de.uni_due.s3.jack3.entities.tenant.Config;
import de.uni_due.s3.jack3.multitenancy.LoggerProvider;
import de.uni_due.s3.jack3.services.DevelopmentService;
import de.uni_due.s3.jack3.services.DevelopmentService.EDatabaseType;
import de.uni_due.s3.jack3.uitests.arquillian.ArquillianExtension;
import de.uni_due.s3.jack3.uitests.arquillian.EDeploymentType;
import de.uni_due.s3.jack3.uitests.arquillian.ExtendedDeployment;

@ExtendedDeployment(EDeploymentType.MULTI)
@ExtendWith(ArquillianExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class AbstractSeleniumTest {

	@Inject
	protected DevelopmentService devService;

	@Inject
	protected FirstTimeSetupBusiness firstTimeSetup;

	@ArquillianResource
	protected URL url;

	@Drone
	protected WebDriver driver;

	/*-
	 * The following code helps us to initialize a test with additional actions
	 * (e.g. clear database, insert demo users, ...). Therefore the first test must not run in client mode.
	 */

	/**
	 * The first test method is only a "demo" test for initializing the following tests.
	 */
	@Test
	@Order(0)
	final void initialize() {
		initializeTest();
	}

	@Test
	@Order(Integer.MAX_VALUE)
	@RunAsClient
	final void tearDown() { // NOSONAR no assertions here
		Driver.get().quit();
	}

	/**
	 * This method is called in the first test to prepare the test. By default, database is cleared and FirstTimeSetup
	 * is called before other tests are executed. Overwrite this method for additional actions.
	 */
	protected void initializeTest() {
		LoggerProvider.get(getClass()).info("*****************************************************************");
		LoggerProvider.get(getClass()).info("NEW TEST STARTED: " + getClass().getName());
		LoggerProvider.get(getClass()).info("*****************************************************************");
		devService.deleteTenantDatabase(EDatabaseType.H2);

		ConfigurationBusiness configurationBusiness = CDI.current().select(ConfigurationBusiness.class).get();
		Config eurekaConfig = new Config("EurekaServerURLs", "[\"http://10.168.68.74:8761/eureka\"]");
		if (configurationBusiness.saveConfig(eurekaConfig)) {
			configurationBusiness.clearCache();
		}

		firstTimeSetup.doFirstTimeSetup();
	}

	/**
	 * This method caches the web driver object.
	 */
	@RunAsClient
	@BeforeEach
	final void cacheWebDriverBeforeTest() {
		if (driver != null) { // Prevents calling from a Wildfly container
			Driver.set(driver);
			Driver.setUrl(url);
		}
	}

	/*-
	 * ------------------------------------------------------------------------------------------------------
	 * Helpers
	 * ------------------------------------------------------------------------------------------------------
	 */

	/**
	 * Waits a specific duration.</br>
	 * <b>NOTE:</b> Only for debugging!!!
	 */
	protected final void waitDebug(Duration duration) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, duration.getSeconds());
			wait.until(webdriver -> false);
		} catch (TimeoutException e) {
			// After waiting, test will be continued
		}
	}

	/**
	 * Waits until the developer presses ENTER on the console.
	 */
	protected final void waitDebug() {
		try {
			System.out.println("Debug Breakpoint, press ENTER to continue > ");
			System.in.read();
		} catch (IOException e) {
			System.err.println("Error in waitDebug()!");
			e.printStackTrace();
		}
	}

}
