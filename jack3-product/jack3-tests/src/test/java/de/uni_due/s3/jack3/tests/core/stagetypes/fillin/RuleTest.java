package de.uni_due.s3.jack3.tests.core.stagetypes.fillin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.stagetypes.fillin.Rule;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

class RuleTest extends AbstractContentTest {

	@Test
	void testConstructor() {
		Rule rule = new Rule("name", 0);

		assertEquals("name", rule.getName());
		assertEquals(0, rule.getOrderIndex());
	}

	@Test
	void testName() {
		Rule rule = new Rule();
		assertNull(rule.getName());

		rule.setName("name of the rule");
		assertEquals("name of the rule", rule.getName());
	}

	@Test
	void testOrderIndex() {
		Rule rule = new Rule();
		assertEquals(0, rule.getOrderIndex());
		rule.setOrderIndex(3);
		assertEquals(3, rule.getOrderIndex());

	}

	@Test
	void testEvaluatorExpression() {

		Rule rule = new Rule();
		assertTrue(rule.getValidationExpression().isEmpty());
		EvaluatorExpression expression = new EvaluatorExpression("42==42");
		rule.setValidationExpression(expression);
		assertEquals(expression, rule.getValidationExpression());
	}

	@Test
	void testFeedbackText() {
		Rule rule = new Rule();
		assertNull(rule.getFeedbackText());
		String feedback = "Leider war deine Antwort nicht richtig";
		rule.setFeedbackText(feedback);
		assertEquals(feedback, rule.getFeedbackText());
	}

	@Test
	void testPoints() {
		Rule rule = new Rule();
		assertEquals(0, rule.getPoints());
		rule.setPoints(20);
		assertEquals(20, rule.getPoints());
	}

	@Test
	void testIllegalPoints() {
		Rule rule = new Rule();
		assertThrows(IllegalArgumentException.class, () -> {
			rule.setPoints(-101);
		});
	}

	@Test
	void compareRules() {
		Rule rule1 = new Rule();
		Rule rule2 = new Rule();

		rule1.setOrderIndex(3);
		rule2.setOrderIndex(2);

		assertTrue(rule1.compareTo(rule2) > 0);
		assertTrue(rule2.compareTo(rule1) < 0);

		rule2.setOrderIndex(3);

		assertEquals(0, rule1.compareTo(rule2));
		assertEquals(0, rule2.compareTo(rule1));
	}

	/**
	 * 
	 * This test checks that a name of fill in rule can not be null.
	 */
	@Test
	void setRuleNameToNull() {
		Rule rule = new Rule();

		assertThrows(NullPointerException.class, () -> {
			rule.setName(null);
		});

	}

	/**
	 * This test checks that a name of fill in rule can not be empty.
	 */
	@Test
	void setEmptyRuleName() {
		Rule rule = new Rule();

		assertThrows(IllegalArgumentException.class, () -> {
			rule.setName("");
		});
	}

	/**
	 * This test checks the deepCopy of a basic rule for the
	 * fill in stage with following fields set:
	 *
	 * - name
	 * - orderIndex
	 * - default: points (=0)
	 */
	@Test
	void deepCopyOfBasicRule() {
		Rule originRule;
		Rule deepCopyOfRule;

		originRule = new Rule("deep copy test of base rule", 3);

		deepCopyOfRule = originRule.deepCopy();

		assertNotEquals(originRule, deepCopyOfRule, "The rule is the origin itself.");
		assertEquals("deep copy test of base rule", deepCopyOfRule.getName(), "The name of the rule are different");
		assertEquals(3, deepCopyOfRule.getOrderIndex(), "The order index of the rule are different");
		assertEquals(0, deepCopyOfRule.getPoints(), "The points of the rule are different");
	}

	/**
	 * This test checks the deepCopy of a complete rule for the fill
	 * in stage with following fields set:
	 *
	 * - name
	 * - orderIndex
	 * - points
	 * - feedbackText
	 * - validationExpression
	 */
	@Test
	void deepCopyOfFullRule() {
		Rule originRule;
		Rule deepCopyOfRule;

		originRule = new Rule("deep copy test of a complete rule", 3);
		originRule.setPoints(42);
		originRule.setFeedbackText("feedback of complete rule");
		originRule.setValidationExpression(new EvaluatorExpression("126/3"));

		deepCopyOfRule = originRule.deepCopy();

		assertNotEquals(originRule, deepCopyOfRule, "The rule is the origin itself.");
		assertEquals("deep copy test of a complete rule", deepCopyOfRule.getName(),
				"The name of the rule are different");
		assertEquals(3, deepCopyOfRule.getOrderIndex(), "The order index of the rule are different");
		assertEquals(42, deepCopyOfRule.getPoints(), "The points of the rule are different");
		assertEquals("feedback of complete rule", deepCopyOfRule.getFeedbackText(),
				"The feedback of the rule are different");
		assertEquals("126/3", deepCopyOfRule.getValidationExpression().getCode(),
				"The validation expression of the rule are different");
	}

}
