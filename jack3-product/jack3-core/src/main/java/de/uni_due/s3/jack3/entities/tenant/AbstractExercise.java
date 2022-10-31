package de.uni_due.s3.jack3.entities.tenant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.ejb.NoSuchEntityException;
import javax.enterprise.inject.spi.CDI;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Transient;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import de.uni_due.s3.jack3.annotations.DeepCopyOmitField;
import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.comparators.StageOrderIndexComparator;
import de.uni_due.s3.jack3.entities.enums.EStageHintMalus;
import de.uni_due.s3.jack3.interfaces.Namable;
import de.uni_due.s3.jack3.services.TagService;
import de.uni_due.s3.jack3.services.utils.RepeatStage;

@Audited
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Entity
public abstract class AbstractExercise extends AbstractEntity implements Namable {

	private static final long serialVersionUID = -5749170722255667209L;

	// Adding new fields here requires to update the constructor of FrozenExercise and to update the deepCopy() Method
	// in Exercise!

	/**
	 * The name of the exercise.
	 */
	@ToString
	@Column(nullable = false)
	@Type(type = "text")
	protected String name;

	@Column
	@Type(type = "text")
	protected String publicDescription;

	@Column
	@Type(type = "text")
	protected String internalNotes;

	@Column(nullable = false)
	@Type(type = "text")
	protected String language;

	/**
	 * The difficulty of the exercise, from 0 (very easy) to 100 (very difficult).
	 */
	@Max(100)
	@Min(0)
	@Column
	protected int difficulty;

	@XStreamOmitField
	@Column
	protected boolean isValid;

	@XStreamOmitField
	@ManyToMany(fetch = FetchType.LAZY)
	protected Set<Tag> tags = new HashSet<>();

	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	protected Set<ExerciseResource> resources = new HashSet<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	protected Set<Stage> stages = new HashSet<>();

	@OneToOne(fetch = FetchType.LAZY)
	protected Stage startStage;

	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinTable( name = "exercise_variableDeclaration",
	joinColumns =  @JoinColumn(name="exercise_id"),
	inverseJoinColumns = @JoinColumn(name = "variabledeclaration_id")
			)
	@OrderColumn(name="variabledeclaration_order")
	protected List<VariableDeclaration> variableDeclarations = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	protected Set<JSXGraph> jSXGraphs = new HashSet<>();

	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	// REVIEW lg - unbenutzt: "ResultFeedbackMapping" wird bisher nur im CourseOffer benutzt
	protected Set<ResultFeedbackMapping> resultFeedbackMappings = new HashSet<>();

	@XStreamOmitField
	@ElementCollection(targetClass = Integer.class, fetch = FetchType.LAZY)
	@DeepCopyOmitField(
			reason = "The suffixWieghts contains id's which refer to the stages. Since the copied stages have other id's than the orignal stages this field is ignored by the DeepCopyTest")
	protected Map<Long, Integer> suffixWeights = new TreeMap<>();

	@Enumerated(EnumType.STRING)
	protected EStageHintMalus hintMalusType;

	@Transient
	@XStreamOmitField
	protected transient boolean isFromEnvers = false;

	@Transient
	@XStreamOmitField
	protected transient List<AbstractEntity> listOfExcerciseEntitiesToRemoveBySaving = new ArrayList<>();

	protected AbstractExercise() {
	}

	public abstract void addExerciseResource(ExerciseResource exRes);

	public abstract void removeExerciseResource(ExerciseResource exerciseResource);

	public abstract void addVariable(VariableDeclaration variable);

	public abstract void removeVariable(VariableDeclaration variable);

	public abstract void addTag(Tag tag);

	public abstract void removeTag(Tag tag);

	/**
	 * @return The regular exercise id if called on an instance of {@link Exercise} or the exercise id of the
	 *         original exercise if this is an instance of {@link FrozenExercise}
	 */
	public abstract long getProxiedOrRegularExerciseId();

	public abstract int getDifficulty();

	public abstract void setFolder(ContentFolder folder);

	public abstract String getInternalNotes();

	public abstract String getPublicDescription();

	public abstract String getLanguage();

	/*
	 * @return unmodifiableSet of resources
	 */
	public abstract Set<ExerciseResource> getExerciseResources();

	/*
	 * @return unmodifiableSet resultFeedbackMappings
	 */
	public abstract Set<ResultFeedbackMapping> getResultFeedbackMappings();

	/*
	 * @return unmodifiableSet of stages
	 */
	public abstract Set<Stage> getStages();

	public abstract void addStage(Stage stage);

	public List<Stage> getStagesAsList() {
		final List<Stage> stagesList = new ArrayList<>(getStages());
		StageOrderIndexComparator indexComparator = new StageOrderIndexComparator();
		Collections.sort(stagesList, indexComparator);
		return stagesList;
	}

	public abstract Stage getStartStage();

	/*
	 * @return unmodifiableSet of tags
	 */
	public abstract Set<Tag> getTags();

	public List<String> getTagsAsStrings() {
		return getTags().stream()
			.map(Tag::getName)
			.sorted(String::compareToIgnoreCase)
			.collect(Collectors.toUnmodifiableList());
	}

	public abstract boolean isValid();

	/*
	 * @return unmodifiableList of variableDeclarations
	 */
	public abstract List<VariableDeclaration> getVariableDeclarations();

	public abstract void setDifficulty(int difficulty);

	public abstract void setInternalNotes(String internalNotes);

	public abstract void setPublicDescription(String publicDescription);

	public abstract void setLanguage(String language);

	public abstract void setName(String name);

	public abstract void setStartStage(Stage startStage);

	public abstract void setValid(boolean isValid);

	/**
	 * Returns the next available default name for a new variable.
	 *
	 */
	public String getNextDefaultNameForVariables() {
		long next = getVariableDeclarations().size();
		for (final VariableDeclaration variableDeclaration : getVariableDeclarations()) {
			if (variableDeclaration.getName().matches("var\\d+")) {
				next = Math.max(next, Long.parseLong(variableDeclaration.getName().substring(3)));
			}
		}
		return "var" + (next + 1);
	}

	/**
	 * Removes the given stage from this exercise. The order index for all other stages is adjusted if necessary.
	 */
	public abstract void removeStage(Stage stage);

	/**
	 * Returns the next available default internal name for stages on this exercise. The default internal name for the
	 * first stage of an exercise is <code>#1</code>, for the second one is <code>#2</code> and so on. However, if
	 * <code>#1</code> has been removed and <code>#2</code> still exists as the only stage, the next available default
	 * name returned by this method is <code>#3</code>.
	 */
	public String getNextDefaultInternalNameForStages() {
		long next = getStages().size();
		for (final Stage stage : getStages()) {
			if (stage.getInternalName().matches("#\\d*")) {
				next = Math.max(next, Long.parseLong(stage.getInternalName().substring(1)));
			}
		}
		return "#" + (next + 1);
	}

	public List<VariableDeclaration> getVariableDeclarationForReoder(){
		List<VariableDeclaration> variableDeclarationList = new ArrayList<>(variableDeclarations);
		return variableDeclarationList;
	}

	public void reorderVariableDeclarations(int from, int to) {
		if ((from < 0) || (to < 0)) {
			throw new IllegalArgumentException("Indexes must be positive.");
		}

		if ((from >= variableDeclarations.size()) || (to >= variableDeclarations.size())) {
			throw new IllegalArgumentException("Indexes are out of list size.");
		}

		variableDeclarations.add(to, variableDeclarations.remove(from));
	}

	public List<AbstractEntity> getListOfExcerciseEntitiesToRemoveBySaving(){
		if(listOfExcerciseEntitiesToRemoveBySaving == null) {
			listOfExcerciseEntitiesToRemoveBySaving = new ArrayList<>();
		}
		return listOfExcerciseEntitiesToRemoveBySaving;
	}

	/**
	 * Calculates and caches the maximum weight for suffix paths for all stages in this exercise.
	 */
	public void generateSuffixWeights() {
		// Clear all cached weights
		if (suffixWeights == null) { // This happens when importing an exercise
			suffixWeights = new TreeMap<>();
		}

		suffixWeights.clear();

		// Get a map of all suffix paths
		Map<Stage, List<List<Stage>>> allSuffixPaths = generateAllSuffixPaths();

		// For all stages and their suffix paths ...
		for (Entry<Stage, List<List<Stage>>> mapEntry : allSuffixPaths.entrySet()) {
			for (List<Stage> onePath : mapEntry.getValue()) {
				// ... calculate the path weight ...
				int pathWeight = 0;
				for (Stage stage : onePath) {
					pathWeight += stage.getWeight();
				}
				// ... and store it in the map if it is new or larger than the existing one.
				if (!suffixWeights.containsKey(mapEntry.getKey().getId())
						|| (suffixWeights.get(mapEntry.getKey().getId()) < pathWeight)) {
					suffixWeights.put(mapEntry.getKey().getId(), pathWeight);
				}
			}
		}
	}

	/**
	 * Generates and returns all suffix paths for all stages in this exercise. Each suffix path is a list of stages that
	 * are pair-wise connected by transitions. The last stage in each path is an end stage. For one stage there can be
	 * many suffix paths, but there is at least one with at least one element (the stage itself).
	 */
	private Map<Stage, List<List<Stage>>> generateAllSuffixPaths() {
		Map<Stage, List<List<Stage>>> pathMap = new TreeMap<>();
		List<List<Stage>> newPaths = new LinkedList<>(stages.stream().filter(Stage::isEndStage).map(this::makeNewPathFromStage)
				.collect(Collectors.toList()));

		// Process all unprocessed paths
		while (!newPaths.isEmpty()) {
			List<List<Stage>> currentPaths = new LinkedList<>(newPaths);
			newPaths.clear();

			// For each unprocessed path ...
			for (List<Stage> onePath : currentPaths) {
				// ... get the first stage ...
				Stage firstStage = onePath.get(0);
				// ... and store the path in the suffix map using the first stage as key
				addPathToPathMap(pathMap, onePath, firstStage);

				// Find all stages that lead to the current first stage
				for (Stage stage : stages) {
					// If there is one that is not yet contained in the path ...
					if (stage.leadsTo(firstStage) && !onePath.contains(stage)) {
						// ... add it to the current path as new first stage to make a new (unprocessed) path
						List<Stage> newPath = makeNewPathFromStage(stage);
						newPath.addAll(onePath);
						newPaths.add(newPath);
					}
				}
			}
		}

		return pathMap;
	}

	private void addPathToPathMap(Map<Stage, List<List<Stage>>> pathMap, List<Stage> onePath, Stage firstStage) {
		if (!pathMap.containsKey(firstStage)) {
			pathMap.put(firstStage, new LinkedList<>());
		}
		pathMap.get(firstStage).add(onePath);
	}

	private List<Stage> makeNewPathFromStage(Stage stage) {
		List<Stage> newPath = new LinkedList<>();
		newPath.add(stage);
		return newPath;
	}

	public Map<Long, Integer> getSuffixWeights() {
		return suffixWeights;
	}

	public abstract EStageHintMalus getHintMalusType();

	public abstract void setHintMalusType(EStageHintMalus hintMalusType);

	public abstract boolean isFrozen();

	public abstract boolean isFromEnvers();

	public Set<JSXGraph> getJSXGraphs(){
		return Collections.unmodifiableSet(jSXGraphs);
	}

	public void removeJSXGraph(JSXGraph jSXGraph) {
		jSXGraphs.remove(jSXGraph);
		listOfExcerciseEntitiesToRemoveBySaving.add(jSXGraph);
	}

	public void addJSXGraph(JSXGraph jSXGraph) {
		jSXGraphs.add(jSXGraph);
	}

	protected void performDeepCopy(AbstractExercise exerciseToCopyFrom, AbstractExercise exerciseToCopyTo) {
		exerciseToCopyTo.name = exerciseToCopyFrom.name;
		exerciseToCopyTo.publicDescription = exerciseToCopyFrom.publicDescription;
		exerciseToCopyTo.internalNotes = exerciseToCopyFrom.internalNotes;
		exerciseToCopyTo.language = exerciseToCopyFrom.language;
		exerciseToCopyTo.difficulty = exerciseToCopyFrom.difficulty;
		exerciseToCopyTo.isValid = exerciseToCopyFrom.isValid;

		// getting Tags from TagService
		TagService tagService = CDI.current().select(TagService.class).get();

		// Since tags are uniquely named, we get the tag with the current name
		// from the database.
		for (Tag tag : exerciseToCopyFrom.tags) {
			Tag copiedTag = tagService.getTagByName(tag.getName()).orElseThrow(NoSuchEntityException::new);
			exerciseToCopyTo.tags.add(copiedTag);
		}

		// Copy resources and create a map from old resources to new resources
		HashMap<ExerciseResource, ExerciseResource> mapResourcesToCopiedResources = new HashMap<>();
		for (ExerciseResource originalResource : exerciseToCopyFrom.resources) {
			ExerciseResource copyResource = originalResource.deepCopy();
			exerciseToCopyTo.resources.add(copyResource);
			mapResourcesToCopiedResources.put(originalResource, copyResource);
		}

		HashMap<Stage, Stage> mapStagesToCopiedStages = new HashMap<>();
		mapStagesToCopiedStages.put(new RepeatStage(), new RepeatStage()); // Dummy so that Repeat can also be mapped
		// copy Stages and set startStage
		for (Stage originalStage : exerciseToCopyFrom.stages) {
			Stage deepCopyStage = originalStage.deepCopy();
			deepCopyStage.updateResourceReferences(mapResourcesToCopiedResources);
			exerciseToCopyTo.stages.add(deepCopyStage);
			mapStagesToCopiedStages.put(originalStage, deepCopyStage);
			if (exerciseToCopyFrom.startStage.equals(originalStage)) {
				exerciseToCopyTo.startStage = deepCopyStage;
			}
		}
		// set the right targets for the Transitions
		for (Stage originalStage : exerciseToCopyFrom.stages) {
			final Stage copiedStage = mapStagesToCopiedStages.get(originalStage);

			// Set Target for the Default Transition
			copiedStage.getDefaultTransition()
			.setTarget(mapStagesToCopiedStages.get(originalStage.getDefaultTransition().getTarget()));
			// Set Targets for the SkipTransitions
			for (int i = 0; i < copiedStage.getSkipTransitions().size(); i++) {
				copiedStage.getSkipTransitions().get(i)
				.setTarget(mapStagesToCopiedStages.get(originalStage.getSkipTransitions().get(i).getTarget()));
			}
			// Set Targets for the StageTransitions
			for (int i = 0; i < copiedStage.getStageTransitions().size(); i++) {
				copiedStage.getStageTransitions().get(i)
				.setTarget(mapStagesToCopiedStages.get(originalStage.getStageTransitions().get(i).getTarget()));
			}
		}

		exerciseToCopyFrom.variableDeclarations.stream()
			.map(VariableDeclaration::deepCopy)
			.forEach(exerciseToCopyTo.variableDeclarations::add);

		exerciseToCopyFrom.jSXGraphs.stream()
			.map(JSXGraph::deepCopy)
			.forEach(exerciseToCopyTo.jSXGraphs::add);

		exerciseToCopyFrom.resultFeedbackMappings.stream()
			.map(ResultFeedbackMapping::deepCopy)
			.forEach(exerciseToCopyTo.resultFeedbackMappings::add);

		exerciseToCopyTo.suffixWeights = new TreeMap<>(exerciseToCopyFrom.suffixWeights);
		exerciseToCopyTo.generateSuffixWeights();

		exerciseToCopyTo.hintMalusType = exerciseToCopyFrom.hintMalusType;
		exerciseToCopyTo.isFromEnvers = exerciseToCopyFrom.isFromEnvers;
	}
}