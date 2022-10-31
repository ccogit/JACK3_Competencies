package de.uni_due.s3.jack3.builders;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.spi.CDI;

import de.uni_due.s3.jack3.business.AbstractBusiness;
import de.uni_due.s3.jack3.entities.enums.EFillInEditorType;
import de.uni_due.s3.jack3.entities.enums.EStageHintMalus;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression.EDomain;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;
import de.uni_due.s3.jack3.services.TagService;

/**
 * Builder for exercises
 *
 * @author lukas.glaser
 */
public class ExerciseBuilder extends AbstractBusiness {

	private final Exercise exercise;

	// Stages are separately stored, see create() method
	private final List<Stage> stages = new ArrayList<>();

	/**
	 * Creates a new instance of the builder from an existing exercise.
	 *
	 * @param exercise
	 *            Existing exercise to be modified
	 */
	public ExerciseBuilder(Exercise exercise) {
		this.exercise = exercise;
	}

	/**
	 * Creates a new instance of the builder.
	 *
	 * @param name
	 *            Name of the exercise to build
	 */
	public ExerciseBuilder(String name) {
		exercise = new Exercise(name, "de");
	}

	/**
	 * Builds the exercise and add all stages in a linear navigation:
	 *
	 * <ol>
	 * <li>Sets the first stage to the start stage.</li>
	 * <li>Sets internal name and order indices.</li>
	 * <li>Adds linear stage transitions.</li>
	 * <li>Generates suffix weights.</li>
	 * </ol>
	 * 
	 * <p>
	 * <strong>Important:</strong> Don't forget to call {@link Exercise#generateSuffixWeights()} after modifying the
	 * exercise object, otherwise a submission may contain a wrong point result.
	 * </p>
	 * 
	 * @return Exercise object, ready to persist
	 */
	public Exercise create() {

		int index = 0;
		Stage previousStage = null;
		for (Stage stage : stages) {
			exercise.addStage(stage);

			// The start stage is the first stage
			if (index == 0) {
				exercise.setStartStage(stage);
			}

			// It must not be possible that taskDescription is null
			if (stage.getTaskDescription() == null) {
				stage.setTaskDescription("");
			}

			// Other stages are added one after the other, with a linear navigation between the stages
			stage.setOrderIndex(index);
			stage.setInternalName("#" + (index + 1));
			if (index != 0) {
				previousStage.setDefaultTransition(new StageTransition(stage));
			}

			previousStage = stage;
			index++;
		}

		exercise.generateSuffixWeights();

		return exercise;
	}

	/**
	 * Directly adds a stage to this exercise, in case the stage was not created by an
	 * {@linkplain AbstractStageBuilder}.
	 * 
	 * @param stage
	 *            {@linkplain Stage} that should be added.
	 */
	public ExerciseBuilder addStage(Stage stage) {
		stages.add(stage);
		return this;
	}

	/**
	 * Adds a new variable declaration.
	 *
	 * @param name
	 *            Identifier of the variable declaration, must be unique
	 * @param code
	 *            Initialization code, evaluated by the Evaluator
	 */
	public ExerciseBuilder withVariableDeclaration(String name, String code) {
		validateVariableName(name);

		VariableDeclaration varDeclaration = new VariableDeclaration(name);
		EvaluatorExpression evalExpr = new EvaluatorExpression();
		evalExpr.setCode(code);
		varDeclaration.setInitializationCode(evalExpr);
		exercise.addVariable(varDeclaration);
		return this;
	}

	/**
	 * Adds a new variable declaration.
	 *
	 * @param name
	 *            Identifier of the variable declaration, must be unique
	 * @param code
	 *            Initialization code, evaluated by the Evaluator
	 * @param domain
	 *            {@linkplain EDomain} for evaluating the initialization code
	 */
	public ExerciseBuilder withVariableDeclaration(String name, String code, EDomain domain) {
		validateVariableName(name);

		VariableDeclaration varDeclaration = new VariableDeclaration(name);
		EvaluatorExpression evalExpr = new EvaluatorExpression();
		evalExpr.setCode(code);
		evalExpr.setDomain(domain);
		varDeclaration.setInitializationCode(evalExpr);
		exercise.addVariable(varDeclaration);
		return this;
	}

	/**
	 * Adds a new random variable declaration (uses "randomIntegerBetween"-function).
	 *
	 * @param name
	 *            Identifier of the variable declaration, must be unique
	 * @param start
	 *            Start value (inclusive)
	 * @param end
	 *            End value (inclusive)
	 */
	public ExerciseBuilder withRandomVariableDeclaration(String name, int start, int end) {
		validateVariableName(name);
		withVariableDeclaration(name, String.format("randomIntegerBetween(%d, %d)", start, end));
		return this;
	}

	private void validateVariableName(String name) {
		if (exercise.getVariableDeclarations().stream().anyMatch(variable -> variable.getName().equals(name))) {
			throw new IllegalArgumentException(
					String.format("A variable with the name '%s' has already been defined.", name));
		}
	}

	/**
	 * Sets the internal exercise description (only visible from exercise edit view).
	 */
	public ExerciseBuilder withPublicDescription(String description) {
		exercise.setPublicDescription(description);
		return this;
	}

	public ExerciseBuilder withTag(String tag) {
		exercise.addTag(CDI.current().select(TagService.class).get().getOrCreateByName(tag));
		return this;
	}

	/**
	 * Sets difficulty of the exercise, value must be between 0 and 100.
	 */
	public ExerciseBuilder withDifficulty(int difficulty) {
		if (difficulty < 0 || difficulty > 100) {
			throw new IllegalArgumentException(
					String.format("%d is out of range [0;100] for difficulty level", difficulty));
		}

		exercise.setDifficulty(difficulty);
		return this;
	}

	/**
	 * Sets hint malus type.
	 */
	public ExerciseBuilder withHintMalusType(EStageHintMalus type) {
		exercise.setHintMalusType(type);
		return this;
	}

	/**
	 * Adds a new MC stage from a {@linkplain MCStageBuilder}.
	 */
	public MCStageBuilder withMCStage() {
		return new MCStageBuilder(this);
	}

	/**
	 * Adds a new Fill-in stage from a {@linkplain FillInStageBuilder}.
	 */
	public FillInStageBuilder withFillInStage() {
		return new FillInStageBuilder(this);
	}

	/**
	 * Adds a new MC stage with the options "Yes" (correct) and "No" and with sample feedback.
	 */
	public ExerciseBuilder withSampleMCStage() {
		Stage stage = new MCStageBuilder()
				.withTitle("Title")
				.withDescription("Select 'Yes'")
				.withAnswerOption("Yes", true)
				.withAnswerOption("No", false)
				.withCorrectFeedback("Correct")
				.withDefaultFeedback("Not correct", 0)
				.getStage();
		this.addStage(stage);
		return this;
	}

	/**
	 * Adds a new FillIn stage with a field where the user is asked to enter 42.
	 */
	public ExerciseBuilder withSampleFillInStage() {
		Stage stage = new FillInStageBuilder()
				.withTitle("Title")
				.withDescription()
				.append("Type 42: ")
				.appendFillInField(EFillInEditorType.NUMBER)
				.and()
				.withFeedbackRule("Rule",
						"isIntegerNumber([input=fillInField1]) && equalsExpr([input=fillInField], 42)",
						"Correct", 100, true)
				.withDefaultFeedback("Not correct", 0)
				.getStage();
		this.addStage(stage);
		return this;
	}

}
