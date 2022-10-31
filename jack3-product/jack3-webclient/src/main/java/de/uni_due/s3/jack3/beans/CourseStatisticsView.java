package de.uni_due.s3.jack3.beans;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.model.menu.DefaultMenuItem;

import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.StatisticsBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.ProfileField;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;
import de.uni_due.s3.jack3.utils.JackFileUtils;
import de.uni_due.s3.jack3.utils.StopWatch;

@ViewScoped
@Named
public class CourseStatisticsView extends AbstractView implements Serializable {

	private static final long serialVersionUID = 2917910711375694599L;
	private long courseId;
	private Course course;
	/** Wether only the key figures are shown. */
	private boolean showOnlyKeyFigures;

	private List<CourseRecord> courseRecordList;
	private Map<Long, Integer> cachedRevisionNumberForCourseRecordId = new HashMap<>();

	private List<User> allParticipants;
	private AccessRight userRights;
	private List<ProfileField> availableProfileFields;

	private long numberOfParticipants;
	private long numberOfStudentCourseRecords;
	private long numberOfTestCourseRecords;
	private double averagePoints;
	private int highestScore;
	private int lowestScore;
	private long numberOfUnreadComments;

	private boolean lazyDataLoaded = false;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private UserBusiness userBusiness;
	
	@Inject
	private AuthorizationBusiness authorizationBusiness;

	@Inject
	private StatisticsBusiness statisticsBusiness;

	// ----------------------------------
	// ---------- Entry Points ----------
	// ----------------------------------

	/**
	 * Loads the full page
	 */
	public void loadFullPage() throws IOException {
		showOnlyKeyFigures = false;

		// Load course
		try {
			if (courseId != 0) {
				course = courseBusiness.getCourseByCourseID(courseId);
			} else {
				sendErrorResponse(400, "You must provide a Course.");
				return;
			}
		} catch (NoSuchJackEntityException e) {
			sendErrorResponse(400, "Course does not exist.");
			return;
		}

		// Check permission
		userRights = authorizationBusiness.getMaximumRightForUser(getCurrentUser(), course.getFolder());
		if (userRights.isNone()) {
			sendErrorResponse(403, getLocalizedMessage("global.forbidden"));
			return;
		}

		availableProfileFields = userBusiness.getAllPublicProfileFields(getCurrentUser(), course.getFolder());
		update();
	}

	public void loadOnlyKeyFigures(final Course course) {
		this.showOnlyKeyFigures = true;
		this.courseId = course.getId();
		this.course = course;
		update();
	}

	public void updateBreadCrumb() {
		createUserSpecificYouAreHereModelForCourse(course, false);
		var menuItem = DefaultMenuItem.builder()
				.value(getLocalizedMessage("statistics.submissionOverview"))
				.disabled(true)
				.outcome(viewId.getCourseStatistics().withParam(course).toOutcome())
				.build();
		addYouAreHereModelMenuEntry(menuItem);
	}

	// --------------------------
	// ---------- Data ----------
	// --------------------------

	public void update() {
		StopWatch watch = new StopWatch().start();

		if (showOnlyKeyFigures) {
			calculateOnlyKeyFigures();
		} else {
			calculateFullStatistics();
		}

		watch.stop();
		getLogger().infof("Calculating %s statistics for Course took %s",
				showOnlyKeyFigures ? "key figure" : "full",
				watch.getElapsedSeconds());
		lazyDataLoaded = true;
	}

	private void calculateOnlyKeyFigures() {
		numberOfParticipants = statisticsBusiness.countParticipants(course);
		numberOfStudentCourseRecords = statisticsBusiness.countCourseRecords(course);
		numberOfTestCourseRecords = statisticsBusiness.countTestCourseRecords(course);

		averagePoints = ((int) (statisticsBusiness.getAverageScore(course) * 100)) / 100.0;
		highestScore = statisticsBusiness.getHighestScore(course);
		lowestScore = statisticsBusiness.getLowestScore(course);
		numberOfUnreadComments = statisticsBusiness.countUnreadComments(course);
	}

	private void calculateFullStatistics() {
		courseRecordList = courseBusiness.getAllCourseRecordsIncludingFrozenCourses(course);
		allParticipants = userBusiness.getParticipantsForCourseIgnoringTestSubmissions(course);

		numberOfParticipants = allParticipants.size();

		numberOfStudentCourseRecords = 0;
		numberOfUnreadComments = 0;
		double sum = 0;
		highestScore = 0;
		lowestScore = 100;
		for (CourseRecord cr : courseRecordList) {
			cr.setCommentsOnSubmissions(courseBusiness.countCommentsForCourseRecord(cr));
			long unreadComments = courseBusiness.countUnreadCommentsForCourseRecord(cr);
			cr.setUnreadCommentsOnSubmissions(unreadComments);
			numberOfUnreadComments += unreadComments;
			cachedRevisionNumberForCourseRecordId.put(cr.getId(),
					courseBusiness.getCourseRevisionNumberForCourseRecord(cr));

			// Only count nontesting course records
			if (!cr.isTestSubmission()) {
				numberOfStudentCourseRecords++;
				sum += cr.getResultPoints();
				if (cr.getResultPoints() > highestScore) {
					highestScore = cr.getResultPoints();
				}
				if (cr.getResultPoints() < lowestScore) {
					lowestScore = cr.getResultPoints();
				}
			}
		}
		lowestScore = Math.min(lowestScore, highestScore);
		averagePoints = numberOfStudentCourseRecords == 0 ? 0
				: Math.round((100.0 * sum) / numberOfStudentCourseRecords) / 100.0;
		numberOfTestCourseRecords = courseRecordList.size() - numberOfStudentCourseRecords;
	}

	// -----------------------------
	// ---------- Actions ----------
	// -----------------------------

	/**
	 * Deletes every Test-courseRecord and their Submission from the current Course, if the User has the rights.
	 */
	public void deleteAllTestSubmissionsInCourse() {
		try {
			courseBusiness.deleteAllTestCourseRecords(getCurrentUser(), course);
		} catch (ActionNotAllowedException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exception.actionNotAllowed");
		}
		update();
	}

	public void deleteTestSubmission(CourseRecord courseRecord) {
		try {
			courseBusiness.deleteTestSubmission(getCurrentUser(), courseRecord, course);
		} catch (ActionNotAllowedException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exception.actionNotAllowed");
		}
		update();
	}

	// -------------------------------------
	// ---------- Computed values ----------
	// -------------------------------------

	public int getRevisionNumber(CourseRecord courseRecord) {
		return cachedRevisionNumberForCourseRecordId.get(courseRecord.getId());
	}

	public boolean isDataAvailable() {
		return !courseRecordList.isEmpty();
	}

	public boolean isRead() {
		return userRights.isRead();
	}

	public boolean isExtendedRead() {
		return userRights.isExtendedRead();
	}

	public String generateDownloadFilename(String type) {
		String name = course.getName();
		switch (type) {
		case "submissions":
			return JackFileUtils.filterNonAlphNumChars(name + "_" + getLocalizedMessage("statistics.submissions"));
		case "participants":
			return JackFileUtils.filterNonAlphNumChars(name + "_" + getLocalizedMessage("statistics.participants"));
		default:
			return JackFileUtils.filterNonAlphNumChars(name);
		}
	}

	public String getShownCounter(String type) {
		switch (type) {
		case "submissions":
			return getShownCounter(courseRecordList);
		case "participants":
			return getShownCounter(allParticipants);
		default:
			return "";
		}
	}

	private String getShownCounter(Collection<?> collection) {
		return collection.isEmpty() ? "" : " (" + collection.size() + ')';
	}

	public boolean isAllowedToDeleteCourseRecord(CourseRecord courseRecord) {
		// In this view, only test submissions can be deleted
		return isRead() && courseRecord.isTestSubmission();
	}

	// -----------------------------------------
	// ---------- Getters and Setters ----------
	// -----------------------------------------
	
	public List<ProfileField> getAvailableProfileFields() {
		return availableProfileFields;
	}

	public long getCourseId() {
		return courseId;
	}

	public void setCourseId(long courseId) {
		this.courseId = courseId;
	}

	public List<CourseRecord> getCourseRecordList() {
		return courseRecordList;
	}

	public List<User> getAllParticipants() {
		return allParticipants;
	}

	public Course getCourse() {
		return course;
	}

	public boolean isShowOnlyKeyFigures() {
		return showOnlyKeyFigures;
	}

	public long getNumberOfParticipants() {
		return numberOfParticipants;
	}

	public long getNumberOfStudentCourseRecords() {
		return numberOfStudentCourseRecords;
	}

	public long getNumberOfTestCourseRecords() {
		return numberOfTestCourseRecords;
	}

	public int getHighestScore() {
		return highestScore;
	}

	public double getAveragePoints() {
		return averagePoints;
	}

	public int getLowestScore() {
		return lowestScore;
	}

	public boolean isLazyDataLoaded() {
		return lazyDataLoaded;
	}

	public long getNumberOfUnreadComments() {
		return numberOfUnreadComments;
	}

}
