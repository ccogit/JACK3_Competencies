package de.uni_due.s3.jack3.entities.stagetypes.python;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@Audited
@Entity
@XStreamAlias("TracingPythonGradingConfig")
public class TracingPythonGradingConfig extends AbstractPythonCheckerConfiguration {

	private static final long serialVersionUID = 4976736955592370263L;

	@Column
	@Type(type = "text")
	private String testDriver;

	@Column
	private String studentModule;

	@Column(columnDefinition = "int4 default 10")
	private int timeoutSeconds = 10;

	public String getTestDriver() {
		return testDriver;
	}

	public void setTestDriver(String testDriver) {
		this.testDriver = testDriver;
	}

	public int getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(int timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public String getStudentModule() {
		return studentModule;
	}

	public void setStudentModule(String studentModule) {
		this.studentModule = studentModule;
	}

	@Override
	public AbstractPythonCheckerConfiguration deepCopy() {
		TracingPythonGradingConfig copy = new TracingPythonGradingConfig();

		copy.copyFrom(this);

		copy.testDriver = testDriver;
		copy.studentModule = studentModule;
		copy.timeoutSeconds = timeoutSeconds;

		return copy;
	}

}
