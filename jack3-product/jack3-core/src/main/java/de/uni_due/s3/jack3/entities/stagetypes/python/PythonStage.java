package de.uni_due.s3.jack3.entities.stagetypes.python;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.tenant.Stage;

@Audited
@Entity
@AttributeOverrides({ @AttributeOverride(name = "id", column = @Column(name = "id") ) })
public class PythonStage extends Stage {

	private static final long serialVersionUID = 6309830000021958929L;

	@Column
	private boolean ignorePendingJobs;

	@Column(columnDefinition = "boolean default false")
	private boolean propagateInternalErrors;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@OrderColumn
	private List<AbstractPythonCheckerConfiguration> gradingSteps = new LinkedList<>();

	@Override
	public PythonStage deepCopy() {
		PythonStage copy = new PythonStage();

		copy.deepCopyStageVars(this);

		copy.ignorePendingJobs = ignorePendingJobs;
		copy.propagateInternalErrors = propagateInternalErrors;

		for (AbstractPythonCheckerConfiguration step : gradingSteps) {
			copy.addGradingStep((AbstractPythonCheckerConfiguration) step.deepCopy());
		}

		return copy;
	}

	@Override
	public boolean mustWaitForPendingJobs() {
		return !ignorePendingJobs;
	}

	public void setIgnorePendingJobs(boolean ignorePendingJobs) {
		this.ignorePendingJobs = ignorePendingJobs;
	}

	public boolean isIgnorePendingJobs() {
		return ignorePendingJobs;
	}

	public boolean isPropagateInternalErrors() {
		return propagateInternalErrors;
	}

	public void setPropagateInternalErrors(boolean propagateInternalErrors) {
		this.propagateInternalErrors = propagateInternalErrors;
	}

	public List<AbstractPythonCheckerConfiguration> getGradingSteps() {
		return gradingSteps;
	}

	public void addGradingStep(AbstractPythonCheckerConfiguration gradingStep) {
		gradingSteps.add(gradingStep);
	}

	public void removeGradingStep(AbstractPythonCheckerConfiguration gradingStep) {
		gradingSteps.remove(gradingStep);
	}
}
