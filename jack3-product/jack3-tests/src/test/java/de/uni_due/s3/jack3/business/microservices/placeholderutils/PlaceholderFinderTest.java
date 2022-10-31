package de.uni_due.s3.jack3.business.microservices.placeholderutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class PlaceholderFinderTest {

	private void assertNoPlaceholder(String text) {
		assertPlaceholders(List.of(), text);
	}

	private void assertPlaceholder(Placeholder expected, String text) {
		assertPlaceholders(List.of(expected), text);
	}

	private void assertPlaceholders(List<Placeholder> expected, String text) {
		List<Placeholder> actual = PlaceholderFinder.findPlaceholderForText(text);
		assertEquals(expected.size(), expected.size());
		for (int i = 0; i < expected.size(); i++) {
			assertEquals(expected.get(i), actual.get(i));
		}
	}

	@Test
	public void testNoPlaceholders() {
		assertNoPlaceholder("");
		assertNoPlaceholder("Text with no Placeholder!");
		assertNoPlaceholder("[] var = name1");
	}

	@Test
	public void testMultiplePlaceholders() {
		assertPlaceholders(
				List.of(new Placeholder("[var=name1]", "var", "name1", false, null, null),
						new Placeholder("[var=name1,latex]", "var", "name1", true, null, null)),
				"Hello [var=name1], calculate  [var=name1,latex]!");
	}

	@Test
	public void testSimpleName() {
		assertPlaceholder(new Placeholder("[var=name1]", "var", "name1", false, null, null), "[var=name1]");
		assertPlaceholder(new Placeholder("[meta=aB]", "meta", "aB", false, null, null), "[meta=aB]");
		assertPlaceholder(new Placeholder("[input=bb]", "input", "bb", false, null, null), "[input=bb]");
		assertPlaceholder(new Placeholder("[check=Name123]", "check", "Name123", false, null, null), "[check=Name123]");
	}

	@Test
	public void testOldLatexSyntax() {
		assertPlaceholder(new Placeholder("[var=$name1]", "var", "name1", true, null, null), "[var=$name1]");
		assertPlaceholder(new Placeholder("[input=$in]", "input", "in", true, null, null), "[input=$in]");
		assertPlaceholder(new Placeholder("[meta=$in,latex]", "meta", "in", true, null, null), "[meta=$in,latex]");
	}

	@Test
	public void testLatexSyntax() {
		assertPlaceholder(new Placeholder("[var=name1,latex]", "var", "name1", true, null, null), "[var=name1,latex]");
		assertPlaceholder(new Placeholder("[input=in,latex]", "input", "in", true, null, null), "[input=in,latex]");
	}

	@Test
	public void testDecimalSyntax() {
		assertPlaceholder(new Placeholder("[var=name1,decimals=-9]", "var", "name1", false, "-9", null),
				"[var=name1,decimals=-9]");
		assertPlaceholder(new Placeholder("[input=in,decimals=3]", "input", "in", false, "3", null),
				"[input=in,decimals=3]");
		assertPlaceholder(new Placeholder("[input=in,decimals=0]", "input", "in", false, "0", null),
				"[input=in,decimals=0]");
	}

	@Test
	public void testSiPrefixesSyntax() {
		assertPlaceholder(new Placeholder("[var=name1,siprefix=base10]", "var", "name1", false, null, "base10"),
				"[var=name1,siprefix=base10]");
		assertPlaceholder(new Placeholder("[input=in,siprefix=symbol]", "input", "in", false, null, "symbol"),
				"[input=in,siprefix=symbol]");
	}

	@Test
	public void testAllMixingSyntax() {
		assertPlaceholder(
				new Placeholder("[var=name1,siprefix=base10,decimals=12,latex]", "var", "name1", true, "12", "base10"),
				"[var=name1,siprefix=base10,decimals=12,latex]");

		assertPlaceholder(new Placeholder("[var=name1,latex,siprefix=symbol,decimals=-12]", "var", "name1", true, "-12",
				"symbol"), "[var=name1,latex,siprefix=symbol,decimals=-12]");
	}

}
