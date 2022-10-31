package de.uni_due.s3.jack3.beans;

import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.model.menu.DefaultMenuItem;

import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.FrozenExercise;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.exceptions.JackSecurityException;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;
import de.uni_due.s3.jack3.services.SubmissionService;
import de.uni_due.s3.jack3.utils.StopWatch;

@Named
@ViewScoped
public class ExerciseSubmissionsView extends AbstractView implements Serializable {

	private static final long serialVersionUID = -7096495306955176311L;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	private AbstractExercise exercise;
	private List<Integer> frozenRevisionIds = new LinkedList<>();
	private long exerciseId;

	private List<Submission> plainSubmissionData = new LinkedList<>();
	private Map<Integer, Integer> exerciseSubmissionCount = new TreeMap<>();
	private long testingSubmissionCount;
	private long nonTestingsubmissionCount;

	private AccessRight userRights;

	@Inject
	private AuthorizationBusiness authorizationBusiness;

	@Inject
	private SubmissionService submissionService;

	@Inject
	private FolderBusiness folderBusiness;

	private static final String fileRegex = "[\\\\/:*?\"<>|]";

	/**
	 * Loads contents of exercise from database.
	 */
	public void loadExercise() throws IOException {
		StopWatch stopWatch = new StopWatch().start();

		setExerciseIfAllowed();

		loadSubmissionData();

		ContentFolder folder = folderBusiness.getFolderForAbstractExercise(exercise);
		userRights = authorizationBusiness.getMaximumRightForUser(getCurrentUser(), folder);

		plainSubmissionData.sort(Comparator.comparing(Submission::getCreationTimestamp).reversed());
		testingSubmissionCount = plainSubmissionData.stream().filter(Submission::isTestSubmission).count();
		nonTestingsubmissionCount = plainSubmissionData.size() - testingSubmissionCount;

		populateSubmissionCountMap();

		getLogger().debug("Loading Submissions for " + exercise + " took " + stopWatch.stop().getElapsedSeconds());
	}
	
	public void updateBreadCrumb() {
		createUserSpecificYouAreHereModelForExercise(exercise);
		final DefaultMenuItem submissions = DefaultMenuItem.builder()
				.value(getLocalizedMessage("statistics.submissions"))
				.disabled(true)
				.outcome(viewId.getExerciseSubmissions().withParam(exercise).toOutcome())
				.build();
		addYouAreHereModelMenuEntry(submissions);
	}

	private void populateSubmissionCountMap() {
		for (Submission submission : plainSubmissionData) {
			if (!exerciseSubmissionCount.containsKey(submission.getShownExerciseRevisionId())) {
				exerciseSubmissionCount.put(submission.getShownExerciseRevisionId(), 1);
			} else {
				int countPlusOne = exerciseSubmissionCount.get(submission.getShownExerciseRevisionId()) + 1;
				exerciseSubmissionCount.put(submission.getShownExerciseRevisionId(), countPlusOne);
			}
		}
	}

	private void loadSubmissionData() {
		plainSubmissionData = new LinkedList<>();
		// The result from "getAllSubmissions..." is already ordered by the creation timestamp (DESC)
		for (Submission submission : exerciseBusiness.getAllSubmissionsForExerciseAndFrozenVersions(exercise)) {
			// Loading only the comments eager here provides a significant performance boost!
			plainSubmissionData.add(exerciseBusiness.getSubmissionWithCommentsEagerBySubmissionId(submission.getId())
					.orElseThrow(NoSuchJackEntityException::new));
		}
	}

	private void setExerciseIfAllowed() throws IOException {
		Exercise exerciseToLoad;
		try {
			exerciseToLoad = exerciseBusiness.getExerciseById(exerciseId)
					.orElseThrow(NoSuchJackEntityException::new);
		} catch (NoSuchJackEntityException e) {
			String notFoundResponse = "Exercise with given exerciseId '" + exerciseId + "' does not exist in database";
			sendErrorResponse(400, notFoundResponse);
			throw new NoSuchJackEntityException(notFoundResponse);
		}

		if (!authorizationBusiness.isAllowedToReadFromFolder(getCurrentUser(), exerciseToLoad.getFolder())) {
			sendErrorResponse(403, getLocalizedMessage("exerciseSubmissionsView.forbiddenExercise"));
			throw new JackSecurityException(getCurrentUser() + "tried to access " + exerciseToLoad.getFolder());
		}

		exercise = exerciseBusiness.getExerciseWithLazyDataByExerciseId(exerciseToLoad.getId());
	}

	public AbstractExercise getExercise() {
		return exercise;
	}

	public long getExerciseId() {
		return exerciseId;
	}

	public void setExerciseId(long exerciseId) {
		this.exerciseId = exerciseId;
	}

	public long getTestingSubmissionCount() {
		return testingSubmissionCount;
	}

	public long getNonTestingsubmissionCount() {
		return nonTestingsubmissionCount;
	}

	public List<Submission> getPlainSubmissionData() {
		return plainSubmissionData;
	}

	public int getSubmissionCount(int revisionId) {
		return exerciseSubmissionCount.get(revisionId);
	}

	public void deleteSubmission(Submission submission) {
		if (userIsAllowedToDeleteSubmission(submission)) {

			ensureUserHasWritePermission();

			plainSubmissionData.remove(submission);
			exerciseSubmissionCount.put(submission.getShownExerciseRevisionId(),
					exerciseSubmissionCount.get(submission.getShownExerciseRevisionId()) - 1);
			submissionService.deleteSubmissionAndDependentEntities(submission);
			if (submission.isTestSubmission()) {
				testingSubmissionCount--;
			} else {
				nonTestingsubmissionCount--;
			}
		}
	}

	private void ensureUserHasWritePermission() {
		ContentFolder folder = folderBusiness.getFolderForAbstractExercise(exercise);
		if (!authorizationBusiness.isAllowedToEditFolder(getCurrentUser(), folder)) {
			throw new JackSecurityException(
					getCurrentUser() + " tried to write to " + folder);
		}
	}

	public void deleteAllTestSubmissions() {
		ensureUserHasWritePermission();
		plainSubmissionData.clear();
		exerciseSubmissionCount.clear();
		exerciseBusiness.deleteAllTestSubmissionsForExercise(exercise);
		List<FrozenExercise> frozenRevisions = exerciseBusiness.getFrozenRevisionsForExercise(exercise);
		for (FrozenExercise frozenRevision : frozenRevisions) {
			exerciseBusiness.deleteAllTestSubmissionsForExercise(frozenRevision);
		}
		loadSubmissionData();
		plainSubmissionData.sort(Comparator.comparing(Submission::getCreationTimestamp).reversed());
		testingSubmissionCount = plainSubmissionData.stream().filter(Submission::isTestSubmission).count();
		nonTestingsubmissionCount = plainSubmissionData.size() - testingSubmissionCount;

		populateSubmissionCountMap();

	}

	public String generateDownloadFileName() {
		// replaces invalid file name characters
		String validFileName = exercise.getName().replaceAll(fileRegex, "");
		return validFileName + "_" + getLocalizedMessage("exerciseSubmissions.fileName");
	}

	public String getDeletionMessage() {
		int submissionForDeletionCount = 0;
		for (Submission sol : plainSubmissionData) {
			if (sol.getCourseRecord() == null) {
				submissionForDeletionCount++;
			}
		}
		return formatLocalizedMessage("exerciseSubmissions.deleteAllSubmissions.message",
				new Object[] { submissionForDeletionCount });
	}

	public int getRevisionNumber(Submission currentSubmission) {
		return exerciseBusiness.getRevisionIndexForRevisionId(exercise,
				currentSubmission.getShownExerciseRevisionId());
	}

	public boolean isFrozenRevision(Submission submission) {
		return submission.getExercise().isFrozen();
	}

	public boolean userNotAllowedToEdit() {
		return !userRights.isWrite();
	}

	public boolean userHasRightsForDeletion(Submission submission) {
		return authorizationBusiness.hasGradeRightOnSubmission(getCurrentUser(), submission);
	}

	public boolean userIsAllowedToDeleteSubmission(Submission submission) {
		return authorizationBusiness.isAllowedToDeleteSubmission(getCurrentUser(), submission, null, null, exercise);
	}

	public boolean isExtendedRead() {
		return userRights.isExtendedRead();
	}

}
