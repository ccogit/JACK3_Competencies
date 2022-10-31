package de.uni_due.s3.jack3.uitests;

import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Misc.login;
import static de.uni_due.s3.jack3.uitests.utils.Misc.navigate;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.Time;

class DevelopmentviewTest extends AbstractSeleniumTest {

	@Inject
	private UserBusiness userBusiness;
	
	@Inject
	private ExerciseBusiness exerciseBusiness;
	
	@Inject
	private CourseBusiness courseBusiness;
	
	@Override
	protected void initializeTest() {
		super.initializeTest();
		userBusiness.createUser("admin", "secret", "admin@foobar.com", true, true);
	}
	
	private void waitUntilActionBarIsNotActive (int timeout, String message) {
		Time.wait(ExpectedConditions.attributeToBe(By.id("activity-bar"), "class", ""), timeout, message);
	}

	@Test
	@Order(1)
	@RunAsClient
	void setup() { // NOSONAR no assertions here
		login("admin","secret");
		navigate("administrator/development.xhtml");
		
		find("developmentForm:setupReadyToPlayEnvironment").click();
		waitUntilActionBarIsNotActive(25,"Set up to play environment didn't stop loading");
		waitClickable(By.id("developmentForm:createAllSampleExercises"));
		find("developmentForm:createAllSampleExercises").click();
		waitUntilActionBarIsNotActive(10,"Creating all sample Exercises didn't stop loading");
	}
	
	@Test
	@Order(2)
	void verifyCreatedEntities() {
		assertTrue(userBusiness.getUserByName("lecturer").isPresent());
		
		assertEquals(9, exerciseBusiness.getAllExercisesForUser(userBusiness.getUserByName("admin").get()).size());
		assertEquals(9, exerciseBusiness.getAllExercisesForUser(userBusiness.getUserByName("lecturer").get()).size());
		assertEquals(1, courseBusiness.getAllCourseOffers().size());
		assertEquals(21, userBusiness.getAllUsers().size());
	}

}
