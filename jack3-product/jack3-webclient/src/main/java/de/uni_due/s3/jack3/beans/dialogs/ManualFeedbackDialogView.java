package de.uni_due.s3.jack3.beans.dialogs;

import java.io.Serializable;
import java.util.Optional;

import javax.annotation.CheckForNull;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import de.uni_due.s3.jack3.beans.AbstractView;
import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.ExercisePlayerBusiness;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.ManualResult;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.Submission;

/**
 * Dialog for giving a stage submssion a manual result that overwrites the automatic result from the checks.
 */
@ViewScoped
@Named
public class ManualFeedbackDialogView extends AbstractView implements Serializable {

	private static final long serialVersionUID = 2458528053222732114L;

	@Inject
	private ExercisePlayerBusiness exercisePlayerBusiness;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private AuthorizationBusiness authorizationBusiness;

	/** The stage submission for the manual feedback */
	private StageSubmission stageSubmission;
	/** To which exercise submission the stage submission belongs */
	private Submission submission;
	/** If manual result is enabled */
	private boolean enableManualResult;
	/** The current manual result that is stored in the dislog */
	private ManualResult manualResult;
	/** From which course offer the user accesses the dialog (nullable) */
	@CheckForNull
	private CourseOffer accessPath;

	/**
	 * Loads the dialog and fills the initial values from the given stage submission
	 *
	 * @param submission
	 *            An existing stage submission
	 */
	public void load(StageSubmission stageSubmission, Submission submission, CourseOffer accessPath) {
		this.stageSubmission = exerciseBusiness.getStageSubmissionWithoutLazyData(stageSubmission);
		this.submission = exerciseBusiness.getSubmissionWithoutLazyData(submission);

		// Load manual result in dialog if present or create a new one
		Optional<ManualResult> manualResultOptional = this.stageSubmission.getManualResult();
		enableManualResult = manualResultOptional.isPresent();
		manualResult = manualResultOptional.orElse(new ManualResult(getCurrentUser()));
		this.accessPath = accessPath;
	}

	/**
	 * Saves the manual feedback settings to the database and unloads the dialog.
	 */
	public void saveAndClose() {
			if (enableManualResult) {
				if (!authorizationBusiness.isAllowedToGiveManualFeedback(getCurrentUser(), submission, accessPath)) {
					getLogger().warnf(
							"Action was rejected: %s edited manual feedback for %s, but the user has no rights to edit feedback for this submission.",
							getCurrentUser().getLoginName(), stageSubmission);
					addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null,
							"exception.actionNotAllowed.noRightsDiscardChanges");
					return;
				}
				exercisePlayerBusiness.updateManualResult(getCurrentUser(), stageSubmission, submission, manualResult,
						accessPath);
			} else {
				exercisePlayerBusiness.updateManualResult(getCurrentUser(), stageSubmission, submission, null,
						accessPath);
			}
	}

	public StageSubmission getSubmission() {
		return stageSubmission;
	}

	public boolean isEnableManualResult() {
		return enableManualResult;
	}

	public void setEnableManualResult(boolean enableManualResult) {
		this.enableManualResult = enableManualResult;
	}

	public ManualResult getManualResult() {
		return manualResult;
	}

}
