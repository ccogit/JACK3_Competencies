package de.uni_due.s3.jack3.tests.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.builders.ExerciseBuilder;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;

/**
 * Tests deleting the start stage of an exercise. See JACK/jack3-core#205
 * 
 * @author kilian.kraus
 *
 */
class DeleteStartstageTest extends AbstractBasicTest {

	private Exercise exercise;

	@Override
	@BeforeEach
	protected void doBeforeTest() {
		super.doBeforeTest();
		persistUser();
		persistFolder();

		exercise = new ExerciseBuilder("exercise")//
				.withMCStage()//
				.withTitle("A1")//
				.and()//
				.withMCStage()//
				.withTitle("A2")//
				.and()//
				.withMCStage()//
				.withTitle("A3")//
				.and()//
				.create();
		folder.addChildExercise(exercise);
		folderService.mergeContentFolder(folder);
		baseService.persist(exercise);
	}

	@Test
	void deletingStartStage() {
		// Exercise has 3 Stages and the first one (with the name A1) is the startStage
		assertEquals(3, exercise.getStages().size());
		assertEquals("A1", exercise.getStartStage().getExternalName());

		// remove Stage A1
		exercise.removeStage(exercise.getStartStage());
		exercise = baseService.merge(exercise);

		// The exercise should have only 2 Stage left and the Stage A2 should now be the startStage
		assertEquals(2, exercise.getStages().size());
		assertEquals("A2", exercise.getStartStage().getExternalName());

		// deleting another Stage should not change the startStage:
		// remove Stage A3
		exercise.removeStage(exercise.getStartStage().getDefaultTransition().getTarget());
		exercise.generateSuffixWeights();
		exercise = baseService.merge(exercise);

		// The exercise should have only 1 Stage left and the Stage A2 should now be the startStage
		assertEquals(1, exercise.getStages().size());
		assertEquals("A2", exercise.getStartStage().getExternalName());
	}

	@Test
	void afterDeletingStageCheckDatabase() {
		// Exercise has 3 Stages and the first one (with the name A1) is the startStage
		assertEquals(3, exercise.getStages().size());
		assertEquals("A1", exercise.getStartStage().getExternalName());

		// All 3 Stages should be in the Database
		assertTrue(querySingleResult("FROM MCStage WHERE externalname = 'A1'", MCStage.class).isPresent());
		assertTrue(querySingleResult("FROM MCStage WHERE externalname = 'A2'", MCStage.class).isPresent());
		assertTrue(querySingleResult("FROM MCStage WHERE externalname = 'A3'", MCStage.class).isPresent());

		// remove Stage A2 and change the transitions so that Stage 1 doesn't have a next Stage anymore, because stage 2
		// is deleted
		exercise.removeStage(exercise.getStartStage().getDefaultTransition().getTarget());
		exercise.getStartStage().getDefaultTransition().setTarget(null);

		exercise = baseService.merge(exercise);

		assertEquals(2, exercise.getStages().size());

		// The Stage shouldn't be in the Database anymore
		assertFalse(querySingleResult("FROM MCStage WHERE externalname = 'A2'", MCStage.class).isPresent());

		// remove now also the other stages:
		// remove Stage A1
		exercise.removeStage(exercise.getStartStage());
		assertEquals(1, exercise.getStages().size());
		exercise.generateSuffixWeights();
		exercise = baseService.merge(exercise);

		assertEquals(1, exercise.getStages().size());

		// The Stage shouldn't be in the Database anymore
		assertFalse(querySingleResult("FROM MCStage WHERE externalname = 'A1'", MCStage.class).isPresent());

		// remove Stage A3
		exercise.removeStage(exercise.getStartStage());
		exercise = baseService.merge(exercise);

		assertEquals(0, exercise.getStages().size());

		// The Stage shouldn't be in the Database anymore
		assertFalse(querySingleResult("FROM MCStage WHERE externalname = 'A3'", MCStage.class).isPresent());
	}
}
