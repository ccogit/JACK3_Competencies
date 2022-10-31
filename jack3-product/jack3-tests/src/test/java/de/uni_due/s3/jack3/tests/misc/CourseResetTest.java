package de.uni_due.s3.jack3.tests.misc;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.entities.providers.AbstractExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.services.CourseService;
import de.uni_due.s3.jack3.services.RevisionService;
import de.uni_due.s3.jack3.tests.annotations.NeedsCourse;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

/**
 * Tests resetting a course with exercise providers
 * 
 * @author lukas.glaser
 *
 */
@NeedsCourse
class CourseResetTest extends AbstractContentTest {

	@Inject
	private RevisionService revisionService;

	@Inject
	private CourseService courseService;

	@Inject
	private CourseBusiness courseBusiness;

	private AbstractExerciseProvider unproxyProvider(AbstractCourse course) {
		return (AbstractExerciseProvider) Hibernate.unproxy(course.getContentProvider());
	}

	/*-
	 * Test scenario 1:
	 * 
	 * revision				exercise provider
	 * --------------------------------------
	 * 0 (original)			null				<- reset to this revision
	 * 1					FixedListExercise
	 * 2 (newest)			FolderExercise
	 */
	@Test
	void reset1() {
		// First revision has a FLEP
		course.setContentProvider(new FixedListExerciseProvider());
		courseService.mergeCourse(course);

		// Second revision has a FEP
		course.setContentProvider(new FolderExerciseProvider());
		courseService.mergeCourse(course);

		// Current course from database
		course = courseService.getCourseWithLazyDataByCourseID(course.getId()).orElse(null);
		assertTrue(course.getContentProvider() instanceof FolderExerciseProvider);

		final List<Course> allRevisions = revisionService.getAllRevisionsForEntityWithLazyData((Course) course);

		// First revision from envers
		course = allRevisions.get(0);
		assertNull(course.getContentProvider());

		// Second revision from envers
		course = allRevisions.get(1);
		assertTrue(unproxyProvider(course) instanceof FixedListExerciseProvider);

		// Third (newest) revision from envers
		course = allRevisions.get(2);
		assertTrue(unproxyProvider(course) instanceof FolderExerciseProvider);

		List<Integer> revNumbers = revisionService.getRevisionNumbersFor(course);

		course = courseBusiness.resetToRevision(course, revNumbers.get(0), user);
		//course = courseService.resetToRevision(course, revNumbers.get(0));
		assertNull(course.getContentProvider());
	}

	/*-
	 * Test scenario 2:
	 * 
	 * revision				exercise provider
	 * --------------------------------------
	 * 0 (original)			null
	 * 1					FixedListExercise	<- reset to this revision
	 * 2 (newest)			FolderExercise
	 */
	@Test
	void reset2() {
		AbstractExerciseProvider provider = new FixedListExerciseProvider();

		// First revision has a FLEP
		course.setContentProvider(provider);
		courseService.mergeCourse(course);

		// Second revision has a FEP
		provider = new FolderExerciseProvider();
		course.setContentProvider(provider);
		courseService.mergeCourse(course);

		// Assertions on the different revisions are above

		List<Integer> revNumbers = revisionService.getRevisionNumbersFor(course);

		course = courseBusiness.resetToRevision(course, revNumbers.get(1), user);
		assertTrue(course.getContentProvider() instanceof FixedListExerciseProvider);
	}

	/*-
	 * Test scenario 3:
	 * 
	 * revision				exercise provider
	 * --------------------------------------
	 * 0 (original)			null
	 * 1					FixedListExercise
	 * 2 (newest)			FolderExercise		<- reset to this revision
	 */
	@Test
	void reset3() {
		AbstractExerciseProvider provider = new FixedListExerciseProvider();

		// First revision has a FLEP
		course.setContentProvider(provider);
		courseService.mergeCourse(course);

		// Second revision has a FEP
		provider = new FolderExerciseProvider();
		course.setContentProvider(provider);
		courseService.mergeCourse(course);

		// Assertions on the different revisions are above

		List<Integer> revNumbers = revisionService.getRevisionNumbersFor(course);

		course = courseBusiness.resetToRevision(course, revNumbers.get(2), user);
		assertTrue(course.getContentProvider() instanceof FolderExerciseProvider);
	}

	/*-
	 * Test scenario 4:
	 * 
	 * revision				exercise provider
	 * --------------------------------------
	 * 0 (original)			null				<- reset to this revision
	 * 1					FolderExercise
	 * 2 (newest)			FixedListExercise
	 */
	@Test
	void reset4() {
		AbstractExerciseProvider provider = new FolderExerciseProvider();

		// First revision has a FLEP
		course.setContentProvider(provider);
		courseService.mergeCourse(course);

		// Second revision has a FEP
		provider = new FixedListExerciseProvider();
		course.setContentProvider(provider);
		courseService.mergeCourse(course);

		// Current course from database
		course = courseService.getCourseWithLazyDataByCourseID(course.getId()).orElse(null);
		assertTrue(course.getContentProvider() instanceof FixedListExerciseProvider);

		final List<Course> allRevisions = revisionService.getAllRevisionsForEntityWithLazyData((Course) course);

		// First revision from envers
		course = allRevisions.get(0);
		assertNull(course.getContentProvider());

		// Second revision from envers
		course = allRevisions.get(1);
		assertTrue(unproxyProvider(course) instanceof FolderExerciseProvider);

		// Third (newest) revision from envers
		course = allRevisions.get(2);
		assertTrue(unproxyProvider(course) instanceof FixedListExerciseProvider);

		List<Integer> revNumbers = revisionService.getRevisionNumbersFor(course);

		course = courseBusiness.resetToRevision(course, revNumbers.get(0), user);
		assertNull(course.getContentProvider());
	}

	/*-
	 * Test scenario 5:
	 * 
	 * revision				exercise provider
	 * --------------------------------------
	 * 0 (original)			null
	 * 1					FolderExercise		<- reset to this revision
	 * 2 (newest)			FixedListExercise
	 */
	@Test
	void reset5() {
		// First revision has a FEP
		course.setContentProvider(new FolderExerciseProvider());
		courseService.mergeCourse(course);

		// Second revision has a FLEP
		course.setContentProvider(new FixedListExerciseProvider());
		courseService.mergeCourse(course);

		// Assertions on the different revisions are above

		List<Integer> revNumbers = revisionService.getRevisionNumbersFor(course);

		course = courseBusiness.resetToRevision(course, revNumbers.get(1), user);
		assertTrue(course.getContentProvider() instanceof FolderExerciseProvider);
	}

	/*-
	 * Test scenario 6:
	 * 
	 * revision				exercise provider
	 * --------------------------------------
	 * 0 (original)			null
	 * 1					FolderExercise
	 * 2 (newest)			FixedListExercise	<- reset to this revision
	 */
	@Test
	void reset6() {

		// First revision has a FEP
		course.setContentProvider(new FolderExerciseProvider());
		courseService.mergeCourse(course);

		// Second revision has a FLEP
		course.setContentProvider(new FixedListExerciseProvider());
		courseService.mergeCourse(course);

		// Assertions on the different revisions are above

		List<Integer> revNumbers = revisionService.getRevisionNumbersFor(course);

		course = courseBusiness.resetToRevision(course, revNumbers.get(2), user);
		assertTrue(course.getContentProvider() instanceof FixedListExerciseProvider);
	}

}
