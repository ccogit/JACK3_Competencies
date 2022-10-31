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

import org.primefaces.model.menu.DefaultMenuItem;

import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.CoursePlayerBusiness;
import de.uni_due.s3.jack3.business.EnrollmentBusiness;
import de.uni_due.s3.jack3.business.exceptions.NotInteractableException;
import de.uni_due.s3.jack3.business.exceptions.PasswordRequiredException;
import de.uni_due.s3.jack3.business.exceptions.SubmissionException;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;

@Named
@ViewScoped
public class ShowCourseRecordView extends AbstractView implements Serializable {

	private static final long serialVersionUID = -5859537182023247849L;

	@Inject
	private CoursePlayerView coursePlayer;

	private long courseOfferId;
	private long exerciseQuickId;
	private CourseOffer courseOffer;
	private long courseRecordId;
	private CourseRecord courseRecord;
	private List<CourseRecord> closedCourseRecords;

	private boolean authenticated = false;
	private boolean recordClosed = false;
	private String personalPassword;
	private boolean personalPasswordRequired;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private CoursePlayerBusiness coursePlayerBusiness;

	@Inject
	private EnrollmentBusiness enrollmentBusiness;

	@Inject
	private UserSession session;

	public void initView() throws IOException {
		if (courseRecordId == 0) {
			sendErrorResponse(400, "CourseRecordId is missing.");
			return;
		}
		try {
			courseRecord = courseBusiness.getCourseRecordById(courseRecordId);
		} catch (NoSuchJackEntityException e) {
			sendErrorResponse(400, "CourseRecord with given courseRecordId does not exist in database");
			return;
		}

		Optional<CourseOffer> offer = courseRecord.getCourseOffer();
		if (!offer.isPresent()) {
			sendErrorResponse(400, "Invalid course offer to course record");
			return;
		}

		courseOfferId = offer.get().getId();
		courseOffer = courseBusiness.getCourseOfferWithLazyDataByCourseOfferID(courseOfferId)
									.orElseThrow(AssertionError::new);

		personalPasswordRequired = enrollmentBusiness.isPersonalPasswordRequired(courseOffer);

		// Security check to avoid access to course records by guessing their id
		if (!courseRecord.getUser().equals(getCurrentUser())) {
			sendErrorResponse(403, "Invalid access to CourseRecord.");
			return;
		}

		recordClosed = courseRecord.isClosed();
		if (!recordClosed) {
			// We only do further initialization if the record is not yet closed
			courseRecord.setLastVisit(LocalDateTime.now());
			courseRecord = coursePlayerBusiness.updateCourseRecord(courseRecord);
			coursePlayer.setCourseRecord(courseRecord);
			coursePlayer.initForCourseOffer(courseOffer);
			if (exerciseQuickId != 0) {
				//redirect to the given exercise
				coursePlayer.setCurrentExercise(exerciseQuickId);
			}
			else if (courseRecord.getCurrentExerciseId() != 0) {
				// FIXME: If a user continues a course offer where meanwhile the exercise has changed to a new
				// frozen revision, we get an exception here
				coursePlayer.setCurrentExercise(courseRecord.getCurrentExerciseId());
			}

			final String storedPassword = session.getPasswordForCourseOffer(courseOffer);
			authenticated = enrollmentBusiness.isPersonalPasswordCorrect(getCurrentUser(), courseOffer,
					storedPassword);
		}
	}

	public void updateBreadCrumb() {
		createYouAreHereModelForCourseOffer(courseOffer);
		final DefaultMenuItem currentSite = DefaultMenuItem.builder()
				.value(getLocalizedMessage("global.currentCourseRecord"))
				.outcome(viewId.getCourseRecordView().withParam(courseRecord).toOutcome())
				.build();
		addYouAreHereModelMenuEntry(currentSite);
	}

	public void confirmPersonalPassword() {
		authenticated = enrollmentBusiness.isPersonalPasswordCorrect(getCurrentUser(), courseOffer, personalPassword);
		if (authenticated) {
			session.addPasswordForCourseOffer(courseOffer, personalPassword);
		} else {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exception.SubmissionException.PASSWORD_WRONG");
		}
		personalPassword = "";
	}

	public long getCourseOfferId() {
		return courseOfferId;
	}

	public void setCourseOfferId(long courseOfferId) {
		this.courseOfferId = courseOfferId;
	}

	public long getCourseRecordId() {
		return courseRecordId;
	}

	public void setCourseRecordId(long courseRecordId) {
		this.courseRecordId = courseRecordId;
	}

	public List<CourseRecord> getClosedCourseRecords() {
		return closedCourseRecords;
	}

	public void setClosedCourseRecords(List<CourseRecord> closedCourseRecords) {
		this.closedCourseRecords = closedCourseRecords;
	}

	public CourseRecord getCourseRecord() {
		return courseRecord;
	}

	public void setCourseRecord(CourseRecord courseRecord) {
		this.courseRecord = courseRecord;
	}

	public void resumeCourseRecord(long courseRecordId) {
		courseRecord = courseBusiness.getCourseRecordById(courseRecordId);
		coursePlayer.setCourseRecord(courseRecord);
	}

	public boolean isAllowSubmissionRestart() {
		try {
			enrollmentBusiness.checkRestartCoursePermission(getCurrentUser(), courseOffer);
		} catch (SubmissionException | NotInteractableException e) {
			return false;
		}
		return true;
	}

	public void restartSubmission() throws IOException {
		try {
			if (personalPasswordRequired) {
				CourseRecord newRecord = enrollmentBusiness.restartCourse(getCurrentUser(), courseOffer,
						personalPassword);
				// Redirect to the new course record
				redirect(viewId.getCourseRecordView().withParam(newRecord));
			} else {
				CourseRecord newRecord = enrollmentBusiness.restartCourse(getCurrentUser(), courseOffer);
				// Redirect to the new course record
				redirect(viewId.getCourseRecordView().withParam(newRecord));
			}
		} catch (SubmissionException e) {
			switch (e.getType()) {
			case ALREADY_PARTICIPATED:
				// Course offer does not allow restart
				addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "exception.actionNotAllowed",
						"showCourseRecord.restartSubmissionError.alreadyParticipated");
				break;
			case SUBMISSION_DEADLINE_ELAPSED:
			case SUBMISSION_NOT_STARTED:
				// Not within submission period
				addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "exception.actionNotAllowed",
						"showCourseRecord.restartSubmissionError.submissionTime");
				break;
			case PASSWORD_WRONG:
				// Wrong password was given
				addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null,
						"exception.SubmissionException.PASSWORD_WRONG");
				break;
			default:
				addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "exception.actionNotAllowed", null);
				break;
			}

		} catch (NotInteractableException e) {
			// The course offer is not visible
			redirect(viewId.getAvailableCourses());
		} catch (PasswordRequiredException e) {
			// The UI holds an outdated state of the course offer, where the course offer does not require a password,
			// but now it requires a password
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exception.PasswordRequiredException");
			personalPasswordRequired = true;
		} finally {
			personalPassword = "";
		}
	}

	public void exitSubmission() throws IOException {
		final CourseRecord record = enrollmentBusiness.closeSubmissionManually(courseRecord);
		final Optional<CourseOffer> offer = record.getCourseOffer();
		if (offer.isPresent()) {
			redirect(viewId.getCourseMainMenu().withParam(offer.get()).withParam("redirect", false));
		} else {
			// No course offer for this course record
			redirect(viewId.getAvailableCourses());
		}

	}

	public CourseOffer getCourseOffer() {
		return courseOffer;
	}

	public void setCourseOffer(CourseOffer courseOffer) {
		this.courseOffer = courseOffer;
	}

	public boolean isAuthenticated() {
		return authenticated;
	}

	public boolean isRecordClosed() {
		return recordClosed;
	}

	public boolean isShowRecord() {
		return authenticated && !recordClosed;
	}

	public String getPersonalPassword() {
		return personalPassword;
	}

	public void setPersonalPassword(String personalPassword) {
		this.personalPassword = personalPassword;
	}

	public boolean isPersonalPasswordRequired() {
		return personalPasswordRequired;
	}

	public String getExitConfirmMessage() {
		Optional<CourseOffer> offer = courseRecord.getCourseOffer()
				.flatMap(found -> courseBusiness.getCourseOfferById(found.getId()));
		if (offer.isPresent() && offer.get().isOnlyOneParticipation()) {
			return getLocalizedMessage("showCourseRecord.exitSubmissionDialog.info") + " "
					+ getLocalizedMessage("showCourseRecord.exitSubmissionDialog.noRestart");
		}
		return getLocalizedMessage("showCourseRecord.exitSubmissionDialog.info");
	}

	public long getMaybeExerciseId() {
		return exerciseQuickId;
	}

	public void setMaybeExerciseId(long maybeExerciseId) {
		exerciseQuickId = maybeExerciseId;
	}
}
