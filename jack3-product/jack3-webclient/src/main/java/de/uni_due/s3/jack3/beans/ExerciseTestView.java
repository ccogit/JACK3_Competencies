package de.uni_due.s3.jack3.beans;

import java.io.IOException;
import java.io.Serializable;

import javax.ejb.EJBTransactionRolledbackException;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.Submission;

@Named
@ViewScoped
public class ExerciseTestView extends AbstractView implements Serializable {

	private static final long serialVersionUID = -7096495306955176311L;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private ExercisePlayerView exercisePlayer;

    private AbstractExercise exercise;
	private long exerciseId;
	private Submission submission;
	private long submissionId;

	/**
	 * Loads contents of exercise from database.
	 */
	public void initTest() throws IOException {
		if (submissionId != 0) {
			try {
				submission = exerciseBusiness	.getSubmissionWithLazyDataBySubmissionId(submissionId)
											.orElseThrow(AssertionError::new);
				if (!submission.getAuthor().equals(getCurrentUser())) {
					sendErrorResponse(400, "Submission with given submissionId is not assigned to the current user.");
				} else {
					exercisePlayer.setShowFeedback(true);
					exercisePlayer.setShowResult(true);
					exercisePlayer.setAllowsHints(true);
					exercisePlayer.initPlayer(submission);
					exercisePlayer.setShowVariablesAndLogs(true);
					exerciseId = submission.getExercise().getId();
				}
			} catch (AssertionError e) {
				// REVIEW bz - Never catch subclasses of Error. Don't misuse exceptions for control flow.
				// Addidion lg - We have to react on an empty Submission-Optional instead.
				sendErrorResponse(400, "Submission with given submissionId does not exist in database.");
			}
		} else if (exerciseId != 0) {
			try {
				exercise = exerciseBusiness.getExerciseWithLazyDataByExerciseId(exerciseId);
				submission = exerciseBusiness.createSubmission(exercise, getCurrentUser(), true);
				redirect(viewId.getExerciseTest()
					.withParam(submission));
			} catch (EJBTransactionRolledbackException e) {
				// REVIEW bz - The EJBTRB exception could be anything. Can't we diagnose this better?
				sendErrorResponse(400, "Exercise with given exerciseId does not exist in database.");
			}
		} else {
			sendErrorResponse(400, "Test cannot be started without parameters.");
		}
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

	public long getSubmissionId() {
		return submissionId;
	}

	public void setSubmissionId(long submissionId) {
		this.submissionId = submissionId;
	}

	public Submission getSubmission() {
		return submission;
	}
}
