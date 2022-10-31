package de.uni_due.s3.jack3.entities.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.AbstractEntity;

/**
 * Representation of a Name-Value-Tuple used by Checkers to attache on a Submission.
 */
@Audited
@Entity
// REVIEW lg - Hat derzeit keine Funktionalität
public class SubmissionAttribute extends AbstractEntity {

	private static final long serialVersionUID = 8191969054062681343L;

	@Column
	@Type(type = "text")
	String name;

	@Column
	@Type(type = "text")
	String value;

	@Column
	private CheckerConfiguration fromChecker;

	public SubmissionAttribute() {
	}

	public CheckerConfiguration getFromChecker() {
		return fromChecker;
	}

	public void setFromChecker(CheckerConfiguration fromChecker) {
		this.fromChecker = fromChecker;
	}
}
