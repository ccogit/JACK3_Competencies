package de.uni_due.s3.jack3.business.microservices.placeholderutils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class PlaceholderFinder extends PlaceholderMatcher {

	private final List<Placeholder> placeholders = new ArrayList<>();

	private PlaceholderFinder(String text) {
		super(text);
	}

	public static List<Placeholder> findPlaceholderForText(String text) {
		return new PlaceholderFinder(text).work();
	}

	private List<Placeholder> work() {
		doMatcherWork();
		return placeholders;
	}

	@Override
	protected void doForMatchingPlaceholder(Matcher matcher, Placeholder p) {
		placeholders.add(p);
	}

}
