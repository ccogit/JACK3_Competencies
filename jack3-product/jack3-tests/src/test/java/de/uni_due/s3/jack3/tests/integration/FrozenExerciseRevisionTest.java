package de.uni_due.s3.jack3.tests.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.google.common.base.Defaults;
import com.google.common.reflect.TypeToken;

import de.uni_due.s3.jack3.builders.ExerciseBuilder;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.entities.enums.EStageHintMalus;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.FrozenExercise;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;
import de.uni_due.s3.jack3.exceptions.JackRuntimeException;
import de.uni_due.s3.jack3.services.TagService;
import de.uni_due.s3.jack3.services.utils.RepeatStage;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

@NeedsExercise
class FrozenExerciseRevisionTest extends AbstractContentTest {

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private TagService tagService;

	@Test
	void testFrozenExercise() {
		assertFalse(exercise.isFrozen());

		exercise.setDifficulty(3);

		exercise.addTag(tagService.getOrCreateByName("myTag"));
		exercise.addVariable(new VariableDeclaration("myVariable"));
		exercise.setLanguage("English");
		exercise.setName("myExercise");
		exercise.setHintMalusType(EStageHintMalus.CUT_ACTUAL);
		exercise.setInternalNotes("importent Node!");
		exercise.setValid(true);

		MCStage stage = new MCStage();
		stage.setInternalName("myInternalStage");
		stage.setExternalName("myExternalStage");

		exercise.addStage(stage);
		exercise.setStartStage(stage);

		exercise = baseService.merge(exercise);

		// create frozen exercise
		exerciseBusiness.createFrozenExercise(exercise, exerciseBusiness.getRevisionNumbersFor(exercise).get(1));

		// changing exercise
		exercise.setDifficulty(95);

		exercise.addTag(tagService.getOrCreateByName("myBetterTage"));
		exercise.addTag(tagService.getOrCreateByName("onmore"));

		exercise.removeTag(exercise.getTags().stream().filter(tag -> tag.getName().equals("myTag")).findAny().get());
		exercise.getVariableDeclarations().get(0).setName("changedVariable");
		exercise.addVariable(new VariableDeclaration("oneMoreVariable"));
		exercise.setLanguage("German");
		exercise.setName("myExercise2.0");
		exercise.setHintMalusType(EStageHintMalus.CUT_MAXIMUM);
		exercise.setInternalNotes("Better Nodes now!");
		exercise.setValid(false);

		stage = (MCStage) exercise.getStagesAsList().get(0);
		stage.setInternalName("myInternalStage2.0");
		stage.setExternalName("myExternalStage2.0");

		FillInStage fillInStage = new FillInStage();
		fillInStage.setExternalName("fillInStage");
		fillInStage.setInternalName("internalStuff");

		exercise.addStage(fillInStage);
		exercise.setStartStage(fillInStage);

		exercise = baseService.merge(exercise);

		// check if the frozen Exercise is still correct

		assertEquals(1, exerciseBusiness.getFrozenRevisionsForExercise(exercise).size());
		FrozenExercise frozenExercise = exerciseBusiness.getFrozenExerciseWithLazyDataById(
				exerciseBusiness.getFrozenRevisionsForExercise(exercise).get(0).getId());

		testThatFrozenExerciseCantChange(frozenExercise);

		assertTrue(frozenExercise.isFrozen());
		assertEquals(3, frozenExercise.getDifficulty());
		assertEquals(1, frozenExercise.getTags().size());
		assertEquals("myTag", frozenExercise.getTags().stream().findAny().get().getName());
		assertEquals("English", frozenExercise.getLanguage());
		assertEquals(EStageHintMalus.CUT_ACTUAL, frozenExercise.getHintMalusType());
		assertEquals("importent Node!", frozenExercise.getInternalNotes());
		assertTrue(frozenExercise.isValid());
		assertEquals(1, frozenExercise.getStages().size());
		assertTrue(frozenExercise.getStagesAsList().get(0) instanceof MCStage);
		assertEquals("myInternalStage", frozenExercise.getStagesAsList().get(0).getInternalName());
		assertEquals("myExternalStage", frozenExercise.getStagesAsList().get(0).getExternalName());
		assertEquals(frozenExercise.getStagesAsList().get(0), frozenExercise.getStartStage());
	}

	private void testThatFrozenExerciseCantChange(FrozenExercise frozenExercise) {
		// First we get all Methods which would change the FrozenExercise using reflection
		Method[] allMethods = FrozenExercise.class.getMethods();
		List<Method> setters = new ArrayList<>();
		for (Method method : allMethods) {
			if (method.getName().startsWith("set") || method.getName().startsWith("add")
					|| method.getName().startsWith("remove")) {
				setters.add(method);
			}
		}

		for (Method method : setters) {
			// For the current method we construct a parameter-array with a default value for each parameter. This is
			// a bit cumbersome for primitive-types, but fortunatly google guava has us covered here.
			Object[] args = new Object[method.getGenericParameterTypes().length];
			for (int i = 0; i < method.getGenericParameterTypes().length; i++) {
				Type type = method.getGenericParameterTypes()[i];
				Class<?> clazz = TypeToken.of(type).getRawType();
				args[i] = Defaults.defaultValue(clazz);
			}

			try {
				// These Methods are okay to call
				if ("setFrozenTitle".equals(method.getName()) //
						|| "setproxiedExerciseRevisionId".equals(method.getName()) //
						|| "setProxiedExerciseId".equals(method.getName()) //
						|| "setUpdateTimeStampToNow".equals(method.getName()) //
						|| "setRevisionAuthor".equals(method.getName()) //
						) {
					continue;
				}

				// Now we try to invoke the the method with our default params and we expect to get a
				// UnsupportedOperationException for each invocation
				method.invoke(frozenExercise, args);
				fail("The following Method did not throw an UnsupportedOperationException: " + method);
			} catch (InvocationTargetException e) {
				// Calling this method by reflection, our exception gets wrapped in an InvocationTargetException
				assertTrue(e.getCause() instanceof UnsupportedOperationException);
				assertEquals("Must not change state of frozen objects!", e.getCause().getMessage());
			} catch (IllegalAccessException | IllegalArgumentException e) {
				throw new JackRuntimeException(e);
			}
		}
	}

	@Test
	void freezeExerciseWithManyTransitions() {
		exercise = new ExerciseBuilder((Exercise) exercise)
				.withMCStage().withInternalName("0").withAnswerOption("Correct", true).and()
				.withMCStage().withInternalName("1").withAnswerOption("Correct", true).and()
				.withMCStage().withInternalName("2").withAnswerOption("Correct", true).and()
				.withMCStage().withInternalName("3").withAnswerOption("Correct", true).and()
				.create();
		List<Stage> stages = exercise.getStagesAsList();

		/*-
		 * Create the transitions as following:
		 * 0 -> 2 (conditional)
		 * 0 -> 3 (conditional)
		 * 1 -> 0 (skipped)
		 * 1 -> 1 (repeat, but without RepeatStage)
		 * 2 -> 2 (repeat stage)
		 */
		stages.get(0).addStageTransition(new StageTransition(stages.get(2)));
		stages.get(0).addStageTransition(new StageTransition(stages.get(3)));
		stages.get(1).addSkipTransition(new StageTransition(stages.get(0)));
		stages.get(1).addStageTransition(new StageTransition(stages.get(1)));
		stages.get(2).addStageTransition(new StageTransition(new RepeatStage()));
		exercise = baseService.merge(exercise);

		// We delete all stages to remove all transitions
		new ArrayList<>(exercise.getStages()).forEach(exercise::removeStage);
		exercise = baseService.merge(exercise);

		// Create a frozen revision of the exercise
		final int revisionNumber = exerciseBusiness.getRevisionNumbersFor(exercise).get(1);
		exerciseBusiness.createFrozenExercise(exercise, revisionNumber);
		FrozenExercise frozenExercise = exerciseBusiness.getFrozenRevisionsForExercise(exercise).get(0);
		frozenExercise = exerciseBusiness.getFrozenExerciseWithLazyDataById(frozenExercise.getId());

		// Check the transitions - we use the internal name for comparing
		stages = frozenExercise.getStagesAsList();

		// linear transitions (0 -> 1 -> 2 -> 3)
		assertStageHasDefaultTransition(stages.get(0), stages.get(1));
		assertStageHasDefaultTransition(stages.get(1), stages.get(2));
		assertStageHasDefaultTransition(stages.get(2), stages.get(3));

		// Other transitions (see above)
		assertStageHasTransition(stages.get(0), stages.get(2));
		assertStageHasTransition(stages.get(0), stages.get(3));
		assertStageHasSkipTransition(stages.get(1), stages.get(0));
		assertStageHasTransition(stages.get(1), stages.get(1));
		assertStageHasTransition(stages.get(2), new RepeatStage());

	}

	/**
	 * Asserts that one of the stage transitions leads to a specific target stage.
	 */
	private void assertStageHasTransition(Stage stage, Stage target) {
		List<StageTransition> transitions = stage.getStageTransitions();

		if (target instanceof RepeatStage) {
			assertTrue(transitions.stream().anyMatch(t -> t.getTarget() instanceof RepeatStage),
					String.format("The stage %s does not lead to a repeat stage.", stage.getInternalName()));

		} else {
			// Match the name of the stage
			String targetName = target.getInternalName();
			assertTrue(transitions.stream().anyMatch(t -> t.getTarget().getInternalName().equals(targetName)),
					String.format("The stage %s does not lead to the target stage %s.", stage.getInternalName(),
							target.getInternalName()));
		}
	}

	/**
	 * Asserts that one of the skip transitions leads to a specific target stage.
	 */
	private void assertStageHasSkipTransition(Stage stage, Stage target) {
		List<StageTransition> transitions = stage.getSkipTransitions();

		if (target instanceof RepeatStage) {
			assertTrue(transitions.stream().anyMatch(t -> t.getTarget() instanceof RepeatStage), String
					.format("The stage %s does not lead to a repeat stage if skipping.", stage.getInternalName()));

		} else {
			// Match the name of the stage
			String targetName = target.getInternalName();
			assertTrue(transitions.stream().anyMatch(t -> t.getTarget().getInternalName().equals(targetName)),
					String.format("The stage %s does not lead to the target stage %s if skipping.",
							stage.getInternalName(), target.getInternalName()));
		}
	}

	/**
	 * Assert that a stage leads to a target stage within a specific transition.
	 */
	private void assertStageHasDefaultTransition(Stage stage, Stage target) {
		StageTransition transition = stage.getDefaultTransition();

		if (target instanceof RepeatStage) {
			assertTrue(transition.getTarget() instanceof RepeatStage);

		} else {
			// Match the name of the stage
			String targetName = target.getInternalName();
			assertEquals(targetName, transition.getTarget().getInternalName());
		}
	}

}
