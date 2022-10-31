package de.uni_due.s3.jack3.entities.stagetypes.java;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@Audited
@Entity
@XStreamAlias("TracingGradingConfig")
public class TracingGradingConfig extends AbstractJavaCheckerConfiguration {

	private static final long serialVersionUID = 4976736955592370263L;

	@Column
	@Type(type = "text")
	private String testDriver;

	@ElementCollection(fetch = FetchType.EAGER)
	private Set<String> classesToTrace = new HashSet<>();

	@Column(columnDefinition = "int4 default 900")
	private int timeoutSeconds = 900;

	public String getTestDriver() {
		return testDriver;
	}

	public void setTestDriver(String testDriver) {
		this.testDriver = testDriver;
	}

	public List<String> getClassesToTrace() {
		return new LinkedList<>(classesToTrace);
	}

	public void setClassesToTrace(List<String> classesToTrace) {
		this.classesToTrace.clear();
		if (classesToTrace != null) {
			this.classesToTrace.addAll(classesToTrace);
		}
	}

	public int getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(int timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	@Override
	public AbstractJavaCheckerConfiguration deepCopy() {
		TracingGradingConfig copy = new TracingGradingConfig();

		copy.copyFrom(this);

		copy.testDriver = testDriver;
		copy.timeoutSeconds = timeoutSeconds;
		copy.classesToTrace.addAll(classesToTrace);

		return copy;
	}

}
