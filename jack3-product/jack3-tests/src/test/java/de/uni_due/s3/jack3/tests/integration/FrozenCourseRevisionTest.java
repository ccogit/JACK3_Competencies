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

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.base.Defaults;
import com.google.common.reflect.TypeToken;

import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.enums.ECourseScoring;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.Comment;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.FrozenCourse;
import de.uni_due.s3.jack3.entities.tenant.FrozenExercise;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.exceptions.JackRuntimeException;
import de.uni_due.s3.jack3.tests.annotations.NeedsCourse;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

@NeedsCourse
class FrozenCourseRevisionTest extends AbstractContentTest {

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	private User secondUser;

	private static final int FROZEN_REVISON_WITH_CONTENTPROVIDER = 1;

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();
		secondUser = TestDataFactory.getUser("secondUser");
		folderBusiness.updateFolderRightsForUser(folder, user, AccessRight.getFull());
		baseService.persist(secondUser);
	}

	@Test
	void testFrozenCourse() throws ActionNotAllowedException {
		// creating a complex Course
		Course courseNotFrozen = (Course) course;
		courseNotFrozen.setName("Worky, Worky");
		courseNotFrozen.setInternalDescription("Zug-zug");
		courseNotFrozen.setScoringMode(ECourseScoring.BEST);
		courseNotFrozen.setValid(true);
		assertFalse(courseNotFrozen.isFrozen());

		// Adding exercises to course and persist
		FixedListExerciseProvider fixedListExerciseProvider = new FixedListExerciseProvider();

		Exercise exercise1 = exerciseBusiness.createExercise("Arbeit, Arbeit!", user, folder, "de_DE");
		FrozenExercise exercise1_frozen = new FrozenExercise(exercise1);

		Exercise exercise2 = exerciseBusiness.createExercise("Something need doing?", user, folder, "de_DE");
		FrozenExercise exercise2_frozen = new FrozenExercise(exercise2);

		fixedListExerciseProvider.addCourseEntry(new CourseEntry(exercise1, exercise1_frozen, 100));
		fixedListExerciseProvider.addCourseEntry(new CourseEntry(exercise2, exercise2_frozen, 50));

		courseNotFrozen.setContentProvider(fixedListExerciseProvider);
		courseNotFrozen = (Course) courseBusiness.updateCourse(courseNotFrozen);

		// We create a frozen version of this course
		List<FrozenCourse> frozenRevisions = courseBusiness.getFrozenRevisionsForCourse(courseNotFrozen);
		assertTrue(frozenRevisions.isEmpty());

		List<Integer> revisions = courseBusiness.getRevisionNumbersFor(courseNotFrozen);

		courseBusiness.createFrozenCourse(courseNotFrozen, revisions.get(FROZEN_REVISON_WITH_CONTENTPROVIDER));
		frozenRevisions = courseBusiness.getFrozenRevisionsForCourse(courseNotFrozen);
		assertEquals(1, frozenRevisions.size());
		FrozenCourse frozenCourse = courseBusiness.getFrozenCourseByProxiedIdsWithLazyData(frozenRevisions.get(0).getRealCourseId(),
				frozenRevisions.get(0).getProxiedCourseRevisionId());

		// We change and persist the original course
		fixedListExerciseProvider.removeCourseEntry(fixedListExerciseProvider.getCourseEntries().get(0));

		Exercise exercise3 = exerciseBusiness.createExercise("Dabu", user, folder, "de_DE");
		FrozenExercise exercise3_frozen = new FrozenExercise(exercise3);

		Exercise exercise4 = exerciseBusiness.createExercise("Lok-Tar!", user, folder, "de_DE");
		FrozenExercise exercise4_frozen = new FrozenExercise(exercise4);

		fixedListExerciseProvider.addCourseEntry(new CourseEntry(exercise3, exercise3_frozen, 25));
		fixedListExerciseProvider.addCourseEntry(new CourseEntry(exercise4, exercise4_frozen, 75));

		courseNotFrozen.setName("my Name");
		courseNotFrozen.setScoringMode(ECourseScoring.LAST);
		courseNotFrozen.setInternalDescription("Aka'magosh");
		courseNotFrozen.setValid(false);
		courseNotFrozen = (Course) courseBusiness.updateCourse(courseNotFrozen);

		// Ensure setters aren't useable on a frozen course
		testThatFrozenCourseCantChange(frozenCourse);

		// Create a CourseRecord, wich references the FrozenCourse
		PresentationFolder presentationFolder = folderBusiness.createPresentationFolder("Kurse, heiß und fettig!",
				folderBusiness.getPresentationRoot());
		CourseOffer courseOffer = courseBusiness.createCourseOffer("Bester Kurs, EUW!", frozenCourse,
				presentationFolder, secondUser);
		CourseRecord courseRecord = new CourseRecord(secondUser, courseOffer, frozenCourse);
		baseService.persist(courseRecord);
		Comment comment = new Comment(user, "LOL xD", false);
		courseRecord.addStudentComment(comment);

		AbstractCourse courseFromCR = courseRecord.getCourse();
		getLogger().debug(courseFromCR.getContentProvider().getCourseEntries());
		getLogger().debug(frozenCourse.getContentProvider().getCourseEntries());
		getLogger().debug(courseNotFrozen.getContentProvider().getCourseEntries());

		// Ensure FrozenCourse is actually the old version
		assertTrue(courseFromCR.isFrozen());

		assertEquals(2, courseFromCR.getContentProvider().getCourseEntries().size());
		AbstractExercise firstExerciseFromCourseRecord = courseFromCR.getContentProvider().getCourseEntries().get(0)
				.getExercise();
		assertEquals(exercise1, Hibernate.unproxy(firstExerciseFromCourseRecord));

		AbstractExercise secondExerciseFromCourseRecord = courseFromCR.getContentProvider().getCourseEntries().get(1)
				.getExercise();
		assertEquals(exercise2, Hibernate.unproxy(secondExerciseFromCourseRecord));

		assertEquals("Worky, Worky", courseFromCR.getName());
		assertEquals("Zug-zug", courseFromCR.getInternalDescription());
		assertEquals(ECourseScoring.BEST, courseFromCR.getScoringMode());
		assertTrue(courseFromCR.isValid());
	}

	private void testThatFrozenCourseCantChange(FrozenCourse frozenCourse) {
		// First we get all Methods starting with "set", "add" or "remove" of a FrozenCourse using reflection
		Method[] allMethods = FrozenCourse.class.getMethods();
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
						|| "setId".equals(method.getName()) //
						|| "setRevisionId".equals(method.getName()) //
						|| "setRevisionAuthor".equals(method.getName()) //
						|| "setUpdateTimeStampToNow".equals(method.getName()) //
				) {
					continue;
				}

				// Now we try to invoke the the method with our default params and we expect to get a
				// UnsupportedOperationException for each invocation
				method.invoke(frozenCourse, args);
				fail("The following setter did not throw an UnsupportedOperationException: " + method);
			} catch (InvocationTargetException e) {
				// Calling this method by reflection, our exception gets wrapped in an InvocationTargetException
				assertTrue(e.getCause() instanceof UnsupportedOperationException);
				assertEquals("Must not change state of frozen objects!", e.getCause().getMessage());
			} catch (IllegalAccessException | IllegalArgumentException e) {
				throw new JackRuntimeException(e);
			}
		}
	}
}
