package de.uni_due.s3.jack3.entities.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;

/**
 * Representation of the configuration of a checker on a stage.
 */
@Audited
@Entity
public class CheckerConfiguration extends AbstractEntity implements DeepCopyable<CheckerConfiguration> {

	private static final long serialVersionUID = -8322304656564679973L;

	@Column
	@Type(type = "text")
	private String name;

	@Column
	@Type(type = "text")
	private String resultLabel;

	@Column(nullable = false)
	private boolean isActive;

	@Column(nullable = false)
	private boolean hasVisibleResult;

	@Column(nullable = false)
	private boolean hasVisibleFeedback;

	// REVIEW: this is redundant if only async checker will have a checker config. Is this the case?
	@Column(columnDefinition = "boolean default false")
	private boolean isAsync;

	@Column(columnDefinition = "integer default 1")
	protected int weight = 1;

	public String getResultLabel() {
		return resultLabel;
	}

	public void setResultLabel(String resultLabel) {
		this.resultLabel = resultLabel;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public boolean isHasVisibleResult() {
		return hasVisibleResult;
	}

	public void setHasVisibleResult(boolean hasVisibleResult) {
		this.hasVisibleResult = hasVisibleResult;
	}

	public boolean isHasVisibleFeedback() {
		return hasVisibleFeedback;
	}

	public void setHasVisibleFeedback(boolean hasVisibleFeedback) {
		this.hasVisibleFeedback = hasVisibleFeedback;
	}

	public CheckerConfiguration() {
		super();
	}

	public boolean isAsync() {
		return isAsync;
	}

	public void setAsync(boolean isAsync) {
		this.isAsync = isAsync;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public CheckerConfiguration deepCopy() {

		CheckerConfiguration deepCopy = new CheckerConfiguration();
		deepCopy.name = name;
		deepCopy.resultLabel = resultLabel;
		deepCopy.isActive = isActive;
		deepCopy.hasVisibleResult = hasVisibleResult;
		deepCopy.hasVisibleFeedback = hasVisibleFeedback;
		deepCopy.isAsync = isAsync;
		deepCopy.weight = weight;

		return deepCopy;
	}

}
