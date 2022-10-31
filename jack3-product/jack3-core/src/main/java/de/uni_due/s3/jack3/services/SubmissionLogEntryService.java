package de.uni_due.s3.jack3.services;

import javax.ejb.Stateless;
import javax.inject.Inject;

import de.uni_due.s3.jack3.entities.enums.ESubmissionLogEntryType;
import de.uni_due.s3.jack3.entities.tenant.Result;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.SubmissionLogEntry;

/**
 * Service for managing {@link SubmissionLogEntry} entities.
 *
 * @author lukas.glaser
 */
@Stateless
public class SubmissionLogEntryService extends AbstractServiceBean {

	@Inject
	private BaseService baseService;

	/**
	 * Creates a new submission log entry and attaches the given stage submission
	 * to it. The submission log entry also gets a reference to the stage to which
	 * the stage submission belongs. The log entry is persisted in the database
	 * and returned.
	 *
	 * @param type
	 *            The type of the log entry
	 * @param stagesubmission
	 *            The submission attached to the log entry
	 * @return The newly created log entry.
	 */
	public SubmissionLogEntry persistSubmissionLogEntryWithStageSubmission(ESubmissionLogEntryType type,
			StageSubmission stagesubmission) {
		final SubmissionLogEntry logEntry = new SubmissionLogEntry(type, stagesubmission.getStageId());
		logEntry.setSubmission(stagesubmission);
		baseService.persist(logEntry);
		return logEntry;
	}

	/**
	 * Creates a new submission log entry and attaches the given result to it. The submission log entry also gets a
	 * reference to the given stage. The log entry is persisted in the database and returned.
	 *
	 * @param type
	 *            The type of the log entry
	 * @param result
	 *            The result attached to the log entry
	 * @param stagesubmission
	 *            The submission attached to the log entry
	 * @return The newly created log entry.
	 */
	public SubmissionLogEntry persistSubmissionLogEntryWithResult(ESubmissionLogEntryType type, Result result,
			StageSubmission stagesubmission) {
		final SubmissionLogEntry logEntry = new SubmissionLogEntry(type, stagesubmission.getStageId());
		logEntry.setSubmission(stagesubmission);
		logEntry.setResult(result);
		baseService.persist(logEntry);
		return logEntry;
	}

	/**
	 * Creates a new submission log entry and sets a reference to the given stage.
	 * No other data is attached to the log entry. The log entry is persisted in
	 * the database and returned.
	 *
	 * @param type
	 *            The type of the log entry
	 * @param stageId
	 *            ID of the stage the log entry refers to
	 * @return The newly created log entry.
	 */
	public SubmissionLogEntry persistSubmissionLogEntry(ESubmissionLogEntryType type, long stageId) {
		final SubmissionLogEntry logEntry = new SubmissionLogEntry(type, stageId);
		baseService.persist(logEntry);
		return logEntry;
	}

	/**
	 * Creates a new submission log entry and attaches the given text message to it.
	 * The entry does not refer to any stage or submission. The log entry is persisted in
	 * the database and returned.
	 *
	 * @param type
	 *            The type of the log entry
	 * @param text
	 *            The text message attached to the log entry
	 * @return The newly created log entry.
	 */
	public SubmissionLogEntry persistSubmissionLogEntryWithText(ESubmissionLogEntryType type, String text) {
		final SubmissionLogEntry logEntry = new SubmissionLogEntry(type, 0);
		logEntry.setText(text);
		baseService.persist(logEntry);
		return logEntry;
	}

	/**
	 * Creates a new submission log entry and attaches the given stage submission
	 * and the given text message to it. The submission log entry also gets a
	 * reference to the stage to which the stage submission belongs. The log
	 * entry is persisted in the database and returned.
	 *
	 * @param type
	 *            The type of the log entry
	 * @param stagesubmission
	 *            The submission attached to the log entry
	 * @param text
	 *            The text message attached to the log entry
	 * @return The newly created log entry.
	 */
	public SubmissionLogEntry persistSubmissionLogEntryWithStageSubmissionAndText(ESubmissionLogEntryType type,
			StageSubmission stagesubmission, String text) {
		final SubmissionLogEntry logEntry = new SubmissionLogEntry(type, stagesubmission.getStageId());
		logEntry.setSubmission(stagesubmission);
		logEntry.setText(text);
		baseService.persist(logEntry);
		return logEntry;
	}
}
