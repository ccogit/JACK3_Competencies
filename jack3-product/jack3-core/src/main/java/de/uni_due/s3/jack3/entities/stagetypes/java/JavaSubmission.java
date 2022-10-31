package de.uni_due.s3.jack3.entities.stagetypes.java;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.SubmissionResource;
import de.uni_due.s3.jack3.utils.JackStringUtils;

@Audited
@Entity
public class JavaSubmission extends StageSubmission {

	private static final long serialVersionUID = -6339536931637788701L;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
	private Set<SubmissionResource> uploadedFiles = new HashSet<>();

	@ElementCollection(fetch = FetchType.EAGER)
	private Map<String, String> reportValues = new HashMap<>();

	@Override
	public void copyFromStageSubmission(StageSubmission stageSubmission) {
		// TODO Auto-generated method stub

	}

	public Set<SubmissionResource> getUploadedFiles() {
		return uploadedFiles;
	}

	public void setUploadedFiles(Set<SubmissionResource> uploadedFiles) {
		this.uploadedFiles = uploadedFiles;
	}

	public void addUploadedFile(SubmissionResource uploadedFile) {
		this.uploadedFiles.add(uploadedFile);
	}

	public void removeUploadedFile(SubmissionResource uploadedFile) {
		this.uploadedFiles.remove(uploadedFile);
	}

	public Map<String, String> getReportValues() {
		return reportValues;
	}

	public void setReportValues(Map<String, String> reportValues) {
		this.reportValues = reportValues;
	}

	public void addReportValue(String key, String value) {
		if (JackStringUtils.isNotBlank(key)) {
			this.reportValues.put(key, value);
		}
	}
}
