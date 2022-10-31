package de.uni_due.s3.jack3.tests.business.stagetypes.r;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Objects;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.ExercisePlayerBusiness;
import de.uni_due.s3.jack3.business.messaging.MessageBusiness;
import de.uni_due.s3.jack3.entities.stagetypes.r.RSubmission;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.SubmissionLogEntry;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.BaseService;
import de.uni_due.s3.jack3.tests.annotations.NeedsEureka;
import de.uni_due.s3.jack3.tests.annotations.NeedsKafka;
import de.uni_due.s3.jack3.tests.utils.AbstractBusinessTest;

@NeedsKafka
@NeedsEureka
class RStageBusinessTest extends AbstractBusinessTest {

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

	@Disabled("Until #971 is resolved")
	@Test
	void endresultCalculationTest1() {
		Exercise exercise = null;
		try {
			exercise = importSampleExercise("testdata/exercises/r-seq-exercise.xml");
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
		RSubmission rSubmission = filterRSubmission(exerciseSubmission);

		// Set the user input to the expected output
		rSubmission.setStudentInput("zahlen <- 1:10");

		// Submit submission
		exerciseSubmission = exercisePlayerBusiness.performStageSubmit(exerciseSubmission, exercise.getStartStage(),
				rSubmission);
		rSubmission = filterRSubmission(exerciseSubmission);
		assertTrue(rSubmission.hasPendingChecks());

		await().atMost(20, SECONDS).until(gradingIsFinished(exerciseSubmission));

		// Check if the points are correct
		assertEquals(23, exerciseSubmission.getResultPoints());

	}

	private synchronized Callable<Boolean> gradingIsFinished(Submission exerciseSubmission) {
		System.out.println("Checking if grading is finished!");

		MessageBusiness kafkaSingleton = CDI.current().select(MessageBusiness.class).get();

		// FIXME
		//		try {
		//			//	kafkaSingleton.checkerResult();
		//		} catch (InvalidProtocolBufferException | InterruptedException e) {
		//			fail("Error while polling Kafka", e);
		//		}

		exerciseSubmission = exerciseBusiness.refreshSubmissionFromDatabase(exerciseSubmission);
		RSubmission rSubmission = filterRSubmission(exerciseSubmission);

		return () -> !rSubmission.hasPendingChecks();
	}

	/**
	 * Lookups the r submission object from the submission log of the given exercise submission object.
	 */
	@Nonnull
	private RSubmission filterRSubmission(Submission submission) {
		return (RSubmission) submission //
				.getSubmissionLog() //
				.stream() //
				.map(SubmissionLogEntry::getSubmission) //
				.filter(Objects::nonNull) //
				.findAny() //
				.orElseThrow(() -> new AssertionError("Submission log does not contain any stage submission."));
	}

}
