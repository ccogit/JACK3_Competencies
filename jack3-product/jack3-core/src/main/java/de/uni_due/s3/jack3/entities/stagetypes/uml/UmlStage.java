package de.uni_due.s3.jack3.entities.stagetypes.uml;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.tenant.Stage;

@Audited
@Entity
@AttributeOverrides({ @AttributeOverride(name = "id", column = @Column(name = "id")) })
public class UmlStage extends Stage {

	private static final long serialVersionUID = -2160276619358437727L;

	@Column
	@Type(type = "text")
	private String greqlRules;

	@Column
	private boolean ignorePendingJobs;

	@Column(columnDefinition = "boolean default false")
	private boolean repeatOnMissingUpload;

	@Column(columnDefinition = "boolean default false")
	private boolean propagateInternalErrors;

	@Override
	public UmlStage deepCopy() {
		UmlStage copy = new UmlStage();

		copy.deepCopyStageVars(this);

		copy.greqlRules = greqlRules;
		copy.ignorePendingJobs = ignorePendingJobs;
		copy.repeatOnMissingUpload = repeatOnMissingUpload;
		copy.propagateInternalErrors = propagateInternalErrors;

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

	public String getGreqlRules() {
		return greqlRules;
	}

	public void setGreqlRules(String greqlRules) {
		this.greqlRules = greqlRules;
	}

	public boolean isRepeatOnMissingUpload() {
		return repeatOnMissingUpload;
	}

	public void setRepeatOnMissingUpload(boolean repeatOnMissingUpload) {
		this.repeatOnMissingUpload = repeatOnMissingUpload;
	}

	public boolean isPropagateInternalErrors() {
		return propagateInternalErrors;
	}

	public void setPropagateInternalErrors(boolean propagateInternalErrors) {
		this.propagateInternalErrors = propagateInternalErrors;
	}
}
