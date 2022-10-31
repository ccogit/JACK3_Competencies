package de.uni_due.s3.jack3.business;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.business.exceptions.AuthorizationException;
import de.uni_due.s3.jack3.business.exceptions.AuthorizationException.EType;
import de.uni_due.s3.jack3.business.exceptions.FolderException;
import de.uni_due.s3.jack3.business.helpers.ECourseOfferAccess;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;
import de.uni_due.s3.jack3.exceptions.PreconditionException;
import de.uni_due.s3.jack3.interfaces.TestableSubmission;
import de.uni_due.s3.jack3.services.BaseService;
import de.uni_due.s3.jack3.services.CourseOfferService;
import de.uni_due.s3.jack3.services.FolderService;
import de.uni_due.s3.jack3.services.UserGroupService;
import de.uni_due.s3.jack3.services.UserService;

@RequestScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class AuthorizationBusiness extends AbstractBusiness {

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private EnrollmentBusiness enrollmentBusiness;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private FolderService folderService;

	@Inject
	private UserGroupService userGroupService;

	@Inject
	private UserService userService;

	@Inject
	private BaseService baseService;

	@Inject
	private CourseOfferService courseOfferService;

	public boolean hasAdminRights(User user) {
		return userService.getUserById(user.getId()).orElseThrow(NoSuchJackEntityException::new).isHasAdminRights();
	}

	public boolean hasRightsOnAllCourseExercises(User user, Course course) {
		if (course.getContentProvider() == null) {
			return true;
		}

		if (course.getContentProvider() instanceof FixedListExerciseProvider) {
			return hasRightsOnAllExercisesOfFixedListExerciseProvider(user, course);
		}
		if (course.getContentProvider() instanceof FolderExerciseProvider) {
			return hasRightsOnAllExercisesOfFolderExerciseProvider(user, course);
		}

		throw new UnsupportedOperationException("Type of Contentprovider not yet supported!");
	}

	private boolean hasRightsOnAllExercisesOfFolderExerciseProvider(User user, Course course) {
		// We need to search all folders and subfolders for exercises
		final FolderExerciseProvider folderExerciseProvider = (FolderExerciseProvider) course.getContentProvider();
		for (final ContentFolder exerciseFolder : folderExerciseProvider.getFolders()) {
			if (!isAllowedToReadFromFolder(user, exerciseFolder)) {
				return false;
			}
		}
		return true;
	}

	private boolean hasRightsOnAllExercisesOfFixedListExerciseProvider(User user, Course course) {
		// We can just take the course entry list
		final FixedListExerciseProvider flep = (FixedListExerciseProvider) course.getContentProvider();
		for (final CourseEntry courseEntry : flep.getCourseEntries()) {
			Exercise exercise = courseEntry.getExercise();
			ContentFolder exerciseFolder = exerciseBusiness.getExerciseWithLazyDataByExerciseId(exercise.getId())
					.getFolder();
			if (!isAllowedToReadFromFolder(user, exerciseFolder)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns User right for given <b>Folder</b>, <b>CourseOffer</b>, <b>AbstractCourse</b> or <b>Exercise</b>.
	 *
	 * @param user
	 *            User, for whom to check rights for
	 * @param abstractEntity
	 *            One of the following: Folder, courseOffer, AbstractCourse or Exercise
	 * @return The right the user has on the folder that "entity" is contained in
	 * @exception IllegalArgumentException
	 *                if unsupported Entities are used
	 */
	@Nonnull
	private AccessRight getRightForEntity(User user, AbstractEntity abstractEntity) {
		if (abstractEntity instanceof Folder) {
			abstractEntity = folderBusiness.getFolderWithManagingRights((Folder) abstractEntity)
					.orElseThrow(() -> new PreconditionException("Couldn't load Folder."));
			Folder folder = (Folder) abstractEntity;
			return getCompleteManagingUsersMap(true, folder).getOrDefault(user, AccessRight.getNone());
		}
		if (abstractEntity instanceof Course) {
			return getRightForEntity(user, ((Course) abstractEntity).getFolder());
		}
		if (abstractEntity instanceof Exercise) {
			return getRightForEntity(user, ((Exercise) abstractEntity).getFolder());
		}
		if (abstractEntity instanceof CourseOffer) {
			return getRightForEntity(user, ((CourseOffer) abstractEntity).getFolder());
		}
		throw new IllegalArgumentException("Requested rights for an unsupported entity: " + abstractEntity);
	}

	/**
	 * Checks, if User has Write-right for given Entity. Supported Entities are documented in
	 * {@link #getRightForEntity(User, AbstractEntity)}.
	 *
	 * @param user
	 *            User, for whom to check rights for
	 * @param abstractEntity
	 *            The Entity, for which the Right is checked.
	 * @return Does the User has the Right "ReadWrite" for the given AbstractEntity.
	 * @see #getRightForEntity(User, AbstractEntity)
	 */
	private boolean hasReadWriteRightForEntity(User user, AbstractEntity abstractEntity) {
		return getRightForEntity(user, abstractEntity).isWrite();
	}

	/**
	 *
	 * @param user
	 *            User, for whom to check rights for
	 * @param courseOffer
	 *            Courseoffer, for which the right to delete should be determined
	 * @return
	 */
	public boolean isAllowedToDeleteCourseOffer(User user, CourseOffer courseOffer) {
		return hasReadWriteRightForEntity(user, courseOffer);
	}

	/**
	 * The user can delete the courserecord if
	 *
	 * a) it is a testing record and the user has at least read right for the corresponding
	 * course (we don't need to check if the test record is accessed correctly, because it is only shown in the
	 * courseStatistics )
	 *
	 * b) it is a non-testing record and the user has grade rights for the corresponding course offer and access the
	 * record from the corresponding course offer
	 *
	 * @param user
	 *            the user who want to delte the courserecord
	 * @param record
	 *            the courserecord which should be deleted
	 * @param accesspath
	 *            CourseOffer from which the record is accessed, null if the record is accessed from a course
	 * @return
	 */
	public boolean isAllowedToDeleteCourseRecord(User user, CourseRecord record, @CheckForNull CourseOffer accesspath) {
		return hasGradeRightOnCourseRecord(user, record) && accessCourseRecordCorrectly(record, accesspath);
	}

	public boolean isAllowedToDeleteCourseRecordsFromCourseOffer(User user, CourseOffer courseOffer) {
		// We don't have to distinguish between testing and non-testing course records because a course offer shows only the latter.
		return hasSpecificRightOnFolder(user, courseOffer.getFolder(), AccessRight::isGrade);
	}

	/**
	 * Is given User allowed to delete test Submissions in given AbstractCourse
	 *
	 * @param user
	 *            User, for whom to check rights for
	 * @param abstractCourse
	 *            Course, for which the Rights should be checked
	 * @return true, if User has right to delete test Submissions in given Course
	 */
	public boolean isAllowedToDeleteTestSubmissionsInCourse(User user, AbstractCourse abstractCourse) {
		return getRightForEntity(user, abstractCourse).isRead();
	}

	public boolean isAllowedToCreateFolder(User user, Folder folder) {
		return hasReadWriteRightForEntity(user, folder);
	}

	/**
	 * Checks whether a Student is allowed to see his submissions in a course record.
	 *
	 * @param student
	 *            The Student who want to see his submission
	 * @param courseRecord
	 *            The CourseRecord of the submission.
	 */
	public boolean isStudentAllowedToSeeCourseRecordSubmissions(User student, CourseRecord courseRecord) {
		// Students are not allowed to see the submissions of other students
		if (courseRecord.getUser().equals(student)) {
			final Optional<CourseOffer> courseOfferOpt = courseRecord.getCourseOffer();
			if (!courseOfferOpt.isPresent()) {
				// Students have no rights on test records
				return false;
			}
			final CourseOffer courseOffer = courseOfferOpt.get();

			// The Student is allowed to see his submissions depending on the review mode in the CourseOffer.
			switch (courseOffer.getReviewMode()) {
			case ALWAYS:
				//The Student can always see his submissions
				return true;

			case AFTER_EXIT:
				// The Student can see his submissions if he finished the CourseRecord.
				return courseRecord.isClosed();

			case AFTER_END:
				//The Student can see his submissions if the deadline for the CourseRecord is reached.
				return (courseOffer.getSubmissionDeadline() != null)
						&& LocalDateTime.now().isAfter(courseOffer.getSubmissionDeadline());

			default:
				return false;
			}

		}
		return false;
	}

	// TODO It's better to ensure that "submission" is really a non-testing submission than simply assume it.
	public boolean isStudentAllowedToSeeFeedbackForSubmission(User user, Submission submission) {
		//A student is not allowed to see the feedback of submissions from other students
		if (!submission.getAuthor().equals(user)) {
			return false;
		}
		// Check if the course offer allows to show the feedback
		// We have to load the course offer from the database to make sure that we have a current version from the main table
		CourseOffer courseOffer = loadCourseOfferForNonTestingSubmission(submission);
		return courseOffer.isShowFeedbackInCourseResults();
	}

	public boolean isStudentAllowedToSeeResultForSubmission(User user, Submission submission) {
		//A student is not allowed to see the result of submissions from other students
		if (!submission.getAuthor().equals(user)) {
			return false;
		}
		// Check if the course offer allows to show the result
		// We have to load the course offer from the database to make sure that we have a current version from the main table
		CourseOffer courseOffer = loadCourseOfferForNonTestingSubmission(submission);
		return courseOffer.isShowResultInCourseResults();
	}

	public boolean isStudentAllowedToSeeResultForCourseRecord(User user, CourseRecord courseRecord) {
		//A student is not allowed to see the result of courseRecors from other students
		if (!courseRecord.getUser().equals(user)) {
			return false;
		}
		// Check if the course offer allows to show the result
		// We have to load the course offer from the database to make sure that we have a current version from the main table
		CourseOffer courseOffer = loadCourseOfferForNonTestingSubmission(courseRecord);
		return courseOffer.isShowResultInCourseResults();
	}

	public boolean isStudentAllowedToSeeDetailsForCourseRecord(User user, CourseRecord courseRecord) {
		//A student is not allowed to see the details of courseRecors from other students
		if (!courseRecord.getUser().equals(user)) {
			return false;
		}
		// Check if the course offer allows to show details
		// We have to load the course offer from the database to make sure that we have a current version from the main table
		CourseOffer courseOffer = loadCourseOfferForNonTestingSubmission(courseRecord);
		return courseOffer.isShowExerciseAndSubmissionInCourseResults();
	}

	public boolean isAllowedToEditFolder(User user, final Folder folder) {
		Folder folderWithManagingRights = folderBusiness.getFolderWithManagingRights(folder)
				.orElseThrow(() -> new PreconditionException("The Folder doesn't exist anymore: " + folder));
		AccessRight userRight = getCompleteManagingUsersMap(false, folderWithManagingRights)
				.getOrDefault(user, AccessRight.getNone());

		return userRight.isWrite();
	}

	public <T extends Folder> void ensureIsAllowedToMoveFolder(User user, T moving, T target)
			throws ActionNotAllowedException {

		if (target.isChildOf(moving)) {
			// a) Drop target must not be child element of the dragged element.
			throw new FolderException(FolderException.EType.RECURSION);
		}
		if (moving.isRoot()) {
			// b) Root folders are fixed.
			throw new FolderException(FolderException.EType.ROOT);
		}
		if (folderBusiness.isPersonalFolder(moving)) {
			// c) Personal folders must not be moved.
			throw new FolderException(FolderException.EType.PERSONAL_FOLDER);
		}
		ensureIsAllowedToMoveElement(user, moving.getParentFolder(), target);
	}

	@SuppressWarnings("unchecked")
	public <T extends Folder> void ensureIsAllowedToMoveElement(User user, T source, T target)
			throws ActionNotAllowedException {
		if (!hasSpecificRightOnFolder(user, source, AccessRight::isWrite)) {
			// d) The user is not allowed to remove the element from the source
			// Note that this case is already covered by (e) and (g), but we give a more specific reason here.
			throw new AuthorizationException(EType.DRAG_TARGET_RIGHT_IS_NOT_WRITE);
		}
		if (!hasSpecificRightOnFolder(user, target, AccessRight::isWrite)) {
			// e) The user is not allowed to put new elements into the target folder
			throw new AuthorizationException(EType.DROP_TARGET_RIGHT_IS_NOT_WRITE);
		}
		if (hasSpecificRightOnFolder(user, source, AccessRight::isManage)) {
			// f) With the MANAGE right the user can do anything with elements in the folder
			return;
		}

		// g) Otherwise the user is allowed to move elements only if the rights of the element would not change
		// It is NOT intended to check "getCompleteManagingUsersMap" because this would swallow differences between user rights and user groups
		source = (T) folderBusiness.getFolderWithManagingRights(source).orElseThrow(NoSuchJackEntityException::new);
		target = (T) folderBusiness.getFolderWithManagingRights(target).orElseThrow(NoSuchJackEntityException::new);
		if (!getAllManagingUsers(source).equals(getAllManagingUsers(target))) {
			throw new AuthorizationException(EType.RIGHTS_WILL_CHANGE);
		}
		if (!getAllManagingUserGroups(source).equals(getAllManagingUserGroups(target))) {
			throw new AuthorizationException(EType.RIGHTS_WILL_CHANGE);
		}
	}

	// REVIEW bo: ich finde "read" zu zweideutig, kann man da irgendwas besseres finden, wie "config_read" oder sowas?`
	// Ich denke bei read auch daran, ob ein Student das überhaupt sehen kann.
	public boolean isAllowedToReadFromFolder(User user, Folder folder) {
		folder = folderBusiness.getFolderWithManagingRights(folder)
				.orElseThrow(() -> new IllegalArgumentException("Invalid folder"));
		AccessRight userRight = getCompleteManagingUsersMap(false, folder).getOrDefault(user,
				AccessRight.getNone());

		return userRight.isRead();
	}

	// REVIEW SW: "teure" Bestimmung der Sichtbarkeit von Ordnern, wenn es Presentationsordner sind
	/**
	 * Note: Call with caution. Call can be resource-expensive with larger PresentationFolder/CourseOffer Structure.
	 *
	 * @param user
	 * @param folderList
	 * @return
	 */
	public Map<Folder, Boolean> isAllowedToSeeFolders(User user, final List<Folder> folderList) {
		Map<Folder, Boolean> folderAllowedToSeeMap = new HashMap<>();
		List<Folder> refreshedFolderList = folderBusiness.getFoldersWithManagingRights(folderList);
		Map<Folder, AccessRight> folderAccessRightMap = getMaximumRightForUser(user, refreshedFolderList);


		//Processing the folders, on wich the user has at least read-rights
		folderAccessRightMap.keySet()
				.forEach(folder -> folderAllowedToSeeMap.put(folder, folderAccessRightMap.get(folder).isRead()));

		for (Folder currentFolder : refreshedFolderList) {
			if((currentFolder instanceof PresentationFolder) && !folderAllowedToSeeMap.get(currentFolder)) {
				folderAllowedToSeeMap.put(currentFolder, isAllowedToSeePresentationFolder(user,(PresentationFolder) currentFolder));
			}
		}

		return folderAllowedToSeeMap;
	}

	public boolean isAllowedToSeePresentationFolder(User currentUser, PresentationFolder presentationFolder) {
		List<CourseOffer> courseOfferList = courseOfferService.getCourseOffersByFolder(presentationFolder);
		for (final CourseOffer currentCourseOffer : courseOfferList) {
			ECourseOfferAccess accessForOffer = getCourseOfferVisibilityForUser(currentUser, currentCourseOffer);
			if ((accessForOffer == ECourseOfferAccess.SEE_AS_STUDENT) || (accessForOffer == ECourseOfferAccess.READ)
					|| (accessForOffer == ECourseOfferAccess.EDIT)) {
				return true;
			}
			for(final Folder currentPresFolder : currentCourseOffer.getFolder().getChildrenFolder()) {
				if(isAllowedToSeePresentationFolder(currentUser, (PresentationFolder) currentPresFolder)) {
					return true;
				}
			}
		}

		return false;
	}

	public boolean isAccessRightRead(AccessRight right) {
		return right.isRead();
	}

	public boolean hasExtendedReadOnFolder(User user, Folder folder) {
		AccessRight userMaxRightOnFolder = getMaximumRightForUser(user, folder);
		return userMaxRightOnFolder.isExtendedRead();
	}

	/**
	 * The managing right is granted for
	 * <ul>
	 * <li>users owning the folder</li>
	 * <li>admins on all presentation folders</li>
	 * <li>users that have {@link AccessRight#MANAGE} rights on the parent folder.</li>
	 * </ul>
	 */
	public boolean canManage(User user, Folder folder) {
		folder = baseService.findById(folder.getClass(), folder.getId(), false).orElseThrow();
		user = baseService.findById(user.getClass(), user.getId(), false).orElseThrow();
		if (folder.isRoot()) {
			// No one is allowed to manage root folders
			return false;
		}
		if ((folder instanceof ContentFolder) && folder.isChildOf(user.getPersonalFolder())) {
			// All users can manage their personal folders and all sub directories
			// NOTE: This case is NOT covered by the last return value below if folder==user.personalFolder!
			return true;
		}
		if ((folder instanceof PresentationFolder) && folder.getParentFolder().isRoot() && user.isHasAdminRights()) {
			// Admins are allowed to manage first-level presentation folders if they can manage itself
			// (see #1043)
			return getMaximumRightForUser(user, folder).isManage();
		}
		return getMaximumRightForUser(user, folder.getParentFolder()).isManage();
	}

	// TODO #585 Revisit for rights refactoring
	public boolean canManage(User user, Course entity) {
		return getMaximumRightForUser(user, entity.getFolder()).isManage();
	}

	public boolean canManage(User user, CourseOffer entity) {
		return getMaximumRightForUser(user, entity.getFolder()).isManage();
	}

	public boolean canManage(User user, Exercise entity) {
		return getMaximumRightForUser(user, entity.getFolder()).isManage();
	}

	public boolean isAllowedToDeleteSingleSubmissionsFromCourseRecord(User user, CourseRecord courseRecord,
			@CheckForNull CourseOffer accesspath) {
		return hasGradeRightOnCourseRecord(user, courseRecord) && accessCourseRecordCorrectly(courseRecord, accesspath);
	}

	/**
	 * The grade right is given for a) testing records when the user has at least read right for the corresponding
	 * course b) non-testing records when the user has grade rights for the corresponding course offer
	 */
	public boolean hasGradeRightOnCourseRecord(User user, CourseRecord courseRecord) {

		courseRecord = baseService.findById(CourseRecord.class, courseRecord.getId(), false)
				.orElseThrow(NoSuchJackEntityException::new);
		final Optional<CourseOffer> offerForSubmission = courseRecord.getCourseOffer();

		if (!courseRecord.isTestSubmission() && !offerForSubmission.isPresent()) {
			throw new IllegalStateException("Non-testing " + courseRecord + " should be linked to a course offer!");
		}

		if (offerForSubmission.isPresent()) {
			// Non-testing record, the user needs grade rights for the
			// course offer
			return hasSpecificRightOnFolder(user, offerForSubmission.get().getFolder(),
					AccessRight::isGrade);

		}
		// Testing records, read right on the course is sufficient
		// Finding the course by ID via "getProxiedOrRegularCourseId" works both for frozen and not-frozen
		// exercises
		AbstractCourse course = baseService
				.findById(AbstractCourse.class, courseRecord.getCourse().getRealCourseId(), false)
				.orElseThrow(NoSuchJackEntityException::new);
		ContentFolder folder = folderBusiness.getFolderForAbstractCourse(course);
		return hasSpecificRightOnFolder(user, folder, AccessRight::isRead);
	}

	/**
	 * true if non-testing records are accessed from the correct-courseoffer
	 *
	 * @param courseRecord
	 *            courseRecord
	 * @param accessPath
	 *            offer from which the record is accessed
	 * @return
	 */
	private boolean accessCourseRecordCorrectly(CourseRecord courseRecord,
			@CheckForNull CourseOffer accessPath) {
		courseRecord = baseService.findById(CourseRecord.class, courseRecord.getId(), false)
				.orElseThrow(NoSuchJackEntityException::new);
		final Optional<CourseOffer> offerForSubmission = courseRecord.getCourseOffer();

		if (!courseRecord.isTestSubmission() && !offerForSubmission.isPresent()) {
			throw new IllegalStateException("Non-testing " + courseRecord + " should be linked to a course offer!");
		}
		if (offerForSubmission.isPresent()) {
			//non-testing record
			return Objects.equals(accessPath, offerForSubmission.get());
		}
		return true;
	}

	/**
	 * <p>
	 * A manual feedback for a testsubmission can be only given if the user has specific rights on the corresponding
	 * exercise / course record of the submission. We distinguish between the following cases:
	 * </p>
	 * <ul>
	 * <li>The submission is a testing submission and therefore it is not linked to a course offer. In this case, manual
	 * feedback can be given in case of simple read rights for the exercise.</li>
	 * <li>The submission is not a testing submission and therefore it is linked to a course offer. In this case, manual
	 * feedback can be only given in case of GRADE rights for the course offer and if the user accesses the submission
	 * from the right course offer.</li>
	 * </ul>
	 *
	 * @param user
	 *            Which user requests the action.
	 * @param submission
	 *            For which exercise submission the user wants to give manual feedback
	 * @param accessPath
	 *            From which course offer the user requests the action or <code>null</code> if the user does not come
	 *            from a course offer.
	 */
	public boolean isAllowedToGiveManualFeedback(User user, Submission submission,
			@CheckForNull CourseOffer accessPath) {
		submission = exerciseBusiness.getSubmissionWithoutLazyData(submission);
		final Optional<CourseOffer> offerForSubmission = submission.getCourseOffer();

		if (!submission.isTestSubmission() && !offerForSubmission.isPresent()) {
			throw new IllegalStateException("Non-testing " + submission + " should be linked to a course offer!");
		}

		if (offerForSubmission.isPresent()) {
			// Non-testing submission, the user must access the submission via the course offer and needs grade rights
			// for the course offer
			boolean correctAccessPath = Objects.equals(accessPath, offerForSubmission.get());
			boolean rightsForCourseOffer = hasSpecificRightOnFolder(user, offerForSubmission.get().getFolder(),
					AccessRight::isGrade);
			return correctAccessPath && rightsForCourseOffer;

		}
		// Testing submission, read right on the exercise is sufficient
		// Finding the exercise by ID via "getProxiedOrRegularExerciseId" works both for frozen and not-frozen
		// exercises
		Exercise exercise = baseService
				.findById(Exercise.class, submission.getExercise().getProxiedOrRegularExerciseId(), false)
				.orElseThrow(NoSuchJackEntityException::new);
		return hasSpecificRightOnFolder(user, exercise.getFolder(), AccessRight::isRead);
	}

	/**
	 * The grade right is given for a) testing submissions when the user has at least read right for the corresponding
	 * exercise b) non-testing submissions when the user has grade rights for the corresponding course offer
	 */
	public boolean hasGradeRightOnSubmission(User user, Submission submission) {
		submission = exerciseBusiness.getSubmissionWithoutLazyData(submission);
		final Optional<CourseOffer> offerForSubmission = submission.getCourseOffer();

		if (!submission.isTestSubmission() && !offerForSubmission.isPresent()) {
			throw new IllegalStateException("Non-testing " + submission + " should be linked to a course offer!");
		}

		if (offerForSubmission.isPresent()) {
			// Non-testing submission, the user needs grade rights
			// for the course offer
			return hasSpecificRightOnFolder(user, offerForSubmission.get().getFolder(),
					AccessRight::isGrade);

		}
		// Testing submission, read right on the exercise is sufficient
		// Finding the exercise by ID via "getProxiedOrRegularExerciseId" works both for frozen and not-frozen
		// exercises
		Exercise exercise = baseService
				.findById(Exercise.class, submission.getExercise().getProxiedOrRegularExerciseId(), false)
				.orElseThrow(NoSuchJackEntityException::new);
		return hasSpecificRightOnFolder(user, exercise.getFolder(), AccessRight::isRead);
	}

	/**
	 * User accessed the submission correctly.
	 *
	 * Student Submissions must be accessed from the corresponding courseoffer.
	 * Testsubmissions for a course must be accessed from the course.
	 * Testsubmissions for an exercise must be accessed from the exercise.
	 *
	 * @return
	 */
	private boolean accessSubmissionCorrectly(Submission submission, @CheckForNull CourseOffer accessedOffer, Course accessedCourse,
			AbstractExercise accessedExercise) {
		submission = exerciseBusiness.getSubmissionWithoutLazyData(submission);

		final Optional<CourseOffer> offerForSubmission = submission.getCourseOffer();

		if (!submission.isTestSubmission() && !offerForSubmission.isPresent()) {
			throw new IllegalStateException("Non-testing " + submission + " should be linked to a course offer!");
		}

		if (offerForSubmission.isPresent()) {
			// studentsubmission and accessed from courseoffer
			return Objects.equals(accessedOffer, offerForSubmission.get());
		}
		if (submission.getCourseRecord() != null) {
			// testsubmission for course and accessed from course
			return Objects.equals(accessedCourse, submission.getCourseRecord().getCourse());
		}
		// testsubmission for exercise and accessed from exercise
		return Objects.equals(accessedExercise, submission.getExercise());
	}

	/**
	 * This utility method simplifies the right-check for folders. Example:
	 *
	 * <pre>
	 * hasSpecificRightOnFolder(user, folder, AccessRight::isRead);
	 * </pre>
	 */
	private boolean hasSpecificRightOnFolder(final User user, Folder folder, final Predicate<AccessRight> rightCheck) {
		final AccessRight rightForUser = getMaximumRightForUser(user, folder);
		return rightCheck.test(rightForUser);
	}

	/**
	 * Extracts a course offer from a <strong>non-testing</strong> submission and loads a "fresh" copy.
	 *
	 * @param submission
	 *            The submission that is linked to a course offer.
	 * @return The course offer from database.
	 * @throws IllegalStateException
	 *             if no course offer was found.
	 * @throws NoSuchJackEntityException
	 *             if a course offer was found for the submission but it does not exist in the database anymore.
	 */
	@Nonnull
	private CourseOffer loadCourseOfferForNonTestingSubmission(TestableSubmission submission) {
		if (submission.isTestSubmission()) {
			throw new IllegalArgumentException(submission + " is a test submission!");
		}

		// Extract the course offer
		CourseOffer courseOffer = submission.getCourseOffer().orElseThrow(() -> new IllegalStateException(
				submission + " is not a test submission and should be linked to a valid course offer!"));
		return courseBusiness.getCourseOfferWithLazyDataByCourseOfferID(courseOffer.getId())
				.orElseThrow(NoSuchJackEntityException::new);
	}

	// #########################################################################
	// Check for student rights on course offers
	// #########################################################################

	/**
	 * Checks how a user can see a course offer: If the user has rights on the course offer's folder, these rights are
	 * granted. Otherwise the conditions for students to visit the course offer are checked.
	 *
	 * @see EnrollmentBusiness#isCourseOfferVisibleForStudent(User, CourseOffer)
	 */
	@Nonnull
	public ECourseOfferAccess getCourseOfferVisibilityForUser(User user, CourseOffer courseOffer) {
		final PresentationFolder parent = courseOffer.getFolder();

		// Lecturer with edit rights on the parent folder can edit course offer
		if (isAllowedToEditFolder(user, parent)) {
			return ECourseOfferAccess.EDIT;
		}

		// Lecturer with read rights on the parent folder can see course offer
		if (isAllowedToReadFromFolder(user, parent)) {
			return ECourseOfferAccess.READ;
		}

		// Student without rights on the parent folder can see the course offer if the conditions are met
		if (enrollmentBusiness.isCourseOfferVisibleForStudent(user, courseOffer)) {
			return ECourseOfferAccess.SEE_AS_STUDENT;
		}

		return ECourseOfferAccess.NONE;
	}

	// REVIEW kk: this is incorrect. Edit right has nothing to do with being allowed to see extended course records statistics
	public boolean isAllowedToSeeExtendedCourseRecordStatistics(User user) {
		return user.isHasEditRights();
	}

	/**
	 * User has GRADE-Rights and accessed the submission correctly.
	 *
	 * Student Submissions must be accessed from the corresponding courseoffer.
	 * Testsubmissions for a course must be accessed from the course.
	 * Testsubmissions for an exercise must be accessed from the exercise.
	 *
	 */
	public boolean isAllowedToDeleteSubmission(User currentUser, Submission submission, CourseOffer accessedOffer,
			Course accessedCourse, AbstractExercise accessedExercise) {
		return accessSubmissionCorrectly(submission, accessedOffer, accessedCourse, accessedExercise)
				&& hasGradeRightOnSubmission(currentUser, submission);
	}

	/**
	 * Returns an AccessRight instance representing the union of all rights the given user has on the given folder.
	 * This union includes all rights the user has directly on this folder, all rights the user has inherited from
	 * parent folders, and all rights the user has because of group memberships.
	 *
	 * @param user
	 *            User, for whom the Right should be determined
	 * @param folder
	 *            Folder, for which the Right should be determined
	 * @return AccessRight
	 *         always not null
	 */
	@Nonnull
	public AccessRight getMaximumRightForUser(User user, final Folder folder) {
		return getMaximumRightForUser(user, folder, userBusiness.getUserGroupsForUser(user));
	}

	/**
	 * Returns an access right instance representing the union of all rights the given user has on the given folder.
	 * This union includes all rights the user has directly on this folder, all rights the user has inherited from
	 * parent folders, and all rights the user has because of group memberships.
	 *
	 * @param user TODO
	 * @param folder TODO
	 * @param userGroups TODO
	 */
	@Nonnull
	public AccessRight getMaximumRightForUser(User user, final Folder folder, final List<UserGroup> userGroups) {
		if (folder == null) {
			throw new IllegalArgumentException("Folder must not be null!");
		}

		if (user == null) {
			throw new IllegalArgumentException("User must not be null!");
		}

		Folder folderWithManagingRights = folderService.getFolderWithManagingRights(folder)
				.orElseThrow(() -> new PreconditionException("The Folder doesn't exist anymore: " + folder));

		// First, we fetch all rights the user themselves has on this folder
		// (either directly or inherited from parent folders).
		AccessRight maximumRight = getAllManagingUsers(folderWithManagingRights)
				.getOrDefault(user, AccessRight.getNone());

		// Second, we fetch the user groups the user is member of and select
		// those with rights on the folder.
		final List<UserGroup> groups = new ArrayList<>(userGroups);
		groups.retainAll(getAllManagingUserGroups(folderWithManagingRights).keySet());

		// Third, we loop over these groups and add the rights found via groups
		// to the ones found in the first step.
		for (final UserGroup g : groups) {
			// This works also if the user has no own rights and thus the first step returned NONE
			maximumRight = maximumRight.add(getAllManagingUserGroups(folderWithManagingRights).get(g));
		}

		return maximumRight;
	}

	/**
	 * Determines a Map of Pairs (ContentFolder, EAccessRight) for a given User and a given List of Folders.
	 *
	 * @param user
	 *            The user, for whom the EAccessRight should be determined
	 * @param folderList
	 *            a List of Folders <b>without</b> duplicates, for which the EAccessRight should be determined
	 * @param userGroups
	 *            a List containing Groups, the User is Member of
	 * @return Map of Pairs (ContentFolder, EAccessRight), EAccessright is never null
	 */
	public Map<Folder, AccessRight> getMaximumRightForUser(User user, List<Folder> folderList,
			List<UserGroup> userGroups) {
		Map<Folder, AccessRight> returnMap = new HashMap<>();
		folderList.forEach(folder -> returnMap.put(folder, getMaximumRightForUser(user, folder, userGroups)));
		return returnMap;
	}

	/**
	 * Determines a Map of Pairs (ContentFolder, EAccessRight) for a given User and a given List of Folders.
	 *
	 * @param user
	 *            The user, for whom the EAccessRight should be determined
	 * @param folderList
	 *            a List of Folders <b>without</b> duplicates, for which the EAccessRight should be determined
	 * @return Map of Pairs (ContentFolder, EAccessRight), EAccessright is never null
	 */
	public Map<Folder, AccessRight> getMaximumRightForUser(User user, List<Folder> folderList) {
		return getMaximumRightForUser(user,folderList,userBusiness.getUserGroupsForUser(user));
	}


	/**
	 *
	 * @param eagerLoadMembersAndGroups
	 *            if set to true, we can avoid a LazyInitializationException here for ug.getMemberUsers() and
	 *            ug.getMemberGroups(), see #135
	 * @param folder
	 *            for which the list of user rights should be fetched
	 * @return a map of all users including their rights on this folder This holds for inherited rights as well as for
	 *         all users that are member of a user group that is allowed to manage the folder
	 */
	public Map<User, AccessRight> getCompleteManagingUsersMap(boolean eagerLoadMembersAndGroups, Folder folder) {

		// Start with a list of all users who manage this folder (directly and inherited)
		final Map<User, AccessRight> resultMap = getAllManagingUsers(folder);

		final TreeMap<UserGroup, AccessRight> userGroupsToProcess = new TreeMap<>(getAllManagingUserGroups(folder));
		// Walk through all the user groups that should be processed
		// NOTE: This algorithm does NOT work with a for-each-loop because during the loop, "userGroupsToProcess" is
		// changed.
		Entry<UserGroup, AccessRight> currentPair;
		while ((currentPair = userGroupsToProcess.pollFirstEntry()) != null) {

			UserGroup userGroup = currentPair.getKey();
			final AccessRight right = currentPair.getValue() == null ? AccessRight.getNone() : currentPair.getValue();

			// if set to true, we can avoid a LazyInitializationException here for userGroup.getMemberUsers() and
			// userGroup.getMemberGroups(), see #135
			if (eagerLoadMembersAndGroups) {
				userGroup = userGroupService.getUserGroupWithLazyData(userGroup);
			}

			// All direct member users of the current user group have at least the right of the user group, combined
			// with the right already stored in the result map
			for (final User memberUser : userGroup.getMemberUsers()) {
				final AccessRight oldRightForUser = resultMap.getOrDefault(memberUser, AccessRight.getNone());
				resultMap.put(memberUser, right.add(oldRightForUser));
			}

			// All member user groups should be processed again because they contain other users and user groups
			for (final UserGroup memberGroup : userGroup.getMemberGroups()) {
				final AccessRight oldRightForGroup = userGroupsToProcess.getOrDefault(memberGroup,
						AccessRight.getNone());
				userGroupsToProcess.put(memberGroup, right.add(oldRightForGroup));
			}
		}

		return resultMap;
	}

	public Map<User, AccessRight> getAllManagingUsers(Folder folder) {
		final Map<User, AccessRight> result = new HashMap<>(folder.getManagingUsers());
		for (final Entry<User, AccessRight> e : folder.getInheritedManagingUsers().entrySet()) {
			User user = e.getKey();
			AccessRight accessRight = e.getValue();
			if (result.containsKey(user)) {
				result.put(user, result.get(user).add(accessRight));
			} else {
				result.put(user, accessRight);
			}
		}
		return result;
	}

	public Map<UserGroup, AccessRight> getAllManagingUserGroups(Folder folder) {
		final Map<UserGroup, AccessRight> result = new HashMap<>(folder.getManagingUserGroups());
		for (final Entry<UserGroup, AccessRight> e : folder.getInheritedManagingUserGroups().entrySet()) {
			UserGroup userGroup = e.getKey();
			AccessRight accessRight = e.getValue();
			if (result.containsKey(userGroup)) {
				result.put(userGroup, result.get(userGroup).add(accessRight));
			} else {
				result.put(userGroup, accessRight);
			}
		}
		return result;

	}

	public boolean isAllowedToTestExercise(User user, Exercise exercise) {
		return isAllowedToReadFromFolder(user, exercise.getFolder());
	}

	public boolean isAllowedToTestCourse(User user, Course course) {
		return isAllowedToReadFromFolder(user, course.getFolder());
	}

	public boolean isCourseOfferVisibleForUserAsStudent(User user, CourseOffer offer) {
		return enrollmentBusiness.isCourseOfferVisibleForStudent(user, offer);
	}

}
