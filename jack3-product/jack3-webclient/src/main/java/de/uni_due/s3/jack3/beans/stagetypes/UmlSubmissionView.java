package de.uni_due.s3.jack3.beans.stagetypes;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import de.uni_due.s3.jack3.entities.stagetypes.uml.UmlStage;
import de.uni_due.s3.jack3.entities.stagetypes.uml.UmlSubmission;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.SubmissionResource;

public class UmlSubmissionView extends AbstractSubmissionView {

	private static final long serialVersionUID = -1921246843821022155L;

	private UmlStage stage;
	private UmlSubmission stageSubmission;

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
		if (!(stageSubmission instanceof UmlSubmission)) {
			throw new IllegalArgumentException("UmlSubmissionView must be used with instances of UmlSubmission");
		}
		if (!(stage instanceof UmlStage)) {
			throw new IllegalArgumentException("UmlSubmissionView must be used with instances of UmlStage");
		}

		this.stageSubmission = (UmlSubmission) stageSubmission;
		this.stage = (UmlStage) stage;
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
		return DefaultStreamedContent.builder()
				.stream(() -> new ByteArrayInputStream(submissionResource.getContent()))
				.contentType(submissionResource.getMimeType())
				.name(submissionResource.getFilename())
				.contentLength(submissionResource.getSize())
				.build();
	}

}
