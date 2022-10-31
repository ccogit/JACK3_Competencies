package de.uni_due.s3.jack3.beans;

import java.io.IOException;
import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.MenuModel;

import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.business.helpers.PublicUserName;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.Comment;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;
import de.uni_due.s3.jack3.services.SubmissionService;

@Named
@ViewScoped
public class SubmissionDetailsView extends AbstractView implements Serializable {

	private static final long serialVersionUID = -7096495306955176311L;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private AuthorizationBusiness authorizationBusiness;

	@Inject
	private ExercisePlayerView exercisePlayer;

	@Inject
	private SubmissionService submissionService;

	@Inject
	private FolderBusiness folderBusiness;

	private Submission submission;
	private Course course;
	private CourseOffer courseOffer;
	private AbstractExercise exercise;
	private boolean isFrozenExercise;
	private CourseRecord courserecord;

	private long submissionId;
	private long courseId;
	private long courseOfferId;
	private boolean showResult;
	private boolean showVariablesAndLogs;
	private AccessRight userRights;

	/**
	 * Loads contents of exercise from database.
	 */
	public void loadSubmission() throws IOException {
		try {
			submission = exerciseBusiness.getSubmissionWithLazyDataBySubmissionIdFromEnvers(submissionId)
					.orElseThrow(NoSuchJackEntityException::new);
			//load courseRecord to show coursename
			Submission tmp = exerciseBusiness.getSubmissionBySubmissionId(submissionId)
					.orElseThrow(NoSuchJackEntityException::new);
			courserecord = tmp.getCourseRecord();
		} catch (NoSuchJackEntityException e) {
			sendErrorResponse(400, "Submission with given submissionId does not exist in database");
			return;
		}

		//load exercise, courseOffer and course to identify access path
		exercise = exerciseBusiness
				.getExerciseWithLazyDataByExerciseId(submission.getExercise().getProxiedOrRegularExerciseId());
		isFrozenExercise = exerciseBusiness.getFrozenExerciseById(submission.getExercise().getId()).isPresent();
		
		if (courseOfferId != 0) {
			courseOffer = courseBusiness.getCourseOfferWithLazyDataByCourseOfferID(courseOfferId).orElse(null);
		}
		if (courseId != 0) {
			try {
				course = courseBusiness.getCourseWithLazyDataByCourseID(courseId);
			} catch (NoSuchJackEntityException e) {
				sendErrorResponse(400, "Course with given courseId does not exist in database");
				return;
			}
		}

		securityChecks();

		if (submission.getAuthor().equals(getCurrentUser()) && !submission.isTestSubmission()) {
			// The user is a Student who looks at his own submission
			showResult = authorizationBusiness.isStudentAllowedToSeeResultForSubmission(getCurrentUser(), submission);
			showVariablesAndLogs = false;
			exercisePlayer.setShowFeedback(
					authorizationBusiness.isStudentAllowedToSeeFeedbackForSubmission(getCurrentUser(), submission));
		} else {
			// The user is a lecturer with rights on the exercise, course or course offer. This is guaranteed because of the previous securityChecks
			showResult = true;
			showVariablesAndLogs = true;
			exercisePlayer.setShowFeedback(true);
		}

		exercisePlayer.setReviewMode(true);
		exercisePlayer.setShowResult(showResult);
		exercisePlayer.setShowVariablesAndLogs(showVariablesAndLogs);

		boolean allowManualFeedback = authorizationBusiness.isAllowedToGiveManualFeedback(getCurrentUser(), submission,
				courseOffer);
		exercisePlayer.setAllowedToGiveManualFeedback(allowManualFeedback);

		exercisePlayer.initPlayer(submission);
		exercisePlayer.setAccessPath(courseOffer);
	}

	public void updateBreadCrumb() {
		final DefaultMenuItem submissionItem = DefaultMenuItem.builder()
				.value(getLocalizedMessage("statistics.submissionDetails")).build();
		if (courseOffer != null) {
			// The submission was accessed via a CourseOffer
			submissionItem
					.setOutcome(viewId.getSubmissionDetails().withParam(submission).withParam(courseOffer).toOutcome());
		} else if (course != null) {
			// The submission was accessed via a Course
			submissionItem.setOutcome(viewId.getSubmissionDetails().withParam(submission).withParam(course).toOutcome());
		} else {
			// The submission was accessed via a Exercise
			submissionItem.setOutcome(viewId.getSubmissionDetails().withParam(submission).toOutcome());
		}
		addYouAreHereModelMenuEntry(submissionItem);
	}

	private void securityChecks() throws IOException {
		if (submission.getAuthor().equals(getCurrentUser()) && !submission.isTestSubmission()) {
			//The user is the owner of the submission
			
			//check if the user is a lecturer who tested his own courseOffer
			boolean testedOwnCourseOffer = courseOffer!=null && authorizationBusiness.isAllowedToReadFromFolder(getCurrentUser(), courseOffer.getFolder());
			//check if the User is allowed to see his own CourseRecordSubmission
			boolean allowedToSeeOwnCourseRecord = authorizationBusiness.isStudentAllowedToSeeCourseRecordSubmissions(getCurrentUser(), courserecord);
			
			if (!allowedToSeeOwnCourseRecord && !testedOwnCourseOffer) {
				sendErrorResponse(403, getLocalizedMessage("submissionDetails.forbiddenSubmission"));
				return;
			}
			//set the correct user rights
			if(testedOwnCourseOffer) {
				userRights = authorizationBusiness.getMaximumRightForUser(getCurrentUser(), courseOffer.getFolder());
			}else{
				userRights = AccessRight.getNone();
			}
			
		} else {
			// The user is allowed to see this page if he has read right on the corresponding Exercise, Course or CourseOffer
			if (courseOffer != null) {
				// The submission was accessed via a CourseOffer
				userRights = authorizationBusiness.getMaximumRightForUser(getCurrentUser(), courseOffer.getFolder());
				if (!userRights.isRead()) {
					sendErrorResponse(403, getLocalizedMessage("submissionDetails.forbiddenSubmission"));
				}
			} else if (course != null) {
				// The submission was accessed via a Course
				userRights = authorizationBusiness.getMaximumRightForUser(getCurrentUser(), course.getFolder());
				if (!userRights.isRead()) {
					sendErrorResponse(403, getLocalizedMessage("submissionDetails.forbiddenSubmission"));
				}
			} else {
				// The submission was accessed via a Exercise
				ContentFolder folder = folderBusiness.getFolderForAbstractExercise(exercise);
				userRights = authorizationBusiness.getMaximumRightForUser(getCurrentUser(), folder);
				if (!userRights.isRead()) {
					sendErrorResponse(403, getLocalizedMessage("submissionDetails.forbiddenSubmission"));
				}
			}
		}
	}

	/**
	 * Reloads the content from database. This method assumes that the view has been fully loaded before .
	 */
	public void reloadSubmission() {
		submission = exerciseBusiness.getSubmissionWithLazyDataBySubmissionIdFromEnvers(submissionId)
				.orElseThrow(NoSuchJackEntityException::new);
	}

	public long getSubmissionId() {
		return submissionId;
	}

	public void setSubmissionId(long submissionId) {
		this.submissionId = submissionId;
	}

	public long getCourseId() {
		return courseId;
	}

	public void setCourseId(long courseId) {
		this.courseId = courseId;
	}

	public long getCourseOfferId() {
		return courseOfferId;
	}

	public void setCourseOfferId(long courseOfferId) {
		this.courseOfferId = courseOfferId;
	}

	public Submission getSubmission() {
		return submission;
	}

	public boolean isShowResult() {
		return showResult;
	}

	public boolean isShowVariablesAndLogs() {
		return showVariablesAndLogs;
	}

	public void updateComment(Comment comment) {
		exerciseBusiness.updateComment(comment);
		if (comment.isRead()) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "submissionDetails.commentMarkedRead", null);
		} else {
			addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "submissionDetails.commentMarkedUnread", null);
		}
	}

	public CourseRecord getCourserecord() {
		return courserecord;
	}
	
	public AbstractExercise getExercise() {
		return exercise;
	}

	public String getSubmissionType() {
		if (submission.isTestSubmission()) {
			if (submission.getCourseRecord() != null) {
				return getLocalizedMessage("submissionDetails.type.testsubmissionCourse");
			} else {
				return getLocalizedMessage("submissionDetails.type.testsubmissionExercise");
			}
		} else {
			return getLocalizedMessage("submissionDetails.type.studentSubmission");
		}
	}

	@Override
	public PublicUserName getPublicUserName(User forUser) {
		if (courseOffer != null) {
			// The submission was accessed via a CourseOffer
			return userBusiness.getPublicUserName(forUser, getCurrentUser(), courseOffer.getFolder());
		} else if (course != null) {
			// The submission was accessed via a Course
			return userBusiness.getPublicUserName(forUser, getCurrentUser(), course.getFolder());
		} else {
			// The submission was accessed via a Exercise
			ContentFolder folder = folderBusiness.getFolderForAbstractExercise(exercise);
			return userBusiness.getPublicUserName(forUser, getCurrentUser(), folder);
		}
	}

	public boolean hasRightsToSeeExtendedStatistics() {
		return authorizationBusiness.isAllowedToSeeExtendedCourseRecordStatistics(getCurrentUser());

	}

	public boolean hasRightsToSeeSubmissionType() {
		if (courseOffer != null) {
			return authorizationBusiness.isAllowedToEditFolder(getCurrentUser(), courseOffer.getFolder());
		} else if (course != null) {
			return authorizationBusiness.isAllowedToEditFolder(getCurrentUser(), course.getFolder());
		}
		ContentFolder folder = folderBusiness.getFolderForAbstractExercise(exercise);
		return authorizationBusiness.isAllowedToEditFolder(getCurrentUser(), folder);
	}

	public void deleteSubmission() throws IOException {
		if (isUserAllowedToDeleteSubmission()) {
			submissionService.deleteSubmissionAndDependentEntities(submission);
			getLogger()
					.info("User " + getCurrentUser().getLoginName() + " successfully deleted Submission "
					+ submission.getId() + ".");
			redirect(viewId.getDeletionSuccess());

		} else {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exception.actionNotAllowed");
		}
	}

	/**
	 * User has GRADE-Rights and accessed the submission correctly.
	 * 
	 * Student Submissions must be accessed from the corresponding courseoffer.
	 * Testsubmissions for a course must be accessed from the course.
	 * Testsubmissions for an exercise must be accessed from the exercise.
	 * 
	 * @return
	 */
	public boolean isUserAllowedToDeleteSubmission() {
		//grade right and correct access
		return authorizationBusiness.isAllowedToDeleteSubmission(getCurrentUser(), submission, courseOffer, course,
				exercise);
	}

	/**
	 * User needs GRADE-rights on the submission to delete it
	 * 
	 */
	public boolean hasUserRightsForDeletion() {
		return authorizationBusiness.hasGradeRightOnSubmission(getCurrentUser(), submission);
	}

	public boolean isExtendedRead() {
		return userRights.isExtendedRead();
	}
	
	public boolean isFrozenExercise(){
		return isFrozenExercise;
	}
	
	public MenuModel getPathOfCourseOffer() {
		return getPathAsModel(courserecord.getCourseOffer().orElseThrow(null), false, true, false);
	}
	
	public MenuModel getPathOfCourse() {		
		final Course nonFrozenCourse = courseBusiness.getNonFrozenCourse(courserecord.getCourse());
		return getUserSpecificPathAsModel(nonFrozenCourse, false, true);
	}
	
	public MenuModel getPathOfExercise() {
		final Exercise nonFrozenExercise = exerciseBusiness.getNonFrozenExercise(exercise);
		return getUserSpecificPathAsModel(nonFrozenExercise,false,true);
	}

}
