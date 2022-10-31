package de.uni_due.s3.jack3.entities.tenant;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToOne;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;
import de.uni_due.s3.jack3.utils.DeepCopyHelper;

@Audited
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@XStreamAlias("VariableDeclaration")
public class VariableDeclaration extends AbstractEntity implements DeepCopyable<VariableDeclaration> {

	private static final long serialVersionUID = -1138810854212771007L;

	@ToString
	@Column
	@Type(type = "text")
	private String name;

	/**
	 * Code to be evaluated by the evaluator to initialize this variable. The syntax to be used may be specific to the
	 * evaluator.
	 */
	@ToString
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private EvaluatorExpression initializationCode = new EvaluatorExpression();

	public VariableDeclaration() {
		super();
	}

	/**
	 * Creates a new variable declaration with the given name. The name must be non-null and not empty.
	 *
	 * @param name
	 *            The name to be set.
	 * @throws NullPointerException
	 *             if {@code name} is {@code null}.
	 * @throws IllegalArgumentException
	 *             if {@code name} is empty.
	 */
	public VariableDeclaration(final String name) {
		setName(name);
	}

	public String getName() {
		return name;
	}

	/**
	 * Sets the variable declarations name to the given string.
	 *
	 * @param name
	 *            The new name to be used.
	 * @throws NullPointerException
	 *             if {@code name} is {@code null}.
	 * @throws IllegalArgumentException
	 *             if {@code name} is empty.
	 */
	public void setName(final String name) {
		this.name = requireIdentifier(name, "name must be a non empty string.");
	}

	public EvaluatorExpression getInitializationCode() {
		return initializationCode;
	}

	public void setInitializationCode(EvaluatorExpression initializationCode) {
		this.initializationCode = initializationCode;
	}

	@Override
	public VariableDeclaration deepCopy() {
		VariableDeclaration deepCopy = new VariableDeclaration();

		deepCopy.name = name;
		deepCopy.initializationCode = DeepCopyHelper.deepCopyOrNull(initializationCode);

		return deepCopy;
	}
}
