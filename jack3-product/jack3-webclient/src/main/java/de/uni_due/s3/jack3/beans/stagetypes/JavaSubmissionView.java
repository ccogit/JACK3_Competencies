package de.uni_due.s3.jack3.beans.stagetypes;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import de.uni_due.s3.jack3.entities.stagetypes.java.JavaStage;
import de.uni_due.s3.jack3.entities.stagetypes.java.JavaSubmission;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.SubmissionResource;
import de.uni_due.s3.jack3.utils.JackStringUtils;

public class JavaSubmissionView extends AbstractSubmissionView {

	private static final long serialVersionUID = -1921246843821022155L;

	private JavaStage stage;
	private JavaSubmission stageSubmission;

	@Override
	public Stage getStage() {
		return stage;
	}

	private String taskDescription;

	/**
	 *
	 * @return Task description with all variables replaced
	 */
	public String getTaskDescription() {
		return taskDescription;
	}

	@Override
	public void setStageSubmission(Submission submission, StageSubmission stageSubmission, Stage stage) {
		if (!(stageSubmission instanceof JavaSubmission)) {
			throw new IllegalArgumentException("JavaSubmissionView must be used with instances of JavaSubmission");
		}
		if (!(stage instanceof JavaStage)) {
			throw new IllegalArgumentException("JavaSubmissionView must be used with instances of JavaStage");
		}

		this.stageSubmission = (JavaSubmission) stageSubmission;
		this.stage = (JavaStage) stage;
		taskDescription = exercisePlayerBusiness.resolvePlaceholders(stage.getTaskDescription(), submission,
				stageSubmission,
				stage, true);
	}

	public List<SubmissionResource> getSubmissionResources() {
		List<SubmissionResource> submissionResourcesAsList = new ArrayList<>(stageSubmission.getUploadedFiles());
		// Default sorting: by filename ascending
		Collections.sort(submissionResourcesAsList);
		return submissionResourcesAsList;
	}

	/**
	 * Adds uploaded file to submission resource list.
	 *
	 * @param event
	 */
	public void handleFileUpload(FileUploadEvent event) {
		final UploadedFile file = event.getFile();
		final byte[] content = file.getContent();
		SubmissionResource submissionResource = new SubmissionResource(file.getFileName(), content, getCurrentUser(),
				"", null, null);
		resourceBusiness.persistSubmissionResource(submissionResource);

		stageSubmission.addUploadedFile(submissionResource);
	}

	/**
	 * Removes file from submission resource list.
	 *
	 * @param submissionResource
	 */
	public void removeSubmissionResource(SubmissionResource submissionResource) {
		stageSubmission.removeUploadedFile(submissionResource);
	}

	/**
	 * Handles file download of an submission resource.
	 */
	public StreamedContent getSubmissionResource(SubmissionResource submissionResource) {
		return new DefaultStreamedContent(new ByteArrayInputStream(submissionResource.getContent()),
				submissionResource.getMimeType(), submissionResource.getFilename(), submissionResource.getSize());
	}

	public String getFileUploadHint() {
		String hint = "";

		if ((stage.getMinimumFileCount() > 0) || (stage.getMaximumFileCount() > 0)) {
			hint = getLocalizedMessage("exercisePlayer.java.expectedFiles") + " ";
			if (stage.getMinimumFileCount() == stage.getMaximumFileCount()) {
				hint = hint.replace("{0}", String.valueOf(stage.getMinimumFileCount()));
			} else if ((stage.getMinimumFileCount() > 0) && (stage.getMaximumFileCount() > 0)) {
				hint = hint.replace("{0}",
						getLocalizedMessage("global.atLeast") + " " + String.valueOf(stage.getMinimumFileCount()) + " "
								+ getLocalizedMessage("global.and") + " " + getLocalizedMessage("global.atMost") + " "
								+ String.valueOf(stage.getMaximumFileCount()));
			} else if (stage.getMinimumFileCount() > 0) {
				hint = hint.replace("{0}",
						getLocalizedMessage("global.atLeast") + " " + String.valueOf(stage.getMinimumFileCount()));
			} else if (stage.getMaximumFileCount() > 0) {
				hint = hint.replace("{0}",
						getLocalizedMessage("global.atMost") + " " + String.valueOf(stage.getMaximumFileCount()));
			}

			// Make sure the first letter is a capital letter
			hint = hint.substring(0, 1).toUpperCase() + hint.substring(1);
		}

		if (JackStringUtils.isNotBlank(stage.getMandatoryFileNames())) {
			hint += getLocalizedMessage("exercisePlayer.java.mandatoryFileNames") + ": ";
			hint += stage.getMandatoryFileNamesAsSet().stream().collect(Collectors.joining(", "));
			hint += ". ";
		}

		if (JackStringUtils.isNotBlank(stage.getAllowedFileNames())) {
			hint += getLocalizedMessage("exercisePlayer.java.allowedFileNames") + ": ";
			hint += stage.getAllowedFileNamesAsSet().stream().collect(Collectors.joining(", "));
			hint += ". ";
		}

		return hint;
	}
}
