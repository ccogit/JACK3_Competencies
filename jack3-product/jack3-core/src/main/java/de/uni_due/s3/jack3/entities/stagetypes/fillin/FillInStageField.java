package de.uni_due.s3.jack3.entities.stagetypes.fillin;

import javax.persistence.Column;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.AbstractEntity;

@Audited
@MappedSuperclass
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class FillInStageField extends AbstractEntity {

	private static final long serialVersionUID = -5640634396067356259L;

	@Column(nullable = false)
	@Type(type = "text")
	protected String name;

	@Column
	protected int orderIndex;

	public FillInStageField() {
	}

	public FillInStageField(String name, int orderIndex) {
		this.name = requireIdentifier(name, "You must specify a non-empty name.");
		this.orderIndex = orderIndex;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getOrderIndex() {
		return orderIndex;
	}

	public void setOrderIndex(int orderIndex) {
		this.orderIndex = orderIndex;
	}

	protected void deepCopyAbstractVars(FillInStageField fillInStageFieldToCopyFrom) {
		name = fillInStageFieldToCopyFrom.name;
		orderIndex = fillInStageFieldToCopyFrom.orderIndex;
	}
}
