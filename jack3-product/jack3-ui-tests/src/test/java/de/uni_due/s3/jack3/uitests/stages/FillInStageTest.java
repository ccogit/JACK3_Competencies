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
import de.uni_due.s3.jack3.entities.stagetypes.fillin.DropDownField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.Rule;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression.EDomain;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.ExerciseService;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.pages.ExerciseEditPage;
import de.uni_due.s3.jack3.uitests.utils.pages.MyWorkspacePage;
import de.uni_due.s3.jack3.uitests.utils.pages.stages.FillInStagePage;

class FillInStageTest extends AbstractSeleniumTest {

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
	void createFillInStage() { // NOSONAR no assertions here
		login("lecturer", "secret");

		MyWorkspacePage.navigateToPage();
		MyWorkspacePage.expandFolder(MyWorkspacePage.getPersonalFolder());
		MyWorkspacePage.openExercise(MyWorkspacePage.getExercise("Aufgabe 1"));
		ExerciseEditPage.createFillInStage("My FillInStage");
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
		assertEquals("My FillInStage", exercise.getStartStage().getInternalName());
		assertTrue(exercise.getStartStage() instanceof FillInStage);
	}
	
	@Test
	@Order(3)
	@RunAsClient
	void changeContentTabSettings () { // NOSONAR no assertions here
		assumeLogin();
		ExerciseEditPage.expandStage("My FillInStage");
		FillInStagePage fillInStage = ExerciseEditPage.getFillInStage("My FillInStage");
		
		fillInStage.navigateToContentTab();
		fillInStage.setExternalTitel("my crazy first stage");
		
		fillInStage.addFillInField("firstFillInField", 20, "Text");
		fillInStage.addFillInField("secondFillInField", 5, "Number");
		fillInStage.addFillInField("remove_me", 10, " ");
		
		fillInStage.removeFillInField(2);
		
		fillInStage.addDropDownField("firstDropDownField", true, "right answer", "false answer 1", "false answer 2", "false answer 3");
		fillInStage.addDropDownField("secondDropDownField", true, "right answer", "false answer");
		fillInStage.addDropDownField("remove_me", false, "first_remove", "second_remove");
		
		fillInStage.removeDropDownField(2);

		fillInStage.setExerciseDescritptionText("you will have to fill in your answer into a fillIn field and a DropDown menu:\n");
			
		ExerciseEditPage.saveExercise();
	}
	
	@Test
	@Order(4)
	void verifyContentChanges() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(() -> new AssertionError("user 'lecturer' could not be found"));
		Exercise exercise = exerciseService.getAllExercisesForUser(lecturer).get(0);
		exercise = exerciseService.getExerciseByIdWithLazyData(exercise.getId()).get();
		FillInStage fillInStage = (FillInStage) exercise.getStartStage();
		
		assertEquals("my crazy first stage", fillInStage.getExternalName());
		
		//verify fillInFields
		assertEquals(2,fillInStage.getFillInFields().size());
		for(FillInField fillInField: fillInStage.getFillInFields()) {
			if(fillInField.getOrderIndex()==0) {
				assertEquals(20, fillInField.getSize());
				assertEquals("firstFillInField", fillInField.getName());
				assertEquals("TEXT", fillInField.getFormularEditorType());
			}else if(fillInField.getOrderIndex()==1) {
				assertEquals(5, fillInField.getSize());
				assertEquals("secondFillInField", fillInField.getName());
				assertEquals("NUMBER", fillInField.getFormularEditorType());
			}else {
				throw new AssertionError("FillInField had the order index '" + fillInField.getOrderIndex() + "' even though only two fillInFields exist.");
			}
		}
		
		//verify dropDownFields
		assertEquals(2,fillInStage.getDropDownFields().size());
		for(DropDownField dropDownField: fillInStage.getDropDownFields()) {
			if(dropDownField.getOrderIndex()==0) {
				assertEquals("firstDropDownField", dropDownField.getName());
				assertTrue(dropDownField.getRandomize());
				assertEquals(4,dropDownField.getItems().size());
				assertEquals("right answer",dropDownField.getItems().get(0));
				assertEquals("false answer 1",dropDownField.getItems().get(1));
				assertEquals("false answer 2",dropDownField.getItems().get(2));
				assertEquals("false answer 3",dropDownField.getItems().get(3));
			}else if(dropDownField.getOrderIndex()==1) {
				assertEquals("secondDropDownField", dropDownField.getName());
				assertTrue(dropDownField.getRandomize());
				assertEquals(2,dropDownField.getItems().size());
				assertEquals("right answer",dropDownField.getItems().get(0));
				assertEquals("false answer",dropDownField.getItems().get(1));
			}else {
				throw new AssertionError("DropDownField had the order index '" + dropDownField.getOrderIndex() + "' even though only two DropDownFields exist.");
			}
		}
		
		//verify taskDescription
		assertTrue(fillInStage.getTaskDescription().contains("you will have to fill in your answer into a fillIn field and a DropDown menu"));
		assertTrue(fillInStage.getTaskDescription().contains("dropdown1"));
		assertTrue(fillInStage.getTaskDescription().contains("dropdown2"));
		assertTrue(fillInStage.getTaskDescription().contains("fillin1"));
		assertTrue(fillInStage.getTaskDescription().contains("fillin2"));
	}
	
	@Test
	@Order(5)
	@RunAsClient
	void changeFeedbackTabSettings() { // NOSONAR no assertions here
		assumeLogin();
		
		FillInStagePage fillInStage = ExerciseEditPage.getFillInStage("My FillInStage");
		fillInStage.navigateToFeedbackTab();
		
		fillInStage.setStageWeight(2);
		fillInStage.addCorrectAnswerRule("my evaluator code", EDomain.CHEM);
		fillInStage.setFeedbackTextForCorrectAnswer("nice");
		fillInStage.setFeedbackTextForWrongAnswer("not so nice");
		fillInStage.setPointsForWrongAnswer(10);
		fillInStage.createAdditionalFeedback("my code", EDomain.MATH, "bla bla", 50, true);
		fillInStage.setAllowSkip(true);
		fillInStage.setSkipFeedbackText("you skipped");
		
		ExerciseEditPage.saveExercise();
	}
	
	@Test
	@Order(6)
	void verifyFeedbackTabChanges() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(() -> new AssertionError("user 'lecturer' could not be found"));
		Exercise exercise = exerciseService.getAllExercisesForUser(lecturer).get(0);
		exercise = exerciseService.getExerciseByIdWithLazyData(exercise.getId()).get();
		FillInStage fillInStage = (FillInStage) exercise.getStartStage();
		
		assertEquals(2, fillInStage.getWeight());
		
		assertEquals(1,fillInStage.getCorrectAnswerRulesAsList().size());
		final Rule correctAnswerRule = fillInStage.getCorrectAnswerRulesAsList().get(0);
		assertEquals("my evaluator code", correctAnswerRule.getValidationExpression().getCode());
		assertEquals(EDomain.CHEM, correctAnswerRule.getValidationExpression().getDomain());
		
		assertEquals("nice",fillInStage.getCorrectAnswerFeedback());
		assertEquals("not so nice", fillInStage.getDefaultFeedback());
		assertEquals(10, fillInStage.getDefaultResult());
		
		assertEquals(1, fillInStage.getFeedbackRulesAsList().size());
		final Rule additionalFeedbackRule = fillInStage.getFeedbackRulesAsList().get(0);
		assertEquals("my code", additionalFeedbackRule.getValidationExpression().getCode());
		assertEquals(EDomain.MATH, additionalFeedbackRule.getValidationExpression().getDomain());
		assertEquals(50, additionalFeedbackRule.getPoints());
		assertEquals("bla bla", additionalFeedbackRule.getFeedbackText());
		assertTrue(additionalFeedbackRule.isTerminal());
		
		assertTrue(fillInStage.getAllowSkip());
		assertEquals("you skipped",fillInStage.getSkipMessage());
	}
}
	