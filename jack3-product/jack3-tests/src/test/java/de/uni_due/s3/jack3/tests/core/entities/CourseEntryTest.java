package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.tests.annotations.NeedsCourse;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

@NeedsExercise
@NeedsCourse
class CourseEntryTest extends AbstractContentTest {

	private CourseEntry entry;
	private FixedListExerciseProvider provider = new FixedListExerciseProvider();

	@Override
	@BeforeEach
	protected void doBeforeTest() {
		super.doBeforeTest();
		// Create CourseEntry after Exercise was persisted
		entry = new CourseEntry(exercise, 100);

		course.setContentProvider(provider);
		provider.addCourseEntry(entry);
		course = baseService.merge(course);

		provider = (FixedListExerciseProvider) course.getContentProvider();
		entry = course.getContentProvider().getCourseEntries().get(0);
	}

	@Test
	void changePoints() {
		assertEquals(100, entry.getPoints());

		entry.setPoints(50);
		course = baseService.merge(course);
		entry = course.getContentProvider().getCourseEntries().get(0);

		assertEquals(50, entry.getPoints());
	}

	@Test
	void changeExercise() {
		assertEquals(exercise, entry.getExercise());

		Exercise newExercise = new Exercise("New Exercise", TestDataFactory.getDefaultLanguage());
		folder.addChildExercise(newExercise);
		baseService.persist(newExercise);
		folderService.mergeContentFolder(folder);

		entry.setExercise(newExercise);
		course = baseService.merge(course);
		entry = course.getContentProvider().getCourseEntries().get(0);

		assertNotEquals(exercise, entry.getExercise());
		assertEquals(newExercise, entry.getExercise());
	}

}
