package de.uni_due.s3.jack3.entities.tenant;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;

@Audited
@Entity
public class EvaluatorExpression extends AbstractEntity implements DeepCopyable<EvaluatorExpression> {

	private static final long serialVersionUID = 1896511935064764399L;

	public EvaluatorExpression() {
	}

	public EvaluatorExpression(String code) {
		this.code = Objects.requireNonNull(code);
	}

	/**
	 * Enumeration of the different domains that can be used to evaluate expressions. Each element corresponds to a
	 * particular set of functions offered by the evaluator. JACK code <i>must not</i> be specific for any of these
	 * domains with two exceptions:
	 * <ul>
	 * <li>UI elements may be offered to ease input specifically for expressions from one particular domain.</li>
	 * <li>The EvaluatorAdapter uses the domain to request the correct evaluation of expressions.</li>
	 * </ul>
	 * Any other reference to a particular domain within JACK code must be considered a serious violation of the
	 * architecture.
	 */
	// REVIEW lg - Should we move this enum to de.uni_due.s3.jack3.entities.enums ?
	public enum EDomain {
		MATH, CHEM;
	}

	@ToString
	@Column
	@Type(type = "text")
	private String code;

	@ToString
	@Column
	private EDomain domain = EDomain.MATH;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public EDomain getDomain() {
		return domain;
	}

	public void setDomain(EDomain domain) {
		this.domain = domain;
	}

	public boolean isEmpty() {
		return (code == null || code.isEmpty());
	}

	@Override
	public EvaluatorExpression deepCopy() {
		EvaluatorExpression deepCopy = new EvaluatorExpression();

		deepCopy.code = code;
		deepCopy.domain = domain;

		return deepCopy;
	}
}
