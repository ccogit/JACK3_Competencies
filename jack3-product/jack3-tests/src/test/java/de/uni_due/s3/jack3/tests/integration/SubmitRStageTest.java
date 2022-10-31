package de.uni_due.s3.jack3.tests.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.ExercisePlayerBusiness;
import de.uni_due.s3.jack3.entities.stagetypes.r.RSubmission;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.SubmissionLogEntry;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.BaseService;
import de.uni_due.s3.jack3.tests.annotations.NeedsEureka;
import de.uni_due.s3.jack3.tests.annotations.NeedsKafka;
import de.uni_due.s3.jack3.tests.utils.AbstractBusinessTest;
import de.uni_due.s3.jack3.utils.StopWatch;

/**
 * This test submits a sample exercise with an R stage and checks if the asynchronous check is performed.
 */
@NeedsKafka
@NeedsEureka
class SubmitRStageTest extends AbstractBusinessTest {

	/** Maximum waiting time for the results from the checker (seconds) */
	private static final int WAITING_TIMEOUT = 20;
	/** Time interval for retrieving the results (seconds) */
	private static final int WAITING_INTERVAL = 2;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private ExercisePlayerBusiness exercisePlayerBusiness;

	@Inject
	private BaseService baseService;

	private User admin;

	@BeforeEach
	@Override
	protected void beforeTest() {
		super.beforeTest();
		admin = getAdmin("admin");
	}

	/**
	 * Lookups the r submission object from the submission log of the given exercise submission object.
	 */
	@Nonnull
	private RSubmission getRSubmissionFromSubmission(Submission submission) {
		StageSubmission stageSubmission = submission.getSubmissionLog().stream().map(SubmissionLogEntry::getSubmission)
				.filter(Objects::nonNull).findAny()
				.orElseThrow(() -> new AssertionError("Submission log does not contain any stage submission."));
		return (RSubmission) stageSubmission;
	}

	@Disabled("Until #971 is resolved")
	@Test
	void submitRStage() {
		Exercise exercise = null;
		try {
			exercise = importSampleExercise("testdata/exercises/r-sample-exercise.xml");
		} catch (Exception e) {
			fail("Importing the sample exercise ended with Exception.", e);
		}

		// Persist exercise
		exercise.setFolder(admin.getPersonalFolder());
		exercise.setName("Exercise");
		baseService.persist(exercise);

		// Init submission
		Submission exerciseSubmission = exerciseBusiness.createSubmission(exercise, admin, true);
		exerciseSubmission = exercisePlayerBusiness.initSubmissionForExercisePlayer(exerciseSubmission);
		RSubmission submission = getRSubmissionFromSubmission(exerciseSubmission);

		// Set the user input to the expected output
		// The expected output is the mean value of 1 and 4 = 2.5
		submission.setStudentInput("mean(c(1,4))");

		// Submit submission
		exerciseSubmission = exercisePlayerBusiness.performStageSubmit(exerciseSubmission, exercise.getStartStage(),
				submission);
		submission = getRSubmissionFromSubmission(exerciseSubmission);
		assertTrue(submission.hasPendingChecks());

		// Wait until there is an result
		StopWatch sw = new StopWatch().start();
		boolean timeout = false;

		// TODO Maybe there is a better way for this while loop and Thread.sleep
		while (submission.hasPendingChecks() && !timeout) {

			if (sw.getCurrentSeconds() >= WAITING_TIMEOUT) {
				timeout = true;
			}

			exerciseSubmission = exerciseBusiness.refreshSubmissionFromDatabase(exerciseSubmission);
			submission = getRSubmissionFromSubmission(exerciseSubmission);

			if (!timeout) {
				try {
					Thread.sleep(WAITING_INTERVAL * 1000);
				} catch (InterruptedException e) {
					fail("Something went wrong while waiting for the check", e);
				}
			}
		}
		sw.stop();

		if (timeout) {
			// TODO Sometimes there is the message "Attempt to heartbeat failed since group is rebalancing" in the log
			// https://medium.com/bakdata/solving-my-weird-kafka-rebalancing-problems-c05e99535435
			getLogger().errorf("Timer expired for results. Total time: %s", sw.getElapsedSeconds());
			throw new TestAbortedException("The result was not received within " + WAITING_TIMEOUT + " seconds.");
		}

		getLogger().infof("Result received. Total time: %s", sw.getElapsedSeconds());
		// Receive the result and check if the points are correct
		assertEquals(23, exerciseSubmission.getResultPoints());


	}

}
