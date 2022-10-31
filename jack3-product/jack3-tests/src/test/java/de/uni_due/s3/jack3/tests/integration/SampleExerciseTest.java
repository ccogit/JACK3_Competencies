package de.uni_due.s3.jack3.tests.integration;

import static de.uni_due.s3.jack3.business.DevelopmentBusiness.FILLIN_FIELD_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.DevelopmentBusiness;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.PlaceholderPatternProducer;
import de.uni_due.s3.jack3.entities.enums.EFormularEditorPalette;
import de.uni_due.s3.jack3.entities.enums.EMCRuleType;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.Rule;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCAnswer;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;
import de.uni_due.s3.jack3.tests.utils.AbstractBusinessTest;

class SampleExerciseTest extends AbstractBusinessTest {

	@Inject
	private DevelopmentBusiness developmentBusiness;

	private User author;

	@Override
	@BeforeEach
	protected void beforeTest() {
		super.beforeTest();
		author = getLecturer("SampleExerciseAuthor");
	}

	@Test
	void testSampleExercise1() {
		String varX = PlaceholderPatternProducer.forExerciseVariable("x");
		String varY = PlaceholderPatternProducer.forExerciseVariable("y");
		String input1 = PlaceholderPatternProducer.forInputVariable(FILLIN_FIELD_PREFIX + "1");

		Exercise exercise = (Exercise) developmentBusiness.createSampleExerciseJACK(author);

		// General settings
		assertEquals("JACK", exercise.getName());
		assertEquals(10, exercise.getDifficulty());

		// Variable declarations
		List<VariableDeclaration> vars = exercise.getVariableDeclarations();
		assertEquals(2, vars.size());
		assertEquals("x", vars.get(0).getName());
		assertEquals("randomIntegerBetween(1,10)", vars.get(0).getInitializationCode().getCode());
		assertEquals("y", vars.get(1).getName());
		assertEquals("randomIntegerBetween(10,20)", vars.get(1).getInitializationCode().getCode());

		// First stage (MC)
		MCStage mcStage = (MCStage) exercise.getStagesAsList().get(0);
		assertEquals("Was ist JACK?", mcStage.getTaskDescription());
		List<MCAnswer> answers = mcStage.getAnswerOptions();
		assertEquals(3, answers.size());
		assertTrue(answers.stream().anyMatch(option -> option.getText().equals("Ein E-Assessment-System")
				&& option.getRule() == EMCRuleType.CORRECT));
		assertTrue(answers.stream().anyMatch(
				option -> option.getText().equals("Eine Raumstation") && option.getRule() == EMCRuleType.WRONG));
		assertTrue(answers.stream()
				.anyMatch(option -> option.getText().equals("Ein Bier") && option.getRule() == EMCRuleType.WRONG));

		// Second stage (Fill-in)
		FillInStage fillInStage = (FillInStage) exercise.getStagesAsList().get(1);
		Logger.getLogger(getClass()).error(fillInStage.getTaskDescription());
		assertTrue(fillInStage.getTaskDescription().contains("Berechnen Sie"));
		List<Rule> rules = fillInStage.getFeedbackRulesAsList();
		assertEquals(3, rules.size());
		assertTrue(rules.stream()
				.anyMatch(rule -> rule.getPoints() == 100
						&& rule.getValidationExpression().getCode().equals(input1 + "=" + varX + "+" + varY)
						&& rule.getFeedbackText().equals("Das ist korrekt.")));
		assertTrue(rules.stream()
				.anyMatch(rule -> rule.getPoints() == 0
						&& rule.getValidationExpression().getCode().equals(input1 + "<" + varX + "+" + varY)
						&& rule.getFeedbackText().equals("Das ist zu wenig.")));
		assertTrue(rules.stream()
				.anyMatch(rule -> rule.getPoints() == 0
						&& rule.getValidationExpression().getCode().equals(input1 + ">" + varX + "+" + varY)
						&& rule.getFeedbackText().equals("Das ist zu viel.")));
		assertEquals(1, fillInStage.getHints().size());
		assertTrue(fillInStage.getHints().stream()
				.anyMatch(hint -> hint.getText().equals("Das Ergebnis ist zweistellig.")));
	}

	@Test
	void testSampleExercise2() {
		String varA = PlaceholderPatternProducer.forExerciseVariable("a");
		String input1 = PlaceholderPatternProducer.forInputVariable(FILLIN_FIELD_PREFIX + "1");

		Exercise exercise = (Exercise) developmentBusiness.createSampleExerciseEinfacheMengenlehre(author);

		// General settings
		assertEquals("Einfache Mengenlehre", exercise.getName());
		assertEquals(40, exercise.getDifficulty());

		// Variable declarations
		List<VariableDeclaration> vars = exercise.getVariableDeclarations();
		assertEquals(1, vars.size());
		assertEquals("a", vars.get(0).getName());
		assertEquals("randomIntegerBetween(1,4)", vars.get(0).getInitializationCode().getCode());

		// First stage (MC)
		MCStage stage1 = (MCStage) exercise.getStagesAsList().get(0);
		assertTrue(stage1.getTaskDescription().contains("A bezeichnet man als"));
		List<MCAnswer> answers = stage1.getAnswerOptions();
		assertEquals(3, answers.size());
		assertTrue(answers.stream()
				.anyMatch(option -> option.getText().equals("Leere Menge") && option.getRule() == EMCRuleType.CORRECT));
		assertTrue(answers.stream()
				.anyMatch(option -> option.getText().equals("Volle Menge") && option.getRule() == EMCRuleType.WRONG));
		assertTrue(answers.stream()
				.anyMatch(option -> option.getText().equals("Menge") && option.getRule() == EMCRuleType.CORRECT));
		assertTrue(stage1.getCorrectAnswerFeedback().contains("korrekt"));
		assertTrue(stage1.getDefaultFeedback().contains("nicht"));
		assertTrue(stage1.isRandomize());

		// Second stage (Fill-in)
		FillInStage stage2 = (FillInStage) exercise.getStagesAsList().get(1);
		assertEquals(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_NO_PALETTE, stage2.getFormularEditorPaletteEnum());
		assertTrue(stage2.getTaskDescription().contains("Mächtigkeit der Potenzmenge"));
		assertTrue(stage2.getTaskDescription().contains("Vereinfachen Sie"));
		List<Rule> rules = stage2.getFeedbackRulesAsList();
		assertEquals(3, rules.size());
		assertTrue(rules.stream()
				.anyMatch(rule -> rule.getFeedbackText().matches(".*nicht.*korrekt.*") && rule.getPoints() == 0
						&& rule.getValidationExpression().getCode().equals(input1 + "!=(2 ^ " + varA + ")")));
		assertTrue(rules.stream()
				.anyMatch(rule -> rule.getFeedbackText().contains("zwar korrekt, aber") && rule.getPoints() == 80
						&& rule.getValidationExpression().getCode()
								.equals(input1 + "==(2 ^ " + varA + ")&&!(isIntegerNumber(" + input1 + "))")));
		assertTrue(rules.stream()
				.anyMatch(rule -> rule.getFeedbackText().contains("korrekt") && rule.getPoints() == 100
						&& rule.getValidationExpression().getCode()
								.equals(input1 + "==(2 ^ " + varA + ")&&isIntegerNumber(" + input1 + ")")));
		assertEquals(1, stage2.getHints().size());
		assertTrue(stage2.getHints().stream()
				.anyMatch(hint -> hint.getText().contains("Bei einer Menge mit n Elementen hat die Potenzmenge")));

		// Third stage (Fill-in)
		FillInStage stage3 = (FillInStage) exercise.getStagesAsList().get(2);
		assertEquals(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_NO_PALETTE, stage3.getFormularEditorPaletteEnum());
		rules = stage3.getFeedbackRulesAsList();
		assertEquals(2, rules.size());
		assertTrue(rules.stream()
				.anyMatch(rule -> rule.getFeedbackText().contains("korrekt") && rule.getPoints() == 100
						&& rule.getValidationExpression().getCode()
								.equals("equalSet(" + input1 + ", list(list(),list(1),list(2),list(1,2)))")));
		assertTrue(rules.stream()
				.anyMatch(rule -> rule.getFeedbackText().matches(".*nicht.*korrekt.*") && rule.getPoints() == 0
						&& rule.getValidationExpression().getCode()
								.equals("!equalSet(" + input1 + ", list(list(),list(1),list(2),list(1,2)))")));
	}

	@Test
	void testSampleExercise3() {
		String varA1 = PlaceholderPatternProducer.forExerciseVariable("a1");
		String varB2 = PlaceholderPatternProducer.forExerciseVariable("b2");
		String input2 = PlaceholderPatternProducer.forInputVariable(FILLIN_FIELD_PREFIX + "2");
		String input9 = PlaceholderPatternProducer.forInputVariable(FILLIN_FIELD_PREFIX + "9");

		// Only a few random tests
		Exercise exercise = (Exercise) developmentBusiness.createSampleExerciseRechentabellen(author);
		assertEquals("Rechentabellen", exercise.getName());
		assertEquals(70, exercise.getDifficulty());

		assertTrue(exercise.getVariableDeclarations().stream()
				.anyMatch(varD -> varD.getInitializationCode().getCode().equals("randomIntegerBetween(1,10)")
						&& varD.getName().equals("a2")));

		FillInStage stage = (FillInStage) exercise.getStagesAsList().get(0);
		assertEquals("Multiplikation", stage.getExternalName());
		assertTrue(stage.getFeedbackRulesAsList().stream().anyMatch(rule -> rule.getValidationExpression().getCode()
				.equals("equalsExpr(" + input2 + "," + varB2 + "*" + varA1 + ")") && rule.getPoints() == 25));

		stage = (FillInStage) exercise.getStagesAsList().get(1);
		assertEquals("Addition", stage.getExternalName());
		assertTrue(stage.getFeedbackRulesAsList().stream().anyMatch(rule -> rule.getValidationExpression().getCode()
				.equals("!equalsExpr(" + input2 + "," + varB2 + "+" + varA1 + ")") && rule.getPoints() == 0));

		stage = (FillInStage) exercise.getStagesAsList().get(2);
		assertEquals("Addition in Restklassen", stage.getExternalName());
		assertTrue(stage.getFeedbackRulesAsList().stream()
				.anyMatch(rule -> rule.getValidationExpression().getCode().equals("!(" + input9 + "==1)")
						&& rule.getPoints() == 0));
	}

	@Test
	void testSampleExercise4() {
		// Only a few random tests
		Exercise exercise = (Exercise) developmentBusiness.createSampleExerciseStaedteUndLaender(author);
		assertEquals("Städte & Länder", exercise.getName());
		assertEquals(30, exercise.getDifficulty());

		Stage[] stages = exercise.getStagesAsList().toArray(new Stage[7]);
		assertEquals(7, stages.length);
		assertEquals(Arrays.asList("Nordrhein-Westfalen", "Bayern", "Bremen"), ((MCStage) stages[0]).getAnswerOptions()
				.stream().map(answer -> answer.getText()).collect(Collectors.toList()));
		assertTrue(exercise.getStages().stream().anyMatch(stage -> stage.getExternalName().equals("Landeshauptstadt")));
		assertTrue(exercise.getStages().stream().anyMatch(stage -> stage.getExternalName().equals("Bremen")));
		assertEquals(Arrays.asList("Bremerhaven", "Bremen", "Oldenburg", "Minden"), ((MCStage) stages[6])
				.getAnswerOptions().stream().map(answer -> answer.getText()).collect(Collectors.toList()));

		assertTrue(stages[0].leadsTo(stages[1]));
		assertTrue(stages[0].leadsTo(stages[3]));
		assertTrue(stages[0].leadsTo(stages[5]));

		assertTrue(stages[1].leadsTo(stages[2]));
		assertTrue(stages[3].leadsTo(stages[4]));
		assertTrue(stages[5].leadsTo(stages[6]));

		assertFalse(stages[0].isEndStage());
		assertFalse(stages[1].isEndStage());
		assertTrue(stages[2].isEndStage());
		assertFalse(stages[3].isEndStage());
		assertTrue(stages[4].isEndStage());
		assertFalse(stages[5].isEndStage());
		assertTrue(stages[6].isEndStage());
	}

}
