package de.uni_due.s3.jack3.uitests.stages;

import static de.uni_due.s3.jack3.uitests.utils.Misc.assumeLogin;
import static de.uni_due.s3.jack3.uitests.utils.Misc.login;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.builders.ExerciseBuilder;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.enums.EStageHintMalus;
import de.uni_due.s3.jack3.entities.stagetypes.r.AbstractTestCase;
import de.uni_due.s3.jack3.entities.stagetypes.r.DynamicRTestCase;
import de.uni_due.s3.jack3.entities.stagetypes.r.ETestCasePointsMode;
import de.uni_due.s3.jack3.entities.stagetypes.r.ETestcaseRuleMode;
import de.uni_due.s3.jack3.entities.stagetypes.r.RStage;
import de.uni_due.s3.jack3.entities.stagetypes.r.StaticRTestCase;
import de.uni_due.s3.jack3.entities.stagetypes.r.TestCaseTuple;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.ExerciseService;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.pages.ExerciseEditPage;
import de.uni_due.s3.jack3.uitests.utils.pages.MyWorkspacePage;
import de.uni_due.s3.jack3.uitests.utils.pages.stages.RStagePage;

class RStageTest extends AbstractSeleniumTest {

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
		ExerciseEditPage.createRStage("My R Stage");
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
		assertEquals("My R Stage", exercise.getStartStage().getInternalName());
		assertTrue(exercise.getStartStage() instanceof RStage);
	}

	@Test
	@Order(3)
	@RunAsClient
	void changeContentTabSettings () { // NOSONAR no assertions here
		assumeLogin();

		ExerciseEditPage.expandStage("My R Stage");
		RStagePage rStage = ExerciseEditPage.getRStage("My R Stage");

		rStage.navigateToContentTab();
		rStage.setExternalTitel("my crazy first stage");
		rStage.setExerciseDescritptionText("this is the description");
		rStage.setInitialStageCode("values <- c(1,2,3,4)");

		ExerciseEditPage.saveExercise();
	}

	@Test
	@Order(4)
	void verifyContentTabChanges() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(() -> new AssertionError("user 'lecturer' could not be found"));
		Exercise exercise = exerciseService.getAllExercisesForUser(lecturer).get(0);
		exercise = exerciseService.getExerciseByIdWithLazyData(exercise.getId()).get();
		RStage rStage = (RStage) exercise.getStartStage();

		assertEquals("my crazy first stage", rStage.getExternalName());
		assertEquals("this is the description", rStage.getTaskDescription());
		assertEquals("values <- c(1,2,3,4)", rStage.getInitialCode());
	}

	@Test
	@Order(5)
	@RunAsClient
	void changeFeedbackTabSettings() { // NOSONAR no assertions here
		assumeLogin();

		RStagePage rStage = ExerciseEditPage.getRStage("My R Stage");
		rStage.navigateToFeedbackTab();

		rStage.setStageWeight(5);
		rStage.setEvaluationRule("0.4 * #{c123} + 0.6 * #{c234}");
		rStage.setAllowSkip(true);
		rStage.setSkipFeedbackText("you skipped the R stage!");

		ExerciseEditPage.saveExercise();
	}

	@Test
	@Order(6)
	void verifyFeedbackTabChanges() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(() -> new AssertionError("user 'lecturer' could not be found"));
		Exercise exercise = exerciseService.getAllExercisesForUser(lecturer).get(0);
		exercise = exerciseService.getExerciseByIdWithLazyData(exercise.getId()).get();
		RStage rStage = (RStage) exercise.getStartStage();

		assertEquals(5, rStage.getWeight());
		assertEquals("0.4 * #{c123} + 0.6 * #{c234}", rStage.getFinalResultComputationString());
		assertTrue(rStage.getAllowSkip());
		assertEquals("you skipped the R stage!", rStage.getSkipMessage());
	}

	@Test
	@Order(7)
	@RunAsClient
	void changeCheckerTabSettings() {
		assumeLogin();

		RStagePage rStage = ExerciseEditPage.getRStage("My R Stage");
		rStage.navigateToCheckerTab();

		rStage.addNewTestCaseGroup();
		assertEquals(2, rStage.getNumberOfTestCaseGroups());
		rStage.addNewTestCaseGroup();
		assertEquals(3, rStage.getNumberOfTestCaseGroups());
		rStage.removeTestCaseGroup(1);
		assertEquals(2,rStage.getNumberOfTestCaseGroups());

		rStage.createDynamicTestCase(0, "my dynamic test", "my post code", "my post processing function", "my expected result", -2, ETestcaseRuleMode.PRESENCE, ETestCasePointsMode.GAIN, 22, "this is my feedback");
		rStage.createStaticTestCase(0, "my static test", "my GReQL query", ETestcaseRuleMode.ABSENCE, 11, ETestCasePointsMode.GAIN, "my static feedback string");
		rStage.createStaticTestCase(1, "my static test1", "my GReQL query1", ETestcaseRuleMode.ABSENCE, 33, ETestCasePointsMode.GAIN, "my static feedback string1");
		rStage.createStaticTestCase(1, "my static test2", "my GReQL query2", ETestcaseRuleMode.PRESENCE, 44, ETestCasePointsMode.DEDUCTION, "my static feedback string2");

		ExerciseEditPage.saveExercise();
	}

	@Test
	@Order(8)
	void verifyCheckerTabSettings() { // NOSONAR no assertions here
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(() -> new AssertionError("user 'lecturer' could not be found"));
		Exercise exercise = exerciseService.getAllExercisesForUser(lecturer).get(0);
		exercise = exerciseService.getExerciseByIdWithLazyData(exercise.getId()).get();
		RStage rStage = (RStage) exercise.getStartStage();

		List<TestCaseTuple> testCaseTuples = rStage.getTestCasetuples();
		assertEquals(2,testCaseTuples.size());
		TestCaseTuple testCaseTuple1 = testCaseTuples.get(0);
		TestCaseTuple testCaseTuple2 = testCaseTuples.get(1);

		//first tuple
		List<AbstractTestCase> testCasesFromTuple1 = testCaseTuple1.getTestCases();
		assertEquals(2,testCasesFromTuple1.size());
		assertTrue(testCasesFromTuple1.get(0) instanceof DynamicRTestCase);
		assertTrue(testCasesFromTuple1.get(1) instanceof StaticRTestCase);

		DynamicRTestCase dynamicTest = (DynamicRTestCase) testCasesFromTuple1.get(0);
		assertEquals("my dynamic test", dynamicTest.getName());
		assertEquals("my post code",dynamicTest.getPostCode());
		assertEquals("my post processing function",dynamicTest.getPostprocessingFunction()); //TODO null?!?
		assertEquals("my expected result",dynamicTest.getExpectedOutput());
		assertEquals(-2, dynamicTest.getTolerance());
		assertEquals(ETestcaseRuleMode.PRESENCE, dynamicTest.getRuleMode());
		assertEquals(ETestCasePointsMode.GAIN, dynamicTest.getPointsMode());
		assertEquals(22, dynamicTest.getPoints());
		assertEquals("this is my feedback", dynamicTest.getFeedbackIfFailed());

		StaticRTestCase staticTest1 = (StaticRTestCase) testCasesFromTuple1.get(1);
		assertEquals("my static test", staticTest1.getName());
		assertEquals("my GReQL query", staticTest1.getQueries().get(0));
		assertEquals(ETestcaseRuleMode.ABSENCE, staticTest1.getRuleMode());
		assertEquals(11, staticTest1.getPoints());
		assertEquals(ETestCasePointsMode.GAIN, staticTest1.getPointsMode());
		assertEquals("my static feedback string", staticTest1.getFeedbackIfFailed());

		//second tuple
		List<AbstractTestCase> testCasesFromTuple2 = testCaseTuple2.getTestCases();
		assertEquals(2,testCasesFromTuple2.size());
		assertTrue(testCasesFromTuple2.get(0) instanceof StaticRTestCase);
		assertTrue(testCasesFromTuple2.get(1) instanceof StaticRTestCase);

		StaticRTestCase staticTest2 = (StaticRTestCase) testCasesFromTuple2.get(0);
		assertEquals("my static test1", staticTest2.getName());
		assertEquals("my GReQL query1", staticTest2.getQueries().get(0));
		assertEquals(ETestcaseRuleMode.ABSENCE, staticTest2.getRuleMode());
		assertEquals(33, staticTest2.getPoints());
		assertEquals(ETestCasePointsMode.GAIN, staticTest2.getPointsMode());
		assertEquals("my static feedback string1", staticTest2.getFeedbackIfFailed());

		StaticRTestCase staticTest3 = (StaticRTestCase) testCasesFromTuple2.get(1);
		assertEquals("my static test2", staticTest3.getName());
		assertEquals("my GReQL query2", staticTest3.getQueries().get(0));
		assertEquals(ETestcaseRuleMode.PRESENCE, staticTest3.getRuleMode());
		assertEquals(44, staticTest3.getPoints());
		assertEquals(ETestCasePointsMode.DEDUCTION, staticTest3.getPointsMode());
		assertEquals("my static feedback string2", staticTest3.getFeedbackIfFailed());

	}


}
