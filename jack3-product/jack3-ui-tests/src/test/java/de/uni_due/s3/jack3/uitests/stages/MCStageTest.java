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
import de.uni_due.s3.jack3.entities.enums.EMCRuleType;
import de.uni_due.s3.jack3.entities.enums.EStageHintMalus;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCAnswer;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCFeedback;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.ExerciseService;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.pages.ExerciseEditPage;
import de.uni_due.s3.jack3.uitests.utils.pages.MyWorkspacePage;
import de.uni_due.s3.jack3.uitests.utils.pages.stages.MCStagePage;

class MCStageTest extends AbstractSeleniumTest {

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
	void createMCStage() { // NOSONAR no assertions here
		login("lecturer", "secret");

		MyWorkspacePage.navigateToPage();
		MyWorkspacePage.expandFolder(MyWorkspacePage.getPersonalFolder());
		MyWorkspacePage.openExercise(MyWorkspacePage.getExercise("Aufgabe 1"));
		ExerciseEditPage.createMCStage("My MCStage");
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
		assertEquals("My MCStage", exercise.getStartStage().getInternalName());
		assertTrue(exercise.getStartStage() instanceof MCStage);
	}
	
	@Test
	@Order(3)
	@RunAsClient
	void changeContentTabSettings () { // NOSONAR no assertions here
		assumeLogin();
		
		ExerciseEditPage.expandStage("My MCStage");
		MCStagePage mcStage = ExerciseEditPage.getMCStage("My MCStage");
		
		mcStage.navigateToContentTab();
		mcStage.setExternalTitel("my crazy first stage");
		mcStage.setExerciseDescription("this is the description");
		
		mcStage.addNewAnswerOption("this is correct", true);
		mcStage.addNewAnswerOption("this is false", false);
		
		mcStage.setRandomizeAnswerOptions(true);
		mcStage.setSingleChoiceOption(true);
		
		ExerciseEditPage.saveExercise();
	}
	
	@Test
	@Order(4)
	void verifyContentTabChanges() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(() -> new AssertionError("user 'lecturer' could not be found"));
		Exercise exercise = exerciseService.getAllExercisesForUser(lecturer).get(0);
		exercise = exerciseService.getExerciseByIdWithLazyData(exercise.getId()).get();
		MCStage mcStage = (MCStage) exercise.getStartStage();
		
		assertEquals("my crazy first stage", mcStage.getExternalName());
		assertEquals("this is the description", mcStage.getTaskDescription());
		assertEquals(2,mcStage.getAnswerOptions().size());
		MCAnswer firstAnswerOption = mcStage.getAnswerOptions().get(0);
		MCAnswer secondAnswerOption = mcStage.getAnswerOptions().get(1);
		
		assertEquals("this is correct", firstAnswerOption.getText());
		assertEquals(EMCRuleType.CORRECT,firstAnswerOption.getRule());
		assertEquals("this is false", secondAnswerOption.getText());
		assertEquals(EMCRuleType.WRONG, secondAnswerOption.getRule());
		
		assertTrue(mcStage.isRandomize());
		assertTrue(mcStage.isSingleChoice());
	}
	
	@Test
	@Order(5)
	@RunAsClient
	void changeFeedbackTabSettings() { // NOSONAR no assertions here
		assumeLogin();
		
		MCStagePage mcStage = ExerciseEditPage.getMCStage("My MCStage");
		mcStage.navigateToFeedbackTab();
		
		mcStage.setStageWeight(5);
		mcStage.setFeedbackTextForCorrectAnswer("Great!");
		mcStage.setFeedbackTextForWrongAnswer("Try it again! You get 10 points for trying though :)");
		mcStage.setPointsForWrongAnswer(10);
		mcStage.createAdditionalFeedback("this is also an interesting answer", 50, EMCRuleType.WRONG, EMCRuleType.NO_MATTER);
		mcStage.setAllowSkip(true);
		mcStage.setSkipFeedbackText("I'm disapointed you skipped this awesome stage :(");
		
		ExerciseEditPage.saveExercise();
	}
	
	@Test
	@Order(6)
	void verifyFeedbackTabChanges() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(() -> new AssertionError("user 'lecturer' could not be found"));
		Exercise exercise = exerciseService.getAllExercisesForUser(lecturer).get(0);
		exercise = exerciseService.getExerciseByIdWithLazyData(exercise.getId()).get();
		MCStage mcStage = (MCStage) exercise.getStartStage();
		
		assertEquals(5, mcStage.getWeight());
		assertEquals("Great!", mcStage.getCorrectAnswerFeedback());
		assertEquals("Try it again! You get 10 points for trying though :)", mcStage.getDefaultFeedback());
		assertEquals(10, mcStage.getDefaultResult());
		assertEquals(1, mcStage.getExtraFeedbacks().size());
		
		MCFeedback mcFeedback = mcStage.getExtraFeedbacks().get(0);
		assertEquals("this is also an interesting answer", mcFeedback.getFeedbackText());
		assertEquals(50, mcFeedback.getResult());
		assertEquals("![input=mcindex_0]&&true()", mcFeedback.getExpression().getCode());
		
		assertTrue(mcStage.getAllowSkip());
		assertEquals("I'm disapointed you skipped this awesome stage :(", mcStage.getSkipMessage());
	}	

}
