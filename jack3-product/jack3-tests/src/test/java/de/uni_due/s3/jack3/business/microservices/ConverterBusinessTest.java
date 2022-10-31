package de.uni_due.s3.jack3.business.microservices;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.microservices.variableutils.VariableValueFactory;
import de.uni_due.s3.jack3.tests.annotations.NeedsEureka;
import de.uni_due.s3.jack3.tests.utils.AbstractTest;

@NeedsEureka
class ConverterBusinessTest extends AbstractTest {

	@Inject
	private ConverterBusiness converter;

	private static EvaluatorMaps maps = new EvaluatorMaps();

	@BeforeAll
	public static void beforeAll() throws Exception {
		maps.getExerciseVariableMap().put("isVar", VariableValueFactory.createVariableValueForOpenMathString("is"));
		maps.getInputVariableMap().put("example1",
				VariableValueFactory.createVariableValueForOpenChemString("example"));
	}

	private void assertEqualsReplacedText(String expected, String inputText) {
		String actual = converter.replaceVariablesByVariableName(inputText, maps);
		assertEquals(expected, actual);
	}

	@Test
	void replaceVariablesByVaribaleName_ForStringReplacement() throws Exception {
		assertEqualsReplacedText("Hello World! This is an example text for string replacement.",
				"Hello World! This [var=isVar] an [input=example1] text for string replacement.");
	}

	@Test
	void replaceVariablesByVaribaleName_ForLatexReplacement() throws Exception {
		assertEqualsReplacedText("Hello World! This \\text{is} an \\text{example} text for latex replacement.",
				"Hello World! This [var=isVar,latex] an [input=example1,latex] text for latex replacement.");
	}

	@Test
	void replaceVariablesByVaribaleName_ForMixedStringLatexReplacement() throws Exception {
		assertEqualsReplacedText(
				"Hello World! This is an example text for string replacement."
						+ " This \\text{is} an \\text{example} text for latex replacement.",
				"Hello World! This [var=isVar] an [input=example1] text for string replacement."
						+ " This [var=$isVar] an [input=example1,latex] text for latex replacement.");
	}

	@Test
	void replaceVariablesByVaribaleName_ForLatexWithDollarEnvsReplacement() throws Exception {
		assertEqualsReplacedText(
				"Hello World! This is an example text for string replacement."
						+ " This $\\text{is}$ an $\\text{example}$ text for latex replacement.",
				"Hello World! This [var=isVar] an [input=example1] text for string replacement."
						+ " This $[var=$isVar]$ an $[input=example1,latex]$ text for latex replacement.");
	}
}
