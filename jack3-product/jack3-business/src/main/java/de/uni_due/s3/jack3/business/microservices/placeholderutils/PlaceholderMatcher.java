package de.uni_due.s3.jack3.business.microservices.placeholderutils;

import static de.uni_due.s3.jack3.business.microservices.placeholderutils.PlaceholderConstants.CHECK_IDENTIFIER;
import static de.uni_due.s3.jack3.business.microservices.placeholderutils.PlaceholderConstants.EXERCISE_IDENTIFIER;
import static de.uni_due.s3.jack3.business.microservices.placeholderutils.PlaceholderConstants.INPUT_IDENTIFIER;
import static de.uni_due.s3.jack3.business.microservices.placeholderutils.PlaceholderConstants.META_IDENTIFIER;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class PlaceholderMatcher {

	private static final String varTypeIdentifier = "type";
	private static final String varTypeGroup = "(?<" + varTypeIdentifier + ">" + EXERCISE_IDENTIFIER + "|"
			+ INPUT_IDENTIFIER + "|" + META_IDENTIFIER + "|" + CHECK_IDENTIFIER + ")";

	private static final String varNameIdentifier = "name";
	private static final String varNameGroup = "(?<" + varNameIdentifier + ">[a-zA-Z_][a-zA-ZäÄöÖüÜß0-9_]*)";
	private static final String oldLatexFlagIdentifier = "oldLatex"; // workaround for old latex syntax
	private static final String olfLatexFlagGroup = "(?<" + oldLatexFlagIdentifier + ">\\$)?"; // workaround for old latex syntax

	private static final String varNamingRegex = varTypeGroup + "=" + olfLatexFlagGroup + varNameGroup;

	private static final String latexFlagIdentifier = "latex";
	private static final String latexFlagGroup = "(?<" + latexFlagIdentifier + ">latex)";

	private static final String decimalsFlagIdentifier = "decimals";
	private static final String decimalsFlagGroup = "(?:decimals=(?<" + decimalsFlagIdentifier + ">\\-?[0-9]+))";

	private static final String siprefixFlagIdentifier = "siprefix";
	private static final String siprefixFlagGroup = "(?:siprefix=(?<" + siprefixFlagIdentifier + ">base10|symbol))";

	private static final String propertiesRegex = "(?:,(?:" + latexFlagGroup + "|" + decimalsFlagGroup + "|"
			+ siprefixFlagGroup + "))*";

	private static final String placeholderRegex = "\\[" + varNamingRegex + propertiesRegex + "\\]";

	private static final Pattern finderPattern = Pattern.compile(placeholderRegex);

	private final Matcher matcher;

	protected PlaceholderMatcher(String text) {
		this.matcher = finderPattern.matcher(text);
	}

	protected void doMatcherWork() {
		doBeforeMatching(matcher);
		doMatcherFind();
		doAfterMatching(matcher);
	}

	private void doMatcherFind() {
		while (matcher.find()) {
			doForPlaceholderMatch();
		}
	}

	private void doForPlaceholderMatch() {
		Placeholder p = generatePlaceholder();
		doForMatchingPlaceholder(matcher, p);
	}

	private Placeholder generatePlaceholder() {
		String whole = matcher.group();
		String varType = matcher.group(varTypeIdentifier);
		String varName = matcher.group(varNameIdentifier);
		boolean latexFlag = matcher.group(latexFlagIdentifier) != null || matcher.group(oldLatexFlagIdentifier) != null; // workaround for old latex syntax
		String decimals = matcher.group(decimalsFlagIdentifier);
		String siprefixes = matcher.group(siprefixFlagIdentifier);
		return new Placeholder(whole, varType, varName, latexFlag, decimals, siprefixes);
	}

	protected void doBeforeMatching(Matcher matcher) {
	}

	protected abstract void doForMatchingPlaceholder(Matcher matcher, Placeholder p);

	protected void doAfterMatching(Matcher matcher) {
	}

}
