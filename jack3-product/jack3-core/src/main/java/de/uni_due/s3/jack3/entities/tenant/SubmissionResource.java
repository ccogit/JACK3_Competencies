package de.uni_due.s3.jack3.entities.tenant;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import org.hibernate.envers.Audited;

/**
 * Concrete Subclass for representing Data attached to Submissions.
 */
@Audited
@Entity
public class SubmissionResource extends Resource {

	private static final long serialVersionUID = 7357983224473205538L;

	public SubmissionResource() {
		super();
	}

	@ManyToOne
	private ExerciseResource fromExerciseResource;

	@ManyToOne
	private CheckerConfiguration fromChecker;

	public SubmissionResource(String filename, byte[] content, User lastEditor, String description,
			ExerciseResource fromExerciseResource, CheckerConfiguration fromChecker) {
		super(filename, content, lastEditor, description);
		this.fromExerciseResource = fromExerciseResource;
		this.fromChecker = fromChecker;
	}

	public ExerciseResource getFromExerciseResource() {
		return fromExerciseResource;
	}

	public void setFromExerciseResource(ExerciseResource fromExerciseResource) {
		this.fromExerciseResource = fromExerciseResource;
	}

	public CheckerConfiguration getFromChecker() {
		return fromChecker;
	}

	public void setFromChecker(CheckerConfiguration fromChecker) {
		this.fromChecker = fromChecker;
	}

	@Override
	public Resource deepCopy() {
		throw new UnsupportedOperationException(
				"Deep copying of " + this.getClass().getSimpleName() + " is not yet implemented");
	}
}
