package de.uni_due.s3.jack3.business.microservices.placeholderutils;

import java.util.List;
import java.util.regex.Matcher;

public class PlaceholderReplacer extends PlaceholderMatcher {

	private final StringBuffer sb = new StringBuffer();
	private final List<PlaceholderReplacement> replacements;

	private PlaceholderReplacer(String text, List<PlaceholderReplacement> replacements) {
		super(text);
		this.replacements = replacements;
	}

	public static String replaceTextBy(String text, List<PlaceholderReplacement> replacements) {
		return new PlaceholderReplacer(text, replacements).work();
	}

	private String work() {
		doMatcherWork();
		return sb.toString();
	}

	@Override
	protected void doForMatchingPlaceholder(Matcher matcher, Placeholder placeholder) {
		matcher.appendReplacement(sb, "");
		sb.append(findReplacementForPlaceholder(placeholder));
	}

	private String findReplacementForPlaceholder(Placeholder placeholder) {
		return replacements.stream().filter((r) -> r.getPlaceholder().equals(placeholder)).findFirst()
				.map(PlaceholderReplacement::getReplacement).orElse(placeholder.getWholeRegex());
	}

	@Override
	protected void doAfterMatching(Matcher matcher) {
		matcher.appendTail(sb);
	}

}
