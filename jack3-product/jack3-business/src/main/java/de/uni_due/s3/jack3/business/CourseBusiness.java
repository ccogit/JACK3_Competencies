package de.uni_due.s3.jack3.business;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Hibernate;

import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.business.exceptions.AuthorizationException;
import de.uni_due.s3.jack3.business.exceptions.CourseException;
import de.uni_due.s3.jack3.entities.enums.ECourseExercisesOrder;
import de.uni_due.s3.jack3.entities.providers.AbstractExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.CourseResource;
import de.uni_due.s3.jack3.entities.tenant.Enrollment;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.FrozenCourse;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;
import de.uni_due.s3.jack3.exceptions.JackRuntimeException;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;
import de.uni_due.s3.jack3.services.CourseOfferService;
import de.uni_due.s3.jack3.services.CourseRecordService;
import de.uni_due.s3.jack3.services.CourseResourceService;
import de.uni_due.s3.jack3.services.CourseService;
import de.uni_due.s3.jack3.services.EnrollmentService;
import de.uni_due.s3.jack3.services.FolderService;
import de.uni_due.s3.jack3.services.RevisionService;
import de.uni_due.s3.jack3.services.SubmissionService;
import de.uni_due.s3.jack3.utils.JackStringUtils;
import de.uni_due.s3.jack3.utils.StringGenerator;

// TODO: get rid of most of the optionals as return values in business classes. We just throw exceptions in the
// calling methods anyway, so this can be done here. This would lead to less overhead and more unification
@RequestScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class CourseBusiness extends AbstractBusiness {

	/** Password generator for course offer */
	private static StringGenerator courseOfferPwdGenerator;

	@Inject
	private RevisionService revisionService;

	@Inject
	private CourseService courseService;

	@Inject
	private CourseOfferService courseOfferService;

	@Inject
	private SubmissionService submissionService;

	@Inject
	private CourseRecordService courseRecordService;

	@Inject
	private CourseResourceService courseResourceService;

	@Inject
	private FolderService folderService;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private AuthorizationBusiness authorizationBusiness;

	@Inject
	private EnrollmentService enrollmentService;

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	static {
		CourseBusiness.courseOfferPwdGenerator = StringGenerator.forCoursePasswords().build();
	}

	public Course createCourse(String newCourseName, User lecturer, ContentFolder parent)
			throws ActionNotAllowedException {
		parent = folderService.getContentFolderWithLazyData(parent);

		if (!authorizationBusiness.isAllowedToEditFolder(lecturer, parent)) {
			throw new AuthorizationException(AuthorizationException.EType.INSUFFICIENT_RIGHT);
		}
		Course course = new Course(newCourseName);
		parent.addChildCourse(course);
		folderService.mergeContentFolder(parent);
		courseService.persistCourse(course);
		return course;
	}

	/**
	 * Deletes course, related frozen courses and all test course records
	 * <br> <b>Won't accept frozen Courses.</b>
	 *
	 * @param course
	 *            which should be deleted
	 * @param user
	 *            current User
	 * @throws ActionNotAllowedException
	 */
	public void deleteCourse(Course course, User user) throws ActionNotAllowedException {
		if (course.isFrozen()) {
			throw new IllegalArgumentException(
					"A Frozen Course can just be deleted, if the coresponding Course is deleted.");
		}
		if (!authorizationBusiness.isAllowedToEditFolder(user, course.getFolder())) {
			throw new AuthorizationException(AuthorizationException.EType.INSUFFICIENT_RIGHT);
		}
		if (courseOrFrozenCourseHasNormalSubmissions(course)) {
			throw new CourseException(CourseException.EType.COURSE_NOT_EMPTY);
		}
		if (!getCourseOffersReferencingCourse(course).isEmpty()) {
			throw new CourseException(CourseException.EType.COURSE_IS_REFERENCED);
		}

		List<FrozenCourse> frozenCourses = getFrozenRevisionsForCourse(course);
		for (FrozenCourse frozenCourse : frozenCourses) {
			deleteAllTestCourseRecordsForFrozenCourse(user, course, frozenCourse);
			courseService.deleteCourse(frozenCourse);
		}
		deleteAllTestCourseRecords(user, course);
		courseService.deleteCourse(course);

	}

	/**
	 * Course is deletable if:
	 * - it is not frozen
	 * - user has rights
	 * - course has none or only testsubmissions
	 * - no courseoffer references course
	 * 
	 * @param course
	 * @param user
	 * @return
	 */
	public boolean isCourseDeletableByUser(Course course, User user) {
		if (course.isFrozen()) {
			return false;
		}
		return (authorizationBusiness.isAllowedToEditFolder(user, course.getFolder())
				&& !courseOrFrozenCourseHasNormalSubmissions(course)
				&& getCourseOffersReferencingCourse(course).isEmpty());
	}

	/**
	 * Deletes all Test Submissions from given Course, which includes the CourseRecords and associated Submissions
	 *
	 * @param user
	 *            User, who invokes the deletion
	 * @param abstractCourse
	 *            Course, from which the Test Submissions should be deleted
	 * @throws ActionNotAllowedException
	 *             if User has not the Right to delete Submissions
	 */
	public void deleteAllTestCourseRecords(User user, AbstractCourse abstractCourse) throws ActionNotAllowedException {
		abstractCourse = courseService.getCourseByCourseID(abstractCourse.getId())
				.orElseThrow(NoSuchJackEntityException::new);
		if (!(isUserAllowedToDeleteTestCourseRecords(user, abstractCourse))) {
			throw new ActionNotAllowedException();
		}
		List<CourseRecord> testCourseRecords = getAllTestCourseRecords(abstractCourse);

		for (CourseRecord courseRecord : testCourseRecords) {
			courseRecordService.removeCourseRecordAndAttachedSubmissions(courseRecord);
		}
	}

	/**
	 * Deletes all Test Submissions from given frozen Course, which includes the CourseRecords and associated
	 * Submissions
	 *
	 * @param user
	 *            User, who invokes the deletion
	 * @param course
	 *            Original Course of the frozen one
	 * @param frozenCourse
	 *            Frozen course, from which the Test Submissions should be deleted
	 * @throws ActionNotAllowedException
	 *             if User has not the Right to delete Submissions
	 */
	public void deleteAllTestCourseRecordsForFrozenCourse(User user, AbstractCourse course, FrozenCourse frozenCourse)
			throws ActionNotAllowedException {
		frozenCourse = courseService.getFrozenCourse(frozenCourse.getId()).get();
		if (!(isUserAllowedToDeleteTestCourseRecords(user, course))) {
			throw new AuthorizationException(AuthorizationException.EType.INSUFFICIENT_RIGHT);
		}
		List<CourseRecord> testCourseRecords = getAllTestCourseRecords(frozenCourse);

		for (CourseRecord courseRecord : testCourseRecords) {
			courseRecordService.removeCourseRecordAndAttachedSubmissions(courseRecord);
		}
	}

	public void deleteTestSubmission(User user, CourseRecord courseRecord, AbstractCourse course)
			throws ActionNotAllowedException {
		course = courseService.getCourseByCourseID(course.getId()).orElseThrow(NoSuchJackEntityException::new);
		courseRecord = courseRecordService.getCourseRecordById(courseRecord.getId())
				.orElseThrow(NoSuchJackEntityException::new);

		if (!(isUserAllowedToDeleteTestCourseRecords(user, course)) || !courseRecord.isTestSubmission()) {
			throw new ActionNotAllowedException();

		}
		courseRecordService.removeCourseRecordAndAttachedSubmissions(courseRecord);

	}

	// TODO #585 Better exception handling
	public void deleteNonTestSubmission(final CourseRecord courseRecord, final CourseOffer offer, final User actor)
			throws ActionNotAllowedException {
		Objects.requireNonNull(courseRecord);
		Objects.requireNonNull(offer);
		Objects.requireNonNull(actor);

		if (courseRecord.isTestSubmission())
			throw new IllegalArgumentException();
		if (!authorizationBusiness.isAllowedToDeleteCourseRecordsFromCourseOffer(actor, offer))
			throw new ActionNotAllowedException();

		courseRecordService.removeCourseRecordAndAttachedSubmissions(courseRecord);
	}

	//REVIEW sw: Methode lässt sich wahrscheinlich noch mit eigenem request in courseRecordService optimieren
	/**
	 * Fetches every Test-CourseRecord for the given Course
	 *
	 * @param abstractCourse
	 *            Course, from which the Test-CourseRecord should be Collected
	 * @return Returns a List of CourseRecords which are created while testing the given Course
	 */
	private List<CourseRecord> getAllTestCourseRecords(AbstractCourse abstractCourse) {
		List<CourseRecord> testCourseRecords = new ArrayList<>();

		//Collecting all courseRecords
		for (CourseRecord courseRecord : courseRecordService.getAllCourseRecordsForCourse(abstractCourse)) {
			if (courseRecord.isTestSubmission()) {
				testCourseRecords.add(courseRecord);
			}
		}
		return testCourseRecords;
	}

	/**
	 * Is given User allowed to delete Testsubmissions
	 *
	 * @param user
	 *            User, for whom to check rights for
	 * @param abstractCourse
	 *            Course, for which the Rights should be checked
	 * @return true, if User is allowed to delete CourseRecords
	 */
	public boolean isUserAllowedToDeleteTestCourseRecords(User user, AbstractCourse abstractCourse) {
		// TODO #585 This should be replaced with Exception handling
		return authorizationBusiness.isAllowedToDeleteTestSubmissionsInCourse(user, abstractCourse);
	}

	//REVIEW kk - the method getCourseOfferWithLazyDataByCourseOfferID doesn't throw an Exception but returns an optional instead. Both methods should react similar if the Id doesn't exist
	public Course getCourseWithLazyDataByCourseID(long courseId) {
		return courseService.getCourseWithLazyDataByCourseID(courseId).orElseThrow(NoSuchJackEntityException::new);
	}

	//REVIEW kk - the method getCourseOfferById doesn't throw an Exception but returns an optional instead. Both methods should react similar if the Id doesn't exist
	public Course getCourseByCourseID(long courseId) {
		return courseService.getCourseByCourseID(courseId).orElseThrow(NoSuchJackEntityException::new);
	}

	/**
	 * Returns the course for {@link Course} objects or the proxied course for {@link FrozenCourse} objects.
	 */
	public Course getNonFrozenCourse(AbstractCourse abstractCourse) {
		if (abstractCourse.isFrozen()) {
			return getCourseByCourseID(abstractCourse.getRealCourseId());
		}
		return (Course) abstractCourse;
	}

	public AbstractCourse updateCourse(AbstractCourse course) {
		return courseService.mergeCourse(course);
	}

	/**
	 * Creates and returns a new regular course record, linked to a course offer.
	 */
	public CourseRecord createCourseRecord(User user, CourseOffer offer) {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);

		CourseRecord record = new CourseRecord(user, offer, offer.getCourse());
		record.getExercises().addAll(drawExercisesFromCourse(offer.getCourse()));
		courseRecordService.persistCourseRecord(record);
		return record;
	}

	/**
	 * Creates and returns a new test course record without a linked course offer.
	 */
	public CourseRecord createTestCourseRecord(User user, AbstractCourse course) {
		Objects.requireNonNull(user);
		Objects.requireNonNull(course);

		CourseRecord record = new CourseRecord(user, course);
		record.getExercises().addAll(drawExercisesFromCourse(course));
		courseRecordService.persistCourseRecord(record);
		return record;
	}

	/**
	 * creates a list of exercises which are used in the course. If the course has a FolderExerciseProvider, only the
	 * specified number of exercises will be used from this folder.
	 *
	 * @param course
	 * @return a list of exercises which can be used for a courseRecord with respect to the given course
	 */
	private Set<AbstractExercise> drawExercisesFromCourse(AbstractCourse course) {
		if (course.getContentProvider() instanceof FixedListExerciseProvider) {
			return drawExercisesFromFixedListExerciseProvider(course);
		}
		if (course.getContentProvider() instanceof FolderExerciseProvider) {
			return drawExercisesFromFolderExerciseProvider(course);
		}
		if (course.getContentProvider() == null) {
			return Collections.emptySet();
		}
		throw new UnsupportedOperationException();
	}

	private Set<AbstractExercise> drawExercisesFromFixedListExerciseProvider(AbstractCourse course) {
		Set<AbstractExercise> exercisesForCourse = new HashSet<>();

		// We can just take the course entry list
		final FixedListExerciseProvider flep = (FixedListExerciseProvider) course.getContentProvider();
		for (final CourseEntry courseEntry : flep.getCourseEntries()) {
			AbstractExercise toAdd = courseEntry.getFrozenExercise();
			exercisesForCourse.add(toAdd != null ? toAdd : courseEntry.getExercise());
		}
		return exercisesForCourse;
	}

	private Set<AbstractExercise> drawExercisesFromFolderExerciseProvider(AbstractCourse course) {
		Set<AbstractExercise> exercisesForCourse = new HashSet<>();

		// We need to search all folders and subfolders for exercises
		final FolderExerciseProvider folderExerciseProvider = (FolderExerciseProvider) course.getContentProvider();
		for (final ContentFolder contentFolder : folderExerciseProvider.getFolders()) {
			// Maybe not all exercises shall be used
			final int numberOfExercisesToBeUsed = folderExerciseProvider.getFoldersMap().get(contentFolder);

			if (numberOfExercisesToBeUsed == FolderExerciseProvider.ALL_EXERCISES) {
				//Use all child exercises from the folder
				exercisesForCourse.addAll(exerciseBusiness.getAllExercisesForContentFolderRecursive(contentFolder));
			} else {
				//Use only a part of the child exercises from the folder
				List<Exercise> childExercises = exerciseBusiness
						.getAllExercisesForContentFolderRecursive(contentFolder);
				Collections.shuffle(childExercises);
				exercisesForCourse
						.addAll(childExercises.stream().limit(numberOfExercisesToBeUsed).collect(Collectors.toList()));
			}
		}
		return exercisesForCourse;
	}

	public CourseRecord getCourseRecordById(long courseRecordId) {
		return courseRecordService.getCourseRecordById(courseRecordId).orElseThrow(NoSuchJackEntityException::new);
	}

	public CourseRecord getCourseRecordWithExercisesById(long courseRecordId) {
		return courseRecordService.getCourseRecordWithExercises(courseRecordId)
				.orElseThrow(NoSuchJackEntityException::new);
	}

	public List<CourseResource> getAllCourseResourcesForCourse(AbstractCourse course) {
		return courseResourceService.getAllCourseResourcesForCourse(course);
	}

	public List<CourseOffer> getAllCourseOffers() {
		return courseOfferService.getAllCourseOffers();
	}

	public CourseOffer updateCourseOffer(CourseOffer courseOffer) {
		return courseOfferService.mergeCourseOffer(courseOffer);
	}

	public CourseOffer createCourseOffer(String courseOfferName, AbstractCourse course, PresentationFolder folder,
			User user) {
		folder = folderService.getPresentationFolderById(folder.getId()).orElseThrow(AssertionError::new);
		CourseOffer courseOffer = new CourseOffer(courseOfferName, course);
		courseOffer.setLtiConsumerSecret(createConsumerSecretForCourseOffer());
		folder.addChildCourseOffer(courseOffer);
		folderService.mergePresentationFolder(folder);
		courseOfferService.persistCourseOffer(courseOffer);
		return courseOffer;
	}

	private String createConsumerSecretForCourseOffer() {
		return StringGenerator.forPasswords().build().generate();
	}

	/**
	 * Duplicates a course offer with all settings.
	 *
	 * @param courseOffer
	 *            The course offer of which a copy is to be made
	 * @param newName
	 *            The name for the copy
	 * @param user
	 *            The user that performs the action.
	 * @param targetFolder
	 *            The target folder for the copied Course Offer
	 * @return New course offer
	 * @throws ActionNotAllowedException
	 *             If either the user is not allowed to read the Course Offer configuration or the user is not allowed
	 *             to place something in the target folder.
	 */
	public CourseOffer duplicateCourseOffer(CourseOffer courseOffer, String newName, User user,
			PresentationFolder targetFolder) throws ActionNotAllowedException {
		if (!authorizationBusiness.isAllowedToReadFromFolder(user, courseOffer.getFolder())) {
			throw new ActionNotAllowedException("User not allowed to read course offer");
		}
		if (!authorizationBusiness.isAllowedToEditFolder(user, targetFolder)) {
			throw new ActionNotAllowedException("User not allowed place duplicated Course Offer in the target folder");
		}

		final CourseOffer copy = courseOffer.deepCopy();
		copy.setName(newName);
		copy.setLtiConsumerSecret(createConsumerSecretForCourseOffer());

		// If the user has no rights on the offer's course, s/he is not allowed to create a new course offer for this
		// course
		if (copy.getCourse() != null) {
			Course course = getNonFrozenCourse(copy.getCourse());
			if (!authorizationBusiness.isAllowedToReadFromFolder(user, course.getFolder())) {
				copy.setCourse(null);
			}
		}

		targetFolder = folderService.getPresentationFolderWithLazyData(targetFolder);
		targetFolder.addChildCourseOffer(copy);
		folderService.mergePresentationFolder(targetFolder);
		courseOfferService.persistCourseOffer(copy);
		return copy;
	}

	/**
	 * Duplicates a course with all settings.
	 *
	 * @param course
	 *            The course of which a copy is to be made
	 * @param newName
	 *            The name for the copy
	 * @param user
	 *            The user that performs the action.
	 * @param folder
	 *            The target folder for the copied Course
	 * @return New course
	 * @throws ActionNotAllowedException
	 *             If either the user is not allowed to read the Course configuration or the user is not allowed to
	 *             place something in the target folder.
	 */
	public Course duplicateCourse(Course course, String newName, User user, ContentFolder folder)
			throws ActionNotAllowedException {
		if (!authorizationBusiness.isAllowedToReadFromFolder(user, course.getFolder())) {
			throw new ActionNotAllowedException("User not allowed to read Course");
		}
		if (!authorizationBusiness.isAllowedToEditFolder(user, folder)) {
			throw new ActionNotAllowedException("User not allowed place duplicated Course in the target folder");
		}

		final Course copy = course.deepCopy();
		copy.setName(newName);
		folder = folderService.getContentFolderWithLazyData(folder);
		folder.addChildCourse(copy);
		
		removeExercisesWithoutUserRightsFromCourse(user, copy);
		
		folderService.mergeContentFolder(folder);
		courseService.persistCourse(copy);
		return copy;
	}
	
	private void removeExercisesWithoutUserRightsFromCourse(User user, Course course) throws ActionNotAllowedException {
		if (!authorizationBusiness.isAllowedToEditFolder(user, course.getFolder())) {
			throw new ActionNotAllowedException("User not allowed to remove exercises from course");
		}
		
		if (course.getContentProvider() == null) return;

		if (course.getContentProvider() instanceof FixedListExerciseProvider) {
			removeExercisesWithoutUserRightsFromFixedListExerciseProvider((FixedListExerciseProvider)course.getContentProvider(), user);
			return;
		}
		
		if (course.getContentProvider() instanceof FolderExerciseProvider) {
			removeExercisesWithoutUserRightsFromFolderExerciseProvider((FolderExerciseProvider)course.getContentProvider(), user);
			return;
		}
		
		throw new UnsupportedOperationException("Type of Contentprovider not yet supported!");
	}
	
	private void removeExercisesWithoutUserRightsFromFixedListExerciseProvider(FixedListExerciseProvider provider, User user) {
		List<CourseEntry> toRemove = new ArrayList<>();
		for( CourseEntry entry : provider.getCourseEntries()) {
			if(!authorizationBusiness.isAllowedToReadFromFolder(user, exerciseBusiness.getExerciseWithLazyDataByExerciseId(entry.getExercise().getId()).getFolder())){
				toRemove.add(entry);
			}
		}
		toRemove.forEach(provider::removeCourseEntry);
	}
	
	private void removeExercisesWithoutUserRightsFromFolderExerciseProvider(FolderExerciseProvider provider, User user) {
		for(ContentFolder folder : provider.getFolders()) {
			if(!authorizationBusiness.isAllowedToReadFromFolder(user, folder)){
				provider.removeFolder(folder);
			}
		}
	}
	
	//REVIEW kk - the method getCourseByID doesn't return an optional but throws a JackEntityException instead. Both methods should react similar if the Id doesn't exist
	public Optional<CourseOffer> getCourseOfferById(long id) {
		return courseOfferService.getCourseOfferById(id);
	}

	//REVIEW kk - the method getCourseWithLazyDataByCourseID doesn't return an optional but throws a JackEntityException instead. Both methods should react similar if the Id doesn't exist
	public Optional<CourseOffer> getCourseOfferWithLazyDataByCourseOfferID(long courseOfferId) {
		return courseOfferService.getCourseOfferById(courseOfferId);
	}

	/**
	 * Collects all courses on which the passed user has rights on. This includes courses in folders that the user owns,
	 * courses in shared folders for the user and courses in folders that are accessible for any user group the user is
	 * in. The list is ordered alphabetically by the course name.
	 */
	public List<Course> getAllCoursesForUser(User user) {
		final List<Course> courses = new ArrayList<>(courseService.getAllCoursesForUser(user));

		// Add all courses in folders that are accessible for the user group
		for (final UserGroup userGroup : userBusiness.getUserGroupsForUser(user)) {
			courses.addAll(courseService.getAllCoursesForUser(userGroup));
		}

		Collections.sort(courses, Comparator.comparing(Course::getName));
		return courses;
	}

	/**
	 * Returns all course records for a course including testing records and frozen versions of the course.
	 */
	public List<CourseRecord> getAllCourseRecordsIncludingFrozenCourses(Course course) {
		return courseRecordService.getAllCourseRecordsForCourseIncludingFrozenCourses(course);
	}

	public List<CourseRecord> getAllCourseRecords(CourseOffer courseOffer) {
		return courseRecordService.getAllCourseRecordsForCourseOfferOrderedByStarttime(courseOffer);
	}

	public List<CourseRecord> getAllCourseRecords(List<CourseOffer> courseOffers) {
		Objects.requireNonNull(courseOffers);
		return courseRecordService.getAllCourseRecordsForCourseOffersOrderedByStarttime(courseOffers);
	}

	public List<Submission> getAllSubmissionsForCourseRecord(CourseRecord courseRecord) {
		return submissionService.getAllSubmissionsForCourseRecord(courseRecord);
	}

	public Optional<Submission> getLatestSubmissionForCourseRecordAndExercise(CourseRecord courseRecord,
			AbstractExercise exercise) {
		return submissionService.getLatestSubmissionForCourseRecordAndExercise(courseRecord, exercise);
	}

	public Optional<Submission> getBestSubmissionForCourseRecordAndExercise(CourseRecord courseRecord,
			AbstractExercise exercise) {
		return submissionService.getBestSubmissionForCourseRecordAndExercise(courseRecord, exercise);
	}

	/**
	 * Returns all submissions to a course record that are not returned by getLatestSubmissionForCourseRecordAndExercise
	 * for
	 * any of the exercises in this course.
	 *
	 * @param courseRecord
	 * @return
	 */
	public List<Submission> getOldSubmissionsForCourseRecord(CourseRecord courseRecord) {
		List<Submission> allSubmissions = submissionService.getAllSubmissionsForCourseRecord(courseRecord);
		List<AbstractExercise> seenExercises = new LinkedList<>();
		List<Submission> filteredSubmissions = new LinkedList<>();

		for (Submission submission : allSubmissions) {
			if (!seenExercises.contains(submission.getExercise())) {
				seenExercises.add(submission.getExercise());
			} else {
				filteredSubmissions.add(submission);
			}
		}

		return filteredSubmissions;
	}

	public List<Course> getAllCoursesForContentFolderList(List<ContentFolder> folderList) {
		return courseService.getAllCoursesForContentFolderList(folderList);
	}

	/**
	 * Handle upload of a CSV file with personal passwords for a course offer
	 */
	public void uploadPersonalPasswordsFile(CourseOffer courseOffer, InputStream inputStream) throws IOException {
		try (InputStreamReader in = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
				BufferedReader reader = new BufferedReader(in)) {

			String line;
			String separator = "(;)|(,)"; // Regex for no specified separator

			// The first line may contains a line separator
			line = reader.readLine();
			if (line.contains("sep=")) {
				// The separator follows directly after 'sep='
				separator = Character.toString(line.charAt(4));
			} else {
				// Add the first line if it does not specifies the line separator
				addPersonalPasswordEntryFromReadedLine(courseOffer, line, separator);
			}

			// Add other lines
			while ((line = reader.readLine()) != null) {
				addPersonalPasswordEntryFromReadedLine(courseOffer, line, separator);
			}
		} catch (IOException e) {
			getLogger().error(
					"Could not process uploaded personal password file for course offer id=" + courseOffer.getId(), e);
			throw e;
		}
	}

	/**
	 * Adds a personal password entry for a course offer from a given line with user and password, separated by a
	 * separator. If no password was given, a new password will be created.
	 */
	private void addPersonalPasswordEntryFromReadedLine(CourseOffer courseOffer, String line, String separator) {

		// lineSplitted[0] - username, lineSplitted[1] - password
		String[] lineSplitted = line.split(separator);

		// Ignore empty lines
		if (lineSplitted.length < 1) {
			return;
		}

		// Check if user exists: Only add existing users, ignore not available users
		Optional<User> user = userBusiness.getUserByName(lineSplitted[0]);

		user.ifPresent(luser -> {
			if (lineSplitted.length < 2 || JackStringUtils.isBlank(lineSplitted[1])) {
				courseOffer.addPersonalPassword(luser, getRandomPasswordForCourseOffer());
			} else {
				courseOffer.addPersonalPassword(luser, lineSplitted[1].strip());
			}
		});
	}

	/**
	 * Gets a random user password for a course offer. The password is 8 digits long and can contain lowercase letters
	 * except "l", "o" and 2-9.
	 */
	private String getRandomPasswordForCourseOffer() {
		return CourseBusiness.courseOfferPwdGenerator.generate();
	}

	/**
	 * Add a new personal password entry for a given user to a course offer
	 */
	public void addPersonalPasswordEntryToCourseOffer(CourseOffer courseOffer, User user) {
		courseOffer.addPersonalPassword(user, getRandomPasswordForCourseOffer());
	}

	/**
	 * Handle download of a CSV file with personal passwords for a course offer
	 */
	public InputStream downloadPersonalPasswordsFile(CourseOffer courseOffer) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {
			// Sort the list
			List<Entry<User, String>> orderedEntryList = new ArrayList<>(courseOffer.getPersonalPasswords().entrySet());
			orderedEntryList.sort(Entry.comparingByKey());

			for (Entry<User, String> entry : orderedEntryList) {
				// Foreach entry, write username, separator, password and a line break
				out.write(entry.getKey().getLoginName().getBytes());
				out.write(";".getBytes());
				out.write(entry.getValue().getBytes());
				out.write("\n".getBytes());
			}
		} catch (IOException e) {
			getLogger().error(
					"Could not download the personal password list for course offer id=" + courseOffer.getId(), e);
		}
		return new ByteArrayInputStream(out.toByteArray());
	}

	public boolean isCourseResourceFilenameAlreadyExisting(String filename, AbstractCourse course) {
		return !courseResourceService.getCourseResourceForCourseByFilename(filename, course.getId()).isEmpty();
	}

	/**
	 * Returns all revisions for a {@link Course} <strong>without</strong> lazy data.
	 */
	public List<Course> getAllRevisionsForCourse(Course course) {
		var revisions = revisionService.getAllRevisionsForEntity(course);
		revisions.forEach(courze -> courze.setFromEnvers(true));
		return revisions;
	}

	public List<Course> getFilteredRevisionsForCourse(Course course, int first, int pageSize, String sortField,
			String sortOrderString) {
		var revisions = revisionService.getFilteredRevisionsOfEntity(course, first, pageSize, sortField,
				sortOrderString);
		revisions.forEach(courze -> courze.setFromEnvers(true));
		return revisions;
	}

	public Optional<AbstractCourse> getRevisionOfCourseWithLazyData(AbstractCourse course, int revisionId) {
		if (course.isFrozen()) {
			long realCourseId = ((FrozenCourse) course).getProxiedCourseId();
			course = courseService.getCourseByCourseID(realCourseId).orElseThrow(AssertionError::new);
		}
		return courseService.getRevisionOfCourseWithLazyData(course, revisionId);
	}

	/**
	 * Updates the folder pointer of the given course and triggers an update. This creates a new revision and returns
	 * it.
	 *
	 * @param course
	 * @param folder
	 * @param user
	 *            - the user that performs this action
	 * @return
	 * @throws ActionNotAllowedException
	 */
	public AbstractCourse moveCourse(Course course, ContentFolder folder, User user) throws ActionNotAllowedException {
		course = courseService.getCourseByCourseID(course.getId())
				.orElseThrow(() -> new IllegalArgumentException("Couldn't find the Course in the Database."));

		ContentFolder oldParent = course.getFolder();
		oldParent = folderService.getContentFolderWithLazyData(oldParent);
		folder = folderService.getContentFolderWithLazyData(folder);

		authorizationBusiness.ensureIsAllowedToMoveElement(user, course.getFolder(), folder);

		oldParent.removeChildCourse(course);
		folder.addChildCourse(course);

		folderService.mergeContentFolder(oldParent);
		folderService.mergeContentFolder(folder);
		return updateCourse(course);
	}

	public CourseOffer moveCourseOffer(CourseOffer courseOffer, PresentationFolder folder, User user)
			throws ActionNotAllowedException {
		authorizationBusiness.ensureIsAllowedToMoveElement(user, courseOffer.getFolder(), folder);

		PresentationFolder oldParent = courseOffer.getFolder();
		oldParent = folderService.getPresentationFolderWithLazyData(oldParent);
		oldParent.removeCourseOffer(courseOffer);
		folder = folderService.getPresentationFolderWithLazyData(folder);
		folder.addChildCourseOffer(courseOffer);
		folderService.mergePresentationFolder(oldParent);
		folderService.mergePresentationFolder(folder);
		return updateCourseOffer(courseOffer);
	}

	/**
	 * Deletes given CourseOffer if invoking User has the rights to do so.
	 *
	 * @param actingUser
	 *            User who invokes the CourseOffer deletion
	 * @param courseOffer
	 *            CourseOffer to Delete
	 * @throws ActionNotAllowedException
	 */
	public void deleteCourseOffer(User actingUser, CourseOffer courseOffer) throws ActionNotAllowedException {

		courseOffer = courseOfferService.getCourseOfferById(courseOffer.getId())
				.orElseThrow(NoSuchJackEntityException::new);

		if (!authorizationBusiness.isAllowedToDeleteCourseOffer(actingUser, courseOffer)) {
			throw new ActionNotAllowedException(
					"User" + actingUser + "blocked to deleted CourseOffer " + courseOffer + ": Insufficient rights.");
		}

		// We have to remove all enrollments for the course offer to avoid foreign key violation
		final List<Enrollment> orphanEnrollments = enrollmentService.getEnrollments(courseOffer);
		orphanEnrollments.forEach(enrollment -> enrollmentService.deleteEnrollment(enrollment));

		courseOfferService.deleteCourseOffer(courseOffer);
	}

	/**
	 * Sorts the Exercise list of a {@link CourseRecord}, based on {@link ECourseExercisesOrder}.
	 *
	 * @param exercises
	 *            List to be sorted.
	 * @param courseRecord
	 *            Course Record, during which the exercises are performed.
	 */
	public void sortExercisesForStudent(List<AbstractExercise> exercises, @Nonnull CourseRecord courseRecord) {
		// The course may be a FrozenCourse or a Course
		var course = courseRecord.getCourse();
		if ((course.getContentProvider() != null) && (course.getExerciseOrder() != null)) {
			exercises.sort(getExerciseComparatorInCourse(course, courseRecord));
		}
	}

	/**
	 * Returns a {@link Comparator} for ordering Exercises within a course.
	 *
	 * @param course
	 *            The course whose exercises are to be sorted.
	 * @param courseRecord
	 *            The Course Record, during which the exercises are performed. May be {@code null} if the user is not in
	 *            a Course Record. In this case, order options based on course record data are ignored and
	 *            {@link ECourseExercisesOrder#ALPHABETIC_ASCENDING} as a default value is used.
	 * @return A comparator based on the {@link ECourseExercisesOrder} of the course.
	 */
	public Comparator<AbstractExercise> getExerciseComparatorInCourse(AbstractCourse course,
			@CheckForNull CourseRecord courseRecord) {
		Objects.requireNonNull(course);
		switch (course.getExerciseOrder()) {
		case ALPHABETIC_ASCENDING:
			return Comparator.comparing(AbstractExercise::getName);
		case ALPHABETIC_DESCENDING:
			return Comparator.comparing(AbstractExercise::getName).reversed();
		case DIFFICULTY_ASCENDING:
			return Comparator.comparing(AbstractExercise::getDifficulty);
		case DIFFICULTY_DESCENDING:
			return Comparator.comparing(AbstractExercise::getDifficulty).reversed();
		case POINTS_ASCENDING:
			if (!(course.getContentProvider() instanceof FixedListExerciseProvider)) {
				throw new IllegalStateException(course.getContentProvider() + " does not support POINTS_ASCENDING.");
			}
			return getExerciseComparatorPoints(course);
		case POINTS_DESCENDING:
			if (!(course.getContentProvider() instanceof FixedListExerciseProvider)) {
				throw new IllegalStateException(course.getContentProvider() + " does not support POINTS_DESCENDING.");
			}
			return getExerciseComparatorPoints(course).reversed();
		case NUMBER_OF_SUBMISSIONS:
			if (courseRecord == null) {
				return Comparator.comparing(AbstractExercise::getName);
			}
			return getExerciseComparatorNumberOfSubmissions(courseRecord);
		case MANUAL:
			if (!(course.getContentProvider() instanceof FixedListExerciseProvider)) {
				throw new IllegalStateException(course.getContentProvider() + " does not support MANUAL.");
			}
			return getExerciseComparatorManual(course);
		default:
			throw new UnsupportedOperationException(course.getExerciseOrder() + " is not supported yet.");
		}
	}

	private Comparator<AbstractExercise> getExerciseComparatorPoints(AbstractCourse course) {
		// Map the course entry list from the provider to a Map containing each exercise and the points
		final var pointsMap = course.getContentProvider().getCourseEntries().stream()
				.collect(Collectors.toMap(CourseEntry::getExerciseOrFrozenExercise, CourseEntry::getPoints));
		return Comparator.comparing(pointsMap::get);
	}

	private Comparator<AbstractExercise> getExerciseComparatorNumberOfSubmissions(CourseRecord courseRecord) {
		// Map the course entry list from the provider to a Map containing each exercise and the submission count
		final var submissionCountMap = courseRecord.getCourse().getContentProvider().getCourseEntries().stream()
				.map(CourseEntry::getExerciseOrFrozenExercise)
				.map(e -> Pair.of(e, submissionService.countAllSubmissionsForCourseRecordAndExercise(courseRecord, e)))
				.collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
		return Comparator.comparing(submissionCountMap::get);
	}

	private Comparator<AbstractExercise> getExerciseComparatorManual(AbstractCourse course) {
		// Map the course entry list from the provider to a list of all exercises
		final var orderedExercises = course.getContentProvider().getCourseEntries().stream()
				.map(CourseEntry::getExerciseOrFrozenExercise).collect(Collectors.toList());
		return Comparator.comparing(orderedExercises::indexOf);
	}

	/**
	 * Returns all frozen course revisions without lazy data for a given course.
	 */
	// REVIEW lg - Ergibt der Parameter "AbstractCourse" hier Sinn? Warum sollten wir hier auch einen FrozenCourse als Parameter erlauben?
	public List<FrozenCourse> getFrozenRevisionsForCourse(AbstractCourse course) {
		return courseService.getFrozenRevisionsForCourse(course.getRealCourseId());
	}

	public void createFrozenCourse(AbstractCourse course, int currentRevisionId) {
		FrozenCourse frozenCourse = new FrozenCourse((Course) course, currentRevisionId);
		frozenCourse = courseService.mergeFrozenCourse(frozenCourse);
		getLogger().info("Frozen Revision \"" + frozenCourse + "\" of course successfully created!");
	}

	public FrozenCourse getFrozenCourseByProxiedIdsWithLazyData(long realCourseId, int proxiedCourseRevisionId) {
		return courseService.getFrozenCourseByProxiedIdsWithLazyData(realCourseId, proxiedCourseRevisionId)
				.orElseThrow(NoSuchJackEntityException::new);
	}

	/**
	 * Returns the merged version of the given course to a revisionIndex from the audit tables. We do this by first
	 * creating a deep copy and then tricking hibernate to save it as an existing version of the exercise. This way we
	 * don't reference anything from the audit tables anymore to avoid updating anything else with old values from the
	 * audit tables. We also deal with meanwhile deleted folders or exercises in contentproviders here.
	 *
	 * @param course
	 *            Course entity that will be reset to a revision of itself. BEWARE we might geht an already old version
	 *            from envers here
	 * @param revisionId
	 *            ID (not index!) of courserevision in the database the course should be set to
	 * @param currentUser
	 *            TODO: see #428
	 * @return Course with values adjusted as closely as possible (see above) to the given revision index.
	 */
	public AbstractCourse resetToRevision(AbstractCourse course, int revisionId, User currentUser) {

		// Ensure we have the newest revision here
		Course newestCourseVersion = getCourseWithLazyDataByCourseID(course.getId());
		ContentFolder currentFolder = newestCourseVersion.getFolder();

		AbstractExerciseProvider contentProviderEnvers = getContentProviderAtRevision(newestCourseVersion, revisionId);
		Course courseAtRevision = new Course(newestCourseVersion, revisionId);

		// REVIEW: Course.setFolder() is package-private and there is a comment indicating that it must be package
		// private, anyone know why that is the case?
		// We want to stay in the same folder, so setting to our current folder here, fixes #645.
		setFolderByReflection(currentFolder, courseAtRevision);

		// To make Hibernate think that this is not a completly new course we also need to copy the hibernateIds
		// This makes the merge below possible and we keep the version history.
		courseAtRevision.copyHibernateAndJackIdsOf(newestCourseVersion);

		if (contentProviderEnvers instanceof FixedListExerciseProvider) {
			FixedListExerciseProvider fixedListExerciseProvider = (FixedListExerciseProvider) contentProviderEnvers;
			courseAtRevision.setContentProvider(createNewProviderForMainDB(fixedListExerciseProvider));
		} else if (contentProviderEnvers instanceof FolderExerciseProvider) {
			FolderExerciseProvider folderExerciseProvider = (FolderExerciseProvider) contentProviderEnvers;
			courseAtRevision.setContentProvider(createNewProviderForMainDB(folderExerciseProvider));
		} else if (contentProviderEnvers == null) {
			courseAtRevision.setContentProvider(null);
		} else {
			throw new UnsupportedOperationException();
		}

		courseService.mergeCourse(courseAtRevision);
		return courseAtRevision;
	}

	private void setFolderByReflection(ContentFolder currentFolder, Course newCourse) {
		try {
			Field field = newCourse.getClass().getDeclaredField("folder");
			// This is used by CourseBusiness.resetToRevision(AbstractCourse, int, User), so muting sonar here until the
			// REVIEW comment there is discussed.
			field.setAccessible(true); // NOSONAR
			field.set(newCourse, currentFolder); // NOSONAR
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			throw new JackRuntimeException(e);
		}
	}

	public AbstractExerciseProvider getContentProviderAtRevision(AbstractCourse course, int revisionId) {
		Course revisionOfCourse = (Course) revisionService.getRevisionOfEntityWithLazyData(course, revisionId)
				.orElseThrow(() -> new IllegalArgumentException("Course with revision-index " + revisionId
						+ " doesn't exist in the audit tables. Given course: " + course));
		AbstractExerciseProvider contentProvider = revisionOfCourse.getContentProvider();
		return (AbstractExerciseProvider) Hibernate.unproxy(contentProvider);
	}

	/**
	 * Returns a new provider created from the given one (that may be from envers and may contain folders, that are
	 * deleted in the main db) containing only folders in the main db that still exist. Additionally, does not contain
	 * references to folders in Envers but to to the ones found in main db to prevent Hibernate updating existing
	 * folders with old values from envers.
	 *
	 * @param contentProvider
	 *            old revision of exercise provider to create our new one from
	 * @return A new FolderExerciseProvider containing folder references still existing in the main-db.
	 */
	private FolderExerciseProvider createNewProviderForMainDB(FolderExerciseProvider contentProvider) {
		final LinkedHashMap<ContentFolder, Integer> map = new LinkedHashMap<>();

		for (ContentFolder contentFolder : contentProvider.getFolders()) {
			if (folderService.getContentFolderById(contentFolder.getId()).isPresent()) {
				map.put(contentFolder, contentProvider.getFoldersMap().get(contentFolder));
			}
		}

		return new FolderExerciseProvider(map);
	}

	/**
	 * Returns a new provider created from the given one (that may be from envers and may contain exercises, that are
	 * deleted in the main db) containing only exercises in the main db that still exist. Additionally, does not contain
	 * references to exercises in Envers but to to the ones found in main db to prevent Hibernate updating existing
	 * exercises with old values from envers.
	 *
	 * @param fixedListExerciseProvider
	 *            old revision of exercise provider to create our new one from
	 * @return A new FixedListExerciseProvider containing exercise references still existing in the main-db.
	 */
	private FixedListExerciseProvider createNewProviderForMainDB(FixedListExerciseProvider fixedListExerciseProvider) {

		FixedListExerciseProvider newProvider = new FixedListExerciseProvider();
		// First filters out CourseEntries, whose Exercises are not found in the main-db anymore, then iterates over the
		// remaining ones and adds Exercise-references (i.e. new CourseEntrys) in the main db to our newProvider.
		fixedListExerciseProvider.getCourseEntries().stream() //
				.filter(courseEntry -> (exerciseBusiness.getExerciseById(courseEntry.getExercise().getId()))
						.isPresent()) //
				.forEach(courseEntry -> { //
					AbstractExercise exerciseHibernate = exerciseBusiness.getExerciseById( //
							courseEntry.getExercise().getId()) //
							.get(); //
					newProvider.addCourseEntry(new CourseEntry(exerciseHibernate, courseEntry.getPoints()));
				});

		return newProvider;
	}

	public List<CourseEntry> getCourseEntrysMissingInMainDb(FixedListExerciseProvider fixedListExerciseProvider) {
		if (fixedListExerciseProvider == null) {
			return new ArrayList<>();
		}

		return fixedListExerciseProvider.getCourseEntries().stream() //
				.filter(courseEntry -> !exerciseBusiness.getExerciseById(courseEntry.getExercise().getId()).isPresent())
				.collect(Collectors.toList());
	}

	public List<ContentFolder> getFoldersMissingInMainDb(FolderExerciseProvider folderExerciseProvider) {
		return folderExerciseProvider.getFolders().stream()
				.filter(folder -> !folderService.getContentFolderById(folder.getId()).isPresent())
				.collect(Collectors.toList());
	}

	public Optional<Course> getNewestRevisionOfFrozenCourse(FrozenCourse course) {
		return courseService.getCourseWithLazyDataByCourseID(course.getProxiedCourseId());
	}

	public FrozenCourse getFrozenCourse(long courseId) {
		return courseService.getFrozenCourse(courseId).orElseThrow(NoSuchJackEntityException::new);
	}

	public FrozenCourse getFrozenCourseWithLazyData(long courseId) {
		return courseService.getFrozenCourseWithLazyData(courseId).orElseThrow(NoSuchJackEntityException::new);
	}

	public List<Integer> getRevisionNumbersFor(AbstractCourse course) {
		return revisionService.getRevisionNumbersFor(course);
	}

	public int getProxiedOrLastPersistedRevisionId(AbstractCourse course) {
		return revisionService.getProxiedOrLastPersistedRevisionId(course);
	}

	public Integer getNumberOfRevisions(AbstractCourse course) {
		return revisionService.getRevisionNumbersFor(course).size();
	}

	public AbstractExerciseProvider getRevisionOfContentproviderWithLazyData(long id, int currentRevisionId) {
		return revisionService.getRevisionOfEntityWithLazyData(AbstractExerciseProvider.class, id, currentRevisionId);
	}

	public int getRevisionIndexForRevisionId(AbstractCourse course, int revisionId) {
		List<Integer> revisions = getRevisionNumbersFor(course);
		return revisions.indexOf(revisionId);
	}

	public long countCommentsForCourseRecord(CourseRecord courseRecord) {
		return courseRecordService.countCommentsForCourseRecord(courseRecord);
	}

	public long countUnreadCommentsForCourseRecord(CourseRecord courseRecord) {
		return courseRecordService.countUnreadCommentsForCourseRecord(courseRecord);
	}

	public List<CourseOffer> getCourseOffersReferencingCourse(AbstractCourse course) {
		return courseOfferService.getCourseOffersReferencingCourse(course);
	}

	public String getCourseOffersReferencingCourseAsString(AbstractCourse course) {
		List<CourseOffer> referencingOffers = getCourseOffersReferencingCourse(course);
		return referencingOffers.stream().map(CourseOffer::getName).collect(Collectors.joining(", "));
	}

	/**
	 * Returns all AbstractCourses which contain the exercise.
	 * <strong>WARNING:</strong> Only Courses with an ExerciseProvider attached are checked!
	 *
	 * @param excercise
	 * @return
	 */
	public List<AbstractCourse> getAbstractCoursesWithExerciseProviderContainingExercise(AbstractExercise excercise) {
		return courseService.getAbstractCoursesReferencingExerciseExerciseProvider(excercise);
	}

	/**
	 * Returns all course offers in a folder.
	 *
	 * @param folder
	 *            The folder to search for course offers.
	 * @param recursive
	 *            Whether subfolders are included in the search.
	 */
	public List<CourseOffer> getCourseOffersByFolder(PresentationFolder folder, boolean recursive) {
		if (recursive) {
			final var folders = folderBusiness.getAllChildPresentationFolders(folder, true);
			return courseOfferService.getCourseOffersByFolders(folders);
		} else {
			return courseOfferService.getCourseOffersByFolder(folder);
		}
	}

	/**
	 * Returns all Courses which contain the exercise.
	 * <strong>WARNING:</strong> Only Courses with an FolderProvider attached are checked!
	 * <br>
	 * Gets all parentfolders for the exercise (all folders on the path from exercise to root, without root).
	 * Checks which course contains at least one of the folders.
	 * <br>
	 * <strong>Reason:</strong> The FolderProvider doesn't contain a list of exercises.
	 * So we have to check for the folders, which contain the exercise.
	 * Next Problem is, that only the top selected folder and not its child folders is in the list of folders.
	 * So we have to get all folders from exercise to root.
	 * One of them could be selected and is therefore in the list of folders of the FolderProvider.
	 * So we check if one of the folders is in the list.
	 *
	 *
	 *
	 * @param exercise
	 * @return
	 */
	public List<Course> getCoursesWithFolderProviderContainingExercise(AbstractExercise exercise) {

		ContentFolder parentFolderOfExercise = folderBusiness.getContentFolderFor((Exercise) exercise);
		List<ContentFolder> foldersContainingExercise = folderBusiness
				.getAllParentFoldersAndContentFolder(parentFolderOfExercise);

		return courseService.getCoursesReferencingContentFoldersByFolderProvider(foldersContainingExercise);
	}

	/**
	 * Returns all courses, which contain the exercise.
	 * Will check for courses with ExerciseProvider and FolderProvider
	 *
	 * @param exercise
	 * @return
	 */
	public List<Course> getAllCoursesContainingExercise(AbstractExercise exercise) {
		//check for exerciseProvider
		List<Course> courses = courseService.getCoursesReferencingExerciseExerciseProvider(exercise);
		// check in folderProvider
		courses.addAll(getCoursesWithFolderProviderContainingExercise(exercise));
		return courses;
	}

	/**
	 * Returns all courses that contain the contentFolder and use an FolderExerciseProvider.
	 *
	 * @param contentFolder
	 * @return
	 */
	public List<AbstractCourse> getCoursesContainingContentFolderByFolderProvider(ContentFolder contentFolder) {
		//if a provider uses one of the parentFolders it automatically uses all of the subFolders
		//because of this we have to check all parentFolders as well as the given contentFolder
		final List<ContentFolder> parents = new ArrayList<>();
		ContentFolder pointer = contentFolder;
		while (pointer != null) {
			parents.add(pointer);
			pointer = (ContentFolder) pointer.getParentFolder();
		}
		return courseService.getAbstractCoursesReferencingContentFoldersByFolderProvider(parents);
	}

	/**
	 * Returns wether a frozen course exists for the id of the proxied ("real") course and the revision ID.
	 */
	public boolean frozenCourseExists(long proxiedCourseId, int proxiedCourseRevisionId) {
		return courseService.getFrozenCourseByProxiedIds(proxiedCourseId, proxiedCourseRevisionId).isPresent();
	}

	public boolean courseOrFrozenCourseHasNormalSubmissions(AbstractCourse abstractcourse) {
		Course course = getCourseByCourseID(abstractcourse.getId());
		for (CourseRecord record : courseRecordService.getAllCourseRecordsForCourseIncludingFrozenCourses(course)) {
			if (!record.isTestSubmission()) {
				return true;
			}
		}
		return false;
	}

	public boolean isStringEqualsCourseOfferName(String input, CourseOffer courseOffer) {
		return input.contentEquals(courseOffer.getName());
	}

	public int getCourseRevisionNumberForCourseRecord(CourseRecord courseRecord) {
		AbstractCourse abstractCourse = courseRecord.getCourse();
		if (abstractCourse.isFrozen()) {
			abstractCourse = getCourseByCourseID(((FrozenCourse) abstractCourse).getRealCourseId());
		}
		return getRevisionIndexForRevisionId(abstractCourse, courseRecord.getCourseRevisionId());
	}
}
