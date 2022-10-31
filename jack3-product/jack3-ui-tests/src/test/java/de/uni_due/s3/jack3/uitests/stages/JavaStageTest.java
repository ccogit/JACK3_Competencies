package de.uni_due.s3.jack3.uitests.stages;

import static de.uni_due.s3.jack3.uitests.utils.Misc.assumeLogin;
import static de.uni_due.s3.jack3.uitests.utils.Misc.login;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.builders.ExerciseBuilder;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.enums.EStageHintMalus;
import de.uni_due.s3.jack3.entities.stagetypes.java.GreqlGradingConfig;
import de.uni_due.s3.jack3.entities.stagetypes.java.JavaStage;
import de.uni_due.s3.jack3.entities.stagetypes.java.MetricsGradingConfig;
import de.uni_due.s3.jack3.entities.stagetypes.java.TracingGradingConfig;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.ExerciseService;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.pages.ExerciseEditPage;
import de.uni_due.s3.jack3.uitests.utils.pages.MyWorkspacePage;
import de.uni_due.s3.jack3.uitests.utils.pages.stages.JavaStagePage;

class JavaStageTest extends AbstractSeleniumTest {

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private ExerciseService exerciseService;

	@Override
	protected void initializeTest() {
		super.initializeTest();
		User lecturer = userBusiness.createUser("lecturer", "secret", "lecturer@foobar.com", false, true);

		// Generate an exercise
		Exercise exercise = new ExerciseBuilder("Aufgabe 1").withRandomVariableDeclaration("variable", 0, 9).withHintMalusType(EStageHintMalus.CUT_MAXIMUM).create();

		// Persist the exercise
		ContentFolder folder = lecturer.getPersonalFolder();
		folder.addChildExercise(exercise);
		exerciseService.persistExercise(exercise);
		folderBusiness.updateFolder(folder);
	}

	@Test
	@Order(1)
	@RunAsClient
	void createJavaStage() { // NOSONAR no assertions here
		login("lecturer", "secret");

		MyWorkspacePage.navigateToPage();
		MyWorkspacePage.expandFolder(MyWorkspacePage.getPersonalFolder());
		MyWorkspacePage.openExercise(MyWorkspacePage.getExercise("Aufgabe 1"));
		ExerciseEditPage.createJavaStage("My Java Stage");
		ExerciseEditPage.saveExercise();
	}

	@Test
	@Order(2)
	void verifyCreatedStage() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(() -> new AssertionError("user 'lecturer' could not be found"));
		Exercise exercise = exerciseService.getAllExercisesForUser(lecturer).get(0);
		exercise = exerciseService.getExerciseByIdWithLazyData(exercise.getId()).get();

		assertEquals(1, exercise.getStages().size());
		assertNotNull(exercise.getStartStage());
		assertEquals("My Java Stage", exercise.getStartStage().getInternalName());
		assertTrue(exercise.getStartStage() instanceof JavaStage);
	}

	@Test
	@Order(3)
	@RunAsClient
	void changeContentTabSettings () { // NOSONAR no assertions here
		assumeLogin();

		ExerciseEditPage.expandStage("My Java Stage");
		JavaStagePage javaStage = ExerciseEditPage.getJavaStage("My Java Stage");

		javaStage.navigateToContentTab();
		javaStage.setExternalTitel("my crazy first stage");
		javaStage.setExerciseDescritptionText("this is the description");
		javaStage.setNeccessaryFileNames("myName", "otherName");
		javaStage.setAllowedFileNames("bla", "blu");
		javaStage.setMinFileCount(2);
		javaStage.setMaxFileCount(5);

		ExerciseEditPage.saveExercise();
	}

	@Test
	@Order(4)
	void verifyContentTabChanges() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(() -> new AssertionError("user 'lecturer' could not be found"));
		Exercise exercise = exerciseService.getAllExercisesForUser(lecturer).get(0);
		exercise = exerciseService.getExerciseByIdWithLazyData(exercise.getId()).get();
		JavaStage javaStage = (JavaStage) exercise.getStartStage();

		assertEquals("my crazy first stage", javaStage.getExternalName());
		assertEquals("this is the description", javaStage.getTaskDescription());
		
		assertEquals(2, javaStage.getMandatoryFileNamesAsSet().size());
		assertTrue(javaStage.getMandatoryFileNamesAsSet().containsAll(Arrays.asList("myName","otherName")));
		
		assertEquals(2, javaStage.getAllowedFileNamesAsSet().size());
		assertTrue(javaStage.getAllowedFileNamesAsSet().containsAll(Arrays.asList("bla","blu")));
		
		assertEquals(5, javaStage.getMaximumFileCount());
		assertEquals(2, javaStage.getMinimumFileCount());
	}

	@Test
	@Order(5)
	@RunAsClient
	void changeFeedbackTabSettings() { // NOSONAR no assertions here
		assumeLogin();

		JavaStagePage javaStage = ExerciseEditPage.getJavaStage("My Java Stage");
		javaStage.navigateToFeedbackTab();

		javaStage.setStageWeight(5);
		javaStage.setRepeatOnMissingUpload(true);
		javaStage.setPropagateInternalErrors(true);
		
		//TODO add content to the checkers (currently we only create them)
		javaStage.addDynamicChecker();
		javaStage.addStaticChecker();
		javaStage.addMetricsConfig();
		
		javaStage.setAllowSkip(true);
		javaStage.setSkipFeedbackText("you skipped the Java stage!");

		ExerciseEditPage.saveExercise();
	}

	@Test
	@Order(6)
	void verifyFeedbackTabChanges() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(() -> new AssertionError("user 'lecturer' could not be found"));
		Exercise exercise = exerciseService.getAllExercisesForUser(lecturer).get(0);
		exercise = exerciseService.getExerciseByIdWithLazyData(exercise.getId()).get();
		JavaStage javaStage = (JavaStage) exercise.getStartStage();

		assertEquals(5, javaStage.getWeight());
		assertTrue(javaStage.isRepeatOnMissingUpload());
		assertTrue(javaStage.isPropagateInternalErrors());
		
		assertEquals(3, javaStage.getGradingSteps().size());
		assertTrue(javaStage.getGradingSteps().get(0) instanceof TracingGradingConfig);
		assertTrue(javaStage.getGradingSteps().get(1) instanceof GreqlGradingConfig);
		assertTrue(javaStage.getGradingSteps().get(2) instanceof MetricsGradingConfig);
		
		assertTrue(javaStage.getAllowSkip());
		assertEquals("you skipped the Java stage!", javaStage.getSkipMessage());
	}

}
