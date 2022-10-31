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
import de.uni_due.s3.jack3.entities.stagetypes.uml.UmlStage;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.ExerciseService;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.pages.ExerciseEditPage;
import de.uni_due.s3.jack3.uitests.utils.pages.MyWorkspacePage;
import de.uni_due.s3.jack3.uitests.utils.pages.stages.UmlStagePage;

class UMLStageTest extends AbstractSeleniumTest {

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
	void createRStage() { // NOSONAR no assertions here
		login("lecturer", "secret");

		MyWorkspacePage.navigateToPage();
		MyWorkspacePage.expandFolder(MyWorkspacePage.getPersonalFolder());
		MyWorkspacePage.openExercise(MyWorkspacePage.getExercise("Aufgabe 1"));
		ExerciseEditPage.createUMLStage("My UML Stage");
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
		assertEquals("My UML Stage", exercise.getStartStage().getInternalName());
		assertTrue(exercise.getStartStage() instanceof UmlStage);
	}

	@Test
	@Order(3)
	@RunAsClient
	void changeContentTabSettings () { // NOSONAR no assertions here
		assumeLogin();

		ExerciseEditPage.expandStage("My UML Stage");
		UmlStagePage UmlStage = ExerciseEditPage.getUmlStage("My UML Stage");

		UmlStage.navigateToContentTab();
		UmlStage.setExternalTitel("my crazy first stage");
		UmlStage.setExerciseDescritptionText("this is the description");

		ExerciseEditPage.saveExercise();
	}

	@Test
	@Order(4)
	void verifyContentTabChanges() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(() -> new AssertionError("user 'lecturer' could not be found"));
		Exercise exercise = exerciseService.getAllExercisesForUser(lecturer).get(0);
		exercise = exerciseService.getExerciseByIdWithLazyData(exercise.getId()).get();
		UmlStage UmlStage = (UmlStage) exercise.getStartStage();

		assertEquals("my crazy first stage", UmlStage.getExternalName());
		assertEquals("this is the description", UmlStage.getTaskDescription());
	}

	@Test
	@Order(5)
	@RunAsClient
	void changeFeedbackTabSettings() { // NOSONAR no assertions here
		assumeLogin();

		UmlStagePage umlStage = ExerciseEditPage.getUmlStage("My UML Stage");
		umlStage.navigateToFeedbackTab();

		umlStage.setStageWeight(5);
		umlStage.setRepeatOnMissingUpload(true);
		umlStage.setPropagateInternalErrors(true);
		umlStage.setGREQLRuleText("this is just some random text");
		umlStage.setAllowSkip(true);
		umlStage.setSkipFeedbackText("you skipped the UML stage!");

		ExerciseEditPage.saveExercise();
	}

	@Test
	@Order(6)
	void verifyFeedbackTabChanges() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(() -> new AssertionError("user 'lecturer' could not be found"));
		Exercise exercise = exerciseService.getAllExercisesForUser(lecturer).get(0);
		exercise = exerciseService.getExerciseByIdWithLazyData(exercise.getId()).get();
		UmlStage umlStage = (UmlStage) exercise.getStartStage();

		assertEquals(5, umlStage.getWeight());
		assertTrue(umlStage.isPropagateInternalErrors());
		assertTrue(umlStage.isRepeatOnMissingUpload());
		assertEquals("this is just some random text", umlStage.getGreqlRules());
		assertTrue(umlStage.getAllowSkip());
		assertEquals("you skipped the UML stage!", umlStage.getSkipMessage());
	}
}
