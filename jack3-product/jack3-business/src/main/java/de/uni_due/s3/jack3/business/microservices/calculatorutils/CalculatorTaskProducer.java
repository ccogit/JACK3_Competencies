package de.uni_due.s3.jack3.business.microservices.calculatorutils;

import static java.util.function.Predicate.not;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.uni_due.s3.evaluator_api.calculator.request.CalculatorTask;
import de.uni_due.s3.evaluator_api.calculator.request.CalculatorTaskType;
import de.uni_due.s3.evaluator_api.properties.EvaluatorProperties;
import de.uni_due.s3.evaluator_api.properties.EvaluatorVariableType;
import de.uni_due.s3.jack3.business.microservices.evaluatorutils.EvaluatorPropertiesProducer;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;
import de.uni_due.s3.jack3.entities.tenant.VariableUpdate;

public class CalculatorTaskProducer {

	private static final String LINE_BREAK = "\n";
	private static final String COMMENT_START = "//";

	public static CalculatorTask byVariableDeclaration(VariableDeclaration declaration) {
		return fromEvaluatorEvaluateExpression(declaration.getName(), declaration.getInitializationCode());
	}

	public static CalculatorTask byVariableUpdate(VariableUpdate update) {
		return fromEvaluatorEvaluateExpression(update.getVariableReference().getName(), update.getUpdateCode());
	}

	public static CalculatorTask singleBooleanTaskByEvaluatorExpression(String name, EvaluatorExpression expression) {
		return fromEvaluatorBooleanExpression(name, expression);
	}

	public static CalculatorTask singleEvaluationTaskByEvaluatorExpression(String name,
			EvaluatorExpression expression) {
		return fromEvaluatorEvaluateExpression(name, expression);
	}

	private static CalculatorTask fromEvaluatorBooleanExpression(String name, EvaluatorExpression expression) {
		EvaluatorProperties properties = EvaluatorPropertiesProducer.forEvaluatorExpression(name,
				EvaluatorVariableType.NONE, expression);
		return new CalculatorTask(deleteCommentLines(expression.getCode()), CalculatorTaskType.BOOLEAN, properties);
	}

	private static CalculatorTask fromEvaluatorEvaluateExpression(String name, EvaluatorExpression expression) {
		EvaluatorProperties properties = EvaluatorPropertiesProducer.forEvaluatorExpression(name,
				EvaluatorVariableType.VAR, expression);
		return new CalculatorTask(deleteCommentLines(expression.getCode()), CalculatorTaskType.EVALUATE, properties);
	}

	private static String deleteCommentLines(String expression) {
		return splitLines(expression).filter(not(CalculatorTaskProducer::isCommentLine))
				.collect(Collectors.joining(LINE_BREAK));
	}

	private static Stream<String> splitLines(String expression) {
		return Arrays.stream(expression.split(LINE_BREAK));
	}

	private static boolean isCommentLine(String line) {
		return line.stripLeading().startsWith(COMMENT_START);
	}

}
