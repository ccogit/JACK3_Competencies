package de.uni_due.s3.jack3.tests.business.samples;

import static de.uni_due.s3.jack3.builders.FillInStageBuilder.DROPDOWN_FIELD_PREFIX;
import static de.uni_due.s3.jack3.builders.FillInStageBuilder.FILLIN_FIELD_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.builders.ExerciseBuilder;
import de.uni_due.s3.jack3.builders.FillInStageBuilder;
import de.uni_due.s3.jack3.entities.enums.EFillInEditorType;
import de.uni_due.s3.jack3.entities.enums.EFormularEditorPalette;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;

class FillInStageBuilderTest extends AbstractStageBuilderTest<FillInStage, FillInStageBuilder> {

	@Override
	protected FillInStageBuilder getNewBuilder() {
		return new ExerciseBuilder("testName").withFillInStage();
	}

	@Override
	protected FillInStageBuilder addAnotherBuilder(ExerciseBuilder exerciseBuilder) {
		return exerciseBuilder.withFillInStage();
	}

	@Test
	void FormularEditorPaletteTest() {
		exercise = stageBuilder.withFormularEditorPalette(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_BASIC)//
				.and()//
				.create();//

		persistUserFolderAndExercise();

		assertEquals(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_BASIC,
				((FillInStage) exercise.getStartStage()).getFormularEditorPaletteEnum());
	}

	@Test
	void feedbackRuleTest() {
		exercise = stageBuilder.withFeedbackRule("myFirstRule", "42==42", "myFirstFeedback", 80, false)//
				.withFeedbackRule("Another Rule", "42==43", "You Failed", 5, false) //
				.and()//
				.create();//

		persistUserFolderAndExercise();

		assertEquals(2, ((FillInStage) exercise.getStartStage()).getFeedbackRulesAsList().size());
		assertEquals("myFirstRule", ((FillInStage) exercise.getStartStage()).getFeedbackRulesAsList().get(0).getName());
		assertEquals("42==42", ((FillInStage) exercise.getStartStage()).getFeedbackRulesAsList().get(0)
				.getValidationExpression().getCode());
		assertEquals("myFirstFeedback",
				((FillInStage) exercise.getStartStage()).getFeedbackRulesAsList().get(0).getFeedbackText());
		assertEquals(80, ((FillInStage) exercise.getStartStage()).getFeedbackRulesAsList().get(0).getPoints());

		assertEquals("Another Rule",
				((FillInStage) exercise.getStartStage()).getFeedbackRulesAsList().get(1).getName());
		assertEquals("42==43", ((FillInStage) exercise.getStartStage()).getFeedbackRulesAsList().get(1)
				.getValidationExpression().getCode());
		assertEquals("You Failed",
				((FillInStage) exercise.getStartStage()).getFeedbackRulesAsList().get(1).getFeedbackText());
		assertEquals(5, ((FillInStage) exercise.getStartStage()).getFeedbackRulesAsList().get(1).getPoints());
	}

	@Test
	void descriptionBuilderTest() {
		exercise = stageBuilder.withDescription().append("Crazy Text")//
				.appendLine()//
				.appendDropDownField("A", "B", "C")//
				.appendLine("line")//
				.appendFillInField(EFillInEditorType.NUMBER, 20)//
				.and()//
				.and()//
				.create();//

		persistUserFolderAndExercise();

		assertEquals(3, ((FillInStage) exercise.getStartStage()).getDropDownFields().stream().findFirst().get()
				.getItems().size());
		assertEquals(20,
				((FillInStage) exercise.getStartStage()).getFillInFields().stream().findFirst().get().getSize());
		assertEquals(EFillInEditorType.NUMBER, ((FillInStage) exercise.getStartStage()).getFillInFields().stream()
				.findFirst().get().getFormularEditorEnumType());

		StringBuilder sb = new StringBuilder();
		sb.append("Crazy Text");
		sb.append("<br />");
		sb.append("<select name=\"" + DROPDOWN_FIELD_PREFIX + "1\" size=\"10\" type=\"text\" value=\""
				+ DROPDOWN_FIELD_PREFIX + "1\" /><option value=\"0\">Drop-Down</option></select>");
		sb.append("line<br />");
		sb.append("<input name=\"" + FILLIN_FIELD_PREFIX + "1\" size=\"20\" type=\"text\" value=\""
				+ FILLIN_FIELD_PREFIX + "1\" />");

		assertEquals(sb.toString(), ((FillInStage) exercise.getStartStage()).getTaskDescription());
	}

}
