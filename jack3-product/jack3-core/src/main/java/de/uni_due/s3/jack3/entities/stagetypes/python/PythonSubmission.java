package de.uni_due.s3.jack3.entities.stagetypes.python;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.utils.JackStringUtils;

@Audited
@Entity
public class PythonSubmission extends StageSubmission {

	private static final long serialVersionUID = -6339536931637788701L;

	@Column
	@Type(type = "text")
	private String pythonCode;

	@ElementCollection(fetch = FetchType.EAGER)
	private Map<String, String> reportValues = new HashMap<>();

	@Override
	public void copyFromStageSubmission(StageSubmission stageSubmission) {
		if (!(stageSubmission instanceof PythonSubmission)) {
			throw new IllegalArgumentException("Method must be used with instances of PythonSubmission");
		}

		this.pythonCode = ((PythonSubmission) stageSubmission).getPythonCode();
	}

	public String getPythonCode() {
		return pythonCode;
	}

	public void setPythonCode(String pythonCode) {
		this.pythonCode = pythonCode;
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
