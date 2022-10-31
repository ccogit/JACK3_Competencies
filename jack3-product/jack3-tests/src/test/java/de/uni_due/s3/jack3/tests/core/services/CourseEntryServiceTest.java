package de.uni_due.s3.jack3.tests.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;
import de.uni_due.s3.jack3.tests.annotations.NeedsCourse;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

@NeedsExercise
@NeedsCourse
// TODO Dieser Test kann ggf. weggeschmissen werden, weil es keinen CourseEntryService mehr gibt.
class CourseEntryServiceTest extends AbstractContentTest {
	/**
	 * Persist and get one course entry
	 */
	@Test
	void getCourseEntry() {
		Course c = new Course("cours");
		c.setContentProvider(new FixedListExerciseProvider());
		CourseEntry entry = new CourseEntry(exercise, 100);
		entry.getExercise().setFolder(folder);
		((FixedListExerciseProvider) c.getContentProvider()).addCourseEntry(entry);
		baseService.persist(c);

		// Entry should be found
		assertEquals(entry, querySingleResult("FROM CourseEntry", CourseEntry.class).get());
	}

	/**
	 * Persist and get multiple course entries
	 */
	@Test
	void getCourseEntries() {
		exercise.setFolder(folder);
		Course c = new Course("cours");
		c.setContentProvider(new FixedListExerciseProvider());
		((FixedListExerciseProvider) c.getContentProvider()).addCourseEntry(new CourseEntry(exercise, 100));
		((FixedListExerciseProvider) c.getContentProvider()).addCourseEntry(new CourseEntry(exercise, 100));

		baseService.persist(c);

		// all entries should be found
		Collection<CourseEntry> courseEntries = queryResultList("FROM CourseEntry", CourseEntry.class);
		assertEquals(2, courseEntries.size());
		assertTrue(courseEntries.containsAll(((FixedListExerciseProvider) c.getContentProvider()).getCourseEntries()));
	}

}
