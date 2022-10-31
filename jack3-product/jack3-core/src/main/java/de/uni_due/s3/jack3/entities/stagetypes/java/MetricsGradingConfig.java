package de.uni_due.s3.jack3.entities.stagetypes.java;

import javax.persistence.Entity;

import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@Audited
@Entity
@XStreamAlias("MetricsGradingConfig")
public class MetricsGradingConfig extends AbstractJavaCheckerConfiguration {

	private static final long serialVersionUID = 4976736955592370263L;

	@Override
	public AbstractJavaCheckerConfiguration deepCopy() {
		MetricsGradingConfig copy = new MetricsGradingConfig();

		copy.copyFrom(this);

		return copy;
	}

}
