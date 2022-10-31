package de.uni_due.s3.jack3.uitests.stages;

import static de.uni_due.s3.jack3.uitests.utils.Misc.assumeLogin;
import static de.uni_due.s3.jack3.uitests.utils.Misc.login;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.builders.ExerciseBuilder;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.enums.EStageHintMalus;
import de.uni_due.s3.jack3.entities.stagetypes.python.PythonStage;
import de.uni_due.s3.jack3.entities.stagetypes.python.TracingPythonGradingConfig;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.ExerciseService;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.pages.ExerciseEditPage;
import de.uni_due.s3.jack3.uitests.utils.pages.MyWorkspacePage;
import de.uni_due.s3.jack3.uitests.utils.pages.stages.PythonStagePage;

class PythonStageTest extends AbstractSeleniumTest {

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
	void createPythonStage() { // NOSONAR no assertions here
		login("lecturer", "secret");

		MyWorkspacePage.navigateToPage();
		MyWorkspacePage.expandFolder(MyWorkspacePage.getPersonalFolder());
		MyWorkspacePage.openExercise(MyWorkspacePage.getExercise("Aufgabe 1"));
		ExerciseEditPage.createPythonStage("My Python Stage");
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
		assertEquals("My Python Stage", exercise.getStartStage().getInternalName());
		assertTrue(exercise.getStartStage() instanceof PythonStage);
	}
	
	@Test
	@Order(3)
	@RunAsClient
	void changeContentTabSettings () { // NOSONAR no assertions here
		assumeLogin();
		
		ExerciseEditPage.expandStage("My Python Stage");
		PythonStagePage pythonStage = ExerciseEditPage.getPythonStage("My Python Stage");
		
		pythonStage.navigateToContentTab();
		pythonStage.setExternalTitel("my crazy first stage");
		pythonStage.setExerciseDescritptionText("this is the description");
		
		ExerciseEditPage.saveExercise();
	}
	
	@Test
	@Order(4)
	void verifyContentTabChanges() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(() -> new AssertionError("user 'lecturer' could not be found"));
		Exercise exercise = exerciseService.getAllExercisesForUser(lecturer).get(0);
		exercise = exerciseService.getExerciseByIdWithLazyData(exercise.getId()).get();
		PythonStage pythonStage = (PythonStage) exercise.getStartStage();
		
		assertEquals("my crazy first stage", pythonStage.getExternalName());
		assertEquals("this is the description", pythonStage.getTaskDescription());
	}
	
	@Test
	@Order(5)
	@RunAsClient
	void changeFeedbackTabSettings() { // NOSONAR no assertions here
		assumeLogin();
		
		PythonStagePage pythonStage = ExerciseEditPage.getPythonStage("My Python Stage");
		pythonStage.navigateToFeedbackTab();
		
		pythonStage.setStageWeight(5);
		pythonStage.setPropagateInternalErrors(true);

		//TODO add content to the checker (currently we only create them)
		pythonStage.addDynamicChecker();
		
		pythonStage.setAllowSkip(true);
		pythonStage.setSkipFeedbackText("I'm disapointed you skipped this awesome stage :(");
		
		ExerciseEditPage.saveExercise();
	}
	
	@Test
	@Order(6)
	void verifyFeedbackTabChanges() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(() -> new AssertionError("user 'lecturer' could not be found"));
		Exercise exercise = exerciseService.getAllExercisesForUser(lecturer).get(0);
		exercise = exerciseService.getExerciseByIdWithLazyData(exercise.getId()).get();
		PythonStage pythonStage = (PythonStage) exercise.getStartStage();
		
		assertEquals(5, pythonStage.getWeight());
		assertEquals(1, pythonStage.getGradingSteps().size());
		assertTrue(pythonStage.getGradingSteps().get(0) instanceof TracingPythonGradingConfig);
		
		assertTrue(pythonStage.getAllowSkip());
		assertEquals("I'm disapointed you skipped this awesome stage :(", pythonStage.getSkipMessage());
	}	

}
