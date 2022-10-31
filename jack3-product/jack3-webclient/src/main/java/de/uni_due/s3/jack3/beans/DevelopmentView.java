package de.uni_due.s3.jack3.beans;

import java.io.IOException;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;

import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.DevelopmentBusiness;
import de.uni_due.s3.jack3.business.EnrollmentBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.ExercisePlayerBusiness;
import de.uni_due.s3.jack3.business.FirstTimeSetupBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.business.exceptions.EnrollmentException;
import de.uni_due.s3.jack3.business.exceptions.NotInteractableException;
import de.uni_due.s3.jack3.business.exceptions.PasswordRequiredException;
import de.uni_due.s3.jack3.business.exceptions.SubmissionException;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.CalculatorException;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.InternalErrorEvaluatorException;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.comparators.TimeComparator;
import de.uni_due.s3.jack3.entities.enums.ESubmissionLogEntryType;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInSubmission;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInSubmissionField;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCSubmission;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.SubmissionLogEntry;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.exceptions.JackRuntimeException;
import de.uni_due.s3.jack3.services.BaseService;
import de.uni_due.s3.jack3.utils.StopWatch;

@Named
@ViewScoped
public class DevelopmentView extends AbstractView implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private ExercisePlayerBusiness exercisePlayerBusiness;

	@Inject
	private DevelopmentBusiness developmentBusiness;

	@Inject
	private FirstTimeSetupBusiness firstTimeSetup;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private EnrollmentBusiness enrollmentBusiness;

	@Inject
	private BaseService baseService;

	private User sampleExerciseAuthor;

	private final int maxStresstestUser = 800;

	private int stresstestProgress;

	private int loadTestExercisesProgress;

	private int loadTestSubmissionsProgress;

	public int getLoadTestExercisesProgress() {
		return loadTestExercisesProgress;
	}

	public void setLoadTestExercisesProgress(int loadTestExercisesProgress) {
		this.loadTestExercisesProgress = loadTestExercisesProgress;
	}

	public User getSampleExerciseAuthor() {
		return sampleExerciseAuthor;
	}

	public void setSampleExerciseAuthor(User sampleExerciseAuthor) {
		this.sampleExerciseAuthor = sampleExerciseAuthor;
	}

	/**
	 * Returns the user with the specified name. If no user exists, a new user is created.
	 */
	private User getOrCreateUser(String loginName, boolean hasAdminRights, boolean hasEditRights) {
		return developmentBusiness.getOrCreateUser(loginName, hasAdminRights, hasEditRights);
	}

	/**
	 * Creates test content:
	 *
	 * <ul>
	 * <li>A user named "Lecturer"</li>
	 * <li>Some folders and courses</li>
	 * </ul>
	 *
	 * @throws ActionNotAllowedException
	 */
	public void createTestContent() throws ActionNotAllowedException {

		// Note: We don't have to check presentationRoot == null because method throws an Exception if there isn't one.
		final PresentationFolder presentationRoot = folderBusiness.getPresentationRoot();

		// Get user and personal folder
		User lecturer = getOrCreateUser("Lecturer", false, true);

		final ContentFolder meinOrdner = lecturer.getPersonalFolder();

		// Create some content folders for user
		ContentFolder mathe = folderBusiness.createContentFolder(lecturer, "Mathematik", meinOrdner);
		ContentFolder info = folderBusiness.createContentFolder(lecturer, "Informatik", meinOrdner);
		folderBusiness.createContentFolder(lecturer, "Deutsch", meinOrdner);
		folderBusiness.createContentFolder(lecturer, "Analysis", mathe);
		folderBusiness.createContentFolder(lecturer, "LinA", mathe);
		folderBusiness.createContentFolder(lecturer, "Programmierung", info);
		folderBusiness.createContentFolder(lecturer, "FMSE", info);

		// Create some presentation folders
		final PresentationFolder mathematik = folderBusiness.createPresentationFolder("Mathe", presentationRoot);
		final PresentationFolder informatik = folderBusiness.createPresentationFolder("Informatik", presentationRoot);
		final PresentationFolder analysis = folderBusiness.createPresentationFolder("Analysis", mathematik);
		final PresentationFolder lina = folderBusiness.createPresentationFolder("Lineare Algebra", mathematik);
		folderBusiness.createPresentationFolder("Numerik", mathematik);
		final PresentationFolder programmierung = folderBusiness.createPresentationFolder("Programmierung", informatik);

		// Grant access to presentation folders to user
		folderBusiness.updateFolderRightsForUser(mathematik, lecturer, AccessRight.getFull());
		folderBusiness.updateFolderRightsForUser(informatik, lecturer, AccessRight.getFull());

		// Create exercises in all content folders
		final List<ContentFolder> allFolders = folderBusiness.getAllContentFoldersForUser(lecturer);
		for (final ContentFolder folder : allFolders) {
			exerciseBusiness.createExercise("Aufgabe " + folder.getName(), lecturer, folder,
					getUserLanguage().toLanguageTag());
		}

		// Create some courses
		final Course anaKlausur = courseBusiness.createCourse("Analysis Klausur", lecturer, mathe);
		final Course linaUebung = courseBusiness.createCourse("LinA Übung", lecturer, mathe);
		final Course vorkursInfo = courseBusiness.createCourse("Vorkurs Informatik", lecturer, info);
		final Course miniProjekte = courseBusiness.createCourse("Miniprojekte Programmierung", lecturer, info);

		// Create some course offers
		courseBusiness.createCourseOffer("Klausur 1", anaKlausur, analysis, lecturer);
		courseBusiness.createCourseOffer("Klausur 2", anaKlausur, analysis, lecturer);
		courseBusiness.createCourseOffer("Übung 1", linaUebung, lina, lecturer);
		courseBusiness.createCourseOffer("Vorkurs Informatik", vorkursInfo, informatik, lecturer);
		courseBusiness.createCourseOffer("Miniprojekte Programmierung", miniProjekte, programmierung, lecturer);
		courseBusiness.createCourseOffer("Kurs 1", anaKlausur, mathematik, lecturer);
		courseBusiness.createCourseOffer("Analysisklausur", anaKlausur, mathematik, lecturer);
	}

	/**
	 * @throws ActionNotAllowedException
	 * @see DevelopmentBusiness#setupReadyToPlayEnvironment()
	 */
	public void setupReadyToPlayEnvironment() throws ActionNotAllowedException {
		developmentBusiness.setupReadyToPlayEnvironment();
	}

	/**
	 * Ensures that 800 Testusers are available
	 * Namepattern: testuser{Number}
	 * Password: secret
	 *
	 */
	public void setupStresstestEnviroment() {
		stresstestProgress = 0;
		for (int i = 1; i <= maxStresstestUser; i++) {
			final String name = "stresstestuser" + i;
			getOrCreateUser(name, false, false);
			stresstestProgress = (int) Math.round(((double) i / maxStresstestUser) * 100);
		}
	}

	/**
	 * Creates 10 test users without admin and edit rights.
	 */
	public void createDummyUsers() {
		getOrCreateUser("lecturer", false, true);
		getOrCreateUser("student", false, false);

		for (int i = 1; i <= 10; i++) {
			final String name = "testuser" + i;
			getOrCreateUser(name, false, false);
		}

		for (int i = 1; i <= 10; i++) {
			final String name = "testlecturer" + i;
			getOrCreateUser(name, false, true);
		}
	}

	public void createTestCourseRecords() {
		final List<CourseOffer> offers = courseBusiness.getAllCourseOffers();

		// There are no course offers so no course record can be created.
		if (offers.isEmpty()) {
			return;
		}

		User student = getOrCreateUser("Dummy Student", false, false);

		final List<CourseRecord> records = enrollmentBusiness.getOpenCourseRecords(student);

		// Only create a test course record if no record already exists.
		if (!records.isEmpty()) {
			return;
		}

		for (final CourseOffer offer : offers) {
			// Enroll each student
			try {
				enrollmentBusiness.enrollUser(student, offer);
			} catch (EnrollmentException | NotInteractableException | PasswordRequiredException
					| MessagingException e) {
				// Do nothing because this is only for test purposes
			}
		}
	}

	public void deleteDatabase() throws IOException {
		// Delete database
		developmentBusiness.deleteTenantDatabase();

		// Do first time setup because root folders were also deleted
		firstTimeSetup.doFirstTimeSetup();

		// Redirect to the setup page
		redirect(viewId.getSetup());
	}

	/**
	 * A method for development testing purposes. Use at free will, but please dont commit changes!
	 */
	public void testMethod() {
		developmentBusiness.testMethod();
		// getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Foobar", null));
	}

	/**
	 * Creates a sample exercise with the given index. The exercise is inserted in the personal folder of the selected
	 * user. If no user was selected, the exercise is inserted to the current user's personal folder.
	 *
	 * @param index
	 *            Sample exercise index
	 */
	public void createSampleExercise(int index) {
		User author = sampleExerciseAuthor != null ? sampleExerciseAuthor : getCurrentUser();
		developmentBusiness.createSampleExercise(author, index);

		addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "development.exerciseAdded", null, author.getLoginName());
	}

	/**
	 * Creates all sample exercises. All exercises are inserted in the personal folder of the selected user. If no user
	 * was selected, the exercises are inserted to the current user's personal folder.
	 */
	public void createAllSampleExercises() {
		User author = sampleExerciseAuthor != null ? sampleExerciseAuthor : getCurrentUser();
		developmentBusiness.createSampleExercises(author);

		addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "development.exercisesAdded", null, author.getLoginName());
	}

	/**
	 * Creates 400 Test Exercises
	 */
	public void createLoadTestExercises() {
		User author = sampleExerciseAuthor != null ? sampleExerciseAuthor : getCurrentUser();

		StopWatch watch = new StopWatch();
		setLoadTestExercisesProgress(0);
		for (int i = 0; i < 25; i++) {
			watch.reset().start();
			developmentBusiness.createSampleExercises(author);
			getLogger().info("Calling createAllSampleExercises() for the " + (i + 1) + "th time took: "
					+ watch.stop().getElapsedSeconds());
			setLoadTestExercisesProgress((i + 1) * 4);
		}
	}

	public void createLoadTestSubmissions() {
		setLoadTestSubmissionsProgress(0);

		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(() -> new IllegalStateException(
				"Could not find user 'lecturer', did you press the Button 'Umgebung Erzeugen'?"));

		User student = userBusiness.getUserByName("student").orElseThrow(() -> new IllegalStateException(
				"Could not find user 'student', did you press the Button 'Umgebung Erzeugen'?"));

		CourseOffer beispielaufgabenCourseOffer = getCourseOfferContainingBeispielkurs(lecturer);

		for (int i = 0; i < 1_000; i++) {
			updateProgressbar(i);
			createCourseRecordAndSubmission(lecturer, student, beispielaufgabenCourseOffer);
		}
		setLoadTestSubmissionsProgress(100);
	}

	private CourseOffer getCourseOfferContainingBeispielkurs(User lecturer) {
		Course beispielkursCourse = courseBusiness.getAllCoursesForUser(lecturer).stream() //
				.filter(course -> "Beispielkurs".equals(course.getName())) //
				.findFirst() //
				.orElseThrow(
						() -> new IllegalStateException("Could not find Course 'Beispielkurs' for user 'lecturer', "
								+ "did you press the Button 'Umgebung Erzeugen'?"));

		return courseBusiness.getCourseOffersReferencingCourse(beispielkursCourse).stream() //
				.filter(co -> "Beispielaufgaben".equals(co.getName())) //
				.findFirst() //
				.orElseThrow(() -> new IllegalStateException("Could not find CourseOffer 'Beispielaufgaben' for user "
						+ "'lecturer', did you press the Button 'Umgebung Erzeugen'?"));

	}

	private void updateProgressbar(int i) {
		if ((i > 0) && ((i % 10) == 0)) {
			int k = getLoadTestSubmissionsProgress();
			k++;
			setLoadTestSubmissionsProgress(k);
		}
	}

	private void createCourseRecordAndSubmission(User lecturer, User student, CourseOffer beispielaufgabenCourseOffer) {
		CourseRecord courseRecord = courseBusiness.createCourseRecord(student, beispielaufgabenCourseOffer);

		try {
			createCourseRecordAndSubmission(student, courseRecord);
		} catch (CalculatorException | SubmissionException | InternalErrorEvaluatorException e) {
			throw new JackRuntimeException(e);
		} finally {
			// If something goes wrong above, we dont want multiple open course records, as this will throw an
			// Exception for the student upon viewing the course!
			courseRecord.closeManually(lecturer, "Closed by loadtest-script");
			baseService.merge(courseRecord);
		}
	}

	private void createCourseRecordAndSubmission(User student, CourseRecord courseRecord)
			throws SubmissionException, CalculatorException, InternalErrorEvaluatorException {

		// We don't need to initialize the lazy collection because the caller of this method has just created the course
		// record. courseBusiness.createCourseRecord returns a course record with the initialized exercises.
		AbstractExercise jackExercise = courseRecord.getExercises().stream() //
				.filter(exercise -> "JACK".equals(exercise.getName())) //
				.findFirst() //
				.orElseThrow(() -> new IllegalStateException("Could not find Exercise 'JACK' for user 'lecturer', "
						+ "did you press the Button 'Umgebung Erzeugen'?"));
		jackExercise = exerciseBusiness.getExerciseWithLazyDataByExerciseId(jackExercise.getId());
		Submission submission = exerciseBusiness.createSubmissionForCourseRecord(jackExercise, student, courseRecord,
				false, false);

		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);
		MCSubmission mcStagesubmission = (MCSubmission) filterSubmissionlog(submission, ESubmissionLogEntryType.ENTER)
				.get(0) //
				.getSubmission();
		mcStagesubmission.setTickedPattern("100");

		Stage startStage = jackExercise.getStartStage();
		submission = exercisePlayerBusiness.performStageSubmit(submission, startStage, mcStagesubmission);
		Stage nextStage = exercisePlayerBusiness.findStageTransition(submission, startStage, mcStagesubmission).getTarget();

		submission = exercisePlayerBusiness.expandSubmissionAfterSubmit(submission, startStage, mcStagesubmission);

		FillInSubmission fillInSubmission = (FillInSubmission) filterSubmissionlog(submission,
				ESubmissionLogEntryType.ENTER) //
				.get(1) //
				.getSubmission();

		randomlySetFirstFillInput(fillInSubmission, 40);
		submission = exercisePlayerBusiness.performStageSubmit(submission, nextStage, fillInSubmission);
	}

	private void randomlySetFirstFillInput(FillInSubmission fillInSubmission, int bound) {
		String input = String.valueOf(new SecureRandom().nextInt(bound));
		((FillInSubmissionField) fillInSubmission.getSubmissionFields().iterator().next()).setUserInput(input);
	}

	/**
	 * Returns all submissionlog entries that are a specific type, sorted by timestamp
	 */
	private List<SubmissionLogEntry> filterSubmissionlog(Submission submission, ESubmissionLogEntryType type) {
		return submission.getSubmissionLogAsSortedList().stream() //
				.filter(logEntry -> logEntry.getType() == type) //
				.sorted(new TimeComparator<SubmissionLogEntry>()) //
				.collect(Collectors.toList());
	}

	public List<User> getAllUsersWithEditRights() {
		return userBusiness.getAllUsersWithEditRights();
	}

	/**
	 * @return the stresstestProgress
	 */
	public int getStresstestProgress() {
		return stresstestProgress;
	}

	/**
	 * @param stresstestProgress
	 *            the stresstestProgress to set
	 */
	public void setStresstestProgress(int stresstestProgress) {
		this.stresstestProgress = stresstestProgress;
	}

	/**
	 * Ensures that each user with edit rights has full access on the owned personal folder.
	 */
	public void fixPersonalFolderRights() {
		StopWatch sw = new StopWatch().start();
		int numberOfChanges = folderBusiness.propagateFullRightsForAllPersonalFolders();
		sw.stop();
		if (numberOfChanges > 0) {
			getLogger().infof("It took %s to grant %s users full access to their personal folders.",
					sw.getElapsedMilliseconds(), numberOfChanges);
		} else {
			getLogger().info("Fixing personal folder rights was not necessary.");
		}
	}

	public int getLoadTestSubmissionsProgress() {
		return loadTestSubmissionsProgress;
	}

	public void setLoadTestSubmissionsProgress(int loadTestSubmissionsProgress) {
		this.loadTestSubmissionsProgress = loadTestSubmissionsProgress;
	}

}
