package de.uni_due.s3.jack3.builders;

import java.util.Objects;

import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageHint;

/**
 * Builder for sample stages
 * 
 * @author lukas.glaser
 *
 * @param <T>
 *            Stage type
 * @param <R>
 *            AbstractStageBuilder sub type, workaround to return the stage-specific builder (sub class) and not only
 *            this class.
 */
public abstract class AbstractStageBuilder<T extends Stage, R extends AbstractStageBuilder<T, R>> {

	private final ExerciseBuilder parent;
	protected final T stage;

	/**
	 * Creates a new instance of this builder without an exercise.
	 */
	protected AbstractStageBuilder() {
		this.parent = null;
		stage = getNewStage();
	}

	/**
	 * Creates a new instance of this builder with an exercise.
	 * 
	 * @param parent
	 *            parent ExerciseBuilder
	 */
	protected AbstractStageBuilder(ExerciseBuilder parent) {
		this.parent = Objects.requireNonNull(parent);
		stage = getNewStage();
	}

	/**
	 * Override this method with an empty object of the stage
	 */
	protected abstract T getNewStage();

	/**
	 * Add the stage to the exercise. This is only possible if the stage builder was instantiated with an exercise
	 * builder.
	 * 
	 * @return Parent exercise builder
	 */
	public final ExerciseBuilder and() {
		if (parent == null) {
			throw new UnsupportedOperationException("Uninitialized ExerciseBuilder. Call 'getStage()' instead.");
		}
		parent.addStage(stage);
		return parent;
	}

	/**
	 * Get the built stage.
	 */
	public T getStage() {
		return stage;
	}

	/**
	 * Adds a title text (external name) to this stage.
	 * 
	 * @param title
	 *            The external name of this stage
	 */
	@SuppressWarnings("unchecked")
	public final R withTitle(String title) {
		stage.setExternalName(title);
		return (R) this;
	}

	/**
	 * Adds a title text (internal name) to this stage.
	 * 
	 * @param title
	 *            The internal name of this stage
	 */
	@SuppressWarnings("unchecked")
	public final R withInternalName(String title) {
		stage.setInternalName(title);
		return (R) this;
	}

	/**
	 * Adds description text to this stage.
	 * 
	 * @param description
	 *            The description of this stage
	 */
	@SuppressWarnings("unchecked")
	public final R withDescription(String description) {
		stage.setTaskDescription(description);
		return (R) this;
	}

	/**
	 * Adds a new hint to this stage.
	 * 
	 * @param text
	 *            The hint text which is shown to the user.
	 */
	@SuppressWarnings("unchecked")
	public final R withHint(String text) {
		StageHint hint = new StageHint();
		hint.setText(text);
		stage.addHint(hint);
		return (R) this;
	}

	/**
	 * Adds a new hint to this stage.
	 * 
	 * @param text
	 *            The hint text which is shown to the user.
	 * @param malus
	 *            points for giving the hint.
	 */
	@SuppressWarnings("unchecked")
	public final R withHint(String text, int malus) {
		StageHint hint = new StageHint();
		hint.setText(text);
		hint.setMalus(malus);
		stage.addHint(hint);
		return (R) this;
	}

	/**
	 * Allows users to skip the stage.
	 */
	@SuppressWarnings("unchecked")
	public final R allowSkip() {
		stage.setAllowSkip(true);
		return (R) this;
	}

	/**
	 * Allows users to skip the stage with the specified message
	 * 
	 * @param message
	 *            The text which is shown when the user skips this stage.
	 */
	@SuppressWarnings("unchecked")
	public final R allowSkip(String message) {
		stage.setAllowSkip(true);
		stage.setSkipMessage(message);
		return (R) this;
	}

	/**
	 * Sets the weight of the stage (default is 1)
	 * 
	 * @param weight
	 *            The weight of this stage: It is used to calculate the result points of the parent exercise.
	 */
	@SuppressWarnings("unchecked")
	public final R withWeight(int weight) {
		stage.setWeight(weight);
		return (R) this;
	}

}
