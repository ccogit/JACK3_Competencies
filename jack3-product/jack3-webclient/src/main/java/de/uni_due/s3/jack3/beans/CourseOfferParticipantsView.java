package de.uni_due.s3.jack3.beans;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;

import org.primefaces.PrimeFaces;
import org.primefaces.model.menu.DefaultMenuItem;

import de.uni_due.s3.jack3.beans.ViewId.Builder;
import de.uni_due.s3.jack3.beans.dialogs.DeletionDialogView;
import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.EnrollmentBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.StatisticsBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.business.exceptions.EnrollmentException;
import de.uni_due.s3.jack3.business.exceptions.LinkedCourseException;
import de.uni_due.s3.jack3.business.helpers.EnrollmentLogEntry;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.enums.EEnrollmentStatus;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.Enrollment;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.ProfileField;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;
import de.uni_due.s3.jack3.interfaces.Namable;
import de.uni_due.s3.jack3.utils.JackFileUtils;
import de.uni_due.s3.jack3.utils.StopWatch;

/**
 * Shows data about participants for course offers. It can be reached either with a PresentationFolder id as parameter
 * or with a CourseOffer id. For Folders, this view provides aggregated data for all Course Offers in the Folder.
 */
@ViewScoped
@Named
public class CourseOfferParticipantsView extends AbstractView implements Serializable {

	private static final long serialVersionUID = -1821943389000266089L;
	
	// View parameters
	private long courseOfferId;
	private long folderId;
	@CheckForNull
	private CourseOffer courseOffer;
	@CheckForNull
	private PresentationFolder folder;
	private List<CourseOffer> courseOffers = new ArrayList<>();

	/** What rights the user has on the current folder / course offer */
	private AccessRight userRights;

	/** Wether only the key figures are shown. */
	private boolean showOnlyKeyFigures;

	/** Wether all data is loaded */
	private boolean lazyDataLoaded = false;

	// Statistics data
	private Map<Long, Integer> cachedRevisionNumberForCourseRecordId = new HashMap<>();

	// Lists
	private List<CourseRecord> courseRecordList = new ArrayList<>();
	private List<Enrollment> enrollments = new ArrayList<>();
	private List<Enrollment> waitlist = new ArrayList<>();
	private List<Enrollment> disenrollments = new ArrayList<>();

	// Key figures
	private long numberOfParticipants;
	private long numberOfEnrolledParticipants;
	private long numberOfWaitinglistParticipants;
	private long numberOfCourseRecords;
	private long numberOfSubmissions;
	private double averagePoints;
	private int highestScore;
	private int lowestScore;
	private long numberOfUnreadComments;

	// Data for actions
	private String manualDisenrollExplanation;
	private String manualEnrollExplanation;
	private String manualCloseExplanation;

	private CourseRecord selectedCourseRecord;
	private Enrollment selectedEnrollment;

	// Enrollment status indicator
	private Enrollment enrollmentStatus;

	// Data for Enrollment log dialog
	private List<EnrollmentLogEntry> enrollmentLog;
	private String selectedStudentForEnrollmentLog;

	// Data for Profile Field selector
	private List<ProfileField> availableProfileFields;

	// Caches for better performance
	private Map<User, String> cachedPublicUsernames = new HashMap<>();

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private AuthorizationBusiness authorizationBusiness;

	@Inject
	private DeletionDialogView deletionDialogView;

	@Inject
	private EnrollmentBusiness enrollmentBusiness;

	@Inject
	private StatisticsBusiness statisticsBusiness;

	// ----------------------------------
	// ---------- Entry Points ----------
	// ----------------------------------

	/**
	 * Loads the full page.
	 */
	public void loadFullPage() throws IOException {
		showOnlyKeyFigures = false;

		// Load folder or course offer
		try {
			if (folderId != 0) {
				folder = folderBusiness.getPresentationFolderById(folderId)
						.orElseThrow(() -> new NoSuchJackEntityException("Folder does not exist."));
				courseOffers = courseBusiness.getCourseOffersByFolder(folder, true);
				courseOffers.sort(Namable.NAME_COMPARATOR);
			} else if (courseOfferId != 0) {
				courseOffer = courseBusiness.getCourseOfferById(courseOfferId)
						.orElseThrow(() -> new NoSuchJackEntityException("Course Offer does not exist."));
			} else {
				sendErrorResponse(400, "You must provide either a Course Offer or a Folder.");
				return;
			}
		} catch (NoSuchJackEntityException e) {
			sendErrorResponse(400, e.getMessage());
			return;
		}

		// Check permission
		userRights = authorizationBusiness.getMaximumRightForUser(getCurrentUser(), getActualFolder());
		if (userRights.isNone()) {
			sendErrorResponse(403, getLocalizedMessage("global.forbidden"));
			return;
		}

		availableProfileFields = userBusiness.getAllPublicProfileFields(getCurrentUser(), getActualFolder());
		update();
	}

	public void loadOnlyKeyFigures(final CourseOffer courseOffer) {
		this.showOnlyKeyFigures = true;
		this.courseOfferId = courseOffer.getId();
		this.courseOffer = courseOffer;
		update();
	}

	public void updateBreadCrumb() {
		Builder outcomeBuilder;
		if (isFolderMode()) {
			createYouAreHereModelForPresentationFolder(folder);
			outcomeBuilder = viewId.getCourseOfferParticipants().withParam(folder);
		} else {
			createYouAreHereModelForCourseOfferEdit(courseOffer);
			outcomeBuilder = viewId.getCourseOfferParticipants().withParam(courseOffer);
		}
		var menuItem = DefaultMenuItem.builder()
				.value(getLocalizedMessage("courseOfferParticipants"))
				.disabled(true)
				.outcome(outcomeBuilder.toOutcome())
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
		} else if (isFolderMode()) {
			calculateFullStatisticsForFolder();
		} else {
			calculateFullStatisticsForCourseOffer();
		}

		watch.stop();
		getLogger().debugf("Update %s statistics for %s took %s",
				isFolderMode() ? "Presentation Folder" : "Course Offer",
				showOnlyKeyFigures ? "key figure" : "full",
				watch.getElapsedMilliseconds());
		lazyDataLoaded = true;
	}

	private void calculateOnlyKeyFigures() {
		numberOfParticipants = statisticsBusiness.countParticipants(courseOffer);
		numberOfEnrolledParticipants = enrollmentBusiness.countCurrentEnrollments(courseOffer);
		numberOfCourseRecords = statisticsBusiness.countCourseRecords(courseOffer);
		numberOfSubmissions = statisticsBusiness.countSubmissions(courseOffer);
		numberOfWaitinglistParticipants = enrollmentBusiness.countCurrentWaitinglist(courseOffer);

		averagePoints = ((int) (statisticsBusiness.getAverageScore(courseOffer) * 100)) / 100.0;
		highestScore = statisticsBusiness.getHighestScore(courseOffer);
		lowestScore = statisticsBusiness.getLowestScore(courseOffer);
		numberOfUnreadComments = statisticsBusiness.countUnreadComments(courseOffer);
	}

	private void calculateFullStatisticsForCourseOffer() {
		// Collect data about enrollments
		// TODO Maybe it is faster to collect all enrollments first and then filter them via Streams.
		// Be aware of sorting the waiting list.
		enrollments = enrollmentBusiness.getCurrentEnrollments(courseOffer);
		waitlist = enrollmentBusiness.getWaitingList(courseOffer);
		disenrollments = enrollmentBusiness.getDisenrolledEnrollments(courseOffer);

		numberOfParticipants = (long) enrollments.size() + waitlist.size()
				+ disenrollments.size();
		numberOfEnrolledParticipants = enrollments.size();
		numberOfWaitinglistParticipants = waitlist.size();

		// Collect data about course records
		courseRecordList = courseBusiness.getAllCourseRecords(courseOffer);
		numberOfCourseRecords = courseRecordList.size();
		numberOfSubmissions = statisticsBusiness.countSubmissions(courseOffer);

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

			sum += cr.getResultPoints();
			if (cr.getResultPoints() > highestScore) {
				highestScore = cr.getResultPoints();
			}
			if (cr.getResultPoints() < lowestScore) {
				lowestScore = cr.getResultPoints();
			}
		}
		lowestScore = Math.min(lowestScore, highestScore);
		averagePoints = Math.round((100.0 * sum) / courseRecordList.size()) / 100.0;
	}

	private void calculateFullStatisticsForFolder() {

		// One single DB query is faster than calling 3 queries for each course offers (like above)
		final var allEnrollments = enrollmentBusiness.getAllParticipations(courseOffers);
		// Don't list implicit Enrollments due to submission actions (see JACK/jack3-core#1191)
		allEnrollments.removeIf(enrollment -> !enrollment.getCourseOffer().isExplicitEnrollment());
		enrollments = allEnrollments
				.stream()
				.filter(e -> e.getStatus().equals(EEnrollmentStatus.ENROLLED))
				.collect(Collectors.toList());
		waitlist = allEnrollments
				.stream()
				.filter(e -> e.getStatus().equals(EEnrollmentStatus.ON_WAITINGLIST))
				.sorted(Comparator.comparing(Enrollment::getLastChange))
				.collect(Collectors.toList());
		disenrollments = allEnrollments
				.stream()
				.filter(e -> e.getStatus().equals(EEnrollmentStatus.DISENROLLED))
				.collect(Collectors.toList());

		courseRecordList = courseBusiness.getAllCourseRecords(courseOffers);

		for (CourseRecord cr : courseRecordList) {
			cr.setCommentsOnSubmissions(courseBusiness.countCommentsForCourseRecord(cr));
			cr.setUnreadCommentsOnSubmissions(courseBusiness.countUnreadCommentsForCourseRecord(cr));

			cachedRevisionNumberForCourseRecordId.put(cr.getId(),
					courseBusiness.getCourseRevisionNumberForCourseRecord(cr));
		}

		// Other statistic data is not available in folder mode
	}

	// -----------------------------
	// ---------- Actions ----------
	// -----------------------------

	public List<Enrollment> autocompleteUserStatus(String query) {
		if (!isExtendedRead()) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "exception.actionNotAllowed",
					"exception.actionNotAllowed.noExtendedReadRight");
			return Collections.emptyList();
		}

		if (query == null || query.isBlank() || !isExtendedRead())
			return Collections.emptyList();

		final var allEnrollments = new ArrayList<Enrollment>(
				enrollments.size() + disenrollments.size() + waitlist.size());
		allEnrollments.addAll(enrollments);
		allEnrollments.addAll(disenrollments);
		allEnrollments.addAll(waitlist);

		return allEnrollments.stream()
				// Filter all enrollment objects where the username matches
				.filter(enr -> getCachedPublicUsername(enr.getUser()).toLowerCase().contains(query.toLowerCase()))
				// Sort by the username
				.sorted(Comparator.comparing(enr -> getCachedPublicUsername(enr.getUser())))
				.collect(Collectors.toList());
	}

	public String getCachedPublicUsername(User forUser) {
		if (forUser == null)
			return "";
		return cachedPublicUsernames.computeIfAbsent(forUser,
				user -> userBusiness.getPublicUserName(user, getCurrentUser(), getActualFolder()).getName());
	}

	public void loadEnrollmentLog(final Enrollment enrollment) {
		enrollmentLog = enrollmentBusiness.getEnrollmentLog(enrollment.getUser(), enrollment.getCourseOffer());
		selectedStudentForEnrollmentLog = getPublicUserName(enrollment.getUser()).getName();
	}

	public void selectCourseRecord(final CourseRecord courseRecord) {
		selectedCourseRecord = courseRecord;
	}

	public void selectEnrollment(final Enrollment enrollment) {
		selectedEnrollment = enrollment;
	}

	public void closeCourseRecordManually() {
		enrollmentBusiness.closeSubmissionManually(selectedCourseRecord, getCurrentUser(), manualCloseExplanation);
		update();
	}

	public void disenrollUserManually() {
		try {
			final var user = selectedEnrollment.getUser();
			final var offer = selectedEnrollment.getCourseOffer();
			enrollmentBusiness.disenrollUserManually(user, offer, getCurrentUser(), manualDisenrollExplanation);
		} catch (EnrollmentException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.error", "exception.actionNotAllowed");
		} catch (MessagingException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_WARN, "global.warn",
					"courseOfferParticipants.noMailOnDisenrollment");
		}
		update();
	}

	public void enrollUserManually() {
		try {
			final var user = selectedEnrollment.getUser();
			final var offer = selectedEnrollment.getCourseOffer();
			enrollmentBusiness.enrollUserManually(user, offer, getCurrentUser(), manualEnrollExplanation);
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
		}
		update();
	}

	public void prepareCourseRecordDeletion(CourseRecord courseRecord) {
		if (courseRecord != null) {
			var expectedUserName = getPublicUserName(courseRecord.getUser());
			deletionDialogView.prepareDeletionDialog(expectedUserName.getName());
			selectedCourseRecord = courseRecord;
		}
	}

	public void deleteCourseRecordAndCloseDialog() {
		final CourseOffer selectedCourseOffer = selectedCourseRecord.getCourseOffer()
				.orElseThrow(() -> new IllegalStateException(
						"All Course Records listed in this view must be linked to a Course Offer."));

		try {
			courseBusiness.deleteNonTestSubmission(selectedCourseRecord, selectedCourseOffer, getCurrentUser());
			courseRecordList.remove(selectedCourseRecord);
			getLogger().info("User " + getCurrentUser().getLoginName() + " successfully deleted CourseRecord "
					+ selectedCourseRecord.getId() + ".");
		} catch (ActionNotAllowedException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.error", "exception.actionNotAllowed");
		}

		selectedCourseRecord = null;
		update();
		deletionDialogView.closeDeletionDialog();

		PrimeFaces.current().ajax().update(":courseOfferParticipantsMainForm");
	}

	// -------------------------------------
	// ---------- Computed values ----------
	// -------------------------------------

	public int getCourseRevisionNumber(CourseRecord courseRecord) {
		return cachedRevisionNumberForCourseRecordId.get(courseRecord.getId());
	}

	public PresentationFolder getActualFolder() {
		return isFolderMode() ? folder : courseOffer.getFolder();
	}

	public boolean isFolderMode() {
		return folder != null;
	}

	public boolean isDataAvailable() {
		if (isFolderMode() && courseOffers.isEmpty()) {
			// No Course Offers selected
			return false;
		}

		return !(courseRecordList.isEmpty() && enrollments.isEmpty() && waitlist.isEmpty() && disenrollments.isEmpty());
	}

	public boolean isExtendedRead() {
		return userRights.isExtendedRead();
	}

	public boolean isGradeRight() {
		return userRights.isGrade();
	}

	public String getCourseOffersAsString() {
		final var resultBuilder = new StringJoiner(", ");
		courseOffers.stream().map(Namable::getName).forEachOrdered(resultBuilder::add);
		return resultBuilder.toString();
	}

	/**
	 * Enrollment data are only shown if explicit enrollment is enabled in at least one viewed Course Offer.
	 */
	public boolean isShowEnrollmentData() {
		if (isFolderMode()) {
			return courseOffers.stream().anyMatch(CourseOffer::isExplicitEnrollment);
		} else {
			return courseOffer.isExplicitEnrollment();
		}
	}

	/**
	 * Submission data are only shown if at least one viewed Course Offer has a linked Course or if at least one Course
	 * Record exists (the latter can happen if the Course Offer had submissions but the course was unlinked)..
	 */
	public boolean isShowSubmissionData() {
		if (!courseRecordList.isEmpty() || numberOfCourseRecords > 0) {
			return true;
		}

		if (isFolderMode()) {
			return courseOffer.getCourse() != null;
		} else {
			return courseOffers.stream().anyMatch(offer -> offer.getCourse() != null);
		}
	}

	public boolean isShowSubmissionSummary() {
		return !isFolderMode() && (!courseRecordList.isEmpty() || numberOfCourseRecords > 0);
	}

	public boolean isShowEnrollmentStatus() {
		return !isFolderMode() && courseOffer.isExplicitEnrollment() && isExtendedRead();
	}

	public String generateDownloadFilename(String type) {
		String name = isFolderMode() ? folder.getName() : courseOffer.getName();
		switch (type) {
		case "submissions":
			return JackFileUtils.filterNonAlphNumChars(name + "_" + getLocalizedMessage("statistics.submissions"));
		case "enrollments":
			return JackFileUtils.filterNonAlphNumChars(name + "_" + getLocalizedMessage("statistics.enrollments"));
		case "waitlist":
			return JackFileUtils.filterNonAlphNumChars(name + "_" + getLocalizedMessage("statistics.waitlist"));
		case "disenrollments":
			return JackFileUtils
					.filterNonAlphNumChars(name + "_" + getLocalizedMessage("statistics.formerParticipants"));
		default:
			return JackFileUtils.filterNonAlphNumChars(name);
		}
	}

	public String getShownCounter(String type) {
		switch (type) {
		case "submissions":
			return getShownCounter(courseRecordList);
		case "enrollments":
			return getShownCounter(enrollments);
		case "waitlist":
			return getShownCounter(waitlist);
		case "disenrollments":
			return getShownCounter(disenrollments);
		default:
			return "";
		}
	}

	private String getShownCounter(Collection<?> collection) {
		return collection.isEmpty() ? "" : " (" + collection.size() + ')';
	}

	// -----------------------------------------
	// ---------- Getters and Setters ----------
	// -----------------------------------------

	public long getCourseOfferId() {
		return courseOfferId;
	}

	public void setCourseOfferId(long courseOfferId) {
		this.courseOfferId = courseOfferId;
	}

	public long getFolderId() {
		return folderId;
	}

	public void setFolderId(long folderId) {
		this.folderId = folderId;
	}

	public boolean isShowOnlyKeyFigures() {
		return showOnlyKeyFigures;
	}

	public String getManualDisenrollExplanation() {
		return manualDisenrollExplanation;
	}

	public void setManualDisenrollExplanation(String manualDisenrollExplanation) {
		this.manualDisenrollExplanation = manualDisenrollExplanation;
	}

	public String getManualEnrollExplanation() {
		return manualEnrollExplanation;
	}

	public void setManualEnrollExplanation(String manualEnrollExplanation) {
		this.manualEnrollExplanation = manualEnrollExplanation;
	}

	public String getManualCloseExplanation() {
		return manualCloseExplanation;
	}

	public void setManualCloseExplanation(String manualCloseExplanation) {
		this.manualCloseExplanation = manualCloseExplanation;
	}

	public Enrollment getEnrollmentStatus() {
		return enrollmentStatus;
	}

	public void setEnrollmentStatus(Enrollment enrollmentStatus) {
		this.enrollmentStatus = enrollmentStatus;
	}

	public List<ProfileField> getAvailableProfileFields() {
		return availableProfileFields;
	}

	public List<CourseOffer> getCourseOffers() {
		return courseOffers;
	}

	public boolean isLazyDataLoaded() {
		return lazyDataLoaded;
	}

	public List<CourseRecord> getCourseRecordList() {
		return courseRecordList;
	}

	public List<Enrollment> getEnrollments() {
		return enrollments;
	}

	public List<Enrollment> getWaitlist() {
		return waitlist;
	}

	public List<Enrollment> getDisenrollments() {
		return disenrollments;
	}

	public long getNumberOfParticipants() {
		return numberOfParticipants;
	}

	public long getNumberOfEnrolledParticipants() {
		return numberOfEnrolledParticipants;
	}

	public long getNumberOfWaitinglistParticipants() {
		return numberOfWaitinglistParticipants;
	}

	public long getNumberOfCourseRecords() {
		return numberOfCourseRecords;
	}

	public long getNumberOfSubmissions() {
		return numberOfSubmissions;
	}

	public double getAveragePoints() {
		return averagePoints;
	}

	public int getHighestScore() {
		return highestScore;
	}

	public int getLowestScore() {
		return lowestScore;
	}

	public long getNumberOfUnreadComments() {
		return numberOfUnreadComments;
	}

	public CourseRecord getSelectedCourseRecord() {
		return selectedCourseRecord;
	}

	public String getSelectedCourseRecordUser() {
		return selectedCourseRecord != null ? getCachedPublicUsername(selectedCourseRecord.getUser()) : "";
	}

	public Enrollment getSelectedEnrollment() {
		return selectedEnrollment;
	}

	public String getSelectedEnrollmentUser() {
		return selectedEnrollment != null ? getCachedPublicUsername(selectedEnrollment.getUser()) : "";
	}

	public List<EnrollmentLogEntry> getEnrollmentLog() {
		return enrollmentLog;
	}

	public String getSelectedStudentForEnrollmentLog() {
		return selectedStudentForEnrollmentLog;
	}
	
	public String getUserNameWhoClosedTheCourseRecord(CourseRecord courseRecord) {
		if(!courseRecord.isClosed()) {
			return getLocalizedMessage("submissionDetails.notFinishedYet");
		}
		
		if(courseRecord.isManuallyClosed()){
			if(courseRecord.getClosedByLecturer().isPresent()) {
				//a lecturer closed it
				return courseRecord.getClosedByLecturer().get().getLoginName();
			}else {
				//the student closed it himself
				return getPublicUserName(courseRecord.getUser()).getName();
			}
		}else {
			//automatically closed
			return getLocalizedMessage("submissionDetails.automaticallyClosed");
		}
	}

}
