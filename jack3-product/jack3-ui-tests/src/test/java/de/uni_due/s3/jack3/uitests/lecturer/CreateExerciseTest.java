package de.uni_due.s3.jack3.uitests.lecturer;

import static de.uni_due.s3.jack3.uitests.utils.Misc.assumeLogin;
import static de.uni_due.s3.jack3.uitests.utils.Misc.login;
import static de.uni_due.s3.jack3.uitests.utils.Misc.logout;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.enums.EStageHintMalus;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.I18nHelper;
import de.uni_due.s3.jack3.uitests.utils.pages.ExerciseEditPage;
import de.uni_due.s3.jack3.uitests.utils.pages.MyWorkspacePage;

/**
 *
 * @author kilian.kraus
 *
 */
class CreateExerciseTest extends AbstractSeleniumTest {

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	private final String ORIGINAL_NAME = "Exercise 1";
	private final String CHANGED_NAME = "Stochastische-Verteilungen";
	private final String DUPLICATE_NAME = "Duplikat";
	private final String TAG_NAME = "Verteilungen";
	private final String INTERNAL_NOTES = "Diese Aufgabe soll eine Einführung in das Themengebiet der stochastischen Verteilungen darstellen.";
	private final String PUBLIC_DESCRIPTION = "Das ist die Public Description der Aufgabe";

	@Override
	protected void initializeTest() {
		super.initializeTest();
		userBusiness.createUser("lecturer", "secret", "lecturer@foobar.com", false, true);
	}

	@Test
	@Order(1)
	@RunAsClient
	void createExercise() { // NOSONAR Only runs ins the UI, no assertions
		login("lecturer", "secret");
		MyWorkspacePage.navigateToPage();

		// Create Exercise
		MyWorkspacePage.createExercise(MyWorkspacePage.getPersonalFolder(), ORIGINAL_NAME);

		// Enter the Exercise
		MyWorkspacePage.openExercise(MyWorkspacePage.getExercise(ORIGINAL_NAME));
	}

	@Test
	@Order(2)
	@RunAsClient
	void setGeneralExerciseSettings() { // NOSONAR Only runs ins the UI, no assertions
		assumeLogin();

		// Change the Name of the Exercise
		ExerciseEditPage.changeTitle(CHANGED_NAME);

		// It looks like you have to wait a bit before confirming the Tag with Keys.Enter
		// Thats why for now we only write the text of Tag and do not press Enter immediately
		// Write "Verteilung" in the Tag Field
		ExerciseEditPage.enterTagName(TAG_NAME);

		ExerciseEditPage.setInternalNotes(INTERNAL_NOTES);

		ExerciseEditPage.setExternalNotes(PUBLIC_DESCRIPTION);

		// Set Malustype to CUT_MAXIMUM
		ExerciseEditPage.setMalusType(EStageHintMalus.CUT_MAXIMUM);

		// Confirm Tag
		ExerciseEditPage.confirmTag();

		// set Difficulty
		ExerciseEditPage.changeDifficulty(10);

		// Create Variable
		ExerciseEditPage.openVariablesTab();
		String variableName = ExerciseEditPage.createNewVariable();
		variableName = ExerciseEditPage.renameVariable(variableName, "result");
		ExerciseEditPage.changeVariableCode(variableName, "111");

		// Create a second Variable and remove it
		variableName = ExerciseEditPage.createNewVariable();
		ExerciseEditPage.removeVariable(variableName);

		ExerciseEditPage.saveExercise();
	}

	@Test
	@Order(3)
	void verfiyGeneralExerciseSettings() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(AssertionError::new);
		AbstractExercise lazyExercise = exerciseBusiness.getAllExercisesForUser(lecturer).get(0);
		Exercise exercise = exerciseBusiness.getExerciseWithLazyDataByExerciseId(lazyExercise.getId());

		// The Exercise should have been saved only one time, so that there should be only 2 Revisions
		assertEquals(2, exerciseBusiness.getAllRevisionsForExercise(exercise).size());

		assertEquals(CHANGED_NAME, exercise.getName());
		assertEquals(lecturer.getLoginName(), exercise.getUpdatedBy());
		assertEquals(10, exercise.getDifficulty());
		assertEquals(1, exercise.getTags().size());
		assertEquals(TAG_NAME, exercise.getTags().iterator().next().getName());
		assertEquals(I18nHelper.PERSONAL_FOLDER, exercise.getFolder().getName());
		assertEquals(EStageHintMalus.CUT_MAXIMUM, exercise.getHintMalusType());
		assertEquals(INTERNAL_NOTES, exercise.getInternalNotes());
		// The description might not exactly be the original because the CKEditor adds HTML tags.
		assertTrue(exercise.getPublicDescription().contains(PUBLIC_DESCRIPTION));
		assertEquals(I18nHelper.LANGUAGE_GERMAN, exercise.getLanguage());
		assertEquals(1, exercise.getVariableDeclarations().size());

		VariableDeclaration variable = exercise.getVariableDeclarations().get(0);
		assertEquals("result", variable.getName());
		assertEquals("111", variable.getInitializationCode().getCode());
	}

	@Test
	@Order(4)
	@RunAsClient
	void duplicateExercise() { // NOSONAR This test ensures that the actions are possible, for assertions see the next test case
		assumeLogin();
		MyWorkspacePage.navigateToPage();

		// dupliziere die Aufgabe
		// Wir nehmen hier die bereits geänderte Version, weil das Original keine Tags hat
		MyWorkspacePage.duplicateExercise(MyWorkspacePage.getExercise(CHANGED_NAME), DUPLICATE_NAME);

		logout();
	}

	@Test
	@Order(5)
	void verifyDuplicatedExercise() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(AssertionError::new);
		List<Exercise> exercises = exerciseBusiness.getAllExercisesForUser(lecturer);

		Exercise original = exercises.stream().filter(exe -> exe.getName().equals(CHANGED_NAME))
				.findAny()
				.orElseThrow(() -> new AssertionFailedError(
						"Die Aufgabe '" + CHANGED_NAME + "' wurde nicht gefunden"));

		Exercise duplicate = exercises.stream().filter(exe -> exe.getName().equals(DUPLICATE_NAME)).findAny()
				.orElseThrow(
						() -> new AssertionFailedError("Die Aufgabe '" + DUPLICATE_NAME + "' wurde nicht gefunden"));

		original = exerciseBusiness.getExerciseWithLazyDataByExerciseId(original.getId());
		duplicate = exerciseBusiness.getExerciseWithLazyDataByExerciseId(duplicate.getId());

		assertEquals(original.getUpdatedBy(), duplicate.getUpdatedBy());
		assertEquals(original.getDifficulty(), duplicate.getDifficulty());
		assertEquals(original.getTags().size(), duplicate.getTags().size());
		assertEquals(original.getTags().iterator().next().getName(), duplicate.getTags().iterator().next().getName());
		assertEquals(original.getFolder().getName(), duplicate.getFolder().getName());
		assertEquals(original.getHintMalusType(), duplicate.getHintMalusType());
		assertEquals(original.getInternalNotes(), duplicate.getInternalNotes());
		assertEquals(original.getPublicDescription(), duplicate.getPublicDescription());
		assertEquals(original.getLanguage(), duplicate.getLanguage());

		assertEquals(original.getVariableDeclarations().size(), duplicate.getVariableDeclarations().size());

		VariableDeclaration originalVariable = original.getVariableDeclarations().get(0);
		VariableDeclaration duplicateVariable = duplicate.getVariableDeclarations().get(0);
		assertEquals(originalVariable.getName(), duplicateVariable.getName());
		assertEquals(originalVariable.getInitializationCode().getCode(),
				duplicateVariable.getInitializationCode().getCode());

	}

}
