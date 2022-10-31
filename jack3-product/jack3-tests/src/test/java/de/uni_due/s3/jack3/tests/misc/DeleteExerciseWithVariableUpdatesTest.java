package de.uni_due.s3.jack3.tests.misc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;
import de.uni_due.s3.jack3.entities.tenant.VariableUpdate;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

/**
 * Tests if it is possible to delete an exercise with removed variable updates. See JACK/jack3-core#204
 *
 * @author lukas.glaser
 *
 */
@NeedsExercise
class DeleteExerciseWithVariableUpdatesTest extends AbstractContentTest {

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();
		folderBusiness.updateFolderRightsForUser(folder, user, AccessRight.getFull());
	}

	/**
	 * <ol>
	 * <li>Create variable declarations and variable updates</li>
	 * <li>Add variable updates</li>
	 * <li>Save</li>
	 * <li>Delete exercise</li>
	 * </ol>
	 * ==> Exercise should be deleted successfully.
	 */
	@Test
	void deleteExerciseWithNotOrphanVarupdates() throws Exception {
		VariableDeclaration varDecl = new VariableDeclaration("var1");
		exercise.addVariable(varDecl);

		Stage stage = new MCStage();
		stage.addVariableUpdateAfterCheck(new VariableUpdate(varDecl));
		stage.addVariableUpdateOnNormalExit(new VariableUpdate(varDecl));
		exercise.addStage(stage);

		exercise = exerciseBusiness.updateExercise(exercise, user);

		assertFalse(folderService.getContentFolderWithLazyData(folder).getChildrenExercises().isEmpty());
		exerciseBusiness.deleteExercise((Exercise) exercise, user);
		assertTrue(folderService.getContentFolderWithLazyData(folder).getChildrenExercises().isEmpty());
	}

	/**
	 * <ol>
	 * <li>Create variable declarations and variable updates</li>
	 * <li>Add variable updates</li>
	 * <li>Save</li>
	 * <li>Remove variable updates</li>
	 * <li>Save</li>
	 * <li>Delete exercise</li>
	 * </ol>
	 * ==> Exercise should be deleted successfully.
	 */
	@Test
	void deleteExerciseWithOrphanVarupdates() throws Exception {
		VariableDeclaration varDecl = new VariableDeclaration("var1");
		exercise.addVariable(varDecl);

		Stage stage = new MCStage();
		stage.addVariableUpdateAfterCheck(new VariableUpdate(varDecl));
		stage.addVariableUpdateOnNormalExit(new VariableUpdate(varDecl));
		exercise.addStage(stage);

		exercise = exerciseBusiness.updateExercise(exercise, user);

		stage = exercise.getStagesAsList().get(0);
		stage.removeVariableUpdate(stage.getVariableUpdatesAfterCheck().get(0));
		stage.removeVariableUpdate(stage.getVariableUpdatesOnNormalExit().get(0));

		exercise = exerciseBusiness.updateExercise(exercise, user);

		assertFalse(folderService.getContentFolderWithLazyData(folder).getChildrenExercises().isEmpty());
		exerciseBusiness.deleteExercise((Exercise) exercise, user);
		assertTrue(folderService.getContentFolderWithLazyData(folder).getChildrenExercises().isEmpty());
	}

}
