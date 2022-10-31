package de.uni_due.s3.jack3.entities.tenant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;
import de.uni_due.s3.jack3.utils.DeepCopyHelper;

@NamedQuery(name = Stage.STAGE_BY_ID, query = "select s FROM Stage s WHERE s.id = :id")
/*
 * Abstract Class for representing a Stage of an Exercise. Subclasses are sorted in Package "de.uni_due.s3.jack3.entities.stagetypes.*" .
 */
@Audited
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Stage extends AbstractEntity implements Comparable<Stage>, DeepCopyable<Stage> {

	private static final long serialVersionUID = 2122075862333473300L;

	public static final String STAGE_BY_ID = "StageById";

	@ToString
	@Column
	@Type(type = "text")
	private String internalName;

	@ToString
	@Column
	@Type(type = "text")
	private String externalName;

	@ToString
	@Column
	@Type(type = "text")
	private String taskDescription;

	@Column
	@Type(type = "text")
	private String skipMessage;

	@Transient
	@XStreamOmitField
	private List<AbstractEntity> listOfStageEntitiesToRemoveBySaving = new ArrayList<>();

	/**
	 * The default transition. It is chosen if no other transitions are defined or if no transition-specific condition
	 * is fulfilled.
	 */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private StageTransition defaultTransition;

	/**
	 * Transitions that are chosen if the user skips this stage.
	 */
	@ToString
	@ManyToMany(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
	@JoinTable( name = "stage_skipTransitions",
	joinColumns =  @JoinColumn(name="stage_id"),
	inverseJoinColumns = @JoinColumn(name = "stagetransition_id")
			)
	@OrderColumn(name="skipTransitions_order")
	private List<StageTransition> skipTransitions = new ArrayList<>();

	/**
	 * Additional transitions.
	 */
	@ToString
	@ManyToMany(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
	@JoinTable( name = "stage_stageTransitions",
	joinColumns =  @JoinColumn(name="stage_id"),
	inverseJoinColumns = @JoinColumn(name = "stagetransition_id")
			)
	@OrderColumn(name="stageTransitions_order")
	protected List<StageTransition> stageTransitions = new ArrayList<>();

	@OneToMany(mappedBy="stage", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@OrderColumn(name="stagehint_order")
	@AuditMappedBy(mappedBy = "stage", positionMappedBy = "stagehint_order") // fixes #316
	private List<StageHint> hints = new ArrayList<>();

	@ToString
	@ManyToMany(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
	@JoinTable(
		name = "stage_variableUpdatesOnEnter",
		joinColumns = @JoinColumn(name = "stage_id"),
		inverseJoinColumns = @JoinColumn(name = "variableupdate_id"))
	@OrderColumn(name = "variableUpdatesOnEnter_order")
	private List<VariableUpdate> variableUpdatesOnEnter = new ArrayList<>();

	@ToString
	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinTable( name = "stage_variableUpdatesBeforeCheck",
	joinColumns =  @JoinColumn(name="stage_id"),
	inverseJoinColumns = @JoinColumn(name = "variableupdate_id")
			)
	@OrderColumn(name="variableUpdatesBeforeCheck_order")
	private List<VariableUpdate> variableUpdatesBeforeCheck = new ArrayList<>();

	@ToString
	@ManyToMany(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
	@JoinTable( name = "stage_variableUpdatesAfterCheck",
	joinColumns =  @JoinColumn(name="stage_id"),
	inverseJoinColumns = @JoinColumn(name = "variableupdate_id")
			)
	@OrderColumn(name="variableUpdatesAfterCheck_order")
	private List<VariableUpdate> variableUpdatesAfterCheck = new ArrayList<>();

	@ToString
	@ManyToMany(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
	@JoinTable( name = "stage_variableUpdatesOnNormalExit",
	joinColumns =  @JoinColumn(name="stage_id"),
	inverseJoinColumns = @JoinColumn(name = "variableupdate_id")
			)
	@OrderColumn(name="variableUpdatesOnNormalExit_order")
	private List<VariableUpdate> variableUpdatesOnNormalExit = new ArrayList<>();

	@ToString
	@ManyToMany(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
	@JoinTable( name = "stage_variableUpdatesOnRepeat",
	joinColumns =  @JoinColumn(name="stage_id"),
	inverseJoinColumns = @JoinColumn(name = "variableupdate_id")
			)
	@OrderColumn(name="variableUpdatesOnRepeat_order")
	private List<VariableUpdate> variableUpdatesOnRepeat = new ArrayList<>();

	@ToString
	@ManyToMany(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
	@JoinTable( name = "stage_variableUpdatesOnSkip",
	joinColumns =  @JoinColumn(name="stage_id"),
	inverseJoinColumns = @JoinColumn(name = "variableupdate_id")
			)
	@OrderColumn(name="variableUpdatesOnSkip_order")
	private List<VariableUpdate> variableUpdatesOnSkip = new ArrayList<>();

	@Column(columnDefinition = "int4 default 1")
	private int weight = 1;

	@Column(columnDefinition = "int4 default 0")
	private int orderIndex;

	@Column(nullable = false, columnDefinition = "boolean default false")
	private boolean allowSkip;

	@OneToMany(mappedBy="stage", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@OrderColumn(name="resources_order")
	@AuditMappedBy(mappedBy = "stage", positionMappedBy = "resources_order")
	private List<StageResource> resources = new ArrayList<>();

	public Stage() {
		super();
		StageTransition defaultStageTransiontion = new StageTransition();
		defaultTransition = defaultStageTransiontion;
	}

	public String getType() {
		final String className = getClass().getSimpleName();
		final String typeName = Character.toLowerCase(className.charAt(0)) + className.substring(1);

		if (typeName.endsWith("Stage")) {
			return typeName.substring(0,className.length() - 5);
		}

		return typeName;
	}

	// This field is temporarily used while deep copying. We save a mapping from the envers-id to the deepCopyed-id of
	// StageTransitions here, so we can later update the "Stage target" to the deep copied values, since the deep copied
	// target stages might not even exist at the time this StageTransition is deep copied!
	@Transient
	@XStreamOmitField
	private Map<Long, Long> transitionsIdMap = new HashMap<>();

	public Map<Long, Long> getTransitionsIdMap() {
		return transitionsIdMap;
	}

	protected void deepCopyStageVars(Stage stageFromEnvers) {
		allowSkip = stageFromEnvers.allowSkip;

		defaultTransition = DeepCopyHelper.deepCopyOrNull(stageFromEnvers.defaultTransition);

		externalName = stageFromEnvers.externalName;

		for (StageHint stageHint : stageFromEnvers.getHints()) {
			StageHint deepCopyStageHint = stageHint.deepCopy();
			deepCopyStageHint.setStage(this);
			hints.add(deepCopyStageHint);
		}

		internalName = stageFromEnvers.internalName;

		orderIndex = stageFromEnvers.orderIndex;

		for (StageResource stageResource : stageFromEnvers.getStageResources()) {
			StageResource deepCopyStageResource = stageResource.deepCopy();
			deepCopyStageResource.setStage(this);
			resources.add(deepCopyStageResource);
		}

		skipMessage = stageFromEnvers.skipMessage;

		for (StageTransition stageTransitionFromEnvers : stageFromEnvers.getStageTransitions()) {
			StageTransition deepCopystageTransition = stageTransitionFromEnvers.deepCopy();
			stageTransitions.add(deepCopystageTransition);
			transitionsIdMap.put(stageTransitionFromEnvers.getId(), deepCopystageTransition.getId());
		}

		for (StageTransition skipTransition : stageFromEnvers.getSkipTransitions()) {
			StageTransition deepCopyskipTransition = skipTransition.deepCopy();
			skipTransitions.add(deepCopyskipTransition);
			transitionsIdMap.put(skipTransition.getId(), deepCopyskipTransition.getId());
		}

		taskDescription = stageFromEnvers.taskDescription;

		for (VariableUpdate variableUpdate : stageFromEnvers.variableUpdatesOnEnter) {
			variableUpdatesOnEnter.add(variableUpdate.deepCopy());
		}

		for (VariableUpdate variableUpdate : stageFromEnvers.variableUpdatesAfterCheck) {
			variableUpdatesAfterCheck.add(variableUpdate.deepCopy());
		}

		for (VariableUpdate variableUpdate : stageFromEnvers.variableUpdatesBeforeCheck) {
			variableUpdatesBeforeCheck.add(variableUpdate.deepCopy());
		}

		for (VariableUpdate variableUpdate : stageFromEnvers.variableUpdatesOnNormalExit) {
			variableUpdatesOnNormalExit.add(variableUpdate.deepCopy());
		}

		for (VariableUpdate variableUpdate : stageFromEnvers.variableUpdatesOnRepeat) {
			variableUpdatesOnRepeat.add(variableUpdate.deepCopy());
		}

		for (VariableUpdate variableUpdate : stageFromEnvers.variableUpdatesOnSkip) {
			variableUpdatesOnSkip.add(variableUpdate.deepCopy());
		}

		weight = stageFromEnvers.weight;
	}

	public StageTransition getDefaultTransition() {
		return defaultTransition;
	}

	public String getExternalName() {
		return externalName;
	}

	/*
	 * @return unmodifiableList of hints
	 */
	public List<StageHint> getHints() {
		return Collections.unmodifiableList(hints);
	}

	public List<StageHint> getHintsForReorder() {
		List<StageHint> hintListForReoder = new ArrayList<>(hints.size());
		hintListForReoder.addAll(hints);
		return hintListForReoder;
	}

	public void addHintAtIndex(int index, StageHint stageHint) {
		hints.add(index, stageHint);
	}

	/*
	 * @return unmodifiableList of variableUpdates
	 */
	public List<VariableUpdate> getVariableUpdatesOnEnter() {
		return Collections.unmodifiableList(variableUpdatesOnEnter);
	}

	public List<VariableUpdate> getVariableUpdatesBeforeCheck() {
		return Collections.unmodifiableList(variableUpdatesBeforeCheck);
	}

	public List<VariableUpdate> getVariableUpdatesAfterCheck() {
		return Collections.unmodifiableList(variableUpdatesAfterCheck);
	}

	public List<VariableUpdate> getVariableUpdatesOnNormalExit() {
		return Collections.unmodifiableList(variableUpdatesOnNormalExit);
	}

	public List<VariableUpdate> getVariableUpdatesOnRepeat() {
		return Collections.unmodifiableList(variableUpdatesOnRepeat);
	}

	public List<VariableUpdate> getVariableUpdatesOnSkip() {
		return Collections.unmodifiableList(variableUpdatesOnSkip);
	}

	public List<VariableUpdate> getVariableUpdatesOnEnterForReorder() {
		return copyList(variableUpdatesOnEnter);
	}

	public List<VariableUpdate> getVariableUpdatesBeforeCheckForReorder() {
		return copyList(variableUpdatesBeforeCheck);
	}

	public List<VariableUpdate> getVariableUpdatesAfterCheckForReorder() {
		return copyList(variableUpdatesAfterCheck);
	}

	public List<VariableUpdate> getVariableUpdatesOnNormalExitForReorder() {
		return copyList(variableUpdatesOnNormalExit);
	}

	public List<VariableUpdate> getVariableUpdatesOnRepeatForReorder() {
		return copyList(variableUpdatesOnRepeat);
	}

	public List<VariableUpdate> getVariableUpdatesOnSkipForReorder() {
		return copyList(variableUpdatesOnSkip);
	}

	private List<VariableUpdate> copyList(List<VariableUpdate> variableUpdates) {
		List<VariableUpdate> variableUpdatesCopy = new ArrayList<>(variableUpdates.size());
		variableUpdatesCopy.addAll(variableUpdates);
		return variableUpdatesCopy;
	}

	public void addVariableUpdateOnEnter(VariableUpdate variableUpdate) {
		variableUpdatesOnEnter.add(variableUpdate);
	}

	public void addVariableUpdateBeforeCheck(VariableUpdate variableUpdate) {
		variableUpdatesBeforeCheck.add(variableUpdate);
	}

	public void addVariableUpdateAfterCheck(VariableUpdate variableUpdate) {
		variableUpdatesAfterCheck.add(variableUpdate);
	}

	public void addVariableUpdateOnNormalExit(VariableUpdate variableUpdate) {
		variableUpdatesOnNormalExit.add(variableUpdate);
	}

	public void addVariableUpdateOnRepeat(VariableUpdate variableUpdate) {
		variableUpdatesOnRepeat.add(variableUpdate);
	}

	public void addVariableUpdateOnSkip(VariableUpdate variableUpdate) {
		variableUpdatesOnSkip.add(variableUpdate);
	}

	public void moveVariableUpdateOnEnter(final int from, final int to) {
		VariableUpdate currentUpdate = variableUpdatesOnEnter.get(from);
		variableUpdatesOnEnter.remove(currentUpdate);
		variableUpdatesOnEnter.add(to, currentUpdate);
	}

	public void addVariableUpdateOnEnterAtIndex(int index, VariableUpdate variableUpdate) {
		variableUpdatesOnEnter.add(index, variableUpdate);
	}

	public void moveVariableUpdateBeforeCheck(final int from, final int to) {
		VariableUpdate currentUpdate = variableUpdatesBeforeCheck.get(from);
		variableUpdatesBeforeCheck.remove(currentUpdate);
		variableUpdatesBeforeCheck.add(to, currentUpdate);
	}

	public void addVariableUpdateBeforeCheckAtIndex(int index, VariableUpdate variableUpdate) {
		variableUpdatesBeforeCheck.add(index, variableUpdate);
	}

	public void moveVariableUpdateAfterCheck(final int from, final int to) {
		VariableUpdate currentUpdate = variableUpdatesAfterCheck.get(from);
		variableUpdatesAfterCheck.remove(currentUpdate);
		variableUpdatesAfterCheck.add(to, currentUpdate);
	}

	public void addVariableUpdateAfterCheckAtIndex(int index, VariableUpdate variableUpdate) {
		variableUpdatesAfterCheck.add(index, variableUpdate);
	}

	public void moveVariableUpdateOnNormalExit(final int from, final int to) {
		VariableUpdate currentUpdate = variableUpdatesOnNormalExit.get(from);
		variableUpdatesOnNormalExit.remove(currentUpdate);
		variableUpdatesOnNormalExit.add(to, currentUpdate);
	}

	public void addVariableUpdateOnNormalExitAtIndex(int index, VariableUpdate variableUpdate) {
		variableUpdatesOnNormalExit.add(index, variableUpdate);
	}

	public void moveVariableUpdateOnRepeat(final int from, final int to) {
		VariableUpdate currentUpdate = variableUpdatesOnRepeat.get(from);
		variableUpdatesOnRepeat.remove(currentUpdate);
		variableUpdatesOnRepeat.add(to, currentUpdate);
	}

	public void addVariableUpdateOnRepeatAtIndex(int index, VariableUpdate variableUpdate) {
		variableUpdatesOnRepeat.add(index, variableUpdate);
	}

	public void moveVariableUpdateOnSkip(final int from, final int to) {
		VariableUpdate currentUpdate = variableUpdatesOnSkip.get(from);
		variableUpdatesOnSkip.remove(currentUpdate);
		variableUpdatesOnSkip.add(to, currentUpdate);
	}

	public void addVariableUpdateOnSkipAtIndex(int index, VariableUpdate variableUpdate) {
		variableUpdatesOnSkip.add(index, variableUpdate);
	}

	public void removeVariableUpdate(VariableUpdate variableUpdate) {
		variableUpdatesOnEnter.remove(variableUpdate);
		variableUpdatesBeforeCheck.remove(variableUpdate);
		variableUpdatesAfterCheck.remove(variableUpdate);
		variableUpdatesOnNormalExit.remove(variableUpdate);
		variableUpdatesOnRepeat.remove(variableUpdate);
		variableUpdatesOnSkip.remove(variableUpdate);
		listOfStageEntitiesToRemoveBySaving.add(variableUpdate);
	}

	public void removeAllUpdatesForVariable(VariableDeclaration variableDeclaration) {
		Predicate<VariableUpdate> refersToVariable = variableUpdate -> variableUpdate.getVariableReference()
				.equals(variableDeclaration);

		Set<VariableUpdate> varUpdatesToRemove = new HashSet<>();
		varUpdatesToRemove
				.addAll(getVariableUpdateFromListForVariableDeclaration(variableUpdatesOnEnter, variableDeclaration));
		varUpdatesToRemove.addAll(getVariableUpdateFromListForVariableDeclaration(variableUpdatesBeforeCheck,variableDeclaration));
		varUpdatesToRemove.addAll(getVariableUpdateFromListForVariableDeclaration(variableUpdatesAfterCheck,variableDeclaration));
		varUpdatesToRemove.addAll(getVariableUpdateFromListForVariableDeclaration(variableUpdatesOnNormalExit,variableDeclaration));
		varUpdatesToRemove.addAll(getVariableUpdateFromListForVariableDeclaration(variableUpdatesOnRepeat,variableDeclaration));
		varUpdatesToRemove.addAll(getVariableUpdateFromListForVariableDeclaration(variableUpdatesOnSkip,variableDeclaration));
		listOfStageEntitiesToRemoveBySaving.addAll(varUpdatesToRemove);

		variableUpdatesOnEnter.removeIf(refersToVariable);
		variableUpdatesBeforeCheck.removeIf(refersToVariable);
		variableUpdatesAfterCheck.removeIf(refersToVariable);
		variableUpdatesOnNormalExit.removeIf(refersToVariable);
		variableUpdatesOnRepeat.removeIf(refersToVariable);
		variableUpdatesOnSkip.removeIf(refersToVariable);
	}

	private Set<VariableUpdate> getVariableUpdateFromListForVariableDeclaration(List<VariableUpdate> list, VariableDeclaration variableDeclaration){
		Set<VariableUpdate> varUpdatesFoundInList = new HashSet<>();
		for(VariableUpdate varUpdate : list) {
			if(varUpdate.getVariableReference().equals(variableDeclaration)) {
				varUpdatesFoundInList.add(varUpdate);
			}
		}
		return varUpdatesFoundInList;
	}

	public String getInternalName() {
		return internalName;
	}

	public int getOrderIndex() {
		return orderIndex;
	}

	public String getSkipMessage() {
		return skipMessage;
	}

	/*
	 * @return unmodifiableList of skipTransitions
	 */
	public List<StageTransition> getSkipTransitions() {
		return Collections.unmodifiableList(skipTransitions);
	}

	/*
	 * @return unmodifiableList of stageTransitions
	 */
	public List<StageTransition> getStageTransitions() {
		return Collections.unmodifiableList(stageTransitions);
	}

	public List<StageTransition> getStageTransitionsForReorder() {
		List<StageTransition> stageTransitionsForReoder = new ArrayList<>(stageTransitions.size());
		stageTransitionsForReoder.addAll(stageTransitions);
		return stageTransitionsForReoder;
	}

	public String getTaskDescription() {
		return taskDescription;
	}

	public void setDefaultTransition(StageTransition defaultTransition) {
		this.defaultTransition = defaultTransition;
	}

	public void setExternalName(String externalName) {
		this.externalName = externalName;
	}

	public void setInternalName(String internalName) {
		this.internalName = requireIdentifier(internalName, "Internal name must not be null or empty.");
	}

	public void setOrderIndex(int orderIndex) {
		this.orderIndex = orderIndex;
	}

	public void setSkipMessage(String skipMessage) {
		this.skipMessage = skipMessage;
	}

	public void setTaskDescription(String taskDescription) {
		this.taskDescription = taskDescription;
	}

	public void addHint(StageHint hint) {
		hint.setStage(this);
		hints.add(hint);
	}

	public void removeHint(StageHint hint) {
		hints.remove(hint);
	}

	public void addSkipTransitionAtIndex(int index, StageTransition transition) {
		skipTransitions.add(index, transition);
	}

	public List<StageTransition> getSkipTransitionsForReorder() {
		List<StageTransition> skipTransitionsForReoder = new ArrayList<>(skipTransitions.size());
		skipTransitionsForReoder.addAll(skipTransitions);
		return skipTransitionsForReoder;
	}

	public void addSkipTransition(StageTransition transition) {
		skipTransitions.add(transition);
	}

	public void removeSkipTransition(StageTransition transition) {
		skipTransitions.remove(transition);
		listOfStageEntitiesToRemoveBySaving.add(transition);
	}

	public void addStageTransition(StageTransition transition) {
		stageTransitions.add(transition);
	}

	public void removeStageTransition(StageTransition transition) {
		stageTransitions.remove(transition);
		listOfStageEntitiesToRemoveBySaving.add(transition);
	}

	public void reorderStageTransition(int from, int to) {
		stageTransitions.add(to, stageTransitions.remove(from));
	}

	public void reorderSkipTransition(int from, int to) {
		skipTransitions.add(to, skipTransitions.remove(from));
	}

	public boolean getAllowSkip() {
		return allowSkip;
	}

	public void setAllowSkip(boolean allowSkip) {
		this.allowSkip = allowSkip;
	}

	public List<StageResource> getStageResources() {
		return resources;
	}

	public void addStageResource(StageResource stageResource) {
		stageResource.setStage(this);
		resources.add(stageResource);
	}

	public void removeStageResource(StageResource stageResource) {
		resources.remove(stageResource);
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	@Override
	public int compareTo(Stage other) {
		// REVIEW bz - This violates the compareTo contract.
		// "Note that null is not an instance of any class, and e.compareTo(null) should throw a NullPointerException"
		if (other == null) {
			return -1;
		}

		return Long.compare(getId(), other.getId());
	}

	public boolean isEndStage() {
		if (defaultTransition.getTarget() == null) {
			return true;
		}

		for (StageTransition transition : skipTransitions) {
			if (transition.getTarget() == null) {
				return true;
			}
		}

		for (StageTransition transition : stageTransitions) {
			if (transition.getTarget() == null) {
				return true;
			}
		}

		return false;
	}

	public boolean leadsTo(Stage stage) {
		if ((defaultTransition.getTarget() != null) && defaultTransition.getTarget().equals(stage)) {
			return true;
		}

		for (StageTransition transition : skipTransitions) {
			if ((transition.getTarget() != null) && transition.getTarget().equals(stage)) {
				return true;
			}
		}

		for (StageTransition transition : stageTransitions) {
			if ((transition.getTarget() != null) && transition.getTarget().equals(stage)) {
				return true;
			}
		}

		return false;
	}

	public List<AbstractEntity> getListOfStageEntitiesToRemoveBySaving(){
		if(listOfStageEntitiesToRemoveBySaving == null) {
			listOfStageEntitiesToRemoveBySaving = new ArrayList<>();
		}
		return listOfStageEntitiesToRemoveBySaving;
	}

	public boolean isHasTestcaseTuples() {
		return false;
	}

	public abstract boolean mustWaitForPendingJobs();

	/**
	 * This method is called when a deep copy of an exercise is created. It passes a map with the old and the new
	 * exercise resources, so that the stage can update references if necessary.
	 */
	public void updateResourceReferences(Map<ExerciseResource, ExerciseResource> referenceMap) {
		resources.forEach(
				resource -> resource.updateExerciseResourceReference(referenceMap.get(resource.getExerciseResource())));
	}
}
