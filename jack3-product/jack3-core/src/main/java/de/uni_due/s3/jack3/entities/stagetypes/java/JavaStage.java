package de.uni_due.s3.jack3.entities.stagetypes.java;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.uni_due.s3.jack3.entities.tenant.ExerciseResource;
import de.uni_due.s3.jack3.entities.tenant.Stage;

@Audited
@Entity
@AttributeOverride(name = "id", column = @Column(name = "id"))
@XStreamAlias("JavaStage")
public class JavaStage extends Stage {

	private static final long serialVersionUID = 6309830000021958929L;

	@ElementCollection(fetch = FetchType.EAGER)
	private Set<String> mandatoryFileNames = new HashSet<>();

	@ElementCollection(fetch = FetchType.EAGER)
	private Set<String> allowedFileNames = new HashSet<>();

	@Column
	private int minimumFileCount = 1;

	@Column
	private int maximumFileCount = 1;

	@Column
	private boolean ignorePendingJobs;

	@Column(columnDefinition = "boolean default false")
	private boolean repeatOnMissingUpload;

	@Column(columnDefinition = "boolean default false")
	private boolean propagateInternalErrors;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@OrderColumn
	private List<AbstractJavaCheckerConfiguration> gradingSteps = new LinkedList<>();

	@Override
	public JavaStage deepCopy() {
		JavaStage copy = new JavaStage();

		copy.deepCopyStageVars(this);

		copy.ignorePendingJobs = ignorePendingJobs;
		copy.repeatOnMissingUpload = repeatOnMissingUpload;
		copy.propagateInternalErrors = propagateInternalErrors;
		copy.minimumFileCount = minimumFileCount;
		copy.maximumFileCount = maximumFileCount;
		copy.allowedFileNames.addAll(allowedFileNames);
		copy.mandatoryFileNames.addAll(mandatoryFileNames);

		for (AbstractJavaCheckerConfiguration step : gradingSteps) {
			copy.addGradingStep((AbstractJavaCheckerConfiguration) step.deepCopy());
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

	public String getMandatoryFileNames() {
		return mandatoryFileNames.stream().collect(Collectors.joining("\n"));
	}

	public Set<String> getMandatoryFileNamesAsSet() {
		return Collections.unmodifiableSet(mandatoryFileNames);
	}

	public void setMandatoryFileNames(String mandatoryFileNames) {
		this.mandatoryFileNames.clear();
		this.mandatoryFileNames.addAll(Arrays.asList(mandatoryFileNames.split("\n")));

		if (minimumFileCount < this.mandatoryFileNames.size()) {
			minimumFileCount = this.mandatoryFileNames.size();
		}

		if (maximumFileCount < this.mandatoryFileNames.size()) {
			maximumFileCount = this.mandatoryFileNames.size();
		}

	}

	public String getAllowedFileNames() {
		return allowedFileNames.stream().collect(Collectors.joining("\n"));
	}

	public Set<String> getAllowedFileNamesAsSet() {
		return Collections.unmodifiableSet(allowedFileNames);
	}

	public void setAllowedFileNames(String allowedFileNames) {
		this.allowedFileNames.clear();
		this.allowedFileNames.addAll(Arrays.asList(allowedFileNames.split("\n")));
	}

	public int getMinimumFileCount() {
		return minimumFileCount;
	}

	public void setMinimumFileCount(int minimumFileCount) {
		if (minimumFileCount < mandatoryFileNames.size()) {
			minimumFileCount = mandatoryFileNames.size();
		}
		this.minimumFileCount = minimumFileCount;
	}

	public int getMaximumFileCount() {
		return maximumFileCount;
	}

	public void setMaximumFileCount(int maximumFileCount) {
		if (maximumFileCount < mandatoryFileNames.size()) {
			maximumFileCount = mandatoryFileNames.size();
		}
		this.maximumFileCount = maximumFileCount;
	}

	public List<AbstractJavaCheckerConfiguration> getGradingSteps() {
		return gradingSteps;
	}

	public void addGradingStep(AbstractJavaCheckerConfiguration gradingStep) {
		gradingSteps.add(gradingStep);
	}

	public void removeGradingStep(AbstractJavaCheckerConfiguration gradingStep) {
		gradingSteps.remove(gradingStep);
	}

	@Override
	public void updateResourceReferences(Map<ExerciseResource, ExerciseResource> referenceMap) {
		super.updateResourceReferences(referenceMap);

		gradingSteps.forEach(step -> step.updateResourceReferences(referenceMap));
	}
}
