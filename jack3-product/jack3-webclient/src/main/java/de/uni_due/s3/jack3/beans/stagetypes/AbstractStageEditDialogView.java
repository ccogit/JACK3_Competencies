package de.uni_due.s3.jack3.beans.stagetypes;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.primefaces.event.ReorderEvent;

import de.uni_due.s3.jack3.beans.AbstractView;
import de.uni_due.s3.jack3.beans.ExerciseEditView;
import de.uni_due.s3.jack3.entities.tenant.ExerciseResource;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageHint;
import de.uni_due.s3.jack3.entities.tenant.StageResource;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;
import de.uni_due.s3.jack3.entities.tenant.VariableUpdate;

// REVIEW bo: Ich finde die Benamsung hier verbesserungsbedürftig. Mir ist aus dem Namen nicht so wirklich klar geworden,
// dass es sich hier quasi um die View für studentische Eingaben während der Bearbeitung der Stage handelt (?) und
// gleichzeitig scheinbar auch in der Lecturer-View genutzt wird um die jeweilige Stage zu konfigurieren.
//
// Zusätzlich werden im Exercise-Player Instanzen dieser Klasse "stageBean", was ich nicht sehr sprechend finde. "Bean"
// sollte man m.E. aus allen Bezeichnungen entfernen, wir nennen ja einen Kurs auch nicht "CourseEntity".
public abstract class AbstractStageEditDialogView extends AbstractView implements Serializable {

	private static final long serialVersionUID = -5495450832776423945L;

	protected ExerciseEditView parentView;

	private Long variableIdToUpdateOnEnter;
	private Long variableIdToUpdateBeforeCheck;
	private Long variableIdToUpdateAfterCheck;
	private Long variableIdToUpdateOnNormalExit;
	private Long variableIdToUpdateOnRepeat;
	private Long variableIdToUpdateOnSkip;

	/*
	 * Abstract method declarations to be implemented by each subclass specifically for a particular stage type.
	 */

	public abstract void setStage(Stage stage);

	public abstract Stage getStage();

	/*
	 * Universal methods for any stage type.
	 */

	public void setParentView(ExerciseEditView parentView) {
		this.parentView = parentView;
	}

	public void addNewSkipTransition() {
		getStage().addSkipTransition(new StageTransition());
	}

	public void removeSkipTransition(StageTransition transition) {
		getStage().removeSkipTransition(transition);
	}

	public void onSkipReorder(ReorderEvent event) {
		final int from = event.getFromIndex();
		final int to = event.getToIndex();
		getStage().reorderSkipTransition(from, to);
	}

	public void addNewHint() {
		getStage().addHint(new StageHint());
	}

	public void removeHint(StageHint hint) {
		getStage().removeHint(hint);
	}

	public void onHintReorder(ReorderEvent event) {
		final int from = event.getFromIndex();
		final int to = event.getToIndex();
		StageHint currentHint = getStage().getHints().get(from);
		getStage().removeHint(currentHint);
		getStage().addHintAtIndex(to, currentHint);
	}

	public void setVariableIdToUpdateOnEnter(Long variableDeclarationId) {
		variableIdToUpdateOnEnter = variableDeclarationId;
	}

	public void setVariableIdToUpdateBeforeCheck(Long variableDeclarationId) {
		variableIdToUpdateBeforeCheck = variableDeclarationId;
	}

	public void setVariableIdToUpdateAfterCheck(Long variableDeclarationId) {
		variableIdToUpdateAfterCheck = variableDeclarationId;
	}

	public void setVariableIdToUpdateOnNormalExit(Long variableDeclarationId) {
		variableIdToUpdateOnNormalExit = variableDeclarationId;
	}

	public void setVariableIdToUpdateOnRepeat(Long variableDeclarationId) {
		variableIdToUpdateOnRepeat = variableDeclarationId;
	}

	public void setVariableIdToUpdateOnSkip(Long variableDeclarationId) {
		variableIdToUpdateOnSkip = variableDeclarationId;
	}

	public Long getVariableIdToUpdateOnEnter() {
		return variableIdToUpdateOnEnter;
	}

	public Long getVariableIdToUpdateBeforeCheck() {
		return variableIdToUpdateBeforeCheck;
	}

	public Long getVariableIdToUpdateAfterCheck() {
		return variableIdToUpdateAfterCheck;
	}

	public Long getVariableIdToUpdateOnNormalExit() {
		return variableIdToUpdateOnNormalExit;
	}

	public Long getVariableIdToUpdateOnRepeat() {
		return variableIdToUpdateOnRepeat;
	}

	public Long getVariableIdToUpdateOnSkip() {
		return variableIdToUpdateOnSkip;
	}

	public void addVariableUpdateOnEnter() {
		for (VariableDeclaration variableDeclaration : parentView.getExercise().getVariableDeclarations()) {
			if (variableDeclaration.getId() == variableIdToUpdateOnEnter) {
				getStage().addVariableUpdateOnEnter(new VariableUpdate(variableDeclaration));
				break;
			}
		}
	}

	public void addVariableUpdateBeforeCheck() {
		for (VariableDeclaration variableDeclaration : parentView.getExercise().getVariableDeclarations()) {
			if (variableDeclaration.getId() == variableIdToUpdateBeforeCheck) {
				getStage().addVariableUpdateBeforeCheck(new VariableUpdate(variableDeclaration));
				break;
			}
		}
	}

	public void addVariableUpdateAfterCheck() {
		for (VariableDeclaration variableDeclaration : parentView.getExercise().getVariableDeclarations()) {
			if (variableDeclaration.getId() == variableIdToUpdateAfterCheck) {
				getStage().addVariableUpdateAfterCheck(new VariableUpdate(variableDeclaration));
				break;
			}
		}
	}

	public void addVariableUpdateOnNormalExit() {
		for (VariableDeclaration variableDeclaration : parentView.getExercise().getVariableDeclarations()) {
			if (variableDeclaration.getId() == variableIdToUpdateOnNormalExit) {
				getStage().addVariableUpdateOnNormalExit(new VariableUpdate(variableDeclaration));
				break;
			}
		}
	}

	public void addVariableUpdateOnRepeat() {
		for (VariableDeclaration variableDeclaration : parentView.getExercise().getVariableDeclarations()) {
			if (variableDeclaration.getId() == variableIdToUpdateOnRepeat) {
				getStage().addVariableUpdateOnRepeat(new VariableUpdate(variableDeclaration));
				break;
			}
		}
	}

	public void addVariableUpdateOnSkip() {
		for (VariableDeclaration variableDeclaration : parentView.getExercise().getVariableDeclarations()) {
			if (variableDeclaration.getId() == variableIdToUpdateOnSkip) {
				getStage().addVariableUpdateOnSkip(new VariableUpdate(variableDeclaration));
				break;
			}
		}
	}

	public void removeVariableUpdate(VariableUpdate variableUpdate) {
		getStage().removeVariableUpdate(variableUpdate);
	}

	public void onVariableUpdateOnEnterReorder(ReorderEvent event) {
		getStage().moveVariableUpdateOnEnter(event.getFromIndex(), event.getToIndex());
	}

	public void onVariableUpdateBeforeCheckReorder(ReorderEvent event) {
		getStage().moveVariableUpdateBeforeCheck(event.getFromIndex(), event.getToIndex());
	}

	public void onVariableUpdateAfterCheckReorder(ReorderEvent event) {
		getStage().moveVariableUpdateAfterCheck(event.getFromIndex(), event.getToIndex());
	}

	public void onVariableUpdateOnNormalExitReorder(ReorderEvent event) {
		getStage().moveVariableUpdateOnNormalExit(event.getFromIndex(), event.getToIndex());
	}

	public void onVariableUpdateOnRepeatReorder(ReorderEvent event) {
		getStage().moveVariableUpdateOnRepeat(event.getFromIndex(), event.getToIndex());
	}

	public void onVariableUpdateOnSkipReorder(ReorderEvent event) {
		getStage().moveVariableUpdateOnSkip(event.getFromIndex(), event.getToIndex());
	}

	public void addResourceToStage(ExerciseResource exerciseResource) {
		getStage().addStageResource(new StageResource(exerciseResource));
	}
	
	public void onTransitionReorder(ReorderEvent event) {
		final int from = event.getFromIndex();
		final int to = event.getToIndex();

		getStage().reorderStageTransition(from, to);
	}

	public void removeStageResource(StageResource stageResource) {
		getStage().removeStageResource(stageResource);
	}

	/**
	 * Gets a set of exercise resources that were not already added to the stage
	 */
	public Set<ExerciseResource> filterExerciseResources(Set<ExerciseResource> allExerciseResources) {
		Set<ExerciseResource> filteredExerciseResources = new HashSet<>();
		Set<ExerciseResource> existingExerciseResources = getStage().getStageResources().stream()
				.map(x -> x.getExerciseResource()).collect(Collectors.toSet());

		for (ExerciseResource exerciseResource : allExerciseResources) {
			if (!existingExerciseResources.contains(exerciseResource)) {
				filteredExerciseResources.add(exerciseResource);
			}
		}

		return filteredExerciseResources;
	}
}
