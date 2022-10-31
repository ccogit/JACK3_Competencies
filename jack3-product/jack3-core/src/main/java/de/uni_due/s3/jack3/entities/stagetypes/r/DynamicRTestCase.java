
package de.uni_due.s3.jack3.entities.stagetypes.r;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.uni_due.s3.jack3.annotations.ToString;

/**
 * <pre>
 * package de.uni_due.s3.jack.backend
 * class DynamicTestCase
 *
 * &#64;Required String name
 *           String postcode
 * &#64;Required String expectedOutput
 *           String postprocessingFunction
 * &#64;Required int points
 * &#64;Required TestCasePointsMode pointsMode
 *           int tolerance
 * </pre>
 *
 * @author Benjamin Otto
 *
 */
@Audited
@Entity
@XStreamAlias("DynamicRTestCase")
public class DynamicRTestCase extends AbstractTestCase {
	private static final int DEFAULT_TOLERANCE_E_TO_THE_POWER_OF = -4;

	private static final long serialVersionUID = -6339536931637788701L;

	@ToString
	@Column
	@Type(type = "text")
	private String postCode;

	@ToString
	@Column
	@Type(type = "text")
	private String expectedOutput;

	@ToString
	@Column
	@Type(type = "text")
	private String postprocessingFunction;

	@ToString
	@Column
	private int tolerance = DEFAULT_TOLERANCE_E_TO_THE_POWER_OF;

	public DynamicRTestCase() {
	}

	public static class Builder {
		private static final String REQUIRED_PROPERTIES = "The properties \"name\", \"expectedOutput\" and \"pointsMode\" are required.";
		private String name;
		private String postcode;
		private String expectedOutput;
		private String postprocessingFunction;
		private int points;
		private ETestCasePointsMode pointsMode;
		private int tolerance;
		private String feedbackIfFailed;
		private ETestcaseRuleMode ruleMode;

		public Builder() {
		}

		Builder(String name, String postcode, String expectedOutput, String postprocessingFunction, // NOSONAR
				int points, ETestCasePointsMode pointsMode, int tolerance, String feedbackIfFailed,
				ETestcaseRuleMode ruleMode) {
			this.ruleMode = ruleMode;
			this.name = name;
			this.postcode = postcode;
			this.expectedOutput = expectedOutput;
			this.postprocessingFunction = postprocessingFunction;
			this.points = points;
			this.pointsMode = pointsMode;
			this.tolerance = tolerance;
			this.feedbackIfFailed = feedbackIfFailed;
		}

		public Builder errorFeedback(String feedbackIfFailed) {
			this.feedbackIfFailed = feedbackIfFailed;
			return Builder.this;
		}

		public Builder name(String name) {
			this.name = name;
			return Builder.this;
		}

		public Builder postcode(String postcode) {
			this.postcode = postcode;
			return Builder.this;
		}

		public Builder expectedOutput(String expectedOutput) {
			this.expectedOutput = expectedOutput;
			return Builder.this;
		}

		public Builder postprocessingFunction(String postprocessingFunction) {
			this.postprocessingFunction = postprocessingFunction;
			return Builder.this;
		}

		public Builder points(int points) {
			this.points = points;
			return Builder.this;
		}

		public Builder pointsMode(ETestCasePointsMode pointsMode) {
			this.pointsMode = pointsMode;
			return Builder.this;
		}

		public Builder ruleMode(ETestcaseRuleMode ruleMode) {
			this.ruleMode = ruleMode;
			return Builder.this;
		}

		public Builder tolerance(int tolerance) {
			this.tolerance = tolerance;
			return Builder.this;
		}

		public AbstractTestCase build() {
			if (name == null) {
				throw new NullPointerException(
						"The property \"name\" is null. " + "Please set the value by \"name()\". "
								+ REQUIRED_PROPERTIES);
			}
			if (expectedOutput == null) {
				throw new NullPointerException(
						"The property \"expectedOutput\" is null. " + "Please set the value by \"expectedOutput()\". "
								+ REQUIRED_PROPERTIES);
			}
			if (pointsMode == null) {
				throw new NullPointerException(
						"The property \"pointsMode\" is null. " + "Please set the value by \"pointsMode()\". "
								+ REQUIRED_PROPERTIES);
			}

			return new DynamicRTestCase(this);
		}
	}

	private DynamicRTestCase(Builder builder) {
		ruleMode = builder.ruleMode;
		name = builder.name;
		postCode = builder.postcode;
		expectedOutput = builder.expectedOutput;
		postprocessingFunction = builder.postprocessingFunction;
		points = builder.points;
		pointsMode = builder.pointsMode;
		tolerance = builder.tolerance;
		feedbackIfFailed = builder.feedbackIfFailed;
	}

	public String getExpectedOutput() {
		return expectedOutput;
	}

	public void setExpectedOutput(String expectedOutput) {
		this.expectedOutput = expectedOutput;
	}

	public String getPostprocessingFunction() {
		return postprocessingFunction;
	}

	public void setPostprocessingFunction(String postprocessingFunction) {
		this.postprocessingFunction = postprocessingFunction;
	}

	public int getTolerance() {
		return tolerance;
	}

	public void setTolerance(int tolerance) {
		this.tolerance = tolerance;
	}

	public String getPostCode() {
		return postCode;
	}

	public void setPostCode(String postCode) {
		this.postCode = postCode;
	}

	@Override
	public AbstractTestCase deepCopy() {
		DynamicRTestCase copy = new DynamicRTestCase();

		copy.deepCopyAbstractTestCaseVars(this);

		copy.postCode = postCode;
		copy.expectedOutput =expectedOutput ;
		copy.postprocessingFunction = postprocessingFunction;
		copy.tolerance = tolerance;

		return copy;
	}

	public AbstractTestCase copyFrom(DynamicRTestCase other) {
		deepCopyAbstractTestCaseVars(other);
		postCode = other.postCode;
		expectedOutput = other.expectedOutput;
		postprocessingFunction = other.postprocessingFunction;
		tolerance = other.tolerance;
		return this;
	}

	@Override
	public boolean isDynamic() {
		return true;
	}

	@Override
	public boolean isStatic() {
		return false;
	}
}
