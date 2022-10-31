package de.uni_due.s3.jack3.entities.tenant;

import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.uni_due.s3.jack3.annotations.DeepCopyOmitField;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;
/**
 * Represents an exercise resource for a specific stage with specific name and description.
 *
 * @author lukas.glaser
 *
 */
@Audited
@Entity
@XStreamAlias("StageResource")
public class StageResource extends AbstractEntity implements DeepCopyable<StageResource> {

	private static final long serialVersionUID = -743333543901183241L;

	// This field is used to capture the value of the column named
	// in the @OrderColumn annotation on the referencing entity.
	@Column(insertable = false, updatable = false)
	private int resources_order;

	@ManyToOne(cascade = { CascadeType.PERSIST })
	@DeepCopyOmitField(
		reason = "the resource reference has to be handled by the caller, since the resource might not have already been deepCopied")
	private ExerciseResource exerciseResource;

	@Column
	@Type(type = "text")
	private String description;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="stage_id")
	@DeepCopyOmitField(
			reason = "the stage reference has to be handled by the caller, since the stage might not have already been deepCopied")
	private Stage stage;

	public StageResource() {
	}

	public StageResource(ExerciseResource exerciseResource) {
		this.exerciseResource = Objects.requireNonNull(exerciseResource, "You must specify an exercise resource.");
		description = exerciseResource.getDescription();
	}

	public ExerciseResource getExerciseResource() {
		return exerciseResource;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	@Override
	public StageResource deepCopy() {
		StageResource stageResourceDeepCopy = new StageResource();

		stageResourceDeepCopy.description = description;
		stageResourceDeepCopy.exerciseResource = exerciseResource; // We keep the existing reference!
		stageResourceDeepCopy.resources_order = resources_order;

		return stageResourceDeepCopy;
	}

	public void updateExerciseResourceReference(ExerciseResource exerciseResource) {
		this.exerciseResource = exerciseResource;
	}
}
