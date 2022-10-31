package de.uni_due.s3.jack3.entities.tenant;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;

/**
 * <p>
 * Abstract superclass representing the actual instance of a stage that was presented to a user and possibly answered
 * with a submission.
 * </p>
 *
 * <p>
 * This superclass contains generic fields for universal information such as variable values, results, status (pending
 * checks, internal errors) and a reference to the stage definition. Subclasses are expected to add specific details
 * that are relevant for the respective stage type only.
 * </p>
 */
@Audited
@NamedQuery(
		name = StageSubmission.STAGESUBMISSION_WITH_LAZY_DATA_BY_STAGESUBMISSION_ID,
		query = "SELECT s FROM StageSubmission s " //
		+ "LEFT JOIN FETCH s.results " //
		+ "LEFT JOIN FETCH s.hintTexts " //
		+ "WHERE s.id = :id")
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class StageSubmission extends AbstractEntity {

	private static final long serialVersionUID = 5019529935878647000L;

	public static final String STAGESUBMISSION_WITH_LAZY_DATA_BY_STAGESUBMISSION_ID = "StageSubmission.stageSubmissionWithLazyDataByStageSubmissionId";

	@Column
	// REVIEW lg - unbenutzt
	private boolean isVisible;

	@ToString
	@Column
	private boolean hasPendingChecks;

	@Column
	private boolean hasInternalErrors;

	@ToString
	@Column
	private long stage_id;

	@ManyToMany(targetEntity = VariableValue.class)
	@MapKeyColumn(length = 10485760)
	private Map<String, VariableValue> variableValues = new HashMap<>();

	@OneToMany
	private Set<Result> results = new HashSet<>();

	/**
	 * Gained points without deductions for requested hints.
	 */
	@Column(nullable = false)
	private int points = 0;

	/**
	 * How many hints the user has requested.
	 */
	@Column
	private int shownHints = 0;

	/**
	 * Sum of all deductions for hints
	 */
	@Column
	private int cumulatedHintMalus = 0;

	@ElementCollection
	@OrderColumn
	@Type(type = "text")
	private List<String> hintTexts = new LinkedList<>();

	/**
	 * A manual result, that can be set by lecturers for any submission type.
	 */
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	private ManualResult manualResult;

	@Column(columnDefinition = "int4 default 0")
	private int attemptCount = 0;

	protected StageSubmission() {
	}

	public long getStageId() {
		return stage_id;
	}

	public void setStageId(long stageId) {
		stage_id = stageId;
	}

	/*
	 * @return unmodifiableMap of variableValues
	 */
	public Map<String, VariableValue> getVariableValues() {
		return Collections.unmodifiableMap(variableValues);
	}

	public void addVariableValues(Map<String, VariableValue> variableValues) {
		this.variableValues.putAll(variableValues);
	}

	/*
	 * @return unmodifiableSet of results
	 */
	public Set<Result> getResults() {
		return Collections.unmodifiableSet(results);
	}

	public void addResult(Result result) {
		results.add(result);
	}

	public void clearResults() {
		results.clear();
	}

	public boolean hasPendingChecks() {
		return hasPendingChecks;
	}

	public void setHasPendingChecks(boolean hasPendingChecks) {
		this.hasPendingChecks = hasPendingChecks;
	}

	public boolean hasInternalErrors() {
		return hasInternalErrors;
	}

	public void setHasInternalErrors(boolean hasInternalErrors) {
		this.hasInternalErrors = hasInternalErrors;
	}

	public int getPoints() {
		return points;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	public List<String> getHintTexts() {
		return Collections.unmodifiableList(hintTexts);
	}

	public void addHint(String hintText, int hintMalus) {
		shownHints++;
		cumulatedHintMalus += hintMalus;
		hintTexts.add(hintText);
	}

	public int getGivenHintCount() {
		return shownHints;
	}

	public int getCumulatedHintMalus() {
		return cumulatedHintMalus;
	}

	public void copyHints(StageSubmission stageSubmission) {
		shownHints = stageSubmission.getGivenHintCount();
		cumulatedHintMalus = stageSubmission.getCumulatedHintMalus();
		hintTexts.addAll(stageSubmission.getHintTexts());
	}

	/**
	 * Updates the submission with data from another submission.
	 */
	public abstract void copyFromStageSubmission(StageSubmission stageSubmission);

	/**
	 * @return Manual result that was set for this submission, wrapped in an {@link Optional}.
	 */
	@Nonnull
	public Optional<ManualResult> getManualResult() {
		return Optional.ofNullable(manualResult);
	}

	/**
	 * @return Whether the submission has a manual result.
	 */
	public boolean isHasManualResult() {
		return manualResult != null;
	}

	/**
	 * Sets the manual result.
	 *
	 * @param manualResult
	 *            The manual result to set or <code>null</code> if the manual result should be unset.
	 */
	public void setManualResult(ManualResult manualResult) {
		this.manualResult = manualResult;
	}

	/**
	 * @return The number of attempts a student made for the stage, including this submission
	 */
	public int getAttemptCount() {
		return attemptCount;
	}

	/**
	 * @param attemptCount
	 *            The number of attempts a student made for the stage, including this submission
	 */
	public void setAttemptCount(int attemptCount) {
		this.attemptCount = attemptCount;
	}
}
