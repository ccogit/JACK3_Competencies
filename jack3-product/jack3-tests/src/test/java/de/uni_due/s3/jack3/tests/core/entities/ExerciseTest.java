package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.enums.EStageHintMalus;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.ExerciseResource;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;
import de.uni_due.s3.jack3.services.TagService;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

@NeedsExercise
class ExerciseTest extends AbstractContentTest {

	@Inject
	private TagService tagService;

	/**
	 * Persist exercise
	 */
	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();

		folder = folderService.getContentFolderWithLazyData(folder);
	}

	/**
	 * Get, add, remove resources
	 */
	@Test
	void testResources() {
		assertTrue(exercise.getExerciseResources().isEmpty());

		// add resources
		for (int i = 1; i <= 4; i++) {
			exercise.addExerciseResource(new ExerciseResource("Resource " + i, ("Content of resource " + i).getBytes(),
					null, "Description of Resource " + i, false));
			exercise = baseService.merge(exercise);
		}

		// resources should be found
		assertEquals(4, exercise.getExerciseResources().size());

		// remove resources
		ArrayList<ExerciseResource> resources = new ArrayList<>(exercise.getExerciseResources());
		for (ExerciseResource item : resources) {
			exercise.removeExerciseResource(item);
		}
		exercise = baseService.merge(exercise);

		// no resources should be found
		assertTrue(exercise.getExerciseResources().isEmpty());
	}

	/**
	 * Get, add stages
	 */
	@Test
	void testStages() {
		assertTrue(exercise.getStages().isEmpty());
		assertTrue(exercise.getStagesAsList().isEmpty());

		// add stages
		Stage stage1 = new MCStage();
		stage1.setOrderIndex(0);
		exercise.addStage(stage1);
		exercise = baseService.merge(exercise);

		Stage stage2 = new FillInStage();
		stage2.setOrderIndex(1);
		exercise.addStage(stage2);
		exercise = baseService.merge(exercise);

		// there should be 1 multiple choice stage and 1 fillIn stage
		assertEquals(2, exercise.getStages().size());
		assertEquals(2, exercise.getStagesAsList().size());

		assertEquals(1,
				exercise.getStages().stream().filter(x -> x instanceof MCStage).collect(Collectors.toSet()).size());
		assertEquals(1, exercise.getStagesAsList().stream().filter(x -> x instanceof MCStage)
				.collect(Collectors.toList()).size());

		assertEquals(1,
				exercise.getStages().stream().filter(x -> x instanceof FillInStage).collect(Collectors.toSet()).size());
		assertEquals(1, exercise.getStagesAsList().stream().filter(x -> x instanceof FillInStage)
				.collect(Collectors.toList()).size());

	}

	@Test
	void testRemoveStage() {
		assertTrue(exercise.getStages().isEmpty());
		assertTrue(exercise.getStagesAsList().isEmpty());
		assertEquals(0, exercise.getStages().size());

		// add stages
		Stage stage1 = new MCStage();
		stage1.setOrderIndex(0);
		exercise.addStage(stage1);
		exercise = baseService.merge(exercise);

		Stage stage2 = new FillInStage();
		stage2.setOrderIndex(1);
		exercise.addStage(stage2);
		exercise = baseService.merge(exercise);

		stage1 = exercise.getStagesAsList().get(0);
		stage2 = exercise.getStagesAsList().get(1);

		// remove stage 1

		exercise.removeStage(stage1);

		exercise = baseService.merge(exercise);

		assertEquals(1, exercise.getStages().size());
		assertEquals(0, stage2.getOrderIndex());

		// remove stage 2
		stage2 = exercise.getStagesAsList().get(0);
		exercise.removeStage(stage2);

		exercise = baseService.merge(exercise);

		assertEquals(0, exercise.getStages().size());

	}

	/**
	 * Get next default internal name for stages
	 */
	@Test
	void getNextStageName() {
		assertEquals("#1", exercise.getNextDefaultInternalNameForStages());

		// add stage
		MCStage stage = new MCStage();
		stage.setInternalName("#1");
		stage.setOrderIndex(0);
		exercise.addStage(stage);

		exercise = baseService.merge(exercise);

		// next available internal name should be "#2" because "#1" is a stored MCStage
		assertEquals("#2", exercise.getNextDefaultInternalNameForStages());
	}

	/**
	 * Change start stage
	 */
	@Test
	void changeStartStage() {
		assertNull(exercise.getStartStage());

		// add stage and set as start stage
		MCStage stage = new MCStage();
		exercise.addStage(stage);
		exercise.setStartStage(stage);

		exercise = baseService.merge(exercise);
		stage = (MCStage) exercise.getStages().stream().findFirst().get();

		assertEquals(stage, exercise.getStartStage());
	}

	/**
	 * Get, add tags
	 */
	@Test
	void testTags() {
		assertTrue(exercise.getTags().isEmpty());
		assertTrue(exercise.getTagsAsStrings().isEmpty());

		// add tags
		for (int i = 1; i <= 4; i++) {
			exercise.addTag(tagService.getOrCreateByName("Tag " + i));
			exercise = baseService.merge(exercise);
		}

		// tag should be found
		assertEquals(4, exercise.getTags().size());
		assertEquals(4, exercise.getTagsAsStrings().size());
		assertTrue(exercise.getTagsAsStrings().containsAll(Arrays.asList("Tag 1", "Tag 2", "Tag 3", "Tag 4")));
	}

	/**
	 * Get, add, remove variable declarations
	 */
	@Test
	void testVariableDeclarations() {
		assertTrue(exercise.getVariableDeclarations().isEmpty());

		// add variable declarations
		for (int i = 1; i <= 4; i++) {
			exercise.addVariable(new VariableDeclaration("Variable Declaration " + i));
			exercise = baseService.merge(exercise);
		}

		// variable declarations should be found
		assertEquals(4, exercise.getVariableDeclarations().size());
		assertEquals(4, exercise.getVariableDeclarationForReoder().size());

		// remove declarations
		HashSet<VariableDeclaration> vars = new HashSet<>(exercise.getVariableDeclarations());
		for (VariableDeclaration item : vars) {
			exercise.removeVariable(item);
		}
		exercise = baseService.merge(exercise);

		// no declarations should be found
		assertTrue(exercise.getVariableDeclarations().isEmpty());
		assertTrue(exercise.getVariableDeclarationForReoder().isEmpty());
	}

	/**
	 * Get next default name for variables
	 */
	@Test
	void getNextVariableName() {
		assertEquals("var1", exercise.getNextDefaultNameForVariables());

		// add variable declaration
		VariableDeclaration varDeclaration = new VariableDeclaration("var1");
		exercise.addVariable(varDeclaration);

		exercise = baseService.merge(exercise);

		// next available variable name should be "var2" because "var1" is a stored variable
		assertEquals("var2", exercise.getNextDefaultNameForVariables());
	}

	/**
	 * Change variable order
	 */
	@Test
	void changeVariableDeclarationOrder() {
		// add variable declarations
		for (int i = 1; i <= 4; i++) {
			VariableDeclaration var = new VariableDeclaration("Variable Declaration " + i);
			var.getInitializationCode().setCode("Code " + i);
			exercise.addVariable(var);
			exercise = baseService.merge(exercise);
		}

		// Illegal arguments (out of bounds)
		assertThrows(IllegalArgumentException.class, () -> {
			exercise.reorderVariableDeclarations(-1, 3);
		});
		assertThrows(IllegalArgumentException.class, () -> {
			exercise.reorderVariableDeclarations(3, -1);
		});
		assertThrows(IllegalArgumentException.class, () -> {
			exercise.reorderVariableDeclarations(3, 7);
		});
		assertThrows(IllegalArgumentException.class, () -> {
			exercise.reorderVariableDeclarations(7, 3);
		});

		// (1,2,3,4) -> (1,4,2,3)
		exercise.reorderVariableDeclarations(3, 1);
		exercise = baseService.merge(exercise);
		assertEquals("Variable Declaration 1", exercise.getVariableDeclarations().get(0).getName());
		assertEquals("Variable Declaration 4", exercise.getVariableDeclarations().get(1).getName());
		assertEquals("Variable Declaration 2", exercise.getVariableDeclarations().get(2).getName());
		assertEquals("Variable Declaration 3", exercise.getVariableDeclarations().get(3).getName());
		assertEquals("Code 1", exercise.getVariableDeclarations().get(0).getInitializationCode().getCode());
		assertEquals("Code 4", exercise.getVariableDeclarations().get(1).getInitializationCode().getCode());
		assertEquals("Code 2", exercise.getVariableDeclarations().get(2).getInitializationCode().getCode());
		assertEquals("Code 3", exercise.getVariableDeclarations().get(3).getInitializationCode().getCode());

		// (1,4,2,3) -> (4,2,3,1)
		exercise.reorderVariableDeclarations(0, 3);
		exercise = baseService.merge(exercise);
		assertEquals("Variable Declaration 4", exercise.getVariableDeclarations().get(0).getName());
		assertEquals("Variable Declaration 2", exercise.getVariableDeclarations().get(1).getName());
		assertEquals("Variable Declaration 3", exercise.getVariableDeclarations().get(2).getName());
		assertEquals("Variable Declaration 1", exercise.getVariableDeclarations().get(3).getName());
		assertEquals("Code 4", exercise.getVariableDeclarations().get(0).getInitializationCode().getCode());
		assertEquals("Code 2", exercise.getVariableDeclarations().get(1).getInitializationCode().getCode());
		assertEquals("Code 3", exercise.getVariableDeclarations().get(2).getInitializationCode().getCode());
		assertEquals("Code 1", exercise.getVariableDeclarations().get(3).getInitializationCode().getCode());

		// (4,2,3,1) -> (4,3,2,1)
		exercise.reorderVariableDeclarations(1, 2);
		exercise = baseService.merge(exercise);
		assertEquals("Variable Declaration 4", exercise.getVariableDeclarations().get(0).getName());
		assertEquals("Variable Declaration 3", exercise.getVariableDeclarations().get(1).getName());
		assertEquals("Variable Declaration 2", exercise.getVariableDeclarations().get(2).getName());
		assertEquals("Variable Declaration 1", exercise.getVariableDeclarations().get(3).getName());
		assertEquals("Code 4", exercise.getVariableDeclarations().get(0).getInitializationCode().getCode());
		assertEquals("Code 3", exercise.getVariableDeclarations().get(1).getInitializationCode().getCode());
		assertEquals("Code 2", exercise.getVariableDeclarations().get(2).getInitializationCode().getCode());
		assertEquals("Code 1", exercise.getVariableDeclarations().get(3).getInitializationCode().getCode());
	}

	/**
	 * Change difficulty
	 */
	@Test
	void changeDifficulty() {
		assertEquals(0, exercise.getDifficulty());

		exercise.setDifficulty(3);
		exercise = baseService.merge(exercise);

		assertEquals(3, exercise.getDifficulty());
	}

	/**
	 * Move exercise to other folder
	 */
	@Test
	void moveExercise() {
		assertEquals(1, folder.getChildrenExercises().size());
		assertTrue(folder.getChildrenExercises().contains(exercise));
		assertEquals(folder, ((Exercise) exercise).getFolder());

		// create a new folder
		ContentFolder newFolder = TestDataFactory.getContentFolder("New Content Folder", null, user);
		baseService.persist(newFolder);

		// move course to the new folder
		folder.removeChildExercise(exercise);
		newFolder.addChildExercise(exercise);

		// merge
		exercise = baseService.merge(exercise);
		folder = folderService.mergeContentFolder(folder);
		newFolder = folderService.mergeContentFolder(newFolder);

		// get with lazy data
		folder = folderService.getContentFolderWithLazyData(folder);
		newFolder = folderService.getContentFolderWithLazyData(newFolder);

		// course should be in new folder
		assertEquals(0, folder.getChildrenExercises().size());
		assertFalse(folder.getChildrenExercises().contains(exercise));
		assertNotEquals(folder, ((Exercise) exercise).getFolder());

		assertEquals(1, newFolder.getChildrenExercises().size());
		assertTrue(newFolder.getChildrenExercises().contains(exercise));
		assertEquals(newFolder, ((Exercise) exercise).getFolder());
	}

	/**
	 * Change notes
	 */
	@Test
	void changeNotes() {
		assertNull(exercise.getInternalNotes());

		exercise.setInternalNotes("Internal Notes");
		exercise = baseService.merge(exercise);

		assertEquals("Internal Notes", exercise.getInternalNotes());
	}

	/**
	 * Change language
	 */
	@Test
	void changeLanguage() {
		assertEquals("de_DE", exercise.getLanguage());

		exercise.setLanguage("en-US");
		exercise = baseService.merge(exercise);

		assertEquals("en-US", exercise.getLanguage());
	}

	/**
	 * Get, (add) result feedback mappings
	 *
	 * Note: No add-method for resultFeedbackMappings
	 */
	@Test
	void getResultFeedbackMappings() {
		assertTrue(exercise.getResultFeedbackMappings().isEmpty());
	}

	/**
	 * Change validity
	 */
	@Test
	void changeValidity() {
		assertFalse(exercise.isValid());

		exercise.setValid(true);
		exercise = baseService.merge(exercise);

		assertTrue(exercise.isValid());
	}

	/**
	 * Change name
	 */
	@Test
	void changeName() {
		assertEquals("Exercise", exercise.getName());

		exercise.setName("Exercise 2.0");
		exercise = baseService.merge(exercise);

		assertEquals("Exercise 2.0", exercise.getName());
	}

	/**
	 * Change malus type
	 */
	@Test
	void changeMalusType() {
		exercise.setHintMalusType(EStageHintMalus.CUT_ACTUAL);
		exercise = baseService.merge(exercise);
		assertEquals(EStageHintMalus.CUT_ACTUAL, exercise.getHintMalusType());

		exercise.setHintMalusType(EStageHintMalus.CUT_MAXIMUM);
		exercise = baseService.merge(exercise);
		assertEquals(EStageHintMalus.CUT_MAXIMUM, exercise.getHintMalusType());
	}

}
