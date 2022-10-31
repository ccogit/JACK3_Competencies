package de.uni_due.s3.jack3.tests.business.samples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.builders.CourseBuilder;
import de.uni_due.s3.jack3.entities.enums.ECourseExercisesOrder;
import de.uni_due.s3.jack3.entities.enums.ECourseScoring;
import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;

/**
 * Test for the CourseBuilder.
 * 
 * @author marc.kasper
 *
 */
class CourseBuilderTest extends AbstractBasicTest {

	/**
	 * This test checks to build a new course.
	 */
	@Test
	void buildNewCourse() {
		CourseBuilder courseBuilder;
		Course course;

		courseBuilder = new CourseBuilder("test for building new course");
		course = courseBuilder.build();

		assertNotNull(course, "The course doesn't exists.");
		assertEquals("test for building new course", course.getName(), "The course name is different.");
	}

	/**
	 * This test checks to build a new course based on an existing course.
	 */
	@Test
	void buildCourseWithExistingCourse() {
		CourseBuilder courseBuilder;
		Course sourceCourse;
		Course creatingCourse;

		sourceCourse = new Course("test for course with existing course");
		courseBuilder = new CourseBuilder(sourceCourse);
		creatingCourse = courseBuilder.build();

		assertNotNull(creatingCourse, "The course doesn't exists.");
		assertEquals("test for course with existing course", creatingCourse.getName(), "The course name is different.");
	}

	/**
	 * This test checks to build a new course with an internal description.
	 */
	@Test
	void buildNewCourseWithInternalDescription() {
		CourseBuilder courseBuilder;
		Course course;

		courseBuilder = new CourseBuilder("test for building new course")
						.withInternalDescription("internal course description");
		course = courseBuilder.build();

		assertNotNull(course, "The course doesn't exists.");
		assertEquals("test for building new course", course.getName(), "The course name is different.");
		assertEquals("internal course description", course.getInternalDescription(),
				"The internal course description is different.");
	}

	/**
	 * This test checks to build a new course with an internal description.
	 */
	@Test
	void buildNewCourseWithExternalDescription() {
		CourseBuilder courseBuilder;
		Course course;

		courseBuilder = new CourseBuilder("test for building new course")
						.withExternalDescription("external course description");
		course = courseBuilder.build();

		assertNotNull(course, "The course doesn't exists.");
		assertEquals("test for building new course", course.getName(), "The course name is different.");
		assertEquals("external course description", course.getExternalDescription(),
				"The external course description is different.");
	}

	/**
	 * This test checks to build a new course with a set exercise order.
	 */
	@Test
	void buildNewCourseWithExerciseOrder() {
		CourseBuilder courseBuilder;
		Course course;
		ECourseExercisesOrder exerciseOrder = ECourseExercisesOrder.MANUAL;

		courseBuilder = new CourseBuilder("test for building new course")
						.withExerciseOrder(exerciseOrder);
		course = courseBuilder.build();

		assertNotNull(course, "The course doesn't exists.");
		assertEquals("test for building new course", course.getName(), "The course name is different.");
		assertEquals(ECourseExercisesOrder.MANUAL, course.getExerciseOrder(),
				"The exercise order of course is different.");
	}

	/**
	 * This test checks to build a new course with a set scoring mode.
	 */
	@Test
	void buildNewCourseWithScoringMode() {
		CourseBuilder courseBuilder;
		Course course;
		ECourseScoring courseScoring = ECourseScoring.BEST;

		courseBuilder = new CourseBuilder("test for building new course")
						.withScoringMode(courseScoring);
		course = courseBuilder.build();

		assertNotNull(course, "The course doesn't exists.");
		assertEquals("test for building new course", course.getName(), "The course name is different.");
		assertEquals(ECourseScoring.BEST, course.getScoringMode(), "The scoring mode of course is different.");
	}

	/**
	 * This test checks to build a new course with a set folder
	 * exercise provider.
	 */
	@Test
	void buildNewCourseWithFolderExerciseProvider() {
		CourseBuilder courseBuilder;
		Course course;
		List<ContentFolder> contentFolders = new ArrayList<>();
		ContentFolder contentFolder = new ContentFolder("content folder for build course test");
		contentFolders.add(contentFolder);

		courseBuilder = new CourseBuilder("test for building new course")
						.withFolderExerciseProvider(contentFolders);
		course = courseBuilder.build();

		assertNotNull(course, "The course doesn't exists.");
		assertEquals("test for building new course", course.getName(), "The course name is different.");
		assertNotNull(course.getContentProvider(),
				"The content provider for folder exercise provider is not set.");
		assertNotNull(((FolderExerciseProvider) course.getContentProvider()).getFolders(),
				"The folder list in the folder exercise provider is not set.");
		assertFalse(((FolderExerciseProvider) course.getContentProvider()).getFolders().isEmpty(),
				"The folder list in the folder exercise provider is empty.");
		assertEquals(1, ((FolderExerciseProvider) course.getContentProvider()).getFolders().size(),
				"The size of folder list in the folder exercise provider is not equalt to 1.");
		assertEquals("content folder for build course test",
				((FolderExerciseProvider) course.getContentProvider()).getFolders().get(0).getName(),
				"The exercise name in the folder exercise provider is different.");
	}

	/**
	 * This test checks to build a new course with a fixed
	 * list exercise.
	 */
	@Test
	void buildNewCourseWithFixedListExerciseProvider() {
		CourseBuilder courseBuilder;
		Course course;
		List<CourseEntry> courseEntries = new ArrayList<>();
		Exercise exercise = new Exercise("Exercise for fixed list", "DE");
		courseEntries.add(new CourseEntry(exercise, 5));

		courseBuilder = new CourseBuilder("test for building new course")
						.withFixedListExerciseProvider(courseEntries);
		course = courseBuilder.build();

		assertNotNull(course, "The course doesn't exists.");
		assertEquals("test for building new course", course.getName(), "The course name is different.");
		assertNotNull(course.getContentProvider(), "The content provider for fixed list content provider is not set.");
		assertNotNull(course.getContentProvider().getCourseEntries(),
				"The entry list in fixed list content provider is not set.");
		assertFalse(course.getContentProvider().getCourseEntries().isEmpty(),
				"The entry list in the fixed list content provider is empty.");
		assertEquals(1, course.getContentProvider().getCourseEntries().size(),
				"The size of entry list in the fixed list content provider is not equalt to 1.");
		assertEquals("Exercise for fixed list",
				course.getContentProvider().getCourseEntries().get(0).getExercise().getName(),
				"The exercise name in the fixed list content provider is different.");
	}
}