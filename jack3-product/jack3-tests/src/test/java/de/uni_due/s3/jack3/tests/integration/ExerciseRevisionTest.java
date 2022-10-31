package de.uni_due.s3.jack3.tests.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import javax.ejb.EJBException;
import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.builders.ExerciseBuilder;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression.EDomain;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.DevelopmentService;
import de.uni_due.s3.jack3.services.DevelopmentService.EDatabaseType;
import de.uni_due.s3.jack3.services.ExerciseService;
import de.uni_due.s3.jack3.services.RevisionService;
import de.uni_due.s3.jack3.tests.utils.AbstractTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

/**
 * Tests exercise revisions
 *
 * @author lukas.glaser
 *
 */
class ExerciseRevisionTest extends AbstractTest {

	private User user = TestDataFactory.getUser("User");
	private ContentFolder folder = TestDataFactory.getContentFolder("Folder", null);

	@Inject
	private ExerciseService exerciseService;

	@Inject
	private DevelopmentService devService;

	@Inject
	private RevisionService revisionService;

    private void initializeExercise(AbstractExercise exercise) {
		// We explicitly don't call "getAdmin()" because we don't want to have the automatic personal folder.
		baseService.persist(user);
		baseService.persist(folder);

		folder.addChildExercise(exercise);

		exerciseService.persistExercise(exercise);
		folder = baseService.merge(folder);

	}

	@AfterEach
	void clearDatabase() {
		devService.deleteTenantDatabase(EDatabaseType.H2);
	}

	private Exercise buildBasicExercise() {
		return new ExerciseBuilder("Exercise").withPublicDescription("Description").withDifficulty(1)
				.withVariableDeclaration("var1", "getRandomFromList(list(1,2,3))", EDomain.MATH).create();
	}

	private Exercise buildExercise(String name) {
		return new ExerciseBuilder(name).create();
	}

	/**
	 * Get revision of exercise with lazy data
	 */
	@Test
	void getRevisionWithLazyData() {

		// persist exercise
		AbstractExercise exercise = buildExercise("Exercise 1.0");
		initializeExercise(exercise);
		long exerciseId = exercise.getId();

		// give a new name and merge to test revision
		exercise.setName("Exercise 2.0");
		exercise = exerciseService.mergeExercise(exercise);

		List<Integer> revisions = revisionService.getRevisionNumbersFor(exercise);
		// get revision with lazy data
		exercise = exerciseService.getRevisionOfExerciseWithLazyData(exerciseId, revisions.get(FIRST_REVISION))
				.orElseThrow(AssertionError::new);

		// test new revision
		assertEquals("Exercise 1.0", exercise.getName());

		// the getProxiedOrLastPersistedRevisionId returns the newest persisted version for this exercise which is
		// different from the first revision

		try {
			revisionService.getProxiedOrLastPersistedRevisionId(exercise);
			fail("We should not get to here!");
		} catch (EJBException e) {
			assertTrue(e.getCause() instanceof IllegalArgumentException);
			assertTrue(e.getCause().getMessage().startsWith(
					"Getting the latest revision of an Exercise from the audit table is not well defined!"));
		}

		// test if all lazy collections were fetched
		assertTrue(exercise.getStages().isEmpty());
		assertTrue(exercise.getTags().isEmpty());
		assertTrue(exercise.getExerciseResources().isEmpty());
		assertTrue(exercise.getVariableDeclarations().isEmpty());
		assertTrue(exercise.getResultFeedbackMappings().isEmpty());
	}

	/**
	 * Tests revision of an Exercise with changing attributes.
	 */
	@Test // TODO aufspalten auf mehrere Tests
	void testBasicRevision() {
        AbstractExercise exercise = buildBasicExercise();
		initializeExercise(exercise);

		/*-
		 * Revision 0:
		 * Name				Exercise
		 * Description		Description
		 * Difficulty		1
		 * var name			var1
		 * var code			getRandomFromList(list(1,2,3))
		 * var domain		MATH
		 */

		exercise.setName("Exercise new");
		exercise.setPublicDescription("Description new");
		exercise.setDifficulty(2);
		exercise.getVariableDeclarations().stream().findFirst().ifPresent(varD -> {
			varD.setName("var2");
			varD.getInitializationCode().setCode("getRandomFromList(list(4,5,6))");
			varD.getInitializationCode().setDomain(EDomain.CHEM);
		});
		exercise = exerciseService.mergeExercise(exercise);

		/*-
		 * Revision 1:
		 * Name				Exercise new
		 * Description		Description new
		 * Difficulty		2
		 * var name			var2
		 * var code			getRandomFromList(list(4,5,6))
		 * var domain		CHEM
		 */

		// Check newest revision
		assertTrue(exercise instanceof Exercise);
		assertEquals("Exercise new", exercise.getName());
		assertEquals("Description new", exercise.getPublicDescription());
		assertEquals(2, exercise.getDifficulty());
		assertFalse(exercise.getVariableDeclarations().isEmpty());
		assertEquals("var2", exercise.getVariableDeclarations().get(0).getName());
		assertEquals("getRandomFromList(list(4,5,6))",
				exercise.getVariableDeclarations().get(0).getInitializationCode().getCode());
		assertEquals(EDomain.CHEM, exercise.getVariableDeclarations().get(0).getInitializationCode().getDomain());

		// Check revisions and revision numbers from Envers
		List<AbstractExercise> revisions = revisionService.getAllRevisionsForEntityWithLazyData(exercise);
		assertEquals(2, revisions.size());

		List<Integer> revisionNumbers = revisionService.getRevisionNumbersFor(exercise);
		assertEquals(2, revisionNumbers.size());

		// Check first ("original") revision via "getAllRevisionsForExercise"
		exercise = revisions.get(0);
		assertTrue(exercise instanceof Exercise);
		assertEquals("Exercise", exercise.getName());
		assertEquals("Description", exercise.getPublicDescription());
		assertEquals(1, exercise.getDifficulty());
		assertFalse(exercise.getVariableDeclarations().isEmpty());
		assertEquals("var1", exercise.getVariableDeclarations().get(0).getName());
		assertEquals("getRandomFromList(list(1,2,3))",
				exercise.getVariableDeclarations().get(0).getInitializationCode().getCode());
		assertEquals(EDomain.MATH, exercise.getVariableDeclarations().get(0).getInitializationCode().getDomain());

		// Check first ("original revision") via "getRevisionOfExerciseWithLazyData"
		exercise = exerciseService.getRevisionOfExerciseWithLazyData(exercise, revisionNumbers.get(0)).get();
		assertTrue(exercise instanceof Exercise);
		assertEquals("Exercise", exercise.getName());
		assertEquals("Description", exercise.getPublicDescription());
		assertEquals(1, exercise.getDifficulty());
		assertFalse(exercise.getVariableDeclarations().isEmpty());
		assertEquals("var1", exercise.getVariableDeclarations().get(0).getName());
		assertEquals("getRandomFromList(list(1,2,3))",
				exercise.getVariableDeclarations().get(0).getInitializationCode().getCode());
		assertEquals(EDomain.MATH, exercise.getVariableDeclarations().get(0).getInitializationCode().getDomain());

		// Check first ("original revision") via "getRevisionOfExercise"
		exercise = exerciseService.getRevisionOfExercise(exercise, revisionNumbers.get(0)).get();
		assertTrue(exercise instanceof Exercise);
		assertEquals("Exercise", exercise.getName());
		assertEquals("Description", exercise.getPublicDescription());
		assertEquals(1, exercise.getDifficulty());

		// Check second ("new") revision via "getAllRevisionsForExercise"
		exercise = revisions.get(1);
		assertTrue(exercise instanceof Exercise);
		assertEquals("Exercise new", exercise.getName());
		assertEquals("Description new", exercise.getPublicDescription());
		assertEquals(2, exercise.getDifficulty());
		assertFalse(exercise.getVariableDeclarations().isEmpty());
		assertEquals("var2", exercise.getVariableDeclarations().get(0).getName());
		assertEquals("getRandomFromList(list(4,5,6))",
				exercise.getVariableDeclarations().get(0).getInitializationCode().getCode());
		assertEquals(EDomain.CHEM, exercise.getVariableDeclarations().get(0).getInitializationCode().getDomain());

		// Check second ("new") revision via "getRevisionOfExercise"
		exercise = exerciseService.getRevisionOfExercise(exercise, revisionNumbers.get(1)).get();
		assertTrue(exercise instanceof Exercise);
		assertEquals("Exercise new", exercise.getName());
		assertEquals("Description new", exercise.getPublicDescription());
		assertEquals(2, exercise.getDifficulty());

		// Check second ("new") revision via "getRevisionOfExerciseWithLazyData"
		exercise = exerciseService.getRevisionOfExerciseWithLazyData(exercise, revisionNumbers.get(1)).get();
		assertTrue(exercise instanceof Exercise);
		assertEquals("Exercise new", exercise.getName());
		assertEquals("Description new", exercise.getPublicDescription());
		assertEquals(2, exercise.getDifficulty());
		assertFalse(exercise.getVariableDeclarations().isEmpty());
		assertEquals("var2", exercise.getVariableDeclarations().get(0).getName());
		assertEquals("getRandomFromList(list(4,5,6))",
				exercise.getVariableDeclarations().get(0).getInitializationCode().getCode());
		assertEquals(EDomain.CHEM, exercise.getVariableDeclarations().get(0).getInitializationCode().getDomain());

		// Reset the revision, lookup revision id
		exercise = exerciseService.resetToRevision(exercise, revisionNumbers.get(0));

		assertEquals("Exercise", exercise.getName());
		assertEquals("Description", exercise.getPublicDescription());
		assertEquals(1, exercise.getDifficulty());
		assertFalse(exercise.getVariableDeclarations().isEmpty());
		assertEquals("var1", exercise.getVariableDeclarations().get(0).getName());
		assertEquals("getRandomFromList(list(1,2,3))",
				exercise.getVariableDeclarations().get(0).getInitializationCode().getCode());
		assertEquals(EDomain.MATH, exercise.getVariableDeclarations().get(0).getInitializationCode().getDomain());
	}

}
