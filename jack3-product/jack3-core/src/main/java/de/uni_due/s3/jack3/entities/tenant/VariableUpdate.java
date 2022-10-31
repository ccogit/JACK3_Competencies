package de.uni_due.s3.jack3.entities.tenant;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;
import de.uni_due.s3.jack3.utils.DeepCopyHelper;

@Audited
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class VariableUpdate extends AbstractEntity implements DeepCopyable<VariableUpdate> {

	private static final long serialVersionUID = 6826553790300785000L;

	@ToString
	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
	private VariableDeclaration variableReference;

	/**
	 * Code to be evaluated by the evaluator to update this variable. The syntax to be used may be specific to the
	 * evaluator.
	 */
	@ToString
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private EvaluatorExpression updateCode = new EvaluatorExpression();

	public VariableUpdate() {
		super();
	}

	/**
	 * Creates a new variable update for the given variable reference. The reference must be non-null.
	 *
	 * @param variableReference
	 *            The variable to be updated.
	 * @throws IllegalArgumentException
	 *             if {@code variableReference} is {@code null}.
	 */
	public VariableUpdate(VariableDeclaration variableReference) {
		setVariableReference(variableReference);
	}

	public VariableDeclaration getVariableReference() {
		return variableReference;
	}

	/**
	 * Changes the target of the update to the given variable reference. The reference must be non-null.
	 *
	 * @param variableReference
	 *            The variable to be updated.
	 * @throws IllegalArgumentException
	 *             if {@code variableReference} is {@code null}.
	 */
	public void setVariableReference(VariableDeclaration variableReference) {
		if (variableReference == null) {
			throw new IllegalArgumentException("Variable reference must not be null.");
		}

		this.variableReference = variableReference;
	}

	public EvaluatorExpression getUpdateCode() {
		return updateCode;
	}

	public void setUpdateCode(EvaluatorExpression updateCode) {
		this.updateCode = updateCode;
	}

	@Override
	public VariableUpdate deepCopy() {
		VariableUpdate deepCopy = new VariableUpdate();
		deepCopy.updateCode = DeepCopyHelper.deepCopyOrNull(updateCode);
		deepCopy.variableReference = DeepCopyHelper.deepCopyOrNull(variableReference);
		return deepCopy;
	}
}
