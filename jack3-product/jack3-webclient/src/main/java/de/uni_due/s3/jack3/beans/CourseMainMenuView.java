package de.uni_due.s3.jack3.beans;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;

import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.EnrollmentBusiness;
import de.uni_due.s3.jack3.business.exceptions.EnrollmentException;
import de.uni_due.s3.jack3.business.exceptions.EnrollmentException.EType;
import de.uni_due.s3.jack3.business.exceptions.LinkedCourseException;
import de.uni_due.s3.jack3.business.exceptions.NotInteractableException;
import de.uni_due.s3.jack3.business.exceptions.PasswordRequiredException;
import de.uni_due.s3.jack3.business.exceptions.SubmissionException;
import de.uni_due.s3.jack3.business.helpers.ECourseOfferAccess;
import de.uni_due.s3.jack3.business.helpers.EnrollmentLogEntry;
import de.uni_due.s3.jack3.entities.enums.EEnrollmentStatus;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.Enrollment;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.utils.JackStringUtils;

@Named
@ViewScoped
public class CourseMainMenuView extends AbstractView implements Serializable {

	private static final long serialVersionUID = -7201913072724787487L;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private EnrollmentBusiness enrollmentBusiness;

	@Inject
	private AuthorizationBusiness authBusiness;

	@Inject
	private UserSession session;

	private long courseOfferId;
	private boolean redirectEnabled = true;
	private long exerciseQuickId;
	private CourseOffer courseOffer;

	// Available features
	private boolean allowEnroll;
	private boolean allowDisenroll;
	private boolean allowJoinWaitingList;
	private boolean allowStart;
	private boolean allowContinue;
	private String submissionStartErrorMsg;
	private boolean interactable;

	// User input
	private boolean globalPasswordRequired;
	private String globalPassword;
	private boolean personalPasswordRequired;
	private String personalPassword;

	// User state
	private Optional<CourseRecord> openCourseRecord;
	private List<CourseRecord> oldCourseRecords;
	private Optional<Long> freePlaces;
	private Optional<Enrollment> enrollment;

	// Cached values
	private LocalDateTime submissionDeadline;

	// Controls the enrollment log dialog
	private List<EnrollmentLogEntry> enrollmentLog;

	//Cache Exceptions for messages
	String linkedCourseName;
	EType enrollmentExceptionType;
	boolean disenrollNotAlllowed;


	/**
	 * Not annotated with @PostConstruct because we need to initialize params
	 * https://stackoverflow.com/a/2451308
	 * Don't localize messages in this method, because the locale is not set correctly when the method is called
	 *
	 * @throws IOException
	 */
	public final void init() throws IOException {
		Optional<CourseOffer> foundOffer = courseBusiness.getCourseOfferWithLazyDataByCourseOfferID(courseOfferId);
		if (!foundOffer.isPresent()) {
			sendErrorResponse(400, "CourseOffer with the given ID does not exist");
			return;
		}
		courseOffer = foundOffer.get();

		if (authBusiness.getCourseOfferVisibilityForUser(getCurrentUser(), courseOffer) == ECourseOfferAccess.NONE) {
			sendErrorResponse(403, "User is not allowed to see this course offer");
			return;
		}

		createYouAreHereModelForCourseOffer(courseOffer);		if (redirectEnabled) {
			// Check if the main menu is skipped
			try {
				Optional<CourseRecord> redirectedCourseRecord = enrollmentBusiness.performAutomaticRedirect(
						getCurrentUser(),
						courseOffer);
				if (redirectedCourseRecord.isPresent()) {
					redirect(viewId.getCourseRecordView().withParam("exerciseId", exerciseQuickId)
							.withParam(redirectedCourseRecord.get()));
				}
			} catch (NotInteractableException e) {
				// The course offer is not visible anymore
				sendErrorResponse(403, "User is not allowed to see this course offer");
				return;
			}
		}

		updateData();
	}

	private void updateData() {
		// Update enrollment state
		final User user = getCurrentUser();
		openCourseRecord = enrollmentBusiness.getOpenCourseRecord(user, courseOffer);
		oldCourseRecords = enrollmentBusiness.getVisibleOldCourseRecords(user, courseOffer);
		freePlaces = enrollmentBusiness.getFreePlaces(courseOffer);
		enrollment = enrollmentBusiness.getEnrollment(user, courseOffer);

		// Clear user input
		globalPassword = null;
		personalPassword = null;

		// Update cached values
		updateCachedValues();

		// Update allowed actions
		try {
			updateEnrollmentPermission();
			updateDisenrollmentPermission();
			updateStartPermission();
			updateContinuePermission();
			interactable = true;
		} catch (NotInteractableException e) {
			// The student is not allowed to see the course
			interactable = false;
			return;
		}

		// Update required password
		globalPasswordRequired = enrollmentBusiness.isGlobalPasswordRequired(courseOffer);
		personalPasswordRequired = enrollmentBusiness.isPersonalPasswordRequired(courseOffer);
		if (personalPasswordRequired && openCourseRecord.isPresent()) {
			// Don't prompt for personal password if the user is already authenticated for the course offer
			final String storedPassword = session.getPasswordForCourseOffer(courseOffer);
			personalPasswordRequired = !enrollmentBusiness.isPersonalPasswordCorrect(user, courseOffer, storedPassword);
		}
	}

	private void updateCachedValues() {
		// Submission deadline
		submissionDeadline = openCourseRecord.isPresent() ? openCourseRecord.get().getDeadline()
				: courseOffer.getSubmissionDeadline();
	}

	private void updateEnrollmentPermission() throws NotInteractableException {
		allowEnroll = false;
		allowJoinWaitingList = false;
		linkedCourseName = "";
		enrollmentExceptionType = null;
		if (!courseOffer.isExplicitEnrollment() || isEnrolled() || isWaiting()) {
			// We don't need to check the enrollment permission because it is not relevant
			return;
		}

		try {
			enrollmentBusiness.checkEnrollmentPermission(getCurrentUser(), courseOffer);
			allowEnroll = true;
		} catch (LinkedCourseException e) {
			// Enrollment not allowed because already enrolled in a linked course
			linkedCourseName = e.getLinkedCourse().getName();
		} catch (EnrollmentException e) {
			// Enrollment not allowed because of other reason
			enrollmentExceptionType = e.getType();
			if (e.getType() == EType.COURSE_IS_FULL) {
				// Additionally check the waiting list permission (waiting list is only enabled if course is full)
				try {
					enrollmentBusiness.checkWaitingListPermission(getCurrentUser(), courseOffer);
					// Waiting list is enabled
					allowJoinWaitingList = true;
				} catch (EnrollmentException waitingListException) {
					// Waiting list is just disabled.
				}
			}
		}
	}

	private void updateDisenrollmentPermission() {
		allowDisenroll = false;
		disenrollNotAlllowed = false;

		if (!courseOffer.isExplicitEnrollment() || isDisenrolled()) {
			// We don't need to check the enrollment permission because it is not relevant
			return;
		}

		try {
			enrollmentBusiness.checkDisenrollmentPermission(courseOffer);
			allowDisenroll = true;
		} catch (EnrollmentException e) {
			// The only reason for forbidden disenrollment is the disenrollment deadline
			disenrollNotAlllowed = true;

		}
	}

	private void updateStartPermission() throws NotInteractableException {
		allowStart = false;
		submissionStartErrorMsg = null;

		if (openCourseRecord.isPresent() || courseOffer.getCourse() == null) {
			// We don't need to check the submission start permission because it is not relevant
			return;
		}

		try {
			enrollmentBusiness.checkStartSubmissionPermission(getCurrentUser(), courseOffer);
			allowStart = true;
		} catch (SubmissionException e) {
			switch (e.getType()) {
			case ALREADY_PARTICIPATED:
				submissionStartErrorMsg = "courseMainMenu.startDenied.alreadyParticipated";
				break;
			case SUBMISSION_NOT_STARTED:
			case SUBMISSION_DEADLINE_ELAPSED:
				submissionStartErrorMsg = "courseMainMenu.startDenied.outsidesubmissionPeriod";
				break;

			default:
				// General information that submission start is not possible
				submissionStartErrorMsg = "courseMainMenu.startDenied";
				break;
			}
		}
	}

	private void updateContinuePermission() throws NotInteractableException {
		allowContinue = false;

		try {
			enrollmentBusiness.checkContinueCoursePermission(getCurrentUser(), courseOffer);
			allowContinue = true;
		} catch (SubmissionException e) {
			// Continue button is just disabled.
		}
	}

	public void loadEnrollmentLog() {
		enrollmentLog = enrollmentBusiness.getEnrollmentLog(getCurrentUser(), courseOffer);
	}

	// #########################################################################
	// Actions
	// #########################################################################

	public void enroll() throws IOException {

		try {
			Optional<CourseRecord> createdCourseRecord = Optional.empty();
			if (globalPasswordRequired) {
				createdCourseRecord = enrollmentBusiness.enrollUser(getCurrentUser(), courseOffer, globalPassword);
			} else {
				createdCourseRecord = enrollmentBusiness.enrollUser(getCurrentUser(), courseOffer);
			}

			// If an implicit submission was created, we redirect the user to the course record
			if (createdCourseRecord.isPresent()) {
				redirect(viewId.getCourseRecordView().withParam(createdCourseRecord.get()).withParam("exerciseId",
						exerciseQuickId));
			} else {
				// Just update the UI
				updateData();
			}

		} catch (LinkedCourseException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "exception.actionNotAllowed",
					"exception.LinkedCourseException", e.getLinkedCourse().getName());
			updateData();
		} catch (EnrollmentException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "exception.actionNotAllowed",
					"exception.EnrollmentException." + e.getType().name());
			updateData();
		} catch (NotInteractableException e) {
			// The student is not allowed to see the course
			interactable = false;
		} catch (PasswordRequiredException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exception.PasswordRequiredException");
			globalPasswordRequired = true;
		} catch (MessagingException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_WARN,
					"global.information", "exception.EnrollmentException.EMAIL_COULD_NOT_BE_SENT");
			updateData();
		}
	}

	public void disenroll() {
		try {
			enrollmentBusiness.disenrollUser(getCurrentUser(), courseOffer);
			// Update the UI
			updateData();

		} catch (EnrollmentException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "exception.actionNotAllowed",
					"exception.EnrollmentException." + e.getType().name());
			updateData();
		}
	}

	public void joinWaitingList() {
		try {
			if (globalPasswordRequired) {
				enrollmentBusiness.joinWaitingList(getCurrentUser(), courseOffer, globalPassword);
			} else {
				enrollmentBusiness.joinWaitingList(getCurrentUser(), courseOffer);
			}

			// Update the UI
			updateData();

		} catch (LinkedCourseException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "exception.actionNotAllowed",
					"exception.LinkedCourseException", e.getLinkedCourse().getName());
			updateData();
		} catch (EnrollmentException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "exception.actionNotAllowed",
					"exception.EnrollmentException." + e.getType().name());
			updateData();
		} catch (NotInteractableException e) {
			// The student is not allowed to see the course
			interactable = false;
		} catch (PasswordRequiredException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exception.PasswordRequiredException");
			globalPasswordRequired = true;
		} catch (MessagingException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_WARN, "global.information",
					"exception.EnrollmentException.EMAIL_COULD_NOT_BE_SENT");
			updateData();
		}
	}
	
	public void startCourse() throws IOException {
		try {
			if (personalPasswordRequired) {
				CourseRecord record = enrollmentBusiness.startSubmission(getCurrentUser(), courseOffer,
						personalPassword);
				// The user is now authenticated for the course offer
				session.addPasswordForCourseOffer(courseOffer, personalPassword);
				redirect(viewId.getCourseRecordView().withParam(record).withParam("exerciseId", exerciseQuickId));
			} else {
				CourseRecord record = enrollmentBusiness.startSubmission(getCurrentUser(), courseOffer);
				redirect(viewId.getCourseRecordView().withParam(record).withParam("exerciseId", exerciseQuickId));
			}
		} catch (PasswordRequiredException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exception.PasswordRequiredException");
			personalPasswordRequired = true;
		} catch (SubmissionException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "exception.actionNotAllowed",
					"exception.SubmissionException." + e.getType().name());
			updateData();
		} catch (NotInteractableException e) {
			// The student is not allowed to see the course
			interactable = false;
		}
	}
	
	public void continueCourse() throws IOException {
		try {
			final String cachedPassword = session.getPasswordForCourseOffer(courseOffer);
			if (personalPasswordRequired) {
				CourseRecord record = enrollmentBusiness.continueCourse(getCurrentUser(), courseOffer,
						personalPassword);
				// The user is now authenticated for the course offer
				session.addPasswordForCourseOffer(courseOffer, personalPassword);
				redirect(viewId.getCourseRecordView().withParam(record).withParam("exerciseId", exerciseQuickId));
			} else if (cachedPassword != null) {
				// Try to continue the course with the cached password for a previous course authentication
				CourseRecord record = enrollmentBusiness.continueCourse(getCurrentUser(), courseOffer, cachedPassword);
				redirect(viewId.getCourseRecordView().withParam(record).withParam("exerciseId", exerciseQuickId));
			} else {
				CourseRecord record = enrollmentBusiness.continueCourse(getCurrentUser(), courseOffer);
				redirect(viewId.getCourseRecordView().withParam(record).withParam("exerciseId", exerciseQuickId));
			}
		} catch (PasswordRequiredException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exception.PasswordRequiredException");
			personalPasswordRequired = true;
		} catch (SubmissionException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "exception.actionNotAllowed",
					"exception.SubmissionException." + e.getType().name());
			updateData();
		} catch (NotInteractableException e) {
			// The student is not allowed to see the course
			interactable = false;
		}
	}

	// #########################################################################
	// Getters & Setters
	// #########################################################################

	public boolean isGlobalPasswordRequired() {
		return globalPasswordRequired;
	}

	public String getGlobalPassword() {
		return globalPassword;
	}

	public void setGlobalPassword(String password) {
		this.globalPassword = password;
	}

	public boolean isPersonalPasswordRequired() {
		return personalPasswordRequired;
	}

	public String getPersonalPassword() {
		return personalPassword;
	}

	public void setPersonalPassword(String password) {
		this.personalPassword = password;
	}

	public long getCourseOfferId() {
		return courseOfferId;
	}

	public void setCourseOfferId(long courseOfferId) {
		this.courseOfferId = courseOfferId;
	}

	public boolean isRedirectEnabled() {
		return redirectEnabled;
	}

	public void setRedirectEnabled(boolean redirectEnabled) {
		this.redirectEnabled = redirectEnabled;
	}

	public CourseOffer getCourseOffer() {
		return courseOffer;
	}

	public Optional<CourseRecord> getOpenCourseRecord() {
		return openCourseRecord;
	}

	public List<CourseRecord> getOldCourseRecords() {
		return oldCourseRecords;
	}

	public long getFreePlaces() {
		return freePlaces.get(); // NOSONAR
	}

	public boolean hasUnlimitedFreePlaces() {
		return !freePlaces.isPresent();
	}

	public boolean isFull() {
		return freePlaces.isPresent() && freePlaces.get() == 0;
	}

	public Optional<Enrollment> getEnrollment() {
		return enrollment;
	}

	public boolean isEnrolled() {
		return enrollment.isPresent() && enrollment.get().getStatus() == EEnrollmentStatus.ENROLLED;
	}

	public boolean isWaiting() {
		return enrollment.isPresent() && enrollment.get().getStatus() == EEnrollmentStatus.ON_WAITINGLIST;
	}

	public boolean isDisenrolled() {
		return !enrollment.isPresent() || enrollment.get().getStatus() == EEnrollmentStatus.DISENROLLED;
	}

	public boolean isManuallyDisenrolled() {
		return enrollment.isPresent() && enrollment.get().getStatus() == EEnrollmentStatus.DISENROLLED;
	}

	public LocalDateTime getLastEnrollmentChange() {
		return enrollment.map(Enrollment::getLastChange).orElse(null);
	}

	public boolean isAllowEnroll() {
		return allowEnroll;
	}

	public boolean isAllowDisenroll() {
		return allowDisenroll;
	}

	public boolean isAllowJoinWaitingList() {
		return allowJoinWaitingList;
	}

	public boolean isAllowStart() {
		return allowStart;
	}

	public boolean isAllowContinue() {
		return allowContinue;
	}

	public String getEnrollmentErrorMsg() {
		String enrollmentErrorMsg = null;

		if (linkedCourseName != null && !linkedCourseName.isEmpty()) {
			enrollmentErrorMsg = formatLocalizedMessage("courseMainMenu.enrollmentDenied.alreadyInLinkedCourse",
					new Object[] { linkedCourseName });
		} else if (enrollmentExceptionType != null) {
			switch (enrollmentExceptionType) {
			case COURSE_IS_FULL:
				enrollmentErrorMsg = getLocalizedMessage("courseMainMenu.enrollmentDenied.full");

				if (allowJoinWaitingList) {
					// Waiting list is enabled
					enrollmentErrorMsg += " ";
					enrollmentErrorMsg += getLocalizedMessage("courseMainMenu.enrollmentDenied.waitingListEnabled");
				}
				break;
			case ENROLLMENT_NOT_STARTED:
			case ENROLLMENT_DEADLINE_ELAPSED:
				enrollmentErrorMsg = getLocalizedMessage("courseMainMenu.enrollmentDenied.outsideEnrollmentPeriod");
				break;
			default:
				// General information that enrollment is not possible
				enrollmentErrorMsg = getLocalizedMessage("courseMainMenu.enrollmentDenied");
				break;
			}

		}
		return enrollmentErrorMsg;
	}

	public String getDisenrollmentErrorMsg() {
		String disenrollmentErrorMsg = "";
		if (disenrollNotAlllowed) {
			disenrollmentErrorMsg = getLocalizedMessage("courseMainMenu.disenrollmentDenied.afterDeadline");
		}
		return disenrollmentErrorMsg;
	}

	public String getSubmissionStartErrorMsg() {
		if (submissionStartErrorMsg != null) {
			return getLocalizedMessage(submissionStartErrorMsg);
		}
		return "";
	}

	public boolean isInteractable() {
		return interactable;
	}

	public String getAdditionalEnrollmentMsg() {
		// Manual enrollment message

		String additionalEnrollmentMsg = "";
		if (enrollment.isPresent()) {
			if (JackStringUtils.isNotBlank(enrollment.get().getExplanation())) {
				// A lecturer has done the action manually and specified a reason
				additionalEnrollmentMsg = "<br />"
						+ formatLocalizedMessage("courseMainMenu.manuallyReason",
								new Object[] { enrollment.get().getExplanation() })
				;
			} else if (enrollment.get().getLastChangedBy() == null) {
				// The system performed the action automatically
				additionalEnrollmentMsg = getLocalizedMessage("courseMainMenu.automatically");
			} else if (!enrollment.get().getLastChangedBy().equals(getCurrentUser())) {
				// A lecturer has done the action manually and did not specified a reason
				additionalEnrollmentMsg = getLocalizedMessage("courseMainMenu.manually");
			}
		}
		return additionalEnrollmentMsg;
	}

	public boolean isEnrollmentDeadlineOver() {
		return courseOffer.getEnrollmentDeadline() != null
				&& LocalDateTime.now().isAfter(courseOffer.getEnrollmentDeadline());
	}

	public boolean isShowSubmissionForm() {
		return courseOffer.getCourse() != null && (!courseOffer.isExplicitEnrollment() || isEnrolled());
	}

	public LocalDateTime getSubmissionDeadline() {
		return submissionDeadline;
	}

	public String getAdditionalSubmissionMsg(CourseRecord courseRecord) {
		// Manual submission closed message
		String additionalSubmissionMsg = "";

		if (JackStringUtils.isNotBlank(courseRecord.getClosedByLecturerExplanation())) {
				// A lecturer has closed the submission manually and specified a reason
			additionalSubmissionMsg =
					"<br />" + formatLocalizedMessage("courseMainMenu.oldParticipations.manuallyClosedReason",
							new Object[] { courseRecord.getClosedByLecturerExplanation() });
		} else if (courseRecord.isAutomaticallyClosed()) {
				// The system closed the submission automatically
				additionalSubmissionMsg = getLocalizedMessage("courseMainMenu.oldParticipations.automaticallyClosed");
		} else if (courseRecord.getClosedByLecturer().isPresent()) {
				// A lecturer has closed the submission manually
			additionalSubmissionMsg =
					getLocalizedMessage("courseMainMenu.oldParticipations.manuallyClosed");
			}

		return additionalSubmissionMsg;
	}

	public List<EnrollmentLogEntry> getEnrollmentLog() {
		return enrollmentLog;
	}

	public long getMaybeExerciseId() {
		return exerciseQuickId;
	}

	public void setMaybeExerciseId(long maybeExerciseId) {
		this.exerciseQuickId = maybeExerciseId;
	}

}
