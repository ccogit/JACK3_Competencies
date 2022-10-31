package de.uni_due.s3.jack3.entities.tenant;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.uni_due.s3.jack3.annotations.DeepCopyOmitField;
import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;
import de.uni_due.s3.jack3.services.utils.RepeatStage;
import de.uni_due.s3.jack3.utils.DeepCopyHelper;

/**
 * Entity representing the transition from one stage to another within an
 * exercise. Each transition may be associated with generic and stage specific
 * expressions defining its trigger condition. A transition can terminate the
 * exercise, force the user to repeat the current stage or name any new stage to
 * be visited.
 */
@Audited
@Entity
public class StageTransition extends AbstractEntity implements DeepCopyable<StageTransition> {

	private static final long serialVersionUID = -4861968221374932968L;

	/**
	 * A generic condition that can be evaluated without knowing any specifics about the stage owning this transition
	 * and user inputs. It is used e.g. in skip transitions without user input.
	 */
	@ToString
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private EvaluatorExpression conditionExpression = new EvaluatorExpression();

	/**
	 * A stage specific condition that can be evaluated by the stage specific business layer bean for the stage owning
	 * this transition. User inputs are included in the evaluation of this expression.
	 */
	@ToString
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private EvaluatorExpression stageExpression = new EvaluatorExpression();

	/**
	 * The target of this transition. The value of this field is either an instance
	 * of {@link Stage} or {@literal null}. If the value is {@literal null}, this
	 * transition either forces the user to repeat the current stage or it
	 * terminates the exercise, depending on the value of field {@link isRepeat}.
	 * BEWARE: When serializing (e.g. with GSON) this can be a circular reference, leading to a stackoverflow
	 * exception. When loading from Envers, Hibernate always returns zero here. This means that
	 * 
	 * <pre>
	 * exerciseFromEnvers.getStagesAsList().get(index).getDefaultTransition().getTarget()
	 * </pre>
	 * 
	 * is always null! Here it helps to load the StageTransition from Hibernate again per Id.
	 */
	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
	@XStreamAlias("targetStage")
	@DeepCopyOmitField(
		reason = "the target reference itself is not deep copied, it should only be a reference to an existing stage. This has to be handled by the caller.")
	private Stage target;

	/**
	 * Boolean flag indicating whether this transition will force users to repeat
	 * the current stage. If set to {@literal true}, field {@link target} is
	 * ignored.
	 */
	@Column
	private boolean isRepeat = false;

	public StageTransition() {
		super();
	}

	public StageTransition(Stage target) {
		if (target instanceof RepeatStage) {
			this.target = null;
			isRepeat = true;
		} else {
			this.target = target;
			isRepeat = false;
		}
	}

	/**
	 * Returns the target stage of this transition. Returns a new instance of
	 * {@link RepeatStage} iff {@link isRepeat()} returns {@literal true}. The
	 * target may be {@literal null} to indicate the end of the exercise.
	 *
	 * @return An instance of {@link Stage} (including {@link RepeatStage}) or null.
	 */
	public Stage getTarget() {
		if (isRepeat) {
			return new RepeatStage();
		}

		return target;
	}

	public void setTarget(Stage target) {
		if (target instanceof RepeatStage) {
			this.target = null;
			isRepeat = true;
		} else {
			this.target = target;
			isRepeat = false;
		}
	}

	public EvaluatorExpression getConditionExpression() {
		return conditionExpression;
	}

	public void setConditionExpression(EvaluatorExpression conditionExpression) {
		this.conditionExpression = conditionExpression;
	}

	public EvaluatorExpression getStageExpression() {
		return stageExpression;
	}

	public void setStageExpression(EvaluatorExpression stageExpression) {
		this.stageExpression = stageExpression;
	}

	public boolean isRepeat() {
		return isRepeat;
	}


	@Override
	public StageTransition deepCopy() {
		StageTransition deepCopy = new StageTransition();
		deepCopy.stageExpression = DeepCopyHelper.deepCopyOrNull(stageExpression);
		deepCopy.conditionExpression = DeepCopyHelper.deepCopyOrNull(conditionExpression);
		deepCopy.isRepeat = isRepeat;
		/**
		 * BEWARE: the "target" reference itself is not deep copied, it should only be a
		 * reference to an existing stage. Also, chances are you need to reference a
		 * "not already deep copied"-stage here, so this has to be handled by the
		 * caller!
		 */
		return deepCopy;
	}
}
