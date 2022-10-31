package de.uni_due.s3.jack3.entities.tenant;

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
 * Hint, that can be given in a Stage.
 */
@Audited
@Entity
@XStreamAlias("StageHint")
public class StageHint extends AbstractEntity implements DeepCopyable<StageHint> {

	private static final long serialVersionUID = 1122486928504426550L;

	// This field is used to capture the value of the column named 
	// in the @OrderColumn annotation on the referencing entity.
	// fixes #316
	@Column(insertable = false, updatable = false)
	private int stagehint_order;

	@Column
	@Type(type = "text")
	private String text;

	@Column
	private int malus;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="stage_id")
	@DeepCopyOmitField(
		reason = "the reference to 'stage' has to be handled by the caller, since the stage might not have already been deepCopied")
	private Stage stage;

	public StageHint() {
		super();
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getMalus() {
		return malus;
	}

	public void setMalus(int malus) {
		if ((malus < 0) || (malus > 100)) {
			throw new IllegalArgumentException("malus out of bounds (0-100): " + malus);
		}

		this.malus = malus;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	public void setStagehint_order(int order) {
		this.stagehint_order = order;
	}

	public int getStagehint_order() {
		return this.stagehint_order;
	}

	@Override
	public StageHint deepCopy() {
		StageHint deepCopy = new StageHint();

		deepCopy.text = text;
		deepCopy.malus = malus;
		deepCopy.stagehint_order = stagehint_order;

		// **Beware** reference to "stage" has to be handled by the caller, since the stage might not have 
		// already been deepCopied

		return deepCopy;
	}	
}
