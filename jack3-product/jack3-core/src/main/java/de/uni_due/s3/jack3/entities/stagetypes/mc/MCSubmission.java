package de.uni_due.s3.jack3.entities.stagetypes.mc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OrderColumn;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.tenant.StageSubmission;

/**
 * Submission entry for a multiple choice stage.
 */
@Audited
@Entity
public class MCSubmission extends StageSubmission {

	private static final long serialVersionUID = -4152713884664884141L;

	/** The pattern ticked by the user. */
	@Column
	@Type(type = "text")
	private String tickedPattern;

	/**
	 * The order in which the options were displayed when the exercise was
	 * presented.
	 */
	@ElementCollection(fetch = FetchType.EAGER)
	@OrderColumn
	private List<Integer> optionsOrder = new ArrayList<>();

	/**
	 * Caches value for meta variable for correct ticks
	 */
	@Column(columnDefinition = "int4 default 0")
	private int correctTicks;

	/**
	 * Caches value for meta variable for incorrect ticks
	 */
	@Column(columnDefinition = "int4 default 0")
	private int incorrectTicks;

	public void setTickedPattern(String pattern) {
		tickedPattern = pattern;
	}

	public String getTickedPattern() {
		return tickedPattern;
	}

	/**
	 * @return unmodifiableList of optionsOrder
	 */
	public List<Integer> getOptionsOrder() {
		return Collections.unmodifiableList(optionsOrder);
	}

	public void clearOptionsOder() {
		optionsOrder.clear();
	}

	public void addOptionsOrder(List<Integer> optionsOrder) {
		this.optionsOrder.addAll(optionsOrder);
	}

	@Override
	public void copyFromStageSubmission(StageSubmission stageSubmission) {
		if (!(stageSubmission instanceof MCSubmission)) {
			throw new IllegalArgumentException("Method must be used with instances of MCSubmission");
		}

		MCSubmission oldSubmission = (MCSubmission) stageSubmission;

		this.setTickedPattern(oldSubmission.getTickedPattern());
		this.clearOptionsOder();
		this.addOptionsOrder(oldSubmission.getOptionsOrder());

	}

	public int getCorrectTicks() {
		return correctTicks;
	}

	public void setCorrectTicks(int correctTicks) {
		this.correctTicks = correctTicks;
	}

	public int getIncorrectTicks() {
		return incorrectTicks;
	}

	public void setIncorrectTicks(int incorrectTicks) {
		this.incorrectTicks = incorrectTicks;
	}

}
