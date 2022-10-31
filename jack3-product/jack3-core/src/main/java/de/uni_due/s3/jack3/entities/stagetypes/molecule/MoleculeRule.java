package de.uni_due.s3.jack3.entities.stagetypes.molecule;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;
import de.uni_due.s3.jack3.utils.DeepCopyHelper;

@Audited
@Entity
@XStreamAlias("MoleculeRule")
public class MoleculeRule extends AbstractEntity implements Comparable<MoleculeRule>, DeepCopyable<MoleculeRule> {

	private static final long serialVersionUID = 3068776822666903115L;

	@Column
	@Type(type = "text")
	private String name;

	@Column
	protected int orderIndex;

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private EvaluatorExpression validationExpression = new EvaluatorExpression();

	@Column(nullable = true)
	@Type(type = "text")
	private String feedbackText;

	@Column
	protected int points;

	@Column(columnDefinition = "boolean default false")
	private boolean terminal;

	public MoleculeRule() {
	}

	public MoleculeRule(String name, int orderIndex) {
		setName(requireIdentifier(name, "You must specify a non-empty name."));
		this.orderIndex = orderIndex;
		points = 0;
	}

	@Override
	public MoleculeRule deepCopy() {
		MoleculeRule deepCopy = new MoleculeRule();

		deepCopy.name = name;
		deepCopy.orderIndex = orderIndex;
		deepCopy.validationExpression = DeepCopyHelper.deepCopyOrNull(validationExpression);
		deepCopy.feedbackText = feedbackText;
		deepCopy.points = points;
		deepCopy.terminal = terminal;

		return deepCopy;
	}


	public String getFeedbackText() {
		return feedbackText;
	}

	public String getName() {
		return name;
	}

	public int getOrderIndex() {
		return orderIndex;
	}

	public int getPoints() {
		return points;
	}

	public boolean isTerminal() {
		return terminal;
	}

	public EvaluatorExpression getValidationExpression() {
		return validationExpression;
	}

	public void setFeedbackText(String feedbackText) {
		this.feedbackText = feedbackText;
	}

	public void setName(String name) {
		this.name = requireIdentifier(name, "Rule name must not be null or empty.");
	}

	public void setOrderIndex(int orderIndex) {
		this.orderIndex = orderIndex;
	}

	public void setPoints(int points) {
		if ((points < -100) || (points > 100)) {
			throw new IllegalArgumentException("Invalid points: " + points + ". Points must be between -100 and 100.");
		}

		this.points = points;
	}

	public void setValidationExpression(EvaluatorExpression validationExpression) {
		this.validationExpression = validationExpression;
	}

	public void setTerminal(final boolean terminal) {
		this.terminal = terminal;
	}

	@Override
	public int compareTo(MoleculeRule otherRule) {
		return Integer.valueOf(orderIndex).compareTo(Integer.valueOf(otherRule.getOrderIndex()));
	}
}
