package de.uni_due.s3.jack3.tests.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageHint;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;
import de.uni_due.s3.jack3.entities.tenant.VariableUpdate;
import de.uni_due.s3.jack3.services.utils.RepeatStage;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.core.stagetypes.mc.MCStageTest;

/**
 * Super class for testing stages. T specifies the tested stage type, the method for creating an empty stage should be
 * overridden by the test class. See {@linkplain MCStageTest} as an example. This class also tests {@linkplain Stage}.
 * <p>
 * Do not use this class only for inserting a stage to a test. Use {@linkplain AbstractContentTest} and insert the stage
 * manually.
 *
 * @author lukas.glaser
 * @see AbstractStageSubmissionTest
 */
@NeedsExercise
public abstract class AbstractStageTest<T extends Stage> extends AbstractContentTest {

	protected T stage;

	/**
	 * Implementations should return a new blank stage, e.g.:
	 * 
	 * <pre>
	 * return new FillInStage();
	 * </pre>
	 */
	protected abstract T getNewStage();

	/**
	 * Implementations should return the type of the stage as a string, e.g.:
	 * 
	 * <pre>
	 * return "fillIn";
	 * </pre>
	 */
	protected abstract String getExpectedType();

	/**
	 * Prepare testing the stage: Add a stage to the exercise
	 */
	@SuppressWarnings("unchecked")
	@BeforeEach
	@Override
	protected final void doBeforeTest() {
		super.doBeforeTest();

		stage = getNewStage();
		exercise.addStage(stage);
		exercise = baseService.merge(exercise);
		stage = (T) exercise.getStages().iterator().next();
	}

	@SuppressWarnings("unchecked")
	protected final T saveExercise() {
		exercise = baseService.merge(exercise);
		stage = (T) exercise.getStages().iterator().next();
		return stage;
	}

	/**
	 * Add, get and remove hints
	 */
	@Test
	final void testHints() {
		assertTrue(stage.getHints().isEmpty());
		assertTrue(stage.getHintsForReorder().isEmpty());

		// Add hints
		StageHint hint = new StageHint();
		hint.setText("Hint 2");
		stage.addHint(hint);

		hint = new StageHint();
		hint.setText("Hint 1");
		stage.addHintAtIndex(0, hint);

		// Get hints
		assertEquals(2, stage.getHints().size());
		assertEquals(stage.getHints(), stage.getHintsForReorder());

		assertEquals("Hint 1", stage.getHints().get(0).getText());
		assertEquals("Hint 2", stage.getHints().get(1).getText());

		// Remove Hint 2
		stage.removeHint(stage.getHints().get(1));
		assertEquals(1, stage.getHints().size());
		assertEquals(stage.getHints(), stage.getHintsForReorder());
		assertEquals("Hint 1", stage.getHints().get(0).getText());

		// Remove Hint 1
		stage.removeHint(stage.getHints().get(0));
		assertTrue(stage.getHints().isEmpty());
		assertTrue(stage.getHintsForReorder().isEmpty());
	}

	/**
	 * Add, get and remove stage transitions
	 */
	@Test
	final void testStageTransitions() {
		assertTrue(stage.getStageTransitions().isEmpty());

		// Add transitions
		stage.addStageTransition(new StageTransition());
		stage.addStageTransition(new StageTransition());

		// Get transitions
		assertEquals(2, stage.getStageTransitions().size());

		// Remove Stage Transition 2
		stage.removeStageTransition(stage.getStageTransitions().get(1));
		assertEquals(1, stage.getStageTransitions().size());

		// Remove Stage Transition 1
		stage.removeStageTransition(stage.getStageTransitions().get(0));
		assertTrue(stage.getStageTransitions().isEmpty());
	}

	@SuppressWarnings("unchecked")
	@Test
	final void reorderStageTransitions() {
		// We use three dummy stages with to differ between the stage transitions
		T target1 = getNewStage();
		target1.setInternalName("Target 1");
		target1.setOrderIndex(1);
		exercise.addStage(target1);
		T target2 = getNewStage();
		target2.setInternalName("Target 2");
		target2.setOrderIndex(2);
		exercise.addStage(target2);
		T target3 = getNewStage();
		target3.setInternalName("Target 3");
		target3.setOrderIndex(3);
		exercise.addStage(target3);
		saveExercise();
		target1 = (T) exercise.getStagesAsList().get(1);
		target2 = (T) exercise.getStagesAsList().get(2);
		target3 = (T) exercise.getStagesAsList().get(3);

		stage.addStageTransition(new StageTransition(target1));
		stage.addStageTransition(new StageTransition(target2));
		stage.addStageTransition(new StageTransition(target3));
		saveExercise();

		// Test the correct order of the transitions
		List<StageTransition> transitions = stage.getStageTransitions();
		assertFalse(transitions.isEmpty());
		assertEquals("Target 1", transitions.get(0).getTarget().getInternalName());
		assertEquals("Target 2", transitions.get(1).getTarget().getInternalName());
		assertEquals("Target 3", transitions.get(2).getTarget().getInternalName());

		// Reorder the transitions
		stage.reorderStageTransition(1, 0);
		saveExercise();
		transitions = stage.getStageTransitions();
		assertEquals("Target 2", transitions.get(0).getTarget().getInternalName());
		assertEquals("Target 1", transitions.get(1).getTarget().getInternalName());
		assertEquals("Target 3", transitions.get(2).getTarget().getInternalName());

		// Reorder again
		stage.reorderStageTransition(0, 2);
		saveExercise();
		transitions = stage.getStageTransitions();
		assertEquals("Target 1", transitions.get(0).getTarget().getInternalName());
		assertEquals("Target 3", transitions.get(1).getTarget().getInternalName());
		assertEquals("Target 2", transitions.get(2).getTarget().getInternalName());
	}

	/**
	 * Add, get and remove skip transitions
	 */
	@Test
	final void testSkipTransitions() {
		assertTrue(stage.getSkipTransitions().isEmpty());
		assertTrue(stage.getSkipTransitionsForReorder().isEmpty());

		// Add transitions
		stage.addSkipTransition(new StageTransition());
		stage.addSkipTransitionAtIndex(0, new StageTransition());

		// Get transitions
		assertEquals(2, stage.getSkipTransitions().size());
		assertEquals(stage.getSkipTransitions(), stage.getSkipTransitionsForReorder());

		// Remove Skip Transition 2
		stage.removeSkipTransition(stage.getSkipTransitions().get(1));
		assertEquals(1, stage.getSkipTransitions().size());
		assertEquals(stage.getSkipTransitions(), stage.getSkipTransitionsForReorder());

		// Remove Skip Transition 1
		stage.removeSkipTransition(stage.getSkipTransitions().get(0));
		assertTrue(stage.getSkipTransitions().isEmpty());
		assertTrue(stage.getSkipTransitionsForReorder().isEmpty());
	}

	/**
	 * Add, get and remove variable updates after check
	 */
	@Test
	final void testVariableUpdatesAfterCheck() {
		assertTrue(stage.getVariableUpdatesAfterCheck().isEmpty());
		assertTrue(stage.getVariableUpdatesAfterCheckForReorder().isEmpty());

		// Add variable updates
		stage.addVariableUpdateAfterCheck(new VariableUpdate());
		stage.addVariableUpdateAfterCheckAtIndex(0, new VariableUpdate());

		// Get variable updates
		assertEquals(2, stage.getVariableUpdatesAfterCheck().size());
		assertEquals(stage.getVariableUpdatesAfterCheck(), stage.getVariableUpdatesAfterCheckForReorder());

		// Remove variable update 2
		stage.removeVariableUpdate(stage.getVariableUpdatesAfterCheck().get(1));
		assertEquals(1, stage.getVariableUpdatesAfterCheck().size());
		assertEquals(stage.getVariableUpdatesAfterCheck(), stage.getVariableUpdatesAfterCheckForReorder());

		// Remove variable update 1
		stage.removeVariableUpdate(stage.getVariableUpdatesAfterCheck().get(0));
		assertTrue(stage.getVariableUpdatesAfterCheck().isEmpty());
		assertTrue(stage.getVariableUpdatesAfterCheckForReorder().isEmpty());
	}

	@Test
	final void reorderVariableUpdatesAfterCheck() {
		exercise.addVariable(new VariableDeclaration("var1"));
		exercise.addVariable(new VariableDeclaration("var2"));
		exercise.addVariable(new VariableDeclaration("var3"));
		saveExercise();

		VariableDeclaration var1 = exercise.getVariableDeclarations().get(0);
		VariableDeclaration var2 = exercise.getVariableDeclarations().get(1);
		VariableDeclaration var3 = exercise.getVariableDeclarations().get(2);

		stage.addVariableUpdateAfterCheck(new VariableUpdate(var1));
		stage.addVariableUpdateAfterCheck(new VariableUpdate(var2));
		stage.addVariableUpdateAfterCheck(new VariableUpdate(var3));
		saveExercise();

		// Test the correct order of the variable updates
		List<VariableUpdate> updates = stage.getVariableUpdatesAfterCheck();
		assertFalse(updates.isEmpty());
		assertEquals("var1", updates.get(0).getVariableReference().getName());
		assertEquals("var2", updates.get(1).getVariableReference().getName());
		assertEquals("var3", updates.get(2).getVariableReference().getName());

		// Reorder the updates
		stage.moveVariableUpdateAfterCheck(1, 0);
		saveExercise();
		updates = stage.getVariableUpdatesAfterCheck();
		assertEquals("var2", updates.get(0).getVariableReference().getName());
		assertEquals("var1", updates.get(1).getVariableReference().getName());
		assertEquals("var3", updates.get(2).getVariableReference().getName());

		// Reorder again
		stage.moveVariableUpdateAfterCheck(0, 2);
		saveExercise();
		updates = stage.getVariableUpdatesAfterCheck();
		assertEquals("var1", updates.get(0).getVariableReference().getName());
		assertEquals("var3", updates.get(1).getVariableReference().getName());
		assertEquals("var2", updates.get(2).getVariableReference().getName());
	}

	/**
	 * Add, get and remove variable updates before check
	 */
	@Test
	final void testVariableUpdatesBeforeCheck() {
		assertTrue(stage.getVariableUpdatesBeforeCheck().isEmpty());
		assertTrue(stage.getVariableUpdatesBeforeCheckForReorder().isEmpty());

		// Add variable updates
		stage.addVariableUpdateBeforeCheck(new VariableUpdate());
		stage.addVariableUpdateBeforeCheckAtIndex(0, new VariableUpdate());

		// Get variable updates
		assertEquals(2, stage.getVariableUpdatesBeforeCheck().size());
		assertEquals(stage.getVariableUpdatesBeforeCheck(), stage.getVariableUpdatesBeforeCheckForReorder());

		// Remove variable update 2
		stage.removeVariableUpdate(stage.getVariableUpdatesBeforeCheck().get(1));
		assertEquals(1, stage.getVariableUpdatesBeforeCheck().size());
		assertEquals(stage.getVariableUpdatesBeforeCheck(), stage.getVariableUpdatesBeforeCheckForReorder());

		// Remove variable update 1
		stage.removeVariableUpdate(stage.getVariableUpdatesBeforeCheck().get(0));
		assertTrue(stage.getVariableUpdatesBeforeCheck().isEmpty());
		assertTrue(stage.getVariableUpdatesBeforeCheckForReorder().isEmpty());
	}

	@Test
	final void reorderVariableUpdatesBeforeCheck() {
		exercise.addVariable(new VariableDeclaration("var1"));
		exercise.addVariable(new VariableDeclaration("var2"));
		exercise.addVariable(new VariableDeclaration("var3"));
		saveExercise();

		VariableDeclaration var1 = exercise.getVariableDeclarations().get(0);
		VariableDeclaration var2 = exercise.getVariableDeclarations().get(1);
		VariableDeclaration var3 = exercise.getVariableDeclarations().get(2);

		stage.addVariableUpdateBeforeCheck(new VariableUpdate(var1));
		stage.addVariableUpdateBeforeCheck(new VariableUpdate(var2));
		stage.addVariableUpdateBeforeCheck(new VariableUpdate(var3));
		saveExercise();

		// Test the correct order of the variable updates
		List<VariableUpdate> updates = stage.getVariableUpdatesBeforeCheck();
		assertFalse(updates.isEmpty());
		assertEquals("var1", updates.get(0).getVariableReference().getName());
		assertEquals("var2", updates.get(1).getVariableReference().getName());
		assertEquals("var3", updates.get(2).getVariableReference().getName());

		// Reorder the updates
		stage.moveVariableUpdateBeforeCheck(1, 0);
		saveExercise();
		updates = stage.getVariableUpdatesBeforeCheck();
		assertEquals("var2", updates.get(0).getVariableReference().getName());
		assertEquals("var1", updates.get(1).getVariableReference().getName());
		assertEquals("var3", updates.get(2).getVariableReference().getName());

		// Reorder again
		stage.moveVariableUpdateBeforeCheck(0, 2);
		saveExercise();
		updates = stage.getVariableUpdatesBeforeCheck();
		assertEquals("var1", updates.get(0).getVariableReference().getName());
		assertEquals("var3", updates.get(1).getVariableReference().getName());
		assertEquals("var2", updates.get(2).getVariableReference().getName());
	}

	/**
	 * Add, get and remove variable updates on normal exit
	 */
	@Test
	final void testVariableUpdatesOnNormalExit() {
		assertTrue(stage.getVariableUpdatesOnNormalExit().isEmpty());
		assertTrue(stage.getVariableUpdatesOnNormalExitForReorder().isEmpty());

		// Add variable updates
		stage.addVariableUpdateOnNormalExit(new VariableUpdate());
		stage.addVariableUpdateOnNormalExitAtIndex(0, new VariableUpdate());

		// Get variable updates
		assertEquals(2, stage.getVariableUpdatesOnNormalExit().size());
		assertEquals(stage.getVariableUpdatesOnNormalExit(), stage.getVariableUpdatesOnNormalExitForReorder());

		// Remove variable update 2
		stage.removeVariableUpdate(stage.getVariableUpdatesOnNormalExit().get(1));
		assertEquals(1, stage.getVariableUpdatesOnNormalExit().size());
		assertEquals(stage.getVariableUpdatesOnNormalExit(), stage.getVariableUpdatesOnNormalExitForReorder());

		// Remove variable update 1
		stage.removeVariableUpdate(stage.getVariableUpdatesOnNormalExit().get(0));
		assertTrue(stage.getVariableUpdatesOnNormalExit().isEmpty());
		assertTrue(stage.getVariableUpdatesOnNormalExitForReorder().isEmpty());
	}

	@Test
	final void reorderVariableUpdatesOnNormalExit() {
		exercise.addVariable(new VariableDeclaration("var1"));
		exercise.addVariable(new VariableDeclaration("var2"));
		exercise.addVariable(new VariableDeclaration("var3"));
		saveExercise();

		VariableDeclaration var1 = exercise.getVariableDeclarations().get(0);
		VariableDeclaration var2 = exercise.getVariableDeclarations().get(1);
		VariableDeclaration var3 = exercise.getVariableDeclarations().get(2);

		stage.addVariableUpdateOnNormalExit(new VariableUpdate(var1));
		stage.addVariableUpdateOnNormalExit(new VariableUpdate(var2));
		stage.addVariableUpdateOnNormalExit(new VariableUpdate(var3));
		saveExercise();

		// Test the correct order of the variable updates
		List<VariableUpdate> updates = stage.getVariableUpdatesOnNormalExit();
		assertFalse(updates.isEmpty());
		assertEquals("var1", updates.get(0).getVariableReference().getName());
		assertEquals("var2", updates.get(1).getVariableReference().getName());
		assertEquals("var3", updates.get(2).getVariableReference().getName());

		// Reorder the updates
		stage.moveVariableUpdateOnNormalExit(1, 0);
		saveExercise();
		updates = stage.getVariableUpdatesOnNormalExit();
		assertEquals("var2", updates.get(0).getVariableReference().getName());
		assertEquals("var1", updates.get(1).getVariableReference().getName());
		assertEquals("var3", updates.get(2).getVariableReference().getName());

		// Reorder again
		stage.moveVariableUpdateOnNormalExit(0, 2);
		saveExercise();
		updates = stage.getVariableUpdatesOnNormalExit();
		assertEquals("var1", updates.get(0).getVariableReference().getName());
		assertEquals("var3", updates.get(1).getVariableReference().getName());
		assertEquals("var2", updates.get(2).getVariableReference().getName());
	}

	/**
	 * Add, get and remove variable updates on repeat
	 */
	@Test
	final void testVariableUpdatesOnRepeat() {
		assertTrue(stage.getVariableUpdatesOnRepeat().isEmpty());
		assertTrue(stage.getVariableUpdatesOnRepeatForReorder().isEmpty());

		// Add variable updates
		stage.addVariableUpdateOnRepeat(new VariableUpdate());
		stage.addVariableUpdateOnRepeatAtIndex(0, new VariableUpdate());

		// Get variable updates
		assertEquals(2, stage.getVariableUpdatesOnRepeat().size());
		assertEquals(stage.getVariableUpdatesOnRepeat(), stage.getVariableUpdatesOnRepeatForReorder());

		// Remove variable update 2
		stage.removeVariableUpdate(stage.getVariableUpdatesOnRepeat().get(1));
		assertEquals(1, stage.getVariableUpdatesOnRepeat().size());
		assertEquals(stage.getVariableUpdatesOnRepeat(), stage.getVariableUpdatesOnRepeatForReorder());

		// Remove variable update 1
		stage.removeVariableUpdate(stage.getVariableUpdatesOnRepeat().get(0));
		assertTrue(stage.getVariableUpdatesOnRepeat().isEmpty());
		assertTrue(stage.getVariableUpdatesOnRepeatForReorder().isEmpty());
	}

	@Test
	final void reorderVariableUpdatesOnRepeat() {
		exercise.addVariable(new VariableDeclaration("var1"));
		exercise.addVariable(new VariableDeclaration("var2"));
		exercise.addVariable(new VariableDeclaration("var3"));
		saveExercise();

		VariableDeclaration var1 = exercise.getVariableDeclarations().get(0);
		VariableDeclaration var2 = exercise.getVariableDeclarations().get(1);
		VariableDeclaration var3 = exercise.getVariableDeclarations().get(2);

		stage.addVariableUpdateOnRepeat(new VariableUpdate(var1));
		stage.addVariableUpdateOnRepeat(new VariableUpdate(var2));
		stage.addVariableUpdateOnRepeat(new VariableUpdate(var3));
		saveExercise();

		// Test the correct order of the variable updates
		List<VariableUpdate> updates = stage.getVariableUpdatesOnRepeat();
		assertFalse(updates.isEmpty());
		assertEquals("var1", updates.get(0).getVariableReference().getName());
		assertEquals("var2", updates.get(1).getVariableReference().getName());
		assertEquals("var3", updates.get(2).getVariableReference().getName());

		// Reorder the updates
		stage.moveVariableUpdateOnRepeat(1, 0);
		saveExercise();
		updates = stage.getVariableUpdatesOnRepeat();
		assertEquals("var2", updates.get(0).getVariableReference().getName());
		assertEquals("var1", updates.get(1).getVariableReference().getName());
		assertEquals("var3", updates.get(2).getVariableReference().getName());

		// Reorder again
		stage.moveVariableUpdateOnRepeat(0, 2);
		saveExercise();
		updates = stage.getVariableUpdatesOnRepeat();
		assertEquals("var1", updates.get(0).getVariableReference().getName());
		assertEquals("var3", updates.get(1).getVariableReference().getName());
		assertEquals("var2", updates.get(2).getVariableReference().getName());
	}

	/**
	 * Add, get and remove variable updates on skip
	 */
	@Test
	final void testVariableUpdatesOnSkip() {
		assertTrue(stage.getVariableUpdatesOnSkip().isEmpty());
		assertTrue(stage.getVariableUpdatesOnSkipForReorder().isEmpty());

		// Add variable updates
		stage.addVariableUpdateOnSkip(new VariableUpdate());
		stage.addVariableUpdateOnSkipAtIndex(0, new VariableUpdate());

		// Get variable updates
		assertEquals(2, stage.getVariableUpdatesOnSkip().size());
		assertEquals(stage.getVariableUpdatesOnSkip(), stage.getVariableUpdatesOnSkipForReorder());

		// Remove variable update 2
		stage.removeVariableUpdate(stage.getVariableUpdatesOnSkip().get(1));
		assertEquals(1, stage.getVariableUpdatesOnSkip().size());
		assertEquals(stage.getVariableUpdatesOnSkip(), stage.getVariableUpdatesOnSkipForReorder());

		// Remove variable update 1
		stage.removeVariableUpdate(stage.getVariableUpdatesOnSkip().get(0));
		assertTrue(stage.getVariableUpdatesOnSkip().isEmpty());
		assertTrue(stage.getVariableUpdatesOnSkipForReorder().isEmpty());
	}

	@Test
	final void reorderVariableUpdatesOnSkip() {
		exercise.addVariable(new VariableDeclaration("var1"));
		exercise.addVariable(new VariableDeclaration("var2"));
		exercise.addVariable(new VariableDeclaration("var3"));
		saveExercise();

		VariableDeclaration var1 = exercise.getVariableDeclarations().get(0);
		VariableDeclaration var2 = exercise.getVariableDeclarations().get(1);
		VariableDeclaration var3 = exercise.getVariableDeclarations().get(2);

		stage.addVariableUpdateOnSkip(new VariableUpdate(var1));
		stage.addVariableUpdateOnSkip(new VariableUpdate(var2));
		stage.addVariableUpdateOnSkip(new VariableUpdate(var3));
		saveExercise();

		// Test the correct order of the variable updates
		List<VariableUpdate> updates = stage.getVariableUpdatesOnSkip();
		assertFalse(updates.isEmpty());
		assertEquals("var1", updates.get(0).getVariableReference().getName());
		assertEquals("var2", updates.get(1).getVariableReference().getName());
		assertEquals("var3", updates.get(2).getVariableReference().getName());

		// Reorder the updates
		stage.moveVariableUpdateOnSkip(1, 0);
		saveExercise();
		updates = stage.getVariableUpdatesOnSkip();
		assertEquals("var2", updates.get(0).getVariableReference().getName());
		assertEquals("var1", updates.get(1).getVariableReference().getName());
		assertEquals("var3", updates.get(2).getVariableReference().getName());

		// Reorder again
		stage.moveVariableUpdateOnSkip(0, 2);
		saveExercise();
		updates = stage.getVariableUpdatesOnSkip();
		assertEquals("var1", updates.get(0).getVariableReference().getName());
		assertEquals("var3", updates.get(1).getVariableReference().getName());
		assertEquals("var2", updates.get(2).getVariableReference().getName());
	}

	@Test
	final void changeAllowSkip() {
		assertFalse(stage.getAllowSkip());

		stage.setAllowSkip(true);

		assertTrue(stage.getAllowSkip());
	}

	@Test
	final void changeDefaultTransition() {
		assertTrue(stage.getStageTransitions().isEmpty());

		stage.addStageTransition(new StageTransition());

		assertEquals(1, stage.getStageTransitions().size());
	}

	@Test
	final void changeExternalName() {
		assertNull(stage.getExternalName());

		stage.setExternalName("External Name");

		assertEquals("External Name", stage.getExternalName());
	}

	@Test
	final void changeInternalName() {
		assertNull(stage.getInternalName());

		stage.setInternalName("Internal Name");

		assertEquals("Internal Name", stage.getInternalName());
	}

	@Test
	final void changeSkipMessage() {
		assertNull(stage.getSkipMessage());

		stage.setSkipMessage("Skip Message");

		assertEquals("Skip Message", stage.getSkipMessage());
	}

	@Test
	final void changeTaskDescription() {
		assertNull(stage.getTaskDescription());

		stage.setTaskDescription("Task Description");

		assertEquals("Task Description", stage.getTaskDescription());
	}

	/**
	 * Remove all variable updates
	 */
	@Test
	final void removeAllVariableUpdatesForVariable() {
		VariableDeclaration declaration1 = new VariableDeclaration("var1");
		VariableDeclaration declaration2 = new VariableDeclaration("var2");

		stage.addVariableUpdateAfterCheck(new VariableUpdate(declaration1));
		stage.addVariableUpdateBeforeCheck(new VariableUpdate(declaration1));
		stage.addVariableUpdateOnNormalExit(new VariableUpdate(declaration1));
		stage.addVariableUpdateOnRepeat(new VariableUpdate(declaration1));
		stage.addVariableUpdateOnSkip(new VariableUpdate(declaration1));

		stage.addVariableUpdateAfterCheck(new VariableUpdate(declaration2));
		stage.addVariableUpdateBeforeCheck(new VariableUpdate(declaration2));
		stage.addVariableUpdateOnNormalExit(new VariableUpdate(declaration2));
		stage.addVariableUpdateOnRepeat(new VariableUpdate(declaration2));
		stage.addVariableUpdateOnSkip(new VariableUpdate(declaration2));

		assertFalse(stage.getVariableUpdatesAfterCheck().isEmpty());
		assertFalse(stage.getVariableUpdatesBeforeCheck().isEmpty());
		assertFalse(stage.getVariableUpdatesOnNormalExit().isEmpty());
		assertFalse(stage.getVariableUpdatesOnRepeat().isEmpty());
		assertFalse(stage.getVariableUpdatesOnSkip().isEmpty());

		List<VariableUpdate> allVarUpdates = new LinkedList<>();
		allVarUpdates.addAll(stage.getVariableUpdatesAfterCheck());
		allVarUpdates.addAll(stage.getVariableUpdatesBeforeCheck());
		allVarUpdates.addAll(stage.getVariableUpdatesOnNormalExit());
		allVarUpdates.addAll(stage.getVariableUpdatesOnRepeat());
		allVarUpdates.addAll(stage.getVariableUpdatesOnSkip());

		// All variable updates should reference var1 or var2
		assertTrue(allVarUpdates.stream()
				.allMatch(varUpdate -> varUpdate.getVariableReference().getName().equals("var1")
						|| varUpdate.getVariableReference().getName().equals("var2")));

		stage.removeAllUpdatesForVariable(declaration1);

		allVarUpdates = new LinkedList<>();
		allVarUpdates.addAll(stage.getVariableUpdatesAfterCheck());
		allVarUpdates.addAll(stage.getVariableUpdatesBeforeCheck());
		allVarUpdates.addAll(stage.getVariableUpdatesOnNormalExit());
		allVarUpdates.addAll(stage.getVariableUpdatesOnRepeat());
		allVarUpdates.addAll(stage.getVariableUpdatesOnSkip());

		// There should be no variable update with reference to "var1"
		assertTrue(allVarUpdates.stream()
				.allMatch(varUpdate -> varUpdate.getVariableReference().getName().equals("var2")));
	}

	/**
	 * Tests {@linkplain Stage#isEndStage()}
	 */
	@Test
	final void isEndStage() {
		// Case 1: end stage (no target)
		T stage = getNewStage();
		assertTrue(stage.isEndStage());

		// Case 2: repeat
		stage = getNewStage();
		stage.getDefaultTransition().setTarget(new RepeatStage());
		assertFalse(stage.isEndStage());

		// Case 3: "normal" target
		stage = getNewStage();
		stage.getDefaultTransition().setTarget(getNewStage());
		assertFalse(stage.isEndStage());

		// Case 4: recursive target
		stage = getNewStage();
		stage.getDefaultTransition().setTarget(stage);
		assertFalse(stage.isEndStage());

		// Case 5: stage without default transition, but with skip transitions
		stage = getNewStage();
		stage.getDefaultTransition().setTarget(getNewStage());
		assertFalse(stage.isEndStage());

		// Case 6: stage without default transition, but with stage transitions
		stage = getNewStage();
		stage.getDefaultTransition().setTarget(getNewStage());
		assertFalse(stage.isEndStage());

		// Case 7.1: regular extra-transition
		stage = getNewStage();
		stage.getDefaultTransition().setTarget(getNewStage());
		stage.addStageTransition(new StageTransition(stage));
		assertFalse(stage.isEndStage());

		// Case 7.2: regular extra-transition that potentially ends the exercise
		stage = getNewStage();
		stage.getDefaultTransition().setTarget(getNewStage());
		stage.addStageTransition(new StageTransition());
		assertTrue(stage.isEndStage());

		// Case 8.1: extra skip transition
		stage = getNewStage();
		stage.getDefaultTransition().setTarget(getNewStage());
		stage.addSkipTransition(new StageTransition(stage));
		assertFalse(stage.isEndStage());

		// Case 8.2: extra skip transition that potentially ends the exercise
		stage = getNewStage();
		stage.getDefaultTransition().setTarget(getNewStage());
		stage.addSkipTransition(new StageTransition());
		assertTrue(stage.isEndStage());
	}

	/**
	 * Tests {@linkplain Stage#leadsTo(Stage)}
	 */
	@Test
	final void leadsTo() {
		// Case 1: A ->
		T stageA = getNewStage();
		T stageB = getNewStage();
		assertFalse(stageA.leadsTo(stageA));
		assertFalse(stageA.leadsTo(stageB));
		assertFalse(stageB.leadsTo(stageA));
		assertFalse(stageB.leadsTo(stageB));

		// Case 1: A -> B
		stageA.getDefaultTransition().setTarget(stageB);
		stageB.getDefaultTransition().setTarget(null);
		assertFalse(stageA.leadsTo(stageA));
		assertTrue(stageA.leadsTo(stageB));
		assertFalse(stageB.leadsTo(stageA));
		assertFalse(stageB.leadsTo(stageB));

		// Case 1: A -> A
		stageA.getDefaultTransition().setTarget(stageA);
		stageB.getDefaultTransition().setTarget(null);
		assertTrue(stageA.leadsTo(stageA));
		assertFalse(stageA.leadsTo(stageB));
		assertFalse(stageB.leadsTo(stageA));
		assertFalse(stageB.leadsTo(stageB));

		// Case 1: A -> B (skip)
		stageA.getDefaultTransition().setTarget(null);
		stageB.getDefaultTransition().setTarget(null);
		stageA.addSkipTransition(new StageTransition(stageB));
		assertFalse(stageA.leadsTo(stageA));
		assertTrue(stageA.leadsTo(stageB));
		assertFalse(stageB.leadsTo(stageA));
		assertFalse(stageB.leadsTo(stageB));

		// Case 1: A -> B (extra stage transition)
		stageA.getDefaultTransition().setTarget(null);
		stageB.getDefaultTransition().setTarget(null);
		stageA.addStageTransition(new StageTransition(stageB));
		assertFalse(stageA.leadsTo(stageA));
		assertTrue(stageA.leadsTo(stageB));
		assertFalse(stageB.leadsTo(stageA));
		assertFalse(stageB.leadsTo(stageB));
	}

	@Test
	final void testType() {
		assertEquals(getExpectedType(), stage.getType());
	}
}
