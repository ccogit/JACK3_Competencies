package de.uni_due.s3.jack3.entities.tenant;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.Min;

import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.providers.AbstractExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;

@Audited
@Entity
public class CourseEntry extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@ToString
	@ManyToOne(
			fetch = FetchType.EAGER,
			// We need to manually persist or remove Exercises here 
			cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH })
	private Exercise exercise;

	@ToString
	@ManyToOne(
			fetch = FetchType.EAGER,
			// We need to manually persist or remove FrozenExercises here
			cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH })
	private FrozenExercise frozenExercise;

	// This is used in {@link FixedListExerciseProvider} "mappedBy"-attribute and gets set through 
	// {@link FixedListExerciseProvider.addCourseEntryAtIndex(int, CourseEntry)} and 
	// {@link FixedListExerciseProvider.addCourseEntry(CourseEntry)}. It dosn't need a getter, because it's only used 
	// by Hibernate 
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "stage_id")
	private AbstractExerciseProvider abstractExerciseProvider;

	// This field is used to capture the value of the column named
	// in the @OrderColumn annotation on the referencing entity.
	// fixes #316
	@Column(insertable = false, updatable = false)
	private int courseEntry_order;

	/**
	 * The points the exercise gains. The total score in a {@link FixedListExerciseProvider} is calculated based on the
	 * weighting of this points. Can be any positive number.
	 */
	@ToString
	@Column
	@Min(0)
	private int points;

	public CourseEntry() {
		super();
	}

	/**
	 * Constructor for convienence, since we have a lot of places where our exercise is saved as an AbstractExercise.
	 * Here AbstractExercise must be instanceOf Exercise or we get a ClassCast-Exception!
	 */
	public CourseEntry(AbstractExercise exercise, int points) {
		this((Exercise) exercise, points);
	}

	public CourseEntry(Exercise exercise, int points) {
		Objects.requireNonNull(exercise);

		this.exercise = exercise;
		this.points = points;
	}

	public CourseEntry(Exercise exercise, FrozenExercise frozenExercise, final int points) {
		Objects.requireNonNull(exercise);

		if (frozenExercise != null && exercise.getId() != frozenExercise.getProxiedOrRegularExerciseId()) {
			throw new AssertionError(
					"You can only create a Frozen CourseEntry with an Exercise and corresponding FrozenExercise: "
							+ exercise + " and " + frozenExercise);
		}

		this.exercise = exercise;
		this.points = points;
		this.frozenExercise = frozenExercise;
	}

	public int getPoints() {
		return points;
	}

	public void setExercise(Exercise exercise) {
		this.exercise = exercise;
	}

	public void setPoints(int points) {
		// This method is coupled to a spinner element in the UI which also enforces these bounds. However, manual input
		// to that field is only corrected in the UI, but passed to this method without change. So we have to enforce
		// these bounds twice. However, no error message is necessary, since the spinner already shows these values.
		if (points < 0) {
			this.points = 0;
		} else if (points > 100) {
			this.points = 100;
		} else {
			this.points = points;
		}
	}

	public FrozenExercise getFrozenExercise() {
		return frozenExercise;
	}

	public void setFrozenExercise(FrozenExercise frozenExercise) {
		this.frozenExercise = frozenExercise;
	}

	public Exercise getExercise() {
		return exercise;
	}

	public void setAbstractExerciseProvider(AbstractExerciseProvider abstractExerciseProvider) {
		this.abstractExerciseProvider = abstractExerciseProvider;
	}

	/**
	 * Returns the frozen exercise if a frozen version was set, otherwise returns the exercise that was set.
	 */
	@Nonnull
	public AbstractExercise getExerciseOrFrozenExercise() {
		return frozenExercise == null ? exercise : frozenExercise;
	}

}
