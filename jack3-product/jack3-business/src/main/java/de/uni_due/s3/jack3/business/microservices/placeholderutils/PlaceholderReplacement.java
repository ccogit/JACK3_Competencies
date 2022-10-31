package de.uni_due.s3.jack3.business.microservices.placeholderutils;

public class PlaceholderReplacement {

	private final Placeholder placeholder;
	private final String replacement;

	public PlaceholderReplacement(Placeholder placeholder, String replacement) {
		this.placeholder = placeholder;
		this.replacement = replacement;
	}

	public Placeholder getPlaceholder() {
		return placeholder;
	}

	public String getReplacement() {
		return replacement;
	}

}
