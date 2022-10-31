package de.uni_due.s3.jack3.business.microservices.placeholderutils;

public class PlaceholderPatternProducer {

	public static String forExerciseVariable(String name) {
		return "[" + PlaceholderConstants.EXERCISE_IDENTIFIER + "=" + name + "]";
	}

	public static String forInputVariable(String name) {
		return "[" + PlaceholderConstants.INPUT_IDENTIFIER + "=" + name + "]";
	}

	public static String forMcInputVariable(int index) {
		return "[" + PlaceholderConstants.INPUT_IDENTIFIER + "=" + PlaceholderConstants.MC + index + "]";
	}

}
