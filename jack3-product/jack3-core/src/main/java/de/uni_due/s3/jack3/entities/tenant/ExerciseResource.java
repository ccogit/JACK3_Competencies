package de.uni_due.s3.jack3.entities.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQuery;

import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/*
 * Subclass for representing on Exercise attached Files.
 */
@Audited
@NamedQuery(
		name = ExerciseResource.EXERCISE_RESOURCE_BY_ID,
		query = "SELECT er FROM ExerciseResource er " //
		+ "WHERE er.id = :id")
@Entity
@XStreamAlias("ExerciseResource")
public class ExerciseResource extends Resource {

	private static final long serialVersionUID = -8252448724586641532L;

	public static final String EXERCISE_RESOURCE_BY_ID = "ExerciseResource.exerciseResourceById";

	/**
	 * BEWARE: If you add fields here, they will only be included in exports, when you also update our custom
	 * ExerciseResourceConverter!
	 */
	@Column(columnDefinition = "boolean default false")
	private boolean replacePlaceholder = false;

	public ExerciseResource() {
		super();
	}

	public ExerciseResource(String filename, byte[] content, User lastEditor, String description,
			boolean replacePlaceholder) {
		super(filename, content, lastEditor, description);
		this.replacePlaceholder = replacePlaceholder;
	}

	public boolean isReplacePlaceholder() {
		return replacePlaceholder;
	}

	public void setReplacePlaceholder(boolean replacePlaceholder) {
		this.replacePlaceholder = replacePlaceholder;
	}

	/**
	 * Calling this method can be dangerous, since the file content may contain placeholders that must be replaced.
	 * Consider calling {@code ExerciseBusiness.getExerciseResourceContent()} instead.
	 */
	@Override
	public byte[] getContent() {
		return super.getContent();
	}

	@Override
	public ExerciseResource deepCopy() {
		ExerciseResource exerciseResourceDeepCopy = new ExerciseResource();
		exerciseResourceDeepCopy.replacePlaceholder = replacePlaceholder;

		exerciseResourceDeepCopy.deepCopyResourceVars(this);

		return exerciseResourceDeepCopy;
	}
}
