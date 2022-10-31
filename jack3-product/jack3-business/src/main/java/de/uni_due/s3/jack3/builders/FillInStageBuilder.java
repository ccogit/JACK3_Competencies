package de.uni_due.s3.jack3.builders;

import de.uni_due.s3.jack3.entities.enums.EFillInEditorType;
import de.uni_due.s3.jack3.entities.enums.EFormularEditorPalette;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.DropDownField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.Rule;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;

/**
 * Builder for Fill-In stages
 *
 * @author lukas.glaser
 */
public class FillInStageBuilder extends AbstractStageBuilder<FillInStage, FillInStageBuilder> {

	private int ruleCount;

	public static final String FILLIN_FIELD_PREFIX = "fillInField";
	public static final String DROPDOWN_FIELD_PREFIX = "dropDownField";

	/**
	 * Creates a new instance of this builder without an exercise.
	 */
	public FillInStageBuilder() {
		super();
	}

	/**
	 * Creates a new instance of this builder with an exercise.
	 */
	public FillInStageBuilder(ExerciseBuilder exerciseBuilder) {
		super(exerciseBuilder);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected FillInStage getNewStage() {
		return new FillInStage();
	}

	/**
	 * Sets the formular editor palette for this stage.
	 *
	 * @param palette
	 *            {@linkplain EFormularEditorPalette} which is used for the whole stage
	 */
	public FillInStageBuilder withFormularEditorPalette(EFormularEditorPalette palette) {
		stage.setFormularEditorPalette(palette.name());
		return this;
	}

	/**
	 * Adds a new feedback rule to this stage.
	 *
	 * @param name
	 *            Name of this feedback (must not be unique)
	 * @param expression
	 *            Expression that will be evaluated to a Boolean which specifies if the feedback is to show
	 * @param feedbackText
	 *            Text that is shown to the user
	 * @param points
	 *            Points the user gets if the feedback is correct
	 * @param terminal
	 *            Defines if evaluation should end if this rule is triggered.
	 */
	public FillInStageBuilder withFeedbackRule(String name, String expression, String feedbackText, int points,
			boolean terminal) {
		Rule rule = new Rule(name, ruleCount);

		rule.setFeedbackText(feedbackText);
		rule.setPoints(points);
		rule.setTerminal(terminal);

		EvaluatorExpression expr = new EvaluatorExpression();
		expr.setCode(expression);
		rule.setValidationExpression(expr);

		stage.addFeedbackRule(rule);
		ruleCount++;
		return this;
	}

	/**
	 * Sets default feedback text and points.
	 * 
	 * @param feedback
	 *            The text that is shown if no rules match the user's input
	 * @param points
	 *            How much points the default feedback gains
	 */
	public FillInStageBuilder withDefaultFeedback(String feedback, int points) {
		stage.setDefaultFeedback(feedback);
		stage.setDefaultResult(points);
		return this;
	}

	/**
	 * Adds a task description from a {@linkplain DescriptionBuilder} e.g. to add fill-in fields.
	 */
	public DescriptionBuilder withDescription() {
		return new DescriptionBuilder();
	}

	/**
	 * Builder for the task description of a {@linkplain FillInStage}. It can add simple text, line breaks and fill-in
	 * or drop-down-fields.
	 * 
	 * @author lukas.glaser
	 */
	public class DescriptionBuilder {
		private final StringBuilder descriptionText = new StringBuilder();
		private int nextFillInFieldIndex = 1;
		private int nextDropDownFieldIndex = 1;

		/**
		 * Adds the builded description to the fill-in stage.
		 *
		 * @return Parent stage builder
		 */
		public FillInStageBuilder and() {
			stage.setTaskDescription(descriptionText.toString());
			return FillInStageBuilder.this;
		}

		/**
		 * Adds simple text.
		 */
		public DescriptionBuilder append(String text) {
			descriptionText.append(text);
			return this;
		}

		/**
		 * Adds simple text and a line break.
		 */
		public DescriptionBuilder appendLine(String text) {
			descriptionText.append(text).append("<br />");
			return this;
		}

		/**
		 * Adds a single line break.
		 */
		public DescriptionBuilder appendLine() {
			descriptionText.append("<br />");
			return this;
		}

		/**
		 * Adds a new fill-in field with default size (10).
		 *
		 * @param editorType
		 *            Type of this field
		 */
		public DescriptionBuilder appendFillInField(EFillInEditorType editorType) {
			return appendFillInField(editorType, 20);
		}

		/**
		 * Adds a new fill-in field.
		 *
		 * @param editorType
		 *            Type of this field
		 * @param size
		 *            Editor size
		 */
		public DescriptionBuilder appendFillInField(EFillInEditorType editorType, int size) {
			FillInField field = new FillInField();
			field.setName(FILLIN_FIELD_PREFIX + nextFillInFieldIndex);
			field.setOrderIndex(nextFillInFieldIndex);
			field.setFormularEditorType(editorType.name());
			field.setSize(size);
			stage.addFillInField(field);
			descriptionText.append("<input name=\"%s\" size=\"%l\" type=\"text\" value=\"%s\" />"
					.replace("%s", field.getName()).replace("%l", Integer.toString(size)));
			nextFillInFieldIndex++;
			return this;
		}

		/**
		 * Adds a new drop-down field with default size (10).
		 *
		 * @param options
		 *            All possible answer options
		 */
		public DescriptionBuilder appendDropDownField(String... options) {
			return appendDropDownField(10, options);
		}

		/**
		 * Adds a new drop-down field.
		 *
		 * @param options
		 *            All possible answer options
		 * @param size
		 *            Editor size
		 */
		public DescriptionBuilder appendDropDownField(int size, String... options) {
			DropDownField field = new DropDownField();
			field.setName(DROPDOWN_FIELD_PREFIX + nextDropDownFieldIndex);
			field.setOrderIndex(nextDropDownFieldIndex);
			for (String option : options) {
				field.addItem(option);
			}
			stage.addDropDownField(field);
			descriptionText.append(
					"<select name=\"%s\" size=\"%l\" type=\"text\" value=\"%s\" /><option value=\"0\">Drop-Down</option></select>"
							.replace("%s", field.getName()).replace("%l", Integer.toString(size)));
			nextDropDownFieldIndex++;
			return this;
		}

	}

}
