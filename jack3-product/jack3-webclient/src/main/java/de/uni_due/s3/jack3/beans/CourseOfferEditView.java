package de.uni_due.s3.jack3.beans;

import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;

import org.primefaces.PrimeFaces;
import org.primefaces.event.SelectEvent;

import de.uni_due.s3.jack3.beans.ViewId.Builder;
import de.uni_due.s3.jack3.beans.data.CourseOfferVisibilityStatus;
import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.ConfigurationBusiness;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.EnrollmentBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.business.exceptions.EnrollmentException;
import de.uni_due.s3.jack3.business.exceptions.LinkedCourseException;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.enums.ECourseOfferReviewMode;
import de.uni_due.s3.jack3.entities.enums.ECourseResultDisplay;
import de.uni_due.s3.jack3.entities.enums.EEnrollmentStatus;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.FrozenCourse;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.exceptions.JackSecurityException;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;
import de.uni_due.s3.jack3.utils.JackStringUtils;

/**
 * This class provides the data for courseOfferEdit.xhtml This class deals with several courses and courseLists: the web
 * interface allows to choose the course for the courseOffer from the list of all available courses for this user. This
 * course list is given through the list 'allCourses'. We make sure that we only add the last revision of each of these
 * courses to this list. Through this approach we make sure that when the user changed the name of the course, he sees
 * the current name in this list. When the user has selected the course from the list of latest revisions by using the
 * corresponding dropdown menu in the view the value of this menu is set to 'referencedCourseLatestRevision'. When this
 * value has been selected we load the list of available revisions of this course into the list
 * 'frozenRevisionsForCurrentCourse' so that the user can choose the particular revision he wants the course offer to
 * point to. The value of the corresponding dropdown menu will be set to the field 'referencedCourseRevision' and when
 * saved the courseOffer will point to this particular course
 *
 * @author nils.schwinning
 *
 */

@ViewScoped
@Named
public class CourseOfferEditView extends AbstractView implements Serializable {

	private static final long serialVersionUID = -2136874388893194432L;
	private static final int NO_PARTICIPANTS_LIMIT = 0;

	private long courseOfferId;

	private CourseOffer courseOffer;

	private List<Course> allCourses;
	private Map<Course, String> courseNames;

	private Course referencedCourse;

	private List<FrozenCourse> frozenRevisionsForCurrentCourse;
	private AbstractCourse referencedFrozenCourse;

	private boolean enrollmentStart;
	private boolean enrollmentDeadline;
	private boolean submissionDeadline;
	private boolean submissionStart;
	private boolean disenrollmentDeadline;
	private boolean visibilityStartTime;
	private boolean visibilityEndTime;

	private String originalCourseOfferName;

	private String userToRegister;
	private EEnrollmentStatus userToRegisterStatus;
	private List<User> allUsers;
	private String manualEnrollExplanation;

	private final List<EnrollmentActionWarning> enrollmentActionWarnings = new ArrayList<>(3);
	private int initialMaxParticipants;
	private boolean initialWaitinglistEnabled;

	/** Determines wether the user can navigate to the referenced course */
	private boolean rightsForCourse;

	/* Hyperlink to the course main menu */
	private boolean autostart = true;
	private String hyperlinkToMainMenu;

	private final CourseOfferVisibilityStatus visibilityStatus = new CourseOfferVisibilityStatus();

	@Inject
	private CourseOfferEditPersonalPasswordsView courseOfferEditPersonalPasswordsView;

	@Inject
	private CourseOfferEditUserFilterView userFilterView;

	@Inject
	private CourseOfferParticipantsView courseOfferParticipantsView;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private AuthorizationBusiness authorizationBusiness;

	@Inject
	private ConfigurationBusiness configurationBusiness;

	@Inject
	private EnrollmentBusiness enrollmentBusiness;

	@Inject
	private PathComponent pathComponent;

	private boolean readOnly;
	private boolean userHasExtendedRead;
	private String mailContext;
	private String mailSubject;

	private boolean hasInheritedLinkedCourses;
	private boolean hasLinkedCoursesChecked;

	/**
	 * Calendar fields on the page
	 */
	private enum ECalendarId {
		// If you add more calendars to courseOfferEdit.xhtml or included files, add them there. This is a mechanism to
		// update all calendar fields via ajax, except one field (see below)
		SUBMISSION_START("courseOfferEdit:courseOfferSubmissionStart"), //
		SUBMISSION_DEADLINE("courseOfferEdit:courseOfferSubmissionDeadline"), //
		ENROLLMENT_START("courseOfferEdit:courseOfferEnrollmentStart"), //
		ENROLLMENT_DEADLINE("courseOfferEdit:courseOfferEnrollmentDeadline"), //
		DISENROLLMENT_DEADLINE("courseOfferEdit:courseOfferDisenrollmentDeadline"), //
		VISIBILITY_START("courseOfferEdit:courseOfferVisibilityStartTime"), //
		VISIBILITY_END("courseOfferEdit:courseOfferVisibilityEndTime");

		private String name;

		private ECalendarId(String name) {
			this.name = name;
		}
	}

	public List<Course> getAllCourses() {
		return allCourses;
	}

	public String getCourseName(Course course) {
		return courseNames.getOrDefault(course, course.getName());
	}

	public CourseOffer getCourseOffer() {
		return courseOffer;
	}

	public CourseOfferEditUserFilterView getUserFilterView() {
		return userFilterView;
	}

	public long getCourseOfferId() {
		return courseOfferId;
	}

	public List<FrozenCourse> getFrozenRevisionsForCurrentCourse() {
		return frozenRevisionsForCurrentCourse;
	}

	public LocalDateTime getMaxEnrollmentStart() {
		// Sign on Start Time is before every other date
		List<LocalDateTime> allDates = new ArrayList<>();
		if (enrollmentDeadline) {
			allDates.add(courseOffer.getEnrollmentDeadline());
		}
		if (submissionStart) {
			allDates.add(courseOffer.getSubmissionStart());
		}
		if (disenrollmentDeadline) {
			allDates.add(courseOffer.getDisenrollmentDeadline());
		}
		if (submissionDeadline) {
			allDates.add(courseOffer.getSubmissionDeadline());
		}
		if (visibilityEndTime) {
			allDates.add(courseOffer.getVisibilityEndTime());
		}
		// Above dates can be null, so filter them out before calling Collections.min
		return allDates.stream().filter(Objects::nonNull).min(LocalDateTime::compareTo).orElse(null);
	}

	public LocalDateTime getMinEnrollmentDeadline() {
		// Access end time is between start time and submission deadline
		if (enrollmentStart) {
			return courseOffer.getEnrollmentStart();
		} else {
			return null;
		}
	}

	public LocalDateTime getMaxEnrollmentDeadline() {
		// Access end time is between start time and submission deadline
		if (submissionDeadline) {
			return courseOffer.getSubmissionDeadline();
		} else if (visibilityEndTime) {
			return courseOffer.getVisibilityEndTime();
		} else {
			return null;
		}
	}

	public LocalDateTime getMinSubmissionDeadline() {
		if (submissionStart) {
			return courseOffer.getSubmissionStart();
		} else if (enrollmentStart) {
			return courseOffer.getEnrollmentStart();
		} else {
			return null;
		}
	}

	public LocalDateTime getMaxSubmissionDeadline() {
		return visibilityEndTime ? courseOffer.getVisibilityEndTime() : null;
	}

	public LocalDateTime getMinSubmissionStart() {
		// Processing Start Time is after start time
		if (enrollmentStart) {
			return courseOffer.getEnrollmentStart();
		} else {
			return null;
		}
	}

	public LocalDateTime getMaxSubmissionStart() {
		// Processing Start Time is after start time
		if (submissionDeadline) {
			return courseOffer.getSubmissionDeadline();
		} else if (visibilityEndTime) {
			return courseOffer.getVisibilityEndTime();
		} else {
			return null;
		}
	}

	public LocalDateTime getMinDisenrollmentDeadline() {
		if (enrollmentStart) {
			return courseOffer.getEnrollmentStart();
		} else {
			return null;
		}
	}

	public LocalDateTime getMinVisibilityStartTime() {
		if (enrollmentStart) {
			return courseOffer.getEnrollmentStart();
		} else {
			return null;
		}
	}

	public LocalDateTime getMaxVisibilityStartTime() {
		if (visibilityEndTime) {
			return courseOffer.getVisibilityEndTime();
		} else {
			return null;
		}
	}

	public LocalDateTime getMinVisibilityEndTime() {
		// Visibility end time is the last date
		Set<LocalDateTime> allValues = new HashSet<>();
		if (enrollmentStart) {
			allValues.add(courseOffer.getEnrollmentStart());
		}
		if (enrollmentDeadline) {
			allValues.add(courseOffer.getEnrollmentDeadline());
		}
		if (submissionStart) {
			allValues.add(courseOffer.getSubmissionStart());
		}
		if (disenrollmentDeadline) {
			allValues.add(courseOffer.getDisenrollmentDeadline());
		}
		if (submissionDeadline) {
			allValues.add(courseOffer.getSubmissionDeadline());
		}
		if (visibilityStartTime) {
			allValues.add(courseOffer.getVisibilityStartTime());
		}

		return allValues.stream().filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null);
	}

	public Course getReferencedCourse() {
		return referencedCourse;
	}

	public AbstractCourse getReferencedFrozenCourse() {
		return referencedFrozenCourse;
	}

	public List<ECourseOfferReviewMode> getReviewTypes() {
		return Arrays.asList(ECourseOfferReviewMode.values());
	}

	public String getUserToRegister() {
		return userToRegister;
	}

	public void setUserToRegister(String userToRegister) {
		this.userToRegister = userToRegister;
	}

	public List<ECourseResultDisplay> getCourseResultDisplayTypes() {
		return Arrays.asList(ECourseResultDisplay.values());
	}

	public void handleEnrollmentDeadline() {
		if (!enrollmentDeadline) {
			courseOffer.setEnrollmentDeadline(null);
		}
	}

	public void handleEnrollmentStart() {
		if (!enrollmentStart) {
			courseOffer.setEnrollmentStart(null);
		}
	}

	public void handleSubmissionDeadline() {
		if (!submissionDeadline) {
			courseOffer.setSubmissionDeadline(null);
		}
	}

	public void handleSubmissionStart() {
		if (!submissionStart) {
			courseOffer.setSubmissionStart(null);
		}
	}

	public void handleDisenrollmentDeadline() {
		if (!disenrollmentDeadline) {
			courseOffer.setDisenrollmentDeadline(null);
		}
	}

	public void handleVisibilityEndTime() {
		if (!visibilityEndTime) {
			courseOffer.setVisibilityEndTime(null);
		}
	}

	public void handleVisibilityStartTime() {
		if (!visibilityStartTime) {
			courseOffer.setVisibilityStartTime(null);
		}
	}

	/**
	 * This method is used to set all the boolean values for the checkboxes of time fields.
	 */
	public void setAllTimeFields() {
		enrollmentStart = courseOffer.getEnrollmentStart() != null;
		enrollmentDeadline = courseOffer.getEnrollmentDeadline() != null;
		submissionDeadline = courseOffer.getSubmissionDeadline() != null;
		submissionStart = courseOffer.getSubmissionStart() != null;
		disenrollmentDeadline = courseOffer.getDisenrollmentDeadline() != null;
		visibilityEndTime = courseOffer.getVisibilityEndTime() != null;
		visibilityStartTime = courseOffer.getVisibilityStartTime() != null;
	}

	public boolean isEnrollmentDeadline() {
		return enrollmentDeadline;
	}

	public boolean isEnrollmentStart() {
		return enrollmentStart;
	}

	public boolean isDisenrollmentDeadline() {
		return disenrollmentDeadline;
	}

	public boolean isSubmissionStart() {
		return submissionStart;
	}

	public boolean isVisibilityEndTime() {
		return visibilityEndTime;
	}

	public boolean isVisibilityStartTime() {
		return visibilityStartTime;
	}

	public void setVisibilityStartTime(boolean visibilityStartTime) {
		this.visibilityStartTime = visibilityStartTime;
	}

	public boolean isSubmissionDeadline() {
		return submissionDeadline;
	}

	public boolean isReviewModeDisabled(ECourseOfferReviewMode reviewMode) {
		return (reviewMode == ECourseOfferReviewMode.AFTER_REVIEW);
	}

	public String getManualEnrollExplanation() {
		return manualEnrollExplanation;
	}

	public void setManualEnrollExplanation(String registerUserExplanation) {
		this.manualEnrollExplanation = registerUserExplanation;
	}

	public List<EnrollmentActionWarning> getEnrollmentActionWarnings() {
		return enrollmentActionWarnings;
	}

	public boolean isRightsForCourse() {
		return rightsForCourse;
	}

	public CourseOfferVisibilityStatus getVisibilityStatus() {
		return visibilityStatus;
	}

	public void loadAccessMode() {
		courseOfferEditPersonalPasswordsView.initialize(courseOffer);
		userFilterView.initialize(courseOffer);
	}

	public void loadCourseOffer() throws IOException {
		try {
			courseOffer = courseBusiness.getCourseOfferById(courseOfferId).orElseThrow(AssertionError::new);
		} catch (AssertionError e) {
			sendErrorResponse(400, "CourseOffer with given CourseOfferId does not exist in database");
			return;
		}

		if (authorizationBusiness.getMaximumRightForUser(getCurrentUser(), courseOffer.getFolder()).isNone()) {
			sendErrorResponse(403, "User is not allowed to see courseOffer");
			return;
		}

		rightsForCourse = !aquireAllCourses();
		updateCourseNames();
		AbstractCourse course = courseOffer.getCourse();

		if ((course != null) && course.isFrozen()) {
			Course newestRevisionOfFrozenCourse = courseBusiness.getNewestRevisionOfFrozenCourse((FrozenCourse) course)
					.orElseThrow(AssertionError::new);
			setReferencedCourse(newestRevisionOfFrozenCourse);
			setReferencedFrozenCourse(course);
		} else {
			setReferencedCourse((Course) course); // course is not frozen
			setReferencedFrozenCourse(null);
		}

		setFrozenRevisionsForCurrentCourse(loadFrozenRevisionsWithLazyData());

		setAllTimeFields();

		loadAccessMode();

		originalCourseOfferName = courseOffer.getName();
		initialMaxParticipants = courseOffer.getMaxAllowedParticipants();
		initialWaitinglistEnabled = courseOffer.isEnableWaitingList();
		updateEnrollmentActionWarning();

		if (!authorizationBusiness.isAllowedToReadFromFolder(getCurrentUser(), courseOffer.getFolder())) {
			sendErrorResponse(403, getLocalizedMessage("courseOfferEdit.forbiddenCourseOffer"));
			return;
		}
		readOnly = !authorizationBusiness.isAllowedToEditFolder(getCurrentUser(), courseOffer.getFolder());
		userHasExtendedRead = authorizationBusiness.hasExtendedReadOnFolder(getCurrentUser(), courseOffer.getFolder());

		updateVisibilityInformation();
		updateHyperlinkToMainMenu();

		hasInheritedLinkedCourses = folderBusiness.hasInheritedLinkedCourses(courseOffer.getFolder());
		hasLinkedCoursesChecked = hasInheritedLinkedCourses || courseOffer.getFolder().isContainsLinkedCourses();
	}

	public void updateBreadCrumb() {
		createYouAreHereModelForCourseOfferEdit(courseOffer);
	}

	private List<FrozenCourse> loadFrozenRevisionsWithLazyData() {
		if (getReferencedCourse() != null) {
			return courseBusiness.getFrozenRevisionsForCourse(getReferencedCourse());
		} else {
			return new ArrayList<>();
		}
	}

	public void onCourseChange() {
		setFrozenRevisionsForCurrentCourse(loadFrozenRevisionsWithLazyData());
		setReferencedFrozenCourse(null); //clear referenced frozen revision to prevent comparing of two revisions from different courses
		forceExplicitEnrollmentWhenNoCourseSelected();
	}

	public void reloadCourseByRedirect() throws IOException {
		redirect(viewId.getCurrent().withParam(CourseOffer.class, courseOfferId));
	}

	public void saveAccessModeConfiguration() {
		// Maybe we need additional save steps for some access modes
	}

	public void saveCourseOffer() {
		if(readOnly) {
			getLogger().warnf(
					"User %s tried to save %s while not having write permission! Users should not be able to even call this function without manipulation of the UI",
					getCurrentUser().getLoginName(), courseOffer);
			return;
		}
		if (referencedFrozenCourse == null) {
			courseOffer.setCourse(getReferencedCourse());
		} else {
			courseOffer.setCourse(getReferencedFrozenCourse());
		}

		rightsForCourse = !aquireAllCourses();
		updateCourseNames();
		saveAccessModeConfiguration();
		forceExplicitEnrollmentWhenNoCourseSelected();
		courseOffer = courseBusiness.updateCourseOffer(courseOffer);
		setAllTimeFields();

		initialMaxParticipants = courseOffer.getMaxAllowedParticipants();
		initialWaitinglistEnabled = courseOffer.isEnableWaitingList();
		enrollmentActionWarnings.clear();

		// Re-initialize the access modes because the courseOffer object stored in the access-mode-specific view beans
		// is not up to date
		loadAccessMode();

		doEnrollmentActionsOnSaving();

		updateVisibilityInformation();
	}

	/**
	 * The following actions are performed:
	 * <ol>
	 * <li>If course is not full, users move up</li>
	 * <li>If waiting list was disabled, waiting users are disenrolled</li>
	 * </ol>
	 */
	private void doEnrollmentActionsOnSaving() {

		/*
		 * NOTE: The order of these operations is important! If both the waitlist is disabled and the participants limit
		 * is increased, first the free places are filled with moved up users, second, all other users are disenrolled.
		 */

		// Possibly let some users move up
		try {
			enrollmentBusiness.moveUpUsersAfterSaving(courseOffer, getCurrentUser());
		} catch (EnrollmentException e) {
			throw new JackSecurityException("Called saveCourseOffer() without edit rights!", e);
		} catch (MessagingException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_WARN, "global.warn",
					"courseOfferEdit.automaticEnrollmentAction.emailFailedOnMoveUp");
		}

		// Possibly disenroll some users
		try {
			enrollmentBusiness.disenrollUsersAfterSaving(courseOffer, getCurrentUser());
		} catch (EnrollmentException e) {
			throw new JackSecurityException("Called saveCourseOffer() without edit rights!", e);
		} catch (MessagingException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_WARN, "global.warn",
					"courseOfferEdit.automaticEnrollmentAction.emailFailedOnDisenrollmentWaitinglist");
		}

		// This is important to show the changed values and because the "enrollmentActionWarnings" uses these values!
		courseOfferParticipantsView.update();
	}

	public void updateVisibilityInformation() {
		visibilityStatus.setFromCourseOffer(courseOffer);
	}

	public void setEnrollmentDeadline(boolean enrollmentDeadline) {
		this.enrollmentDeadline = enrollmentDeadline;
	}

	public void setEnrollmentStart(boolean enrollmentStart) {
		this.enrollmentStart = enrollmentStart;
	}

	public void setAllCourses(List<Course> allCourses) {
		this.allCourses = allCourses;
	}

	public void setCourseOffer(CourseOffer courseOffer) {
		this.courseOffer = courseOffer;
	}

	public void setUserFilterView(CourseOfferEditUserFilterView userFilterView) {
		this.userFilterView = userFilterView;
	}

	public void setCourseOfferId(long courseOfferId) {
		this.courseOfferId = courseOfferId;
	}

	public void setFrozenRevisionsForCurrentCourse(List<FrozenCourse> frozenRevisionsForCurrentCourse) {
		this.frozenRevisionsForCurrentCourse = frozenRevisionsForCurrentCourse;
	}

	public void setReferencedCourse(Course referencedCourse) {
		this.referencedCourse = referencedCourse;
	}

	public void setReferencedFrozenCourse(AbstractCourse referencedFrozenCourse) {
		this.referencedFrozenCourse = referencedFrozenCourse;
	}

	public void setSubmissionDeadline(boolean submissionDeadline) {
		this.submissionDeadline = submissionDeadline;
	}

	public void setSubmissionStart(boolean submissionStart) {
		this.submissionStart = submissionStart;
	}

	public void setVisibilityEndTime(boolean visibilityEndTime) {
		this.visibilityEndTime = visibilityEndTime;
	}

	public void setDisenrollmentDeadline(boolean disenrollmentDeadline) {
		this.disenrollmentDeadline = disenrollmentDeadline;
	}

	public CourseOfferEditPersonalPasswordsView getCourseOfferPersonalPasswordsView() {
		return courseOfferEditPersonalPasswordsView;
	}

	public int getProxiedOrLastPersistedRevisionId() {
		if (referencedFrozenCourse != null) {
			return courseBusiness.getProxiedOrLastPersistedRevisionId(referencedFrozenCourse);
		}
		return courseBusiness.getProxiedOrLastPersistedRevisionId(referencedCourse);
	}

	public int getRevisionIndexForRevisionId(int revisionId) {
		AbstractCourse tmpCourse = referencedCourse;
		if (tmpCourse.isFrozen()) {
			tmpCourse = courseBusiness.getCourseByCourseID(tmpCourse.getRealCourseId());
		}
		return courseBusiness.getRevisionIndexForRevisionId(tmpCourse, revisionId);
	}

	/**
	 * Generate the list {@link #allCourses} from all courses the user has access to. However, the list will
	 * <strong>always</strong> contain the currently referenced course, even if it is not accessable for the user.
	 *
	 * @return <code>true</code> if the currently referenced course had to be added (it is not accessable by the user),
	 *         <code>false</code> otherwise.
	 */
	private boolean aquireAllCourses() {
		allCourses = courseBusiness.getAllCoursesForUser(getCurrentUser());
		final AbstractCourse abstractCourse = courseOffer.getCourse();

		if (abstractCourse == null) {
			return false;
		}

		Course course;
		if(abstractCourse.isFrozen()) {
			course = courseBusiness.getCourseByCourseID(abstractCourse.getRealCourseId());
		} else {
			course = (Course) abstractCourse;
		}

		if (!allCourses.contains(course)) {
			allCourses.add(course);
			return true;
		}
		return false;
	}

	/**
	 * Checks if a course name ocurrs more than once and adds the breadcrumb name if needed.
	 */
	private void updateCourseNames() {

		courseNames = new HashMap<>();

		final Map<String, Long> nameOccurrences = allCourses.stream()
				.collect(Collectors.groupingBy(Course::getName, Collectors.counting()));

		for (final Course course : allCourses) {
			if (nameOccurrences.get(course.getName()) > 1) {
				String breadcrumb = pathComponent.getPathAsString(course);
				courseNames.put(course, breadcrumb);
			}
		}
	}

	public void validateCourseOfferName(FacesContext context, UIComponent component, Object value) {

		final String newValue = (String) value;
		final String oldValue = (String) component.getAttributes().get("oldValue");

		// Name must not be empty
		if ((value == null) || newValue.strip().isEmpty()) {
			throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
					getLocalizedMessage("global.invalidName"),
					getLocalizedMessage("global.invalidName.empty")));
		}

		// Name is always valid if user did not change input.
		if (newValue.equals(oldValue) || newValue.equals(originalCourseOfferName)) {
			return;
		}
	}

	/*
	 * Handles the button click when manually enrolling a user.
	 */
	public void enrollUserManually() {
		if (!userHasExtendedRead) {
			getLogger().warnf(
					"User %s tried to register user in %s while not having write permission! Users should not be able to even call this function without manipulation of the UI.",
					getCurrentUser().getLoginName(), courseOffer);
			return;
		}
		if (userToRegister == null || userToRegister.isBlank()) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.error",
					"courseOfferEdit.manualEnrollment.noValidSelection", userToRegister);
			return;
		}
		Optional<User> user = userBusiness.getUserByName(userToRegister);
		if (!user.isPresent()) {
			// This is not expected to happen, the autocomplete should handle this case
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.error",
					"courseOfferEdit.manualEnrollment.noValidSelection", userToRegister);
			return;
		}
		try {
			enrollmentBusiness.enrollUserManually(user.get(), courseOffer, getCurrentUser(), manualEnrollExplanation);
			addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "global.information",
					"courseOfferEdit.manualEnrollment.success", userToRegister);
		} catch (LinkedCourseException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.error",
					"exception.LinkedCourseExceptionLecturer", e.getLinkedCourse().getName());
		} catch (EnrollmentException e) {
			if (e.getType() == EnrollmentException.EType.ALREADY_ENROLLED) {
				addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.error",
						"exception.EnrollmentException.alreadyEnrolledLecturer");
			} else {
				addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.error", "exception.actionNotAllowed");
			}
		} catch (MessagingException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_WARN, "global.warn",
					"courseOfferParticipants.noMailOnEnrollment");
		} finally {
			manualEnrollExplanation = null;
			userToRegister = null;
			userToRegisterStatus = null;
			courseOfferParticipantsView.update();
		}
	}

	/*
	 * This method generates the List of Users that can be selected to be registered to the course. If you are here
	 * because it's hammering the server too hard, you can change the "delay" value of the <p:autocomplete> in
	 * accessSettings.xhtml.
	 *
	 */
	public List<String> userToRegisterAutoComplete(String query) {
		// if the user isn't allowed to register users, there is no reason to perform the auto completion
		if (!userHasExtendedRead) {
			getLogger().warnf(
					"User %s tried to auto complete for manually registraion of users in %s while not having necessary permission! Users should not be able to even call this function without manipulation of the UI.",
					getCurrentUser().getLoginName(), courseOffer);
			return new ArrayList<>(0);
		}

		// List of all Users will only be generated once when the autocomplete is used.
		if (allUsers == null) {
			allUsers = userBusiness.getAllUsers();

			// Avoid LazyInitializationException
			Folder folder = folderBusiness.getFolderWithManagingRights(courseOffer.getFolder())
					.orElseThrow(() -> new IllegalArgumentException("Invalid folder"));

			// Remove users that have rights on this courseoffer
			Map<User, AccessRight> completeManagingUsersMap = authorizationBusiness.getCompleteManagingUsersMap(true, folder);
			for (Entry<User, AccessRight> managingUsersEntry : completeManagingUsersMap.entrySet()) {
				User currentUser = managingUsersEntry.getKey();
				AccessRight currentUserAccessRight = managingUsersEntry.getValue();
				if (allUsers.contains(currentUser) && currentUserAccessRight != null
						&& !currentUserAccessRight.isNone()) {
					allUsers.remove(currentUser);
				}
			}
		}
		query = query.toLowerCase();
		final List<String> filteredUsers = new ArrayList<>();
		for (final User user : allUsers) {
			if (user.getLoginName().toLowerCase().contains(query)) {
				if (enrollmentBusiness.isEnrolled(user, courseOffer)) {
					String alreadyRegisteredUserEntry = user.getLoginName() + " "
							+ getLocalizedMessage("courseOfferEdit.manualEnrollment.alreadyEnrolled");
					filteredUsers.add(alreadyRegisteredUserEntry);
				} else {
					filteredUsers.add(user.getLoginName());
				}
			}
		}
		return filteredUsers;
	}

	/*
	 * Handles the autocomplete selection.
	 */
	public void autoCompleteSelectHandler(SelectEvent<String> event) {
		// if the user isn't allowed to register users, there is no reason to perform the auto completion
		if (!userHasExtendedRead) {
			getLogger().warnf(
					"User %s tried to auto complete for manually registraion of users in %s while not having necessary permission! Users should not be able to even call this function without manipulation of the UI",
					getCurrentUser().getLoginName(), courseOffer);
			return;
		}

		// get the user from the event and remove the appended already registered suffix if present
		String selectedUserName = event.getObject().replace(
				" " + getLocalizedMessage("courseOfferEdit.manualEnrollment.alreadyEnrolled"), "");
		Optional<User> foundUserToRegister = userBusiness.getUserByName(selectedUserName);
		if (foundUserToRegister.isPresent()) {
			userToRegister = selectedUserName;
			userToRegisterStatus = enrollmentBusiness.getStatus(foundUserToRegister.get(), courseOffer);
		} else {
			userToRegister = null;
			userToRegisterStatus = null;
		}
	}

	public EEnrollmentStatus getUserToRegisterStatus() {
		return userToRegisterStatus;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public boolean isUserHasExtendedRead() {
		return userHasExtendedRead;
	}

	/*
	 * This method will set the default value for dependent enrollment settings, when their dependency is disabled
	 */
	public void validateDependentEnrollmentSettings(FacesContext context, UIComponent component, Object value) {
		if (!courseOffer.isExplicitEnrollment()) {
			courseOffer.setEnrollmentStart(null);
			courseOffer.setEnrollmentDeadline(null);
			courseOffer.setDisenrollmentDeadline(null);
			courseOffer.getFolder().setContainsLinkedCourses(false);
			courseOffer.setMaxAllowedParticipants(0);
			courseOffer.setGlobalPassword(null);
			courseOffer.setEnableWaitingList(false);
		}
		updateEnrollmentActionWarning();
	}

	/*
	 * This method will set the default value for dependent processing settings, when no explicit processing start is
	 * required
	 */
	public void validateDependentSubmissionSettings(FacesContext context, UIComponent component, Object value) {
		if (!courseOffer.isExplicitSubmission()) {
			courseOffer.setSubmissionStart(null);
			courseOffer.setSubmissionDeadline(null);
			courseOffer.setTimeLimit(Duration.ZERO);
			courseOffer.setOnlyOneParticipation(false);
			courseOffer.setEnablePersonalPasswords(false);
		}
	}

	/*
	 * This method is used in a calendar, to update every other calendar. This has to be done, in order for min/max
	 * dates to work
	 */
	public void updateAllOtherCalendars() {
		// get every calendar in use
		List<String> allRenderedCalendarIdStrings = new ArrayList<>();
		for (ECalendarId calendarId : ECalendarId.values()) {
			if (isCalendarRendered(calendarId)) {
				allRenderedCalendarIdStrings.add(calendarId.name);
			}
		}
		// get the calling component and remove it from the list
		UIComponent component = UIComponent.getCurrentComponent(FacesContext.getCurrentInstance());
		allRenderedCalendarIdStrings.remove(component.getClientId());

		// do an ajax update on all calendars, except the calling one
		PrimeFaces.current().ajax().update(allRenderedCalendarIdStrings);
	}

	private boolean isCalendarRendered(ECalendarId id) {
		switch (id) {
		case DISENROLLMENT_DEADLINE:
			return disenrollmentDeadline;
		case ENROLLMENT_DEADLINE:
			return enrollmentDeadline;
		case ENROLLMENT_START:
			return enrollmentStart;
		case SUBMISSION_DEADLINE:
			return submissionDeadline;
		case SUBMISSION_START:
			return submissionStart;
		case VISIBILITY_END:
			return visibilityEndTime;
		case VISIBILITY_START:
			return visibilityStartTime;
		default:
			return false;
		}
	}

	/*
	 * Should a courseOffer have no referenced course, then it has to have explicit enrollment (#503)
	 */
	private void forceExplicitEnrollmentWhenNoCourseSelected() {
		if (referencedCourse == null && !courseOffer.isExplicitEnrollment()) {
			courseOffer.setExplicitEnrollment(true);
		}
	}

	public boolean isExerciseRepetitionLimit() {
		return courseOffer.getMaxSubmissionsPerExercise() > 0;
	}

	public void setExerciseRepetitionLimit(boolean exerciseRepetitionLimit) {
		if (exerciseRepetitionLimit) {
			// Default value: 1 repetition = 2 allowed submissions per exercise
			courseOffer.setMaxSubmissionsPerExercise(2);
		} else {
			// No limitation. In our data model, "0" means "no limit"
			courseOffer.setMaxSubmissionsPerExercise(0);
		}
	}

	public int getMaxRepetitionsPerExercise() {
		return courseOffer.getMaxSubmissionsPerExercise() - 1;
	}

	public void setMaxRepetitionsPerExercise(int maxRepetitionsPerExercise) {
		courseOffer.setMaxSubmissionsPerExercise(maxRepetitionsPerExercise + 1);
	}

	public void clearGlobalPassword() {
		courseOffer.setGlobalPassword(null);
	}

	public boolean isParticipantsLimit() {
		return courseOffer.getMaxAllowedParticipants() > 0;
	}

	public void setParticipantsLimit(boolean participantsLimit) {
		if (participantsLimit) {
			// Default value: 20 participants
			courseOffer.setMaxAllowedParticipants(20);
		} else {
			// No limitation. In our data model, "0" means "no limit"
			courseOffer.setMaxAllowedParticipants(0);
			courseOffer.setEnableWaitingList(false);
		}
	}

	public boolean isHasTimeLimit() {
		return !courseOffer.getTimeLimit().isZero();
	}

	public void setHasTimeLimit(boolean hasTimeLimit) {
		if (hasTimeLimit) {
			// Default value: 90 minutes
			courseOffer.setTimeLimit(Duration.ofMinutes(90));
		} else {
			// No limitation. In our data model, zero time limit means "no limit"
			courseOffer.setTimeLimit(Duration.ZERO);
		}
	}

	public boolean isAutostart() {
		return autostart;
	}

	public void setAutostart(boolean preventRedirection) {
		this.autostart = preventRedirection;
	}

	public void updateHyperlinkToMainMenu() {
		Builder viewIdBuilder = viewId.getCourseMainMenu().withParam(courseOffer).withParam("redirect", autostart);
		hyperlinkToMainMenu = getServerUrl() + viewIdBuilder.toActionUrl();
	}

	public String getHyperlinkToMainMenu() {
		return hyperlinkToMainMenu;
	}

	public void preparepreviewWaitinglistMail() {
		String language = getLanguageForMailDialog();

		final FacesContext facesContext = FacesContext.getCurrentInstance();
		final Application application = facesContext.getApplication();
		final String bundleName = application.getResourceBundle(facesContext, "msg").getBaseBundleName();
		final ResourceBundle bundle = ResourceBundle.getBundle(bundleName, Locale.forLanguageTag(language));
		final String courseOfferName = "'" + courseOffer.getName() + "'";

		//create a link to the CourseOffer
		String link = hyperlinkToMainMenu;

		//get localized subject and content of the mail
		String content = bundle.getString("courseOffer.waitingListEmail.content");

		//Check if there is an waitingListMessage
		String waitingListMessage = "";
		if (courseOffer.isWaitingListEmail() && JackStringUtils.isNotBlank(courseOffer.getWaitingListEmailText())) {
			waitingListMessage = bundle.getString("courseOffer.enrollmentEmail.lecturerMessage")
					+ courseOffer.getWaitingListEmailText();
		}

		mailContext = MessageFormat.format(content, courseOfferName, link, waitingListMessage);
		mailSubject = MessageFormat.format(bundle.getString("courseOffer.waitingListEmail.subject"), courseOfferName);
	}

	public void preparePreviewEnrollmentMail() {
		String language = getLanguageForMailDialog();

		final FacesContext facesContext = FacesContext.getCurrentInstance();
		final Application application = facesContext.getApplication();
		final String bundleName = application.getResourceBundle(facesContext, "msg").getBaseBundleName();
		final ResourceBundle bundle = ResourceBundle.getBundle(bundleName, Locale.forLanguageTag(language));
		final String courseOfferName = "'" + courseOffer.getName() + "'";

		//create a link to the CourseOffer
		//String link = getEmailLinkToCourseOffer(courseOffer);
		String link = hyperlinkToMainMenu;

		//get localized content of the mail
		String content = bundle.getString("courseOffer.enrollmentEmail.content");


		//if there is an additional message for the mail add it to the content
		if (JackStringUtils.isNotBlank(courseOffer.getEnrollmentEmailText())) {
			content = MessageFormat.format(content, courseOfferName, link,
					bundle.getString("courseOffer.enrollmentEmail.lecturerMessage")
							+ courseOffer.getEnrollmentEmailText());
		} else {
			content = MessageFormat.format(content, courseOfferName, link, "");
		}

		mailContext = content;
		mailSubject = MessageFormat.format(bundle.getString("courseOffer.enrollmentEmail.subject"), courseOfferName);
	}

	private String getLanguageForMailDialog() {
		if (courseOffer.getLanguage() != null) {
			return courseOffer.getLanguage();
		} else if (courseOffer.getCourse() != null && courseOffer.getCourse().getLanguage() != null) {
			return courseOffer.getCourse().getLanguage();
		} else {
			return "de";
		}
	}

	public void setMailContext(String mailContext) {
		this.mailContext = mailContext;
	}

	public String getMailContext() {
		return mailContext;
	}

	public void setMailSubject(String mailSubject) {
		this.mailSubject = mailSubject;
	}

	public String getMailSubject() {
		return mailSubject;
	}

	public String getLtiUrl() {
		return getTenantUrl() + LtiServlet.SERVLET_URL;
	}

	public boolean isLtiEnabled() {
		return configurationBusiness.booleanOf(LtiServlet.LTI_CONFIGURATION_KEY);
	}

	public boolean isHasInheritedLinkedCourses() {
		return hasInheritedLinkedCourses;
	}

	public boolean isHasLinkedCoursesChecked() {
		return hasLinkedCoursesChecked;
	}

	public void switchLinkedCourses() {
		try {
			hasLinkedCoursesChecked = folderBusiness.switchLinkedCourses(courseOffer.getFolder());
		} catch (ActionNotAllowedException e) {
			// A folder above was meanwhile linked
			PresentationFolder folder = folderBusiness
					.getPresentationFolderById(courseOffer.getFolder().getId())
					.orElseThrow(NoSuchJackEntityException::new);
			hasLinkedCoursesChecked = true;
			hasInheritedLinkedCourses = true;
			PresentationFolder highestLinkedCourse = folderBusiness
					.getHighestLinkedCourseFolder(folder)
					.orElseThrow(() -> new IllegalStateException("Folder " + folder
							+ " inherited linkedCourse property, but there was no folder with linked courses."));
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "exception.actionNotAllowed",
					"start.linkFolderForbiddenParent", highestLinkedCourse.getName());
		}
	}
	
	public void updateEnrollmentActionWarning() {
		
		// For this algorithm, see the uploaded design in issue #1094
		
		// Old and new values
		final boolean oldWaitlistEnabled = initialWaitinglistEnabled;
		final boolean newWaitlistEnabled = courseOffer.isEnableWaitingList();
		final boolean waitlistChanged = oldWaitlistEnabled != newWaitlistEnabled;
		final int oldMaxParticipants = initialMaxParticipants;
		final boolean oldMaxParticipantsEnabled = oldMaxParticipants != NO_PARTICIPANTS_LIMIT;
		final int newMaxParticipants = courseOffer.getMaxAllowedParticipants();
		final boolean newMaxParticipantsEnabled = newMaxParticipants != NO_PARTICIPANTS_LIMIT;
		final boolean bothMaxParticipantsEnabled = oldMaxParticipantsEnabled && newMaxParticipantsEnabled;
		final boolean maxParticipantsIncreased = bothMaxParticipantsEnabled && oldMaxParticipants < newMaxParticipants;
		final boolean maxParticipantsDecreased = bothMaxParticipantsEnabled && oldMaxParticipants > newMaxParticipants;
		final boolean maxParticipantsChanged = oldMaxParticipants != newMaxParticipants;
		
		// These first conditions don't show a warning because no action will be performed
		if ((!maxParticipantsChanged && !waitlistChanged) || // (1)
				(!maxParticipantsChanged && !oldWaitlistEnabled && newWaitlistEnabled) || // (2)
				(maxParticipantsIncreased && !oldWaitlistEnabled && newWaitlistEnabled)) { // (8)
			// (1) No change
			// (2) Only the waiting list will be enabled
			// (8) Capacity is increased, but there was no waitinglist, so nobody moves up
			enrollmentActionWarnings.clear();
			return;
		}
		
		final long currentUsersEnrolled = courseOfferParticipantsView.getNumberOfEnrolledParticipants();
		final long currentUsersWaiting = courseOfferParticipantsView.getNumberOfWaitinglistParticipants();
		
		// Computed values
		long usersTooMuch = 0;
		long willMoveUp = 0;
		long willBeDisenrolled = 0;

		// Compute usersTooMuch
		if ((!oldMaxParticipantsEnabled && newMaxParticipantsEnabled) || (maxParticipantsDecreased)) {
			// (4) (5) (6) Some users may be too much
			usersTooMuch = Math.max(0, currentUsersEnrolled - newMaxParticipants);
		}

		// Compute willMoveUp
		if (oldMaxParticipantsEnabled && !newMaxParticipantsEnabled) {
			// (10) All users move up
			willMoveUp = currentUsersWaiting;
		} else if (maxParticipantsIncreased) {
			// (7) (9) Users move up until the course is full
			willMoveUp = Math.max(0, Math.min(currentUsersWaiting, newMaxParticipants - currentUsersEnrolled));
		}

		// Compute willBeDisenrolled
		if ((oldWaitlistEnabled && !newWaitlistEnabled) && (newMaxParticipantsEnabled == oldMaxParticipantsEnabled)) {
			// (3) (6) (9) All remaining users on the waitinglist are disenrolled
			willBeDisenrolled = Math.max(0, currentUsersWaiting - willMoveUp);
		}
		
		computeEnrollmentActionWarnings(usersTooMuch, willMoveUp, willBeDisenrolled);
	}
	
	private void computeEnrollmentActionWarnings(long usersTooMuch, long willMoveUp, long willBeDisenrolled) {

		if (usersTooMuch < 0)
			throw new IllegalArgumentException("usersTooMuch negative:" + usersTooMuch);
		if (willMoveUp < 0)
			throw new IllegalArgumentException("willMoveUp negative:" + willMoveUp);
		if (willBeDisenrolled < 0)
			throw new IllegalArgumentException("willBeDisenrolled negative:" + willBeDisenrolled);

		enrollmentActionWarnings.clear();
		if (usersTooMuch + willMoveUp + willBeDisenrolled == 0) {
			return;
		}

		if (usersTooMuch == 1) {
			final String msg = getLocalizedMessage("courseOfferEdit.automaticEnrollmentAction.userTooMuch");
			final String hint = getLocalizedMessage("courseOfferEdit.automaticEnrollmentAction.usersTooMuchHint");
			enrollmentActionWarnings.add(new EnrollmentActionWarning(msg, hint, false));
		} else if (usersTooMuch > 1) {
			final String msg = formatLocalizedMessage("courseOfferEdit.automaticEnrollmentAction.usersTooMuch",
					new Object[] { usersTooMuch });
			final String hint = getLocalizedMessage("courseOfferEdit.automaticEnrollmentAction.usersTooMuchHint");
			enrollmentActionWarnings.add(new EnrollmentActionWarning(msg, hint, false));
		}
		if (willMoveUp == 1) {
			final String msg = getLocalizedMessage("courseOfferEdit.automaticEnrollmentAction.userWillMoveUp");
			enrollmentActionWarnings.add(new EnrollmentActionWarning(msg, false));
		} else if (willMoveUp > 1) {
			final String msg = formatLocalizedMessage("courseOfferEdit.automaticEnrollmentAction.usersWillMoveUp",
					new Object[] { willMoveUp });
			enrollmentActionWarnings.add(new EnrollmentActionWarning(msg, false));
		}
		if (willBeDisenrolled == 1) {
			final String msg = getLocalizedMessage("courseOfferEdit.automaticEnrollmentAction.userWillBeDisenrolled");
			enrollmentActionWarnings.add(new EnrollmentActionWarning(msg, true));
		} else if (willBeDisenrolled > 1) {
			final String msg = formatLocalizedMessage(
					"courseOfferEdit.automaticEnrollmentAction.usersWillBeDisenrolled",
					new Object[] { willBeDisenrolled });
			enrollmentActionWarnings.add(new EnrollmentActionWarning(msg, true));
		}
	}

	/**
	 * Represents a warning for a user (intended to use for the enrollment warnings)
	 */
	public final class EnrollmentActionWarning {
		private final String text;
		private final String hint;
		private final boolean isWarning;

		public EnrollmentActionWarning(String text, String hint, boolean isWarning) {
			this.text = text;
			this.hint = hint;
			this.isWarning = isWarning;
		}

		public EnrollmentActionWarning(String text, boolean isWarning) {
			this.text = text;
			this.hint = null;
			this.isWarning = isWarning;
		}

		public String getText() {
			return text;
		}

		public String getHint() {
			return hint;
		}

		public boolean isWarning() {
			return isWarning;
		}

		@Override
		public String toString() {
			return (isWarning ? "{Warning: " : "{Info: ") + text + '}';
		}
	}

}
