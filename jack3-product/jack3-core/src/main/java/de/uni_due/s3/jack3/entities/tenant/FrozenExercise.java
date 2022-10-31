package de.uni_due.s3.jack3.entities.tenant;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.enterprise.inject.spi.CDI;
import javax.persistence.Entity;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.enums.EStageHintMalus;
import de.uni_due.s3.jack3.services.RevisionService;

/**
 * This class provides a way to represent an unmodifiable state of an exercise that require a certain level of
 * traceability by lecturers and students. All setters therefore throw an {@link UnsupportedOperationException}.
 *
 * All E-Assessment domain relevant fields of exercises are in the superclass {@link AbstractExercise} (which is the
 * same for regular {@link Exercise}). This class here provides only technical tools needed, e.g. the copy-constructors
 * to create a FrozenExercise of a given revision of a regular Exercise.
 *
 * @author Benjamin Otto
 *
 */
@Entity
@NamedQuery(
		name = FrozenExercise.FROZEN_REVISIONS_FOR_EXERCISE, //
		query = "SELECT fe FROM FrozenExercise fe WHERE fe.proxiedExerciseId=:proxiedExerciseId")
@NamedQuery(
		name = FrozenExercise.FROZEN_REVISION_WITH_LAZY_DATA_BY_ID, //
		query = "SELECT fe FROM FrozenExercise fe " //
		+ "LEFT JOIN FETCH fe.stages " //
		+ "LEFT JOIN FETCH fe.suffixWeights " //
		+ "LEFT JOIN FETCH fe.tags " //
		+ "LEFT JOIN FETCH fe.resources " //
		+ "LEFT JOIN FETCH fe.variableDeclarations " //
		+ "LEFT JOIN FETCH fe.resultFeedbackMappings " //
		+ "WHERE fe.id=:id")
@NamedQuery( //
		name = FrozenExercise.FROZEN_REVISION_FOR_EXERCISE, //
		query = "SELECT fe FROM FrozenExercise fe " //
		+ "WHERE fe.proxiedExerciseId=:proxiedExerciseId " //
		+ "AND fe.proxiedExerciseRevisionId=:proxiedExerciseRevisionId")
@Audited
@XStreamAlias("ExportedExercise")
public class FrozenExercise extends AbstractExercise implements Comparable<FrozenExercise> {

	private static final String MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS = "Must not change state of frozen objects!";

	private static final long serialVersionUID = 2917691919992588533L;

	public static final String FROZEN_REVISIONS_FOR_EXERCISE = "frozenRevisionsForExercise";

	public static final String FROZEN_REVISION_WITH_LAZY_DATA_BY_ID = "frozenRevisionWithLazyDataById";

	public static final String FROZEN_REVISION_FOR_EXERCISE = "frozenRevisionForExercise";

	// Only fields specific to a frozen exercise are declared here!
	// General fields for Exercise are declared in AbstractExercise.

	@ToString
	@XStreamOmitField
	private long proxiedExerciseId;

	// Since Hibernates returns a List that can only be evaluated to Integer, this is an int not a long.
	// See {@link RevisionService#getRevisionNumbersFor}
	@ToString
	@XStreamOmitField
	private int proxiedExerciseRevisionId;

	@ToString
	@Type(type = "text")
	@XStreamOmitField
	private String frozenTitle;

	FrozenExercise() {
		// Argumentless constructor for Hibernate, package-private, because we shouldn't use it!
	}

	/**
	 * Copy-constructor for exercises. Creates a frozen exercise of the last persisted version of the given exercise by
	 * deep copying the fields from envers. <b>Callers must call {@link #generateSuffixWeights()} on the new entity
	 * after it is persisted in the database.</b>
	 *
	 * @param exercise
	 *            Exercise to be frozen (by deep copying the object tree into this FrozenExercise (which is a subclass
	 *            of AbstractExercise)).
	 */
	public FrozenExercise(Exercise exercise) {
		// Since the call to another Constructor needs to be the first, we have to write
		// it like this.
		this(exercise, CDI.current().select(RevisionService.class).get().getProxiedOrLastPersistedRevisionId(exercise));
	}

	/**
	 * Copy-constructor for exercises. Creates a frozen exercise of the given revision id (not revision-number!) of the
	 * given exercise by deep copying the fields from envers. <b>Callers must call {@link #generateSuffixWeights()} on
	 * the new entity after it is persisted in the database.</b>
	 *
	 * @param exercise
	 *            Exercise to be frozen (by deep copying the object tree into this FrozenExercise (which is a subclass
	 *            of AbstractExercise)).
	 * @param exerciseRevisionId
	 *            Revision id of the above exercise that this FrozenExercise shall be constructed from.
	 */
	public FrozenExercise(Exercise exercise, int exerciseRevisionId) {
		if (exercise.isTransient() || (exerciseRevisionId == 0)) {
			throw new AssertionError(
					"Exercise needs to be persisted, before a frozen copy can be created! " + exercise);
		}

		RevisionService revisionService = CDI.current().select(RevisionService.class).get();

		proxiedExerciseId = exercise.getId();
		proxiedExerciseRevisionId = exerciseRevisionId;

		Exercise exercisetoDeepCopyFrom = revisionService.getRevisionOfEntityWithLazyData(Exercise.class,
				proxiedExerciseId, exerciseRevisionId);

		performDeepCopy(exercisetoDeepCopyFrom, this);
	}

	@Override
	public int compareTo(FrozenExercise other) {
		if (other == null) {
			return -1;
		}

		if (other.getProxiedOrRegularExerciseId() != getProxiedOrRegularExerciseId()) {
			throw new IllegalStateException(
					"Comparing of frozen exercises only implemented for courses having the same realCourseId: " + this
					+ "!=" + other);
		}

		Integer otherProxiedCourseRevisionId = other.proxiedExerciseRevisionId;
		return ((Integer) proxiedExerciseRevisionId).compareTo(otherProxiedCourseRevisionId);
	}

	@Override
	public int getDifficulty() {
		return difficulty;
	}

	@Override
	public Set<ExerciseResource> getExerciseResources() {
		return Collections.unmodifiableSet(resources);
	}

	public String getFrozenTitle() {
		return frozenTitle;
	}

	@Override
	public EStageHintMalus getHintMalusType() {
		return hintMalusType;
	}

	@Override
	public String getInternalNotes() {
		return internalNotes;
	}

	@Override
	public String getLanguage() {
		return language;
	}

	@Override
	@Nonnull
	public String getName() {
		return name;
	}

	@Override
	public long getProxiedOrRegularExerciseId() {
		return proxiedExerciseId;
	}

	@Override
	public Set<ResultFeedbackMapping> getResultFeedbackMappings() {
		return Collections.unmodifiableSet(resultFeedbackMappings);
	}

	@Override
	public Set<Stage> getStages() {
		return stages;
	}

	@Override
	public Stage getStartStage() {
		return startStage;
	}

	@Override
	public Set<Tag> getTags() {
		return Collections.unmodifiableSet(tags);
	}

	@Override
	public boolean isValid() {
		return isValid;
	}

	@Override
	public List<VariableDeclaration> getVariableDeclarations() {
		return Collections.unmodifiableList(variableDeclarations);
	}

	@Override
	public boolean isFrozen() {
		return true;
	}

	public void setProxiedExerciseId(long proxiedExerciseId) {
		this.proxiedExerciseId = proxiedExerciseId;
	}

	public void setproxiedExerciseRevisionId(int proxiedExerciseRevisionId) {
		this.proxiedExerciseRevisionId = proxiedExerciseRevisionId;
	}

	public void setFrozenTitle(String frozenTitle) {
		this.frozenTitle = frozenTitle;
	}

	public int getProxiedExerciseRevisionId() {
		return proxiedExerciseRevisionId;
	}

	/**
	 * ----------------------------------------------------------------------------------------------------------------
	 * Methods that change state and therefore mustn't be called
	 */

	@Override
	public void addExerciseResource(ExerciseResource exRes) {
		throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
	}

	@Override
	public void addJSXGraph(JSXGraph jSXGraph) {
		throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
	}

	@Override
	public void addStage(Stage stage) {
		throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
	}

	@Override
	public void addTag(Tag tag) {
		throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
	}

	@Override
	public void addVariable(VariableDeclaration variable) {
		throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
	}

	@Override
	public void removeExerciseResource(ExerciseResource exerciseResource) {
		throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
	}

	@Override
	public void removeJSXGraph(JSXGraph jSXGraph) {
		throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
	}

	@Override
	public void removeStage(Stage stage) {
		throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
	}

	@Override
	public void removeTag(Tag tag) {
		throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
	}

	@Override
	public void removeVariable(VariableDeclaration variable) {
		throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
	}

	@Override
	public void setDifficulty(int difficulty) {
		throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
	}

	@Override
	public void setFolder(ContentFolder folder) {
		throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
	}

	@Override
	public void setHintMalusType(EStageHintMalus hintMalusType) {
		throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
	}

	@Override
	public void setInternalNotes(String internalNotes) {
		throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
	}

	public void setJSXGraphs(Set<JSXGraph> jsxGraphs) {
		throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
	}

	@Override
	public void setLanguage(String language) {
		throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
	}

	@Override
	public void setName(String name) {
		throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
	}

	@Override
	public void setStartStage(Stage startStage) {
		throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
	}

	@Override
	public void setValid(boolean isValid) {
		throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
	}

	public void setStages(Set<Stage> stages) {
		throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
	}

	@Override
	public boolean isFromEnvers() {
		return false;
	}

	@Override
	public String getPublicDescription() {
		return publicDescription;
	}

	@Override
	public void setPublicDescription(String publicDescription) {
		throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
	}

}
