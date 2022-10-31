package de.uni_due.s3.jack3.uitests.stages;

import static de.uni_due.s3.jack3.uitests.utils.Misc.assumeLogin;
import static de.uni_due.s3.jack3.uitests.utils.Misc.login;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.builders.ExerciseBuilder;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.enums.EStageHintMalus;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression.EDomain;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.StageHint;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.VariableUpdate;
import de.uni_due.s3.jack3.services.ExerciseService;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.pages.ExerciseEditPage;
import de.uni_due.s3.jack3.uitests.utils.pages.MyWorkspacePage;
import de.uni_due.s3.jack3.uitests.utils.pages.stages.MCStagePage;

class GenerelStageTest extends AbstractSeleniumTest {

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
		Exercise exercise = new ExerciseBuilder("Aufgabe 1").withRandomVariableDeclaration("variable", 0, 9).withHintMalusType(EStageHintMalus.CUT_MAXIMUM).withMCStage().allowSkip().and().withMCStage().and().create();

		// Persist the exercise
		ContentFolder folder = lecturer.getPersonalFolder();
		folder.addChildExercise(exercise);
		exerciseService.persistExercise(exercise);
		folderBusiness.updateFolder(folder);
	}
	
	@Test
	@Order(1)
	@RunAsClient
	void hintTab() { // NOSONAR no assertions here
		login("lecturer", "secret");

		MyWorkspacePage.navigateToPage();
		MyWorkspacePage.expandFolder(MyWorkspacePage.getPersonalFolder());
		MyWorkspacePage.openExercise(MyWorkspacePage.getExercise("Aufgabe 1"));		
		MCStagePage mcStage = ExerciseEditPage.getMCStage("#1");
		ExerciseEditPage.getNumberOfRevisions(); //if the revisions are loaded, the stage is also completly loaded an can be expanded
		ExerciseEditPage.expandStage("#1");
		mcStage.navigateToHintTab();
		
		mcStage.createHint("this is the first hint", 12);
		mcStage.createHint("This is the second hint", 25);
		mcStage.removeHint(1);
		
		ExerciseEditPage.saveExercise();
	}
	
	@Test
	@Order(2)
	void verifyChangesInHintTab() { 
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(() -> new AssertionError("user 'lecturer' could not be found"));
		Exercise exercise = exerciseService.getAllExercisesForUser(lecturer).get(0);
		exercise = exerciseService.getExerciseByIdWithLazyData(exercise.getId()).get();
		MCStage mcStage = (MCStage) exercise.getStartStage();
		
		assertEquals(1, mcStage.getHints().size());
		final StageHint stageHint = mcStage.getHints().get(0);
		assertEquals("this is the first hint", stageHint.getText());
		assertEquals(12, stageHint.getMalus());
	}
	
	@Test
	@Order(3)
	@RunAsClient
	void transitionsTab() { // NOSONAR no assertions here
		assumeLogin();

		MCStagePage mcStage = ExerciseEditPage.getMCStage("#1");
		mcStage.navigateToTransitionsTab();
		
		//let the one stage simply repeat itself endlessly 
		mcStage.setDefaultTransition("Aufgabenteil wiederholen");
		
		mcStage.addSkipTransition("test", EDomain.CHEM, "#1");
		mcStage.addSkipTransition("remove me", EDomain.MATH, "Ende der Aufgabe");
		mcStage.removeSkipTransition(1);
		
		ExerciseEditPage.saveExercise();
	}
	
	@Test
	@Order(4)
	void verifyTransitionTabChanges() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(() -> new AssertionError("user 'lecturer' could not be found"));
		Exercise exercise = exerciseService.getAllExercisesForUser(lecturer).get(0);
		exercise = exerciseService.getExerciseByIdWithLazyData(exercise.getId()).get();
		MCStage mcStage = (MCStage) exercise.getStartStage();
		
		StageTransition defaultTransition = mcStage.getDefaultTransition();
		assertTrue(defaultTransition.isRepeat());
		
		assertEquals(1,mcStage.getSkipTransitions().size());
		StageTransition skipTransition = mcStage.getSkipTransitions().get(0);
		assertEquals("test", skipTransition.getConditionExpression().getCode());
		assertEquals(EDomain.CHEM, skipTransition.getConditionExpression().getDomain());
		assertEquals("#1", skipTransition.getTarget().getInternalName());
	}
	
	@Test
	@Order(5)
	@RunAsClient
	void updateTab() { // NOSONAR no assertions here
		assumeLogin();

		MCStagePage mcStage = ExerciseEditPage.getMCStage("#1");
		mcStage.navigateToUpdateTab();
		
		mcStage.addVariableUpdateBeforeCheck("variable","testCode",EDomain.CHEM);
		mcStage.addVariableUpdateBeforeCheck("variable","other code",EDomain.MATH);
		mcStage.removeVariableUpdateBeforeCheck(1);
		
		mcStage.addVariableUpdateAfterCheck("variable","testCode",EDomain.CHEM);
		mcStage.addVariableUpdateAfterCheck("variable","other code",EDomain.MATH);
		mcStage.removeVariableUpdateAfterCheck(1);
		
		mcStage.addVariableUpdateOnNormalExit("variable","testCode",EDomain.CHEM);
		mcStage.addVariableUpdateOnNormalExit("variable","other code",EDomain.MATH);
		mcStage.removeVariableUpdateOnNormalExit(1);
		
		mcStage.addVariableUpdateOnRepeat("variable","testCode",EDomain.CHEM);
		mcStage.addVariableUpdateOnRepeat("variable","other code",EDomain.MATH);
		mcStage.removeVariableUpdateOnRepeat(1);
		
		mcStage.addVariableUpdateOnSkip("variable","testCode",EDomain.CHEM);
		mcStage.addVariableUpdateOnSkip("variable","other code",EDomain.MATH);
		mcStage.removeVariableUpdateOnSkip(1);
		
		ExerciseEditPage.saveExercise();
	}
	
	@Test
	@Order(9)
	void verifyUpdateTabChanges() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(() -> new AssertionError("user 'lecturer' could not be found"));
		Exercise exercise = exerciseService.getAllExercisesForUser(lecturer).get(0);
		exercise = exerciseService.getExerciseByIdWithLazyData(exercise.getId()).get();
		MCStage mcStage = (MCStage) exercise.getStartStage();
		
		assertEquals(1,mcStage.getVariableUpdatesOnNormalExit().size());
		assertEquals(1,mcStage.getVariableUpdatesAfterCheck().size());
		assertEquals(1,mcStage.getVariableUpdatesBeforeCheck().size());
		assertEquals(1,mcStage.getVariableUpdatesOnRepeat().size());
		assertEquals(1,mcStage.getVariableUpdatesOnSkip().size());
		
		VariableUpdate update = mcStage.getVariableUpdatesAfterCheck().get(0);
		assertEquals("variable", update.getVariableReference().getName());
		assertEquals(EDomain.CHEM, update.getUpdateCode().getDomain());
		assertEquals("testCode", update.getUpdateCode().getCode());
		
		update = mcStage.getVariableUpdatesBeforeCheck().get(0);
		assertEquals("variable", update.getVariableReference().getName());
		assertEquals(EDomain.CHEM, update.getUpdateCode().getDomain());
		assertEquals("testCode", update.getUpdateCode().getCode());
		
		update = mcStage.getVariableUpdatesOnNormalExit().get(0);
		assertEquals("variable", update.getVariableReference().getName());
		assertEquals(EDomain.CHEM, update.getUpdateCode().getDomain());
		assertEquals("testCode", update.getUpdateCode().getCode());
		
		update = mcStage.getVariableUpdatesOnSkip().get(0);
		assertEquals("variable", update.getVariableReference().getName());
		assertEquals(EDomain.CHEM, update.getUpdateCode().getDomain());
		assertEquals("testCode", update.getUpdateCode().getCode());
		
		update = mcStage.getVariableUpdatesOnRepeat().get(0);
		assertEquals("variable", update.getVariableReference().getName());
		assertEquals(EDomain.CHEM, update.getUpdateCode().getDomain());
		assertEquals("testCode", update.getUpdateCode().getCode());
	}

}
