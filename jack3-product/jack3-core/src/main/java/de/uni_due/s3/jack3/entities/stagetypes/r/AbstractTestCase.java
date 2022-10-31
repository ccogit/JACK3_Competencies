package de.uni_due.s3.jack3.entities.stagetypes.r;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;

@Audited
@Entity
public abstract class AbstractTestCase extends AbstractEntity implements DeepCopyable<AbstractTestCase> {

	private static final long serialVersionUID = -6339536931637788701L;

	@ToString
	@Column
	protected int points;

	@ToString
	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	protected ETestCasePointsMode pointsMode;

	@ToString
	@Column
	@Type(type = "text")
	protected String name;

	@Column
	@Type(type = "text")
	protected String feedbackIfFailed;

	@ToString
	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	protected ETestcaseRuleMode ruleMode;

	protected AbstractTestCase() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPoints() {
		return points;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	public ETestCasePointsMode getPointsMode() {
		return pointsMode;
	}

	public void setPointsMode(ETestCasePointsMode pointsMode) {
		this.pointsMode = pointsMode;
	}

	public boolean isGainMode() {
		return pointsMode.equals(ETestCasePointsMode.GAIN);
	}

	public boolean isDeductionMode() {
		return pointsMode.equals(ETestCasePointsMode.DEDUCTION);
	}

	public boolean isAbsenceMode() {
		return ruleMode.equals(ETestcaseRuleMode.ABSENCE);
	}

	public boolean isPresenceMode() {
		return ruleMode.equals(ETestcaseRuleMode.PRESENCE);
	}

	public String getFeedbackIfFailed() {
		return feedbackIfFailed;
	}

	public void setFeedbackIfFailed(String feedbackIfFailed) {
		this.feedbackIfFailed = feedbackIfFailed;
	}

	protected void deepCopyAbstractTestCaseVars(AbstractTestCase testCaseToCopyFrom) {
		points = testCaseToCopyFrom.points;
		pointsMode = testCaseToCopyFrom.pointsMode;
		name = testCaseToCopyFrom.name;
		feedbackIfFailed = testCaseToCopyFrom.feedbackIfFailed;
		ruleMode = testCaseToCopyFrom.ruleMode;
	}

	public abstract boolean isDynamic();

	public abstract boolean isStatic();

	public ETestcaseRuleMode getRuleMode() {
		return ruleMode;
	}

	public void setRuleMode(ETestcaseRuleMode ruleMode) {
		this.ruleMode = ruleMode;
	}

}
