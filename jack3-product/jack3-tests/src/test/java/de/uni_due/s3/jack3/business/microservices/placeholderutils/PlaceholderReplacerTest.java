package de.uni_due.s3.jack3.business.microservices.placeholderutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PlaceholderReplacerTest {

	public static List<PlaceholderReplacement> replacements = new ArrayList<>();

	@BeforeAll
	public static void beforeAll() {
		replacements.add(
				new PlaceholderReplacement(new Placeholder("[var=name]", "var", "name", false, null, null), "World"));
		replacements.add(new PlaceholderReplacement(
				new Placeholder("[var=name,latex]", "var", "name", true, null, null), "\\text{World}"));

		replacements.add(new PlaceholderReplacement(
				new Placeholder("[input=field1]", "input", "field1", false, null, null), "1.123456"));
		replacements.add(new PlaceholderReplacement(
				new Placeholder("[input=field1,latex]", "input", "field1", true, null, null), "{1.123456}"));
		replacements.add(new PlaceholderReplacement(
				new Placeholder("[input=field1,decimals=2]", "input", "field1", false, "2", null), "1.12"));
		replacements.add(new PlaceholderReplacement(
				new Placeholder("[input=field1,latex,decimals=2]", "input", "field1", true, "2", null), "{1.12}"));

	}

	private static void assertReplacedText(String expected, String toReplace) {
		String actual = PlaceholderReplacer.replaceTextBy(toReplace, replacements);
		assertEquals(expected, actual);
	}

	@Test
	public void test() {
		assertReplacedText("Hello World!", "Hello [var=name]!");
		assertReplacedText("Hello $\\text{World}$!", "Hello $[var=name,latex]$!");
		assertReplacedText("Hello $\\text{World}$!", "Hello $[var=$name]$!");

		assertReplacedText("Your result is 1.12", "Your result is [input=field1,decimals=2]");
		assertReplacedText("Your result is 1.123456", "Your result is [input=field1]");

		assertReplacedText("Your result is ${1.12}$", "Your result is $[input=field1,decimals=2,latex]$");
		assertReplacedText("Your result is ${1.123456}$", "Your result is $[input=field1,latex]$");
	}

}
