package de.uni_due.s3.jack3.beans;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFCreationHelper;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.PrimeFaces;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.MenuModel;

import de.uni_due.s3.jack3.beans.dialogs.DeletionDialogView;
import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.CoursePlayerBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.business.exceptions.SubmissionException;
import de.uni_due.s3.jack3.business.helpers.PublicUserName;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.CourseResource;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;
import de.uni_due.s3.jack3.services.BaseService;
import de.uni_due.s3.jack3.services.CourseRecordService;
import de.uni_due.s3.jack3.services.CourseResourceService;
import de.uni_due.s3.jack3.services.SubmissionService;

@ViewScoped
@Named
public class CourseRecordSubmissionsView extends AbstractView implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -7309959198984231628L;
	private long courseRecordId;
	private CourseRecord courseRecord;

	private long courseId;
	private AbstractCourse course;
	private long courseOfferId;
	private CourseOffer courseOffer;

	private List<Submission> submissionList;

	private List<AbstractExercise> neverStartedExercises;

	private Map<Long, Long> latestSubmissionPerExercise;

	private Map<Long, Integer> bestPointsPerExercise;

	private Map<Long, Long> bestSubmissionPerExercise;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private CoursePlayerBusiness coursePlayerBusiness;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private AuthorizationBusiness authenticationBusiness;

	// REVIEW lg - We should not use services in views!
	@Inject
	private CourseResourceService resourceService;

	// REVIEW lg - We should not use services in views!
	@Inject
	private SubmissionService submissionService;

	@Inject
	private UserBusiness userBusiness;

	// REVIEW lg - We should not use services in views!
	@Inject
	private BaseService baseService;

	@Inject
	private CourseRecordService courseRecordService;

	@Inject
	private DeletionDialogView deletionDialogView;

	@Inject
	protected FolderBusiness folderBusiness;

	private Submission submissionForDeletion;

	public void loadCourseRecord() throws IOException {
		try {
			courseRecord = courseBusiness.getCourseRecordWithExercisesById(courseRecordId);
		} catch (NoSuchJackEntityException e) {
			sendErrorResponse(400, "CourseRecord with given courseRecordId does not exist in database");
			return;
		}

		//load courseOffer/course to identify access path
		if ((courseOfferId == 0) && (courseId == 0)) {
			sendErrorResponse(400, "Neither Course nor CourseOffer could be found for this courseRecord");
			return;
		}
		if (courseOfferId != 0) {
			//load CourseOffer
			courseOffer = courseBusiness.getCourseOfferWithLazyDataByCourseOfferID(courseOfferId).orElse(null);
			if (courseOffer == null) {
				sendErrorResponse(400, "CourseOffer with given courseOfferId does not exist in database");
				return;
			}
		}

		// load Course
		try {
			course = courseBusiness
					.getRevisionOfCourseWithLazyData(courseRecord.getCourse(), courseRecord.getCourseRevisionId())
					.get();
		} catch (NoSuchJackEntityException e) {
			sendErrorResponse(400, "Course with given courseId does not exist in database");
			return;
		}

		//Check if User has the Rights to see this page
		if (!courseRecord.isTestSubmission() && courseRecord.getUser().equals(getCurrentUser())) {
			//The user is the owner of the courseRecord

			//check if the user is a lecturer who tested his own courseOffer
			boolean testedOwnCourseOffer = courseOffer!=null && authenticationBusiness.isAllowedToReadFromFolder(getCurrentUser(), courseOffer.getFolder());
			//check if the User is allowed to see his own CourseRecordSubmission
			boolean allowedToSeeOwnCourseRecord = authenticationBusiness.isStudentAllowedToSeeCourseRecordSubmissions(getCurrentUser(), courseRecord);

			if (!allowedToSeeOwnCourseRecord && !testedOwnCourseOffer) {
				sendErrorResponse(403, getLocalizedMessage("courseRecordSubmissions.forbiddenSubmission"));
				return;
			}
		} else // The user is allowed to see this page if he has read right on the corresponding CourseOffer or Course
			if (courseOffer != null) {
				// The CourseRecord was accessed via a CourseOffer
				if (!authenticationBusiness.isAllowedToReadFromFolder(getCurrentUser(), courseOffer.getFolder())) {
					sendErrorResponse(403, getLocalizedMessage("courseRecordSubmissions.forbiddenSubmission"));
				}
			} else // The CourseRecord was accessed via a Course
			if (!authenticationBusiness.isAllowedToReadFromFolder(getCurrentUser(), ((Course) course).getFolder())) {
					sendErrorResponse(403, getLocalizedMessage("courseRecordSubmissions.forbiddenSubmission"));
				}

		updateSubmissionList();

		neverStartedExercises = new LinkedList<>();
		final List<AbstractExercise> startedExercises = submissionList.stream()
				.map(Submission::getExercise).collect(Collectors.toList());

		for (AbstractExercise exercise : courseRecord.getExercises()) {
			if (!startedExercises.contains(exercise)) {
				neverStartedExercises.add(exercise);
			}
		}
	}

	public void updateBreadCrumb() {
		final DefaultMenuItem stats = DefaultMenuItem.builder()
				.value(getLocalizedMessage("global.courseRecordDetails")).build();

		if (courseOffer != null) {
			stats.setOutcome(
					viewId.getCourseRecordSubmissions().withParam(courseRecord).withParam(courseOffer).toOutcome());
		} else {
			stats.setOutcome(viewId.getCourseRecordSubmissions().withParam(courseRecord).withParam(course).toOutcome());
		}
		addYouAreHereModelMenuEntry(stats);
	}

	public long getCourseRecordId() {
		return courseRecordId;
	}

	public void setCourseRecordId(long courseRecordId) {
		this.courseRecordId = courseRecordId;
	}

	public CourseRecord getCourseRecord() {
		return courseRecord;
	}

	public void setCourseRecord(CourseRecord courseRecord) {
		this.courseRecord = courseRecord;
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

	public String getRevisionNumber(Submission currentSubmission) {
		AbstractExercise exercise = currentSubmission.getExercise();
		String frozenSuffix = "";
		if (exercise.isFrozen()) {
			frozenSuffix = " (frozen)";
			exercise = baseService.findById(Exercise.class, exercise.getProxiedOrRegularExerciseId(), false)
					.orElseThrow(NoSuchJackEntityException::new);
		}
		int revisionIndexForRevisionId = exerciseBusiness.getRevisionIndexForRevisionId(exercise,
				currentSubmission.getShownExerciseRevisionId());
		return revisionIndexForRevisionId + frozenSuffix;
	}

	public String getScoringMode() {
		return course.getScoringMode().name();
	}

	public List<Submission> getSubmissionList() {
		return submissionList;
	}

	public void setSubmissionList(List<Submission> submissionList) {
		this.submissionList = submissionList;
	}

	public List<AbstractExercise> getNeverStartedExercises() {
		return neverStartedExercises;
	}

	public void setNeverStartedExercises(List<AbstractExercise> neverStartedExericses) {
		neverStartedExercises = neverStartedExericses;
	}

	public boolean showResultPoints() {
		if (courseRecord.getUser().equals(getCurrentUser()) && !courseRecord.isTestSubmission()) {
			// The user is a Student who looks at his own course record
			return authenticationBusiness.isStudentAllowedToSeeResultForCourseRecord(getCurrentUser(), courseRecord);
		}
		// The user is a lecturer with rights on the course or course offer. This is guaranteed because of the previous securityChecks in loadCourseRecord()
		return true;

	}

	public boolean showSubmissionDetails() {
		if (courseRecord.getUser().equals(getCurrentUser()) && !courseRecord.isTestSubmission()) {
			// The user is a Student who looks at his own course record
			return authenticationBusiness.isStudentAllowedToSeeDetailsForCourseRecord(getCurrentUser(), courseRecord);
		}
		// The user is a lecturer with rights on the course or course offer. This is guaranteed because of the previous securityChecks in loadCourseRecord()
		return true;

	}

	public boolean showLecturerInfos() {
		if (courseRecord.getUser().equals(getCurrentUser()) && !courseRecord.isTestSubmission()) {
			// The user is a Student who looks at his own course record
			return false;
		}
		// The user is a lecturer with rights on the course or course offer. This is guaranteed because of the previous securityChecks in loadCourseRecord()
		return true;
	}

	// REVIEW lg - Can we use the PrimeFaces "FileDownload" element here instead of working with streams manually?
	public void exportFeedback() {
		FacesContext faces = FacesContext.getCurrentInstance();
		HttpServletResponse response = (HttpServletResponse) faces.getExternalContext().getResponse();

		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", 0);
		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet ");

		response.setHeader("Content-disposition", "attachment; filename=" + "feedback.xlsx");

		CourseResource template = null;
		List<CourseResource> resourceList = new ArrayList<>();
		resourceList = resourceService.getAllCourseResourcesForCourse(courseRecord.getCourse());
		for (CourseResource cr : resourceList) {
			if ("Feedback.xlsx".equals(cr.getFilename())) {
				template = cr;
			}
		}

		if (template != null) {
			try {
				List<Submission> submissions = submissionService.getAllSubmissionsForCourseRecord(courseRecord);
				HashMap<String, Submission> solMap = new HashMap<>();
				for (Submission s : submissions) {
					solMap.put(s.getExercise().getName(), s);
				}

				ByteArrayInputStream bais = new ByteArrayInputStream(template.getContent());

				try (XSSFWorkbook wb = new XSSFWorkbook(bais)) {
					XSSFSheet importSheet = wb.getSheetAt(2);

					XSSFCreationHelper createHelper = wb.getCreationHelper();
					XSSFCellStyle cellStyle = wb.createCellStyle();
					cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd.MM.yy"));

					XSSFRow row0 = importSheet.getRow(0);
					XSSFRow row2 = importSheet.getRow(2);
					for (int i = 1; i < importSheet.getRow(0).getLastCellNum(); i++) {
						if (solMap.get(row0.getCell(i).getStringCellValue()) != null) {
							row2.createCell(i)
							.setCellValue(solMap.get(row0.getCell(i).getStringCellValue()).getResultPoints());
						}
						if ("Einreichungsdatum:".equalsIgnoreCase(row0.getCell(i).getStringCellValue().strip())) {
							XSSFCell cell = row2.createCell(i);
							SimpleDateFormat df = new SimpleDateFormat("dd.MM.yy");
							String x = df.format(java.sql.Timestamp.valueOf(courseRecord.getStartTime()));
							cell.setCellValue(x);
							cell.setCellStyle(cellStyle);
						}
					}

					wb.getCreationHelper().createFormulaEvaluator().evaluateAll();
					bais.close();

					OutputStream out = response.getOutputStream();
					wb.write(out);
					faces.responseComplete();
				}
			} catch (Exception e) {
				addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.error", "global.errorMessage");
				getLogger().errorf(e, "Error while exporting feedback for %s and %s.", courseRecord, getCurrentUser().getLoginName());
			}
		}
	}

	public boolean courseHasCourseResource() {
		AbstractCourse course = courseRecord.getCourse();

		if (course == null) {
			return false;
		}

		if (course.isFrozen()) {
			course = courseBusiness.getFrozenCourseWithLazyData(course.getId());
		} else {
			course = courseBusiness.getCourseWithLazyDataByCourseID(course.getId());
		}

		if ((course.getCourseResources() == null) || course.getCourseResources().isEmpty()) {
			return false;
		}
		for (CourseResource courseResource : course.getCourseResources()) {
			if ("Feedback.xlsx".equalsIgnoreCase(courseResource.getFilename())) {
				return true;
			}
		}
		return false;
	}

	public String getCourseRecordType() {
		if (courseRecord.isTestSubmission()) {
			return getLocalizedMessage("submissionDetails.type.testsubmissionCourse");
		}
		return getLocalizedMessage("submissionDetails.type.studentSubmission");
	}

	public String getCourseRecordCloser() {
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

	public void deleteSubmissionAndCloseDialog() throws IOException {
		if (isAllowedToDeleteCourseRecord() && deletionDialogView.isInputtextEqualsNameToCheck()) {
			submissionService.deleteSubmissionAndDependentEntities(submissionForDeletion);
			coursePlayerBusiness.updateCourseResult(courseRecord);
			coursePlayerBusiness.updateCourseRecord(courseRecord);
			updateSubmissionList();
			getLogger().info("User " + getCurrentUser().getLoginName() + " successfully deleted Submission "
					+ submissionForDeletion.getId() + ".");
		} else {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exception.actionNotAllowed");
		}
		setSubmissionForDeletion(null);
		deletionDialogView.closeDeletionDialog();
		PrimeFaces.current().ajax().update("courseRecordSubmissionsMainForm");

	}

	public void prepareSubmissionDeletion(Submission submission) {
		if (submission != null) {
			setSubmissionForDeletion(submission);
			deletionDialogView.prepareDeletionDialog(submission.getExercise().getName());
		}
	}

	public Submission getSubmissionForDeletion() {
		return submissionForDeletion;
	}

	public void setSubmissionForDeletion(Submission submissionForDeletion) {
		this.submissionForDeletion = submissionForDeletion;
	}

	private void updateSubmissionList() {
		submissionList = new LinkedList<>();
		latestSubmissionPerExercise = new HashMap<>();
		bestSubmissionPerExercise = new HashMap<>();
		bestPointsPerExercise = new HashMap<>();
		for (Submission submission : courseBusiness.getAllSubmissionsForCourseRecord(courseRecord)) {
			submissionList.add(exerciseBusiness.getSubmissionWithLazyDataBySubmissionId(submission.getId())
					.orElseThrow(NoSuchJackEntityException::new));
			long exerciseId = submission.getExercise().getId();
			if (!latestSubmissionPerExercise.containsKey(exerciseId)
					|| latestSubmissionPerExercise.get(exerciseId) < submission.getId()) {
				latestSubmissionPerExercise.put(exerciseId, submission.getId());
			}
			if (!bestSubmissionPerExercise.containsKey(exerciseId)
					|| bestPointsPerExercise.get(exerciseId) < submission.getResultPoints()) {
				bestSubmissionPerExercise.put(exerciseId, submission.getId());
				bestPointsPerExercise.put(exerciseId, submission.getResultPoints());
			}
		}
	}

	@Override
	public PublicUserName getPublicUserName(User forUser) {
		if (courseOffer != null) {
			return userBusiness.getPublicUserName(forUser, getCurrentUser(), courseOffer.getFolder());
		}
		return userBusiness.getPublicUserName(forUser, getCurrentUser(), ((Course) course).getFolder());
	}

	public boolean hasRightsToSeeExtendedStatistics() {
		return authenticationBusiness.isAllowedToSeeExtendedCourseRecordStatistics(getCurrentUser());

	}

	/**
	 * User can delete a single submission, if he can delete the corresponding courseRecord
	 *
	 * @return
	 */
	public boolean isAllowedToDeleteCourseRecord() {
		return authenticationBusiness.isAllowedToDeleteCourseRecord(getCurrentUser(), courseRecord, courseOffer);
	}

	public boolean userHasGradeRightsOnCourseRecord() {
		return authenticationBusiness.hasGradeRightOnCourseRecord(getCurrentUser(), courseRecord);
	}

	public void deleteCourseRecord() throws IOException {
		if (isAllowedToDeleteCourseRecord()) {
			courseRecordService.removeCourseRecordAndAttachedSubmissions(courseRecord);
			getLogger().info(
					"User " + getCurrentUser().getLoginName() + " successfully deleted CourseRecord "
							+ courseRecord.getId() + ".");
			redirect(viewId.getDeletionSuccess());

		} else {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exception.actionNotAllowed");
		}

	}

	public boolean isLatestSubmissionForExercise(Submission submission) {
		long exerciseId = submission.getExercise().getId();
		return latestSubmissionPerExercise.get(exerciseId) == submission.getId();
	}

	public boolean isBestSubmissionForExercise(Submission submission) {
		long exerciseId = submission.getExercise().getId();
		return bestSubmissionPerExercise.get(exerciseId) == submission.getId();
	}

	public void restartExercise(Submission submission) throws IOException {
		CourseRecord courseRecord = submission.getCourseRecord();

		if (!authenticationBusiness.hasGradeRightOnSubmission(getCurrentUser(), submission) || courseRecord.isClosed()
				|| !isLatestSubmissionForExercise(submission)) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exception.actionNotAllowed");
		} else {
			try {
				exerciseBusiness.createSubmissionForCourseRecord(submission.getExercise(), submission.getAuthor(),
						courseRecord, false, true);
			} catch (SubmissionException se) {
				addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exception.actionFailed");
				return;
			}
			coursePlayerBusiness.updateCourseResult(courseRecord);
			loadCourseRecord();
		}
	}

	public MenuModel getPathOfCourseOffer() {
		return getPathAsModel(getCourseRecord().getCourseOffer().orElseThrow(null), false, true, false);
	}

	public MenuModel getPathOfCourse() {
		final Course nonFrozenCourse = courseBusiness.getNonFrozenCourse(getCourseRecord().getCourse());
		return getUserSpecificPathAsModel(nonFrozenCourse, false, true);
	}
}
