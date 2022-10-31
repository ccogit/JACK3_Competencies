package de.uni_due.s3.jack3.entities.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;

/**
 * Rule for a text-based Feedback conditioned by Points.
 */
@Audited
@Entity
public class ResultFeedbackMapping extends AbstractEntity implements DeepCopyable<ResultFeedbackMapping> {

	private static final long serialVersionUID = 6468659929254650800L;

	@Column
	@Type(type = "text")
	private String expression;

	@Column
	@Type(type = "text")
	private String title;

	@Column
	@Type(type = "text")
	private String text;

	public ResultFeedbackMapping() {
		super();
	}

	public ResultFeedbackMapping(String expression, String title, String text) {
		super();
		this.expression = expression;
		this.title = title;
		this.text = text;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	@Override
	public ResultFeedbackMapping deepCopy() {

		ResultFeedbackMapping resultFeedbackMappingDeepCopy = new ResultFeedbackMapping();

		resultFeedbackMappingDeepCopy.expression = expression;
		resultFeedbackMappingDeepCopy.title = title;
		resultFeedbackMappingDeepCopy.text = text;

		return resultFeedbackMappingDeepCopy;
	}

}
