package de.uni_due.s3.jack3.entities.stagetypes.python;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@Audited
@Entity
@XStreamAlias("GreqlnGradingConfig")
public class GreqlPythonGradingConfig extends AbstractPythonCheckerConfiguration {

	private static final long serialVersionUID = 4976736955592370263L;

	@Column
	@Type(type = "text")
	private String greqlRules;

	public String getGreqlRules() {
		return greqlRules;
	}

	public void setGreqlRules(String greqlRules) {
		this.greqlRules = greqlRules;
	}

	@Override
	public AbstractPythonCheckerConfiguration deepCopy() {
		GreqlPythonGradingConfig copy = new GreqlPythonGradingConfig();

		copy.copyFrom(this);

		copy.greqlRules = greqlRules;

		return copy;
	}

}
