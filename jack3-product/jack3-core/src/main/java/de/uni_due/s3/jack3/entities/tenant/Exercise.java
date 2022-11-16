package de.uni_due.s3.jack3.entities.tenant;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;

import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import de.uni_due.s3.jack3.annotations.DeepCopyOmitField;
import de.uni_due.s3.jack3.entities.enums.EStageHintMalus;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;
import de.uni_due.s3.jack3.services.TagService;

/*
 * Image of an Exercise, consisting of one ore more Stages.
 */
@Audited
@NamedQuery(
		name = Exercise.ALL_EXERCISES_FOR_USER, //
		query = "SELECT DISTINCT e FROM Exercise e " //
		+ "LEFT JOIN FETCH e.folder as c " //
		+ "LEFT JOIN FETCH e.tags " //
		+ "LEFT JOIN c.managingUsers mu " //
		+ "LEFT JOIN c.inheritedManagingUsers imu " //
		+ "WHERE (index(mu) = :user OR index(imu) = :user) " //
		+ "ORDER BY e.name ASC")
@NamedQuery(
		name = Exercise.ALL_EXERCISES_FOR_CONTENTFOLDER_LIST,
		query = "SELECT DISTINCT e FROM Exercise e " //
		+ "LEFT JOIN FETCH e.folder as c " //
		+ "LEFT JOIN FETCH e.tags " //
		+ "WHERE c.id in (:idsFolderList) " //
		+ "ORDER BY e.name ASC")
@NamedQuery(
		name = Exercise.ALL_EXERCISES_FOR_CONTENTFOLDER_LIST_BY_SUBJECT,
		query = "SELECT DISTINCT e FROM Exercise e " //
				+ "LEFT JOIN FETCH e.folder as c " //
				+ "LEFT JOIN FETCH e.tags " //
				+ "WHERE c.id in (:idsFolderList) " //
				+ "AND e.subject =:subject " //
				+ "ORDER BY e.name ASC")
@NamedQuery(
		name = Exercise.ALL_EXERCISES_FOR_CONTENTFOLDER, //
		query = "SELECT e FROM Exercise e " //
		+ "LEFT JOIN FETCH e.folder " //
		+ "WHERE e.folder = :folder")
@NamedQuery(
		name = Exercise.ALL_EXERCISES_FOR_CONTENTFOLDER_EAGERLY, //
		query = "SELECT e FROM Exercise e " //
		+ "LEFT JOIN FETCH e.stages " //
		+ "LEFT JOIN FETCH e.suffixWeights " //
		+ "LEFT JOIN FETCH e.tags " //
		+ "LEFT JOIN FETCH e.resources " //
		+ "LEFT JOIN FETCH e.variableDeclarations " //
		+ "LEFT JOIN FETCH e.resultFeedbackMappings " //
		+ "LEFT JOIN FETCH e.jSXGraphs " //
		+ "LEFT JOIN FETCH e.folder " //
		+ "WHERE e.folder = :folder")
@NamedQuery(
		name = Exercise.ALL_EXERCISES_FOR_TAG,
		query = "SELECT e FROM Exercise e " //
		+ "LEFT JOIN e.tags etags " //
		+ "LEFT JOIN FETCH e.folder " //
		+ "WHERE etags.id = :id " //
		+ "ORDER BY e.name ASC")
@NamedQuery(
		name = Exercise.EXERCISES_REFERENCING_EXERCISE_RESOURCE, //
		query = "SELECT e FROM Exercise e " //
		+ "LEFT JOIN FETCH e.folder " //
		+ "WHERE :resource IN elements(e.resources)")
@NamedQuery(
		name = Exercise.EXERCISE_WITH_LAZY_DATA_BY_EXERCISE_ID,
		query = "select e FROM Exercise e " //
		+ "LEFT JOIN FETCH e.stages " //
		+ "LEFT JOIN FETCH e.suffixWeights " //
		+ "LEFT JOIN FETCH e.tags " //
		+ "LEFT JOIN FETCH e.resources " //
		+ "LEFT JOIN FETCH e.variableDeclarations " //
		+ "LEFT JOIN FETCH e.resultFeedbackMappings " //
		+ "LEFT JOIN FETCH e.jSXGraphs " //
		+ "LEFT JOIN FETCH e.folder " //
		+ "WHERE e.id = :id")
@NamedQuery(
		name = Exercise.EXERCISE_BY_ID,
		query = "select e FROM Exercise e " //
		+ "LEFT JOIN FETCH e.folder " //
		+ "WHERE e.id = :id")
@NamedQuery(
	name = Exercise.TAGS_FOR_EXERCISE,
	query = "SELECT DISTINCT t.name FROM Exercise e " //
			+ "LEFT JOIN e.tags t " //
			+ "WHERE e = :exercise")
@NamedQuery(
		name = Exercise.EXERCISES_REFERENCING_SUBJECT, //
		query = "SELECT exercise FROM Exercise exercise " //
				+ "WHERE exercise.subject=:subject")
@NamedQuery(
		name = Exercise.EXERCISES_REFERENCING_SUBCOMPETENCE, //
		query = "SELECT e FROM Exercise e " //
				+ "JOIN CompetenceGoal cg on cg.exercise=e.id " //
				+ "WHERE cg.competence=:subcompetence")
@Entity
@XStreamAlias("Exercise")
public class Exercise extends AbstractExercise implements DeepCopyable<Exercise> {

	private static final long serialVersionUID = 8382954029420875509L;

	// DO NOT ADD ANY FIELDS HERE (unless it explicitly makes sense to only have in Exercise, like folder below)!
	// Fields for Exercises are declared in AbstractExercise.

	@XStreamOmitField
	@ManyToOne(fetch = FetchType.LAZY)
	@DeepCopyOmitField(copyTheReference = true, reason = "Copying an Exercise doesn't mean copying the entire folder")
	protected ContentFolder folder;

	public Exercise() {

	}

	/**
	 * Name of the query that returns all current revisions of all exercises where user has rights on the parent folder.
	 * Exercises ordered alphabetical by name.
	 */
	public static final String ALL_EXERCISES_FOR_USER = "Exercise.allExercisesForUser";

	/**
	 * Name of the query that returns all exercises that are children of a folder in the folder list.
	 */
	public static final String ALL_EXERCISES_FOR_CONTENTFOLDER_LIST = "Exercise.allExercisesForContentFolderList";

	/**
	 * Name of the query that returns all exercises that are children of a folder in the folder list by given subject.
	 */
	public static final String ALL_EXERCISES_FOR_CONTENTFOLDER_LIST_BY_SUBJECT = "Exercise.allExercisesForContentFolderListBySubject";

	/**
	 * Name of the query that returns all exercises that are children of the given folder.
	 */
	public static final String ALL_EXERCISES_FOR_CONTENTFOLDER = "Exercise.allExercisesForContentFolder";

	/**
	 * Name of the query that returns all exercises that are children of the given folder.
	 */
	public static final String ALL_EXERCISES_FOR_CONTENTFOLDER_EAGERLY = "Exercise.allExercisesForContentFolderEagerly";

	/** Name of the query that returns all exercises to the given tag. */
	public static final String ALL_EXERCISES_FOR_TAG = "Exercise.allExercisesForTag";

	/**
	 * Name of the query that that returns all exercises which are referencing to a exercise resource
	 */
	public static final String EXERCISES_REFERENCING_EXERCISE_RESOURCE = "Exercise.exerciseReferencingExerciseResource";

	/**
	 * Name of the query that loads all lazy data of the exercise with the given id.
	 */
	public static final String EXERCISE_WITH_LAZY_DATA_BY_EXERCISE_ID = "Exercise.exerciseWithLazyDataByExerciseId";

	/**
	 * Name of the query that just gets an exercise without lazy data
	 */
	public static final String EXERCISE_BY_ID = "Exercise.exerciseById";

	public static final String TAGS_FOR_EXERCISE = "Exercise.tagsForExercise";

	/**
	 * Name of the query that returns all exercises which have a reference to the given subject
	 */
	public static final String EXERCISES_REFERENCING_SUBJECT = "Exercise.exercisesReferencingSubject";

	/**
	 * Name of the query that returns all exercises, which have a reference to the given subcompetence
	 */
	public static final String EXERCISES_REFERENCING_SUBCOMPETENCE = "Exercise.exercisesReferencingSubcompetence";

	// TODO bz: Sollte statt "String language" nicht lieber java.util.Locale verwendet werden?
	public Exercise(String name, String language) {
		setName(name);
		this.language = language;
	}

	@Override
	public void addExerciseResource(ExerciseResource exRes) {
		resources.add(exRes);
	}

	@Override
	public void removeExerciseResource(ExerciseResource exerciseResource) {
		resources.remove(exerciseResource);
	}

	@Override
	public void addVariable(VariableDeclaration variable) {
		variableDeclarations.add(variable);
	}

	@Override
	public void removeVariable(VariableDeclaration variable) {
		variableDeclarations.remove(variable);

		listOfExcerciseEntitiesToRemoveBySaving.add(variable);

		for (Stage s : stages) {
			s.removeAllUpdatesForVariable(variable);
		}
	}

	/**
	 * Tag has to be already persisted when calling this or you will get exceptions when merging the exercise. Use e.g.
	 * {@link TagService#getOrCreateByName(String)} for this!
	 */
	@Override
	public void addTag(Tag tag) {
		tags.add(tag);
	}

	@Override
	public void removeTag(Tag tag) {
		tags.remove(tag);
	}

	@Override
	public int getDifficulty() {
		return difficulty;
	}

	/**
	 * Returns the parent Folder. <strong>Note that the underlying field is lazy!</strong>
	 */
	public ContentFolder getFolder() {
		return folder;
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

	/*
	 * @return unmodifiableSet of resources
	 */
	@Override
	public Set<ExerciseResource> getExerciseResources() {
		return Collections.unmodifiableSet(resources);
	}

	/*
	 * @return unmodifiableSet resultFeedbackMappings
	 */
	@Override
	public Set<ResultFeedbackMapping> getResultFeedbackMappings() {
		return Collections.unmodifiableSet(resultFeedbackMappings);
	}

	/*
	 * @return unmodifiableSet of stages
	 */
	@Override
	public Set<Stage> getStages() {
		return Collections.unmodifiableSet(stages);
	}

	@Override
	public void addStage(Stage stage) {
		stages.add(stage);
	}

	@Override
	public Stage getStartStage() {
		return startStage;
	}

	/*
	 * @return unmodifiableSet of tags
	 */
	@Override
	public Set<Tag> getTags() {
		return Collections.unmodifiableSet(tags);
	}

	@Override
	public Subject getSubject() {
		return subject;
	}

	@Override
	public List<CompetenceGoal> getCompetenceGoals() {
		return this.competenceGoals;
	}

	/*
	 * @return unmodifiableList of variableDeclarations
	 */
	@Override
	public List<VariableDeclaration> getVariableDeclarations() {
		return Collections.unmodifiableList(variableDeclarations);
	}

	@Override
	public void setDifficulty(int difficulty) {
		this.difficulty = difficulty;
	}

	@Override
	public void setFolder(ContentFolder folder) {
		this.folder = folder;
	}

	@Override
	public void setInternalNotes(String internalNotes) {
		this.internalNotes = internalNotes;
	}

	@Override
	public void setLanguage(String language) {
		this.language = language;
	}

	@Override
	public void setName(final String name) {
		this.name = requireIdentifier(name, "You must provide a non-empty name.");
	}

	@Override
	public void setStartStage(Stage startStage) {
		this.startStage = startStage;
	}

	@Override
	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	@Override
	public void setSubject(Subject subject) {
		this.subject = subject;
	}

	@Override
	public void setCompetenceGoals(List<CompetenceGoal> competenceGoals) {
		this.competenceGoals.addAll(competenceGoals);
	}

	@Override
	public void addCompetenceGoal(CompetenceGoal competenceGoal) {
		competenceGoals.add(competenceGoal);
	}

	@Override
	public void removeCompetenceGoal(CompetenceGoal competenceGoal) {
		competenceGoals.remove(competenceGoal);
	}

	@Override
	public void removeStage(Stage stage) {
		final int orderIndex = stage.getOrderIndex();

		if (stages.remove(stage)) {
			for (final Stage s : stages) {
				if (s.getOrderIndex() > orderIndex) {
					s.setOrderIndex(s.getOrderIndex() - 1);
				}
			}

			if (stage.equals(startStage)) {
				if (stages.isEmpty()) {
					startStage = null;
				} else {
					startStage = getStagesAsList().get(0);
				}
			}
		}
	}

	@Override
	public EStageHintMalus getHintMalusType() {
		return hintMalusType;
	}

	@Override
	public void setHintMalusType(EStageHintMalus hintMalusType) {
		this.hintMalusType = hintMalusType;
	}

	@Override
	public boolean isFrozen() {
		return false;
	}

	@Override
	public long getProxiedOrRegularExerciseId() {
		// We are in a non frozen exercise, so just returning our id here!
		return getId();
	}

	@Override
	public boolean isValid() {
		return isValid;
	}

	@Override
	public boolean isFromEnvers() {
		return isFromEnvers;
	}

	public void setFromEnvers(boolean isFromEnvers) {
		this.isFromEnvers = isFromEnvers;
	}

	@Override
	public String getPublicDescription() {
		return publicDescription;
	}

	@Override
	public void setPublicDescription(String publicDescription) {
		this.publicDescription = publicDescription;

	}

	@Override
	public Exercise deepCopy() {
		Exercise clone = new Exercise();
		clone.folder = folder; // not deepcopied

		performDeepCopy(this, clone);

		return clone;
	}
}
