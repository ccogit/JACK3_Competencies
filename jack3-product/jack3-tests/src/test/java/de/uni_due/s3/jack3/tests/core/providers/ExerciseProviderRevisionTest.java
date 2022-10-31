package de.uni_due.s3.jack3.tests.core.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import de.uni_due.s3.jack3.entities.providers.AbstractExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.CourseService;
import de.uni_due.s3.jack3.services.DevelopmentService;
import de.uni_due.s3.jack3.services.DevelopmentService.EDatabaseType;
import de.uni_due.s3.jack3.services.RevisionService;
import de.uni_due.s3.jack3.tests.utils.AbstractTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

/**
 * Tests if courses revisions save exercise providers correctly, see jack3-core#168
 *
 * @author lukas.glaser
 *
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExerciseProviderRevisionTest extends AbstractTest {

	@Inject
	private CourseService courseService;

	@Inject
	private DevelopmentService devService;

	@Inject
	private RevisionService revisionService;

	private static User user = TestDataFactory.getUser("User");
	private static ContentFolder folder = TestDataFactory.getContentFolder("Folder", null);
	private static AbstractCourse course = new Course("Course");
	private static Exercise exercise = new Exercise("Exercise", TestDataFactory.getDefaultLanguage());

	private void initialize() {
		baseService.persist(user);
		baseService.persist(folder);

		folder.addChildCourse(course);
		folder.addChildExercise(exercise);

		baseService.persist(course);
		baseService.persist(exercise);
		folder = baseService.merge(folder);
	}

	private void clean() {
		devService.deleteTenantDatabase(EDatabaseType.H2);
	}

	private <T extends AbstractExerciseProvider> T getProviderOfCourseRevision(int revision, Class<T> clazz) {
		AbstractCourse courseRevision = courseService	.getRevisionOfCourseWithLazyData(course, revision)
														.orElseThrow(AssertionError::new);
		if (courseRevision.getContentProvider() == null) {
			return null;
		} else {
			return clazz.cast(Hibernate.unproxy(courseRevision.getContentProvider()));
		}
	}

	/**
	 * The course was first created without a provider, if we save a new course revision with a provider (here:
	 * {@link FixedListExerciseProvider}), the original revision (lastPersistedRevisionId=0) should not have a provider.
	 */
	@Test
	@Order(0)
	void testRevision0() {
		initialize();

		FixedListExerciseProvider flep = new FixedListExerciseProvider();
		course.setContentProvider(flep);
		course = baseService.merge(course);

		List<Integer> revisions = revisionService.getRevisionNumbersFor(course);

		assertTrue(course.getContentProvider() instanceof FixedListExerciseProvider);
		AbstractCourse revision0 = courseService.getRevisionOfCourse(course, revisions.get(FIRST_REVISION))
												.orElseThrow(AssertionError::new);
		AbstractCourse revision1 = courseService.getRevisionOfCourse(course, revisions.get(SECOND_REVISION))
												.orElseThrow(AssertionError::new);

		assertNull(revision0.getContentProvider());
		assertNotNull(revision1.getContentProvider());
		// only check equality of IDs because revisonIDs should not be equal
		FixedListExerciseProvider revision1Provider = getProviderOfCourseRevision(revisions.get(SECOND_REVISION),
				FixedListExerciseProvider.class);
		assertEquals(course.getContentProvider().getId(), revision1Provider.getId());
		assertTrue(revision1Provider.getCourseEntries().isEmpty());
	}

	/**
	 * Change content of the created provider, check if the previous revision of this course stores the old state of the
	 * content provider
	 */
	@Test
	@Order(1)
	void testRevision1() {
		FixedListExerciseProvider flep = (FixedListExerciseProvider) course.getContentProvider();
		flep.addCourseEntry(new CourseEntry(exercise, 100));
		course = baseService.merge(course);

		List<Integer> revisions = revisionService.getRevisionNumbersFor(course);
		AbstractExerciseProvider revision0Provider = getProviderOfCourseRevision(revisions.get(FIRST_REVISION),
				FixedListExerciseProvider.class);
		FixedListExerciseProvider revision1Provider = getProviderOfCourseRevision(revisions.get(SECOND_REVISION),
				FixedListExerciseProvider.class);
		FixedListExerciseProvider revision2Provider = getProviderOfCourseRevision(revisions.get(THIRD_REVISION),
				FixedListExerciseProvider.class);

		assertNull(revision0Provider);
		assertNotNull(revision1Provider);
		assertNotNull(revision2Provider);

		assertEquals(course.getContentProvider().getId(), revision2Provider.getId());
		assertFalse(course.getContentProvider().getCourseEntries().isEmpty());

		assertTrue(revision1Provider.getCourseEntries().isEmpty());
		assertFalse(revision2Provider.getCourseEntries().isEmpty());
	}

	/**
	 * Change current provider to an other provider, check if previous revisions still store the other content providers
	 * at the specific revision.
	 */
	@Test
	@Order(2)
	void testRevision2() {
		FolderExerciseProvider fep = new FolderExerciseProvider();
		course.setContentProvider(fep);
		course = baseService.merge(course);

		List<Integer> revisions = revisionService.getRevisionNumbersFor(course);

		AbstractExerciseProvider revision0Provider = getProviderOfCourseRevision(revisions.get(FIRST_REVISION),
				FixedListExerciseProvider.class);
		FixedListExerciseProvider revision1Provider = getProviderOfCourseRevision(revisions.get(SECOND_REVISION),
				FixedListExerciseProvider.class);
		FixedListExerciseProvider revision2Provider = getProviderOfCourseRevision(revisions.get(THIRD_REVISION),
				FixedListExerciseProvider.class);
		FolderExerciseProvider revision3Provider = getProviderOfCourseRevision(revisions.get(FOURTH_REVISION),
				FolderExerciseProvider.class);

		assertNull(revision0Provider);
		assertNotNull(revision1Provider);
		assertNotNull(revision2Provider);
		assertNotNull(revision3Provider);

		assertEquals(course.getContentProvider().getId(), revision3Provider.getId());

		assertTrue(revision1Provider.getCourseEntries().isEmpty());
		assertFalse(revision2Provider.getCourseEntries().isEmpty());
		assertTrue(revision3Provider instanceof FolderExerciseProvider);
	}

	/**
	 * Remove content provider from current course revision and check if old revisions are kept.
	 */
	@Test
	@Order(3)
	void testRevision3() {
		course.setContentProvider(null);
		course = baseService.merge(course);

		List<Integer> revisions = revisionService.getRevisionNumbersFor(course);

		AbstractExerciseProvider revision0Provider = getProviderOfCourseRevision(revisions.get(FIRST_REVISION),
				FixedListExerciseProvider.class);
		FixedListExerciseProvider revision1Provider = getProviderOfCourseRevision(revisions.get(SECOND_REVISION),
				FixedListExerciseProvider.class);
		FixedListExerciseProvider revision2Provider = getProviderOfCourseRevision(revisions.get(THIRD_REVISION),
				FixedListExerciseProvider.class);
		FolderExerciseProvider revision3Provider = getProviderOfCourseRevision(revisions.get(FOURTH_REVISION),
				FolderExerciseProvider.class);
		AbstractExerciseProvider revision4Provider = getProviderOfCourseRevision(revisions.get(FIFTH_REVISION),
				FixedListExerciseProvider.class);

		assertNull(revision0Provider);
		assertNotNull(revision1Provider);
		assertNotNull(revision2Provider);
		assertNotNull(revision3Provider);
		assertNull(revision4Provider);

		assertTrue(revision1Provider.getCourseEntries().isEmpty());
		assertTrue(revision1Provider instanceof FixedListExerciseProvider);
		assertTrue(revision1Provider.getCourseEntries().isEmpty());
		assertTrue(revision1Provider instanceof FixedListExerciseProvider);
		assertFalse(revision2Provider.getCourseEntries().isEmpty());
		assertTrue(revision3Provider instanceof FolderExerciseProvider);

		clean();
	}

}
