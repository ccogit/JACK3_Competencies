package de.uni_due.s3.jack3.entities.stagetypes.mc;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.uni_due.s3.jack3.annotations.DeepCopyOmitField;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.enums.EMCRuleType;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;

/**
 * Answer option for an MCStage
 */
@Audited
@Entity
@XStreamAlias("MCAnswer")
public class MCAnswer extends AbstractEntity implements DeepCopyable<MCAnswer> {

	private static final long serialVersionUID = 8876360565665185778L;

	// This field is used to capture the value of the column named 
	// in the @OrderColumn annotation on the referencing entity.
	// fixes #363
	@Column(insertable = false, updatable = false)
	@XStreamAlias("order")
	private int answer_options_order;

	public MCAnswer() {
		super();
		rule = EMCRuleType.WRONG;
	}

	public MCAnswer(String text) {
		super();
		this.text = text;
		rule = EMCRuleType.WRONG;
	}

	/**
	 * Rule to determine if the answer is correct.
	 */
	@Enumerated(EnumType.STRING)
	private EMCRuleType rule;

	/**
	 * Text that is shown to the user.
	 */
	@Column
	@Type(type = "text")
	private String text;

	/**
	 * Variable name to determine if answer is correct
	 */
	@Column
	@Type(type = "text")
	private String variableName = "";
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="mcstage_id")
	@DeepCopyOmitField(reason = "the MCStage has to be set by the caller!")
	private MCStage mcstage;

	public EMCRuleType getRule() {
		return rule;
	}

	public String getText() {
		return text;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setRule(EMCRuleType rule) {
		this.rule = rule;
	}

	public void setText(String text) {
		this.text = text;
	}
	
	public void setMCStage(MCStage mcStage) {
		mcstage = mcStage;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	/**
	 * ATTENTION: This deepCopy does not copy the mc stage. By calling this 
	 * method you must ensure that the mc stage will copied.
	 * <br><br>
	 *
	 * {@inheritDoc}
	 */
	@Override
	public MCAnswer deepCopy() {
		MCAnswer entityDeepCopy = new MCAnswer();

		entityDeepCopy.text = text;
		entityDeepCopy.variableName = variableName;
		entityDeepCopy.rule = rule;
		entityDeepCopy.answer_options_order = answer_options_order;

		/*
		 * CAUTION: the mc stage have to be set by the caller!
		 */

		return entityDeepCopy;

	}

}
