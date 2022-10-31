package de.uni_due.s3.jack3.tests.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.services.ExerciseProviderService;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

/**
 * Test class for AbstractExerciseProvider.
 *
 * It manages the CourseEntries (= Exercises in the course) for a course.
 */
class AbstractExerciseProviderServiceTest extends AbstractContentTest {

	@Inject
	private ExerciseProviderService providerService;

	@Test
	void testFixedListExerciseProvider() {
		FixedListExerciseProvider provider = new FixedListExerciseProvider();
		baseService.persist(provider);

		// The provider should be found after persisting it
		assertNotEquals(0, provider.getId());
		assertEquals(provider, providerService.getFixedListExerciseProviderByID(provider.getId())
														.orElseThrow(AssertionError::new));

		// The provider should be successfully deleted.
		long id = provider.getId();
		baseService.deleteEntity(provider);
		assertFalse(providerService.getFixedListExerciseProviderByID(id).isPresent());
	}

	@Test
	void testFolderExerciseProvider() {
		FolderExerciseProvider provider = new FolderExerciseProvider();
		baseService.persist(provider);

		// The provider should be found after persisting it
		assertNotEquals(0, provider.getId());
		assertEquals(provider, providerService.getFolderExerciseProviderByID(provider.getId())
														.orElseThrow(AssertionError::new));

		// The provider should be successfully deleted.
		long id = provider.getId();
		baseService.deleteEntity(provider);
		assertFalse(providerService.getFolderExerciseProviderByID(id).isPresent());
	}

	// TODO Other exercise providers are not implemented yet.

	@Test
	void getNotExistingProviders() {
		assertFalse(providerService.getFolderExerciseProviderByID(0).isPresent());
		assertFalse(providerService.getFixedListExerciseProviderByID(0).isPresent());
	}

}
