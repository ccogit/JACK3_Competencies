package de.uni_due.s3.jack3.tests.core.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.services.ExerciseProviderService;
import de.uni_due.s3.jack3.tests.annotations.NeedsCourse;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

@NeedsExercise
@NeedsCourse
class FolderExerciseProviderTest extends AbstractContentTest {

	@Inject
	private ExerciseProviderService providerService;

	private FolderExerciseProvider provider = new FolderExerciseProvider();

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();
		course.setContentProvider(provider);
		course = baseService.merge(course);
		provider = providerService	.getFolderExerciseProviderByID(course.getContentProvider()
																		.getId())
									.orElseThrow(AssertionError::new);
	}

	@Test
	void getEmptyFolderList() {
		assertTrue(provider.getFolders().isEmpty());
	}

	@Test
	void addFolderEntry() {
		assertTrue(provider.getFolders().isEmpty());
		provider.addFolder(folder);
		assertEquals(1, provider.getFolders()
								.size());
	}

	@Test
	void removeCourseEntry() {
		assertTrue(provider.getFolders().isEmpty());
		provider.addFolder(folder);
		assertEquals(1, provider.getFolders()
								.size());

		provider.removeFolder(folder);
		assertTrue(provider.getFolders().isEmpty());
	}
}
